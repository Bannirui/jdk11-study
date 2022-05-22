/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.nio.ch;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static sun.nio.ch.KQueue.EVFILT_READ;
import static sun.nio.ch.KQueue.EVFILT_WRITE;
import static sun.nio.ch.KQueue.EV_ADD;
import static sun.nio.ch.KQueue.EV_DELETE;

/**
 * KQueue based Selector implementation for macOS
 */

class KQueueSelectorImpl extends SelectorImpl {

    // maximum number of events to poll in one call to kqueue
    private static final int MAX_KEVENTS = 256;

    // kqueue file descriptor
    private final int kqfd; // kqueue()这个os sc创建kqueue返回的fd

    // address of poll array (event list) when polling for pending events
    private final long pollArrayAddress; // 这个值猜测应该是内存区域的起始地址 kqueue中的事件概念 A kevent is identified by an (ident, filter, and optional udata value) tuple

    // file descriptors used for interrupt
    private final int fd0;
    private final int fd1;

    // maps file descriptor to selection key, synchronize on selector
    private final Map<Integer, SelectionKeyImpl> fdToKey = new HashMap<>();

    // pending new registrations/updates, queued by setEventOps
    private final Object updateLock = new Object();
    private final Deque<SelectionKeyImpl> updateKeys = new ArrayDeque<>(); // 复用器感兴趣的事件 ServerSocketChannel#register(selector, SelectionKey.OP_ACCEPT)传进来的

    // interrupt triggering and clearing
    private final Object interruptLock = new Object();
    private boolean interruptTriggered;

    // used by updateSelectedKeys to handle cases where the same file
    // descriptor is polled by more than one filter
    private int pollCount;

    KQueueSelectorImpl(SelectorProvider sp) throws IOException { // sp是KQueueSelectorProvider的实例
        super(sp);

        this.kqfd = KQueue.create(); // 应该是对应的一次系统调用 The kqueue() system call allocates a kqueue file descriptor.  This file descriptor provides a generic method of notifying the user when a kernel event (kevent) happens or a condition holds, based on the results of small pieces of ker-nel code termed filters
        this.pollArrayAddress = KQueue.allocatePollArray(MAX_KEVENTS); // os实现kqueue定义了kevent数据结构 申请指定数量的kevent内存

        try {
            long fds = IOUtil.makePipe(false);
            this.fd0 = (int) (fds >>> 32); // 高32位 读
            this.fd1 = (int) fds; // 低32位 写
        } catch (IOException ioe) {
            KQueue.freePollArray(pollArrayAddress);
            FileDispatcherImpl.closeIntFD(kqfd);
            throw ioe;
        }

        // register one end of the socket pair for wakeups
        KQueue.register(kqfd, fd0, EVFILT_READ, EV_ADD); // os sc 这个地方猜测应该是调用的这个int kevent(int kq, const struct kevent *changelist, int nchanges, struct kevent *eventlist, int nevents, const struct timespec *timeout);
    }

    private void ensureOpen() {
        if (!isOpen())
            throw new ClosedSelectorException();
    }

    @Override
    protected int doSelect(Consumer<SelectionKey> action, long timeout)
        throws IOException
    { // 从Selector#select()跟进来 action=null timeout=-1
        assert Thread.holdsLock(this); // 这个方法是实现了SelectorImpl#deoSelect()预留的模板钩子 入口在SelectorImpl#lockAndDoSelect() 进入之前外面包了synchronized 这个地方再判断一下当前执行线程是否持有了管程锁

        long to = Math.min(timeout, Integer.MAX_VALUE);  // max kqueue timeout // to表示在有超时机制下还有多行时间可用 timeout=-1
        boolean blocking = (to != 0); // true
        boolean timedPoll = (to > 0); // false

        int numEntries;
        this.processUpdateQueue(); // 重要
        this.processDeregisterQueue();
        try {
            this.begin(blocking);

            do {
                long startTime = timedPoll ? System.nanoTime() : 0; // 0
                numEntries = KQueue.poll(kqfd, pollArrayAddress, MAX_KEVENTS, to); // 本地方法 应该是类似于epoll的一次os sc 阻塞等待系统调用的返回
                if (numEntries == IOStatus.INTERRUPTED && timedPoll) { // 系统调用中断(有可能是超时导致的) 如果客户端设置了超时限制 判定果真超时就将返回值修改为对应的结果
                    // timed poll interrupted so need to adjust timeout
                    long adjust = System.nanoTime() - startTime;
                    to -= TimeUnit.MILLISECONDS.convert(adjust, TimeUnit.NANOSECONDS);
                    if (to <= 0) {
                        // timeout expired so no retry
                        numEntries = 0;
                    }
                }
            } while (numEntries == IOStatus.INTERRUPTED); // os sc返回被中断状态 异常情况下单独判定是否是超时导致的
            assert IOStatus.check(numEntries);

        } finally {
            end(blocking);
        } // 截止目前 从内核拿到了numEntries
        this.processDeregisterQueue();
        return this.processEvents(numEntries, action); // action=null
    }

    /**
     * Process changes to the interest ops.
     */
    private void processUpdateQueue() {
        assert Thread.holdsLock(this);

        synchronized (updateLock) {
            SelectionKeyImpl ski;
            while ((ski = this.updateKeys.pollFirst()) != null) { // channel.register(selector, SelectionKey.OP_ACCEPT)的时候触发过这个队列里面有值了
                if (ski.isValid()) {
                    int fd = ski.getFDVal();
                    // add to fdToKey if needed
                    SelectionKeyImpl previous = fdToKey.putIfAbsent(fd, ski); // fd对应的SelectionKey 只有这一个地方会添加fd的映射
                    assert (previous == null) || (previous == ski);

                    int newEvents = ski.translateInterestOps();
                    int registeredEvents = ski.registeredEvents();
                    if (newEvents != registeredEvents) {

                        // add or delete interest in read events
                        if ((registeredEvents & Net.POLLIN) != 0) {
                            if ((newEvents & Net.POLLIN) == 0) {
                                KQueue.register(kqfd, fd, EVFILT_READ, EV_DELETE);
                            }
                        } else if ((newEvents & Net.POLLIN) != 0) {
                            KQueue.register(kqfd, fd, EVFILT_READ, EV_ADD);
                        }

                        // add or delete interest in write events
                        if ((registeredEvents & Net.POLLOUT) != 0) {
                            if ((newEvents & Net.POLLOUT) == 0) {
                                KQueue.register(kqfd, fd, EVFILT_WRITE, EV_DELETE);
                            }
                        } else if ((newEvents & Net.POLLOUT) != 0) {
                            KQueue.register(kqfd, fd, EVFILT_WRITE, EV_ADD);
                        }

                        ski.registeredEvents(newEvents);
                    }
                }
            }
        }
    }

    /**
     * Process the polled events.
     * If the interrupt fd has been selected, drain it and clear the interrupt.
     */
    private int processEvents(int numEntries, Consumer<SelectionKey> action)
        throws IOException
    {
        assert Thread.holdsLock(this);

        int numKeysUpdated = 0;
        boolean interrupted = false;

        // A file descriptor may be registered with kqueue with more than one
        // filter and so there may be more than one event for a fd. The poll
        // count is incremented here and compared against the SelectionKey's
        // "lastPolled" field. This ensures that the ready ops is updated rather
        // than replaced when a file descriptor is polled by both the read and
        // write filter.
        this.pollCount++;

        for (int i = 0; i < numEntries; i++) {
            long kevent = KQueue.getEvent(this.pollArrayAddress, i); // 从连续的内存空间上找到对应的kevent
            int fd = KQueue.getDescriptor(kevent); // kevent对应的fd
            if (fd == fd0) {
                interrupted = true;
            } else {
                SelectionKeyImpl ski = this.fdToKey.get(fd); // fd对应的SelectionKey 这个SelectionKey会在注册的时候触发缓存映射起来
                if (ski != null) {
                    int rOps = 0;
                    short filter = KQueue.getFilter(kevent);
                    if (filter == EVFILT_READ) {
                        rOps |= Net.POLLIN;
                    } else if (filter == EVFILT_WRITE) {
                        rOps |= Net.POLLOUT;
                    }
                    int updated = super.processReadyEvents(rOps, ski, action); // rOps=应该是kqueue关注的kevent事件 action=null 过滤出os内核给出的kevent发生过事件变更的
                    if (updated > 0 && ski.lastPolled != pollCount) {
                        numKeysUpdated++;
                        ski.lastPolled = pollCount;
                    }
                }
            }
        }

        if (interrupted) {
            clearInterrupt();
        }
        return numKeysUpdated;
    }

    @Override
    protected void implClose() throws IOException {
        assert !isOpen();
        assert Thread.holdsLock(this);

        // prevent further wakeup
        synchronized (interruptLock) {
            interruptTriggered = true;
        }

        FileDispatcherImpl.closeIntFD(kqfd);
        KQueue.freePollArray(pollArrayAddress);

        FileDispatcherImpl.closeIntFD(fd0);
        FileDispatcherImpl.closeIntFD(fd1);
    }

    @Override
    protected void implDereg(SelectionKeyImpl ski) throws IOException {
        assert !ski.isValid();
        assert Thread.holdsLock(this);

        int fd = ski.getFDVal();
        int registeredEvents = ski.registeredEvents();
        if (fdToKey.remove(fd) != null) {
            if (registeredEvents != 0) {
                if ((registeredEvents & Net.POLLIN) != 0)
                    KQueue.register(kqfd, fd, EVFILT_READ, EV_DELETE);
                if ((registeredEvents & Net.POLLOUT) != 0)
                    KQueue.register(kqfd, fd, EVFILT_WRITE, EV_DELETE);
                ski.registeredEvents(0);
            }
        } else {
            assert registeredEvents == 0;
        }
    }

    @Override
    public void setEventOps(SelectionKeyImpl ski) {
        ensureOpen();
        synchronized (updateLock) {
            this.updateKeys.addLast(ski); // channel.register(selector, SelectionKey.OP_ACCEPT)触发的 重要 后面找fd跟ski映射之前要用到
        }
    }

    @Override
    public Selector wakeup() {
        synchronized (interruptLock) {
            if (!interruptTriggered) {
                try {
                    IOUtil.write1(fd1, (byte)0);
                } catch (IOException ioe) {
                    throw new InternalError(ioe);
                }
                interruptTriggered = true;
            }
        }
        return this;
    }

    private void clearInterrupt() throws IOException {
        synchronized (interruptLock) {
            IOUtil.drain(fd0);
            interruptTriggered = false;
        }
    }
}
