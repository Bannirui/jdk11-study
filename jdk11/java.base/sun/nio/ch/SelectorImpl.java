/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


/**
 * Base Selector implementation class.
 */

abstract class SelectorImpl
    extends AbstractSelector
{
    // The set of keys registered with this Selector
    private final Set<SelectionKey> keys; // 缓存了SelectionKey(相当于对channel和复用器的封装)

    // The set of keys with data ready for an operation
    private final Set<SelectionKey> selectedKeys; // 事件就绪的kevent对应的selectionKey <-> publicSelectedKeys

    // Public views of the key sets
    private final Set<SelectionKey> publicKeys;             // Immutable
    private final Set<SelectionKey> publicSelectedKeys;     // Removal allowed, but not addition // Selector#selectedKeys()返回的就是这个集合 <->selectedKeys

    // used to check for reentrancy
    private boolean inSelect; // 标识位 该属性的赋值/修改发生在同步代码块 用来判断同个线程是否重入

    protected SelectorImpl(SelectorProvider sp) { // 属性初始化工作
        super(sp);
        this.keys = ConcurrentHashMap.newKeySet();
        this.selectedKeys = new HashSet<>();
        this.publicKeys = Collections.unmodifiableSet(keys);
        this.publicSelectedKeys = Util.ungrowableSet(this.selectedKeys); // 这个就是select()的结果 又自己实现了数据结构 不支持写 相当于读写分离的设计 本质上引用指针指向的是selectedKeys
    }

    private void ensureOpen() {
        if (!this.isOpen())
            throw new ClosedSelectorException();
    }

    @Override
    public final Set<SelectionKey> keys() {
        ensureOpen();
        return publicKeys;
    }

    @Override
    public final Set<SelectionKey> selectedKeys() {
        this.ensureOpen();
        return this.publicSelectedKeys; // 返回了一个集合作为结果 那么只要关注这个集合的生成和添加过程就行
    }

    /**
     * Marks the beginning of a select operation that might block
     */
    protected final void begin(boolean blocking) {
        if (blocking) super.begin();
    }

    /**
     * Marks the end of a select operation that may have blocked
     */
    protected final void end(boolean blocking) {
        if (blocking) end();
    }

    /**
     * Selects the keys for channels that are ready for I/O operations.
     *
     * @param action  the action to perform, can be null
     * @param timeout timeout in milliseconds to wait, 0 to not wait, -1 to
     *                wait indefinitely
     */
    protected abstract int doSelect(Consumer<SelectionKey> action, long timeout)
        throws IOException; // 实现留给子类关注 macos的实现类在KQueueSelectorImpl

    private int lockAndDoSelect(Consumer<SelectionKey> action, long timeout)
        throws IOException
    { // 从Selector#select()过来 action=null timeout=-1
        synchronized (this) { // 管程锁
            this.ensureOpen(); // 通过AbstractSelector#selectorOpen标识判断
            if (inSelect) // 线程重入判断
                throw new IllegalStateException("select in progress");
            inSelect = true; // 在同步代码块中修改值 释放锁后修改回去 以此为线程重入的依据
            try {
                synchronized (this.publicSelectedKeys) {
                    return this.doSelect(action, timeout);
                }
            } finally {
                inSelect = false;
            }
        }
    }

    @Override
    public final int select(long timeout) throws IOException {
        if (timeout < 0)
            throw new IllegalArgumentException("Negative timeout");
        return lockAndDoSelect(null, (timeout == 0) ? -1 : timeout);
    }

    @Override
    public final int select() throws IOException {
        return this.lockAndDoSelect(null, -1);
    }

    @Override
    public final int selectNow() throws IOException {
        return lockAndDoSelect(null, 0);
    }

    @Override
    public final int select(Consumer<SelectionKey> action, long timeout)
        throws IOException
    {
        Objects.requireNonNull(action);
        if (timeout < 0)
            throw new IllegalArgumentException("Negative timeout");
        return lockAndDoSelect(action, (timeout == 0) ? -1 : timeout);
    }

    @Override
    public final int select(Consumer<SelectionKey> action) throws IOException {
        Objects.requireNonNull(action);
        return lockAndDoSelect(action, -1);
    }

    @Override
    public final int selectNow(Consumer<SelectionKey> action) throws IOException {
        Objects.requireNonNull(action);
        return lockAndDoSelect(action, 0);
    }

    /**
     * Invoked by implCloseSelector to close the selector.
     */
    protected abstract void implClose() throws IOException;

    @Override
    public final void implCloseSelector() throws IOException {
        wakeup();
        synchronized (this) {
            implClose();
            synchronized (publicSelectedKeys) {
                // Deregister channels
                Iterator<SelectionKey> i = keys.iterator();
                while (i.hasNext()) {
                    SelectionKeyImpl ski = (SelectionKeyImpl)i.next();
                    deregister(ski);
                    SelectableChannel selch = ski.channel();
                    if (!selch.isOpen() && !selch.isRegistered())
                        ((SelChImpl)selch).kill();
                    selectedKeys.remove(ski);
                    i.remove();
                }
                assert selectedKeys.isEmpty() && keys.isEmpty();
            }
        }
    }

    @Override
    protected final SelectionKey register(AbstractSelectableChannel ch,
                                          int ops,
                                          Object attachment)
    { // ServerSocketChannel#register(selector, SelectionKey.OP_ACCEPT)跟进来的 att=null
        if (!(ch instanceof SelChImpl))
            throw new IllegalSelectorException();
        SelectionKeyImpl k = new SelectionKeyImpl((SelChImpl)ch, this); // SelectionKey数据结构封装channel和复用器
        k.attach(attachment);

        // register (if needed) before adding to key set
        this.implRegister(k); // channel注册复用器前置检查channel

        // add to the selector's key set, removing it immediately if the selector
        // is closed. The key is not in the channel's key set at this point but
        // it may be observed by a thread iterating over the selector's key set.
        this.keys.add(k); // 缓存起来
        try {
            k.interestOps(ops); // 告诉复用器对channel的哪个事件感兴趣
        } catch (ClosedSelectorException e) {
            assert ch.keyFor(this) == null;
            keys.remove(k);
            k.cancel();
            throw e;
        }
        return k;
    }

    /**
     * Register the key in the selector.
     *
     * The default implementation checks if the selector is open. It should
     * be overridden by selector implementations as needed.
     */
    protected void implRegister(SelectionKeyImpl ski) {
        ensureOpen();
    }

    /**
     * Removes the key from the selector
     */
    protected abstract void implDereg(SelectionKeyImpl ski) throws IOException;

    /**
     * Invoked by selection operations to process the cancelled-key set
     */
    protected final void processDeregisterQueue() throws IOException {
        assert Thread.holdsLock(this);
        assert Thread.holdsLock(publicSelectedKeys);

        Set<SelectionKey> cks = cancelledKeys();
        synchronized (cks) {
            if (!cks.isEmpty()) {
                Iterator<SelectionKey> i = cks.iterator();
                while (i.hasNext()) {
                    SelectionKeyImpl ski = (SelectionKeyImpl)i.next();
                    i.remove();

                    // remove the key from the selector
                    implDereg(ski);

                    selectedKeys.remove(ski);
                    keys.remove(ski);

                    // remove from channel's key set
                    deregister(ski);

                    SelectableChannel ch = ski.channel();
                    if (!ch.isOpen() && !ch.isRegistered())
                        ((SelChImpl)ch).kill();
                }
            }
        }
    }

    /**
     * Invoked by selection operations to handle ready events. If an action
     * is specified then it is invoked to handle the key, otherwise the key
     * is added to the selected-key set (or updated when it is already in the
     * set).
     */
    protected final int processReadyEvents(int rOps,
                                           SelectionKeyImpl ski,
                                           Consumer<SelectionKey> action) { // 返回值要么是0 要么是1 rOps是os从kqueue的kevent中拿来的
        if (action != null) {
            ski.translateAndSetReadyOps(rOps);
            if ((ski.nioReadyOps() & ski.nioInterestOps()) != 0) {
                action.accept(ski);
                ensureOpen();
                return 1;
            }
        } else { // action=null
            assert Thread.holdsLock(publicSelectedKeys);
            if (this.selectedKeys.contains(ski)) { // 事件就绪的kevent对应的fd(映射成SelectionKey)已经被缓存过了
                if (ski.translateAndUpdateReadyOps(rOps)) {
                    return 1;
                }
            } else {
                ski.translateAndSetReadyOps(rOps); // 可能会去修改ski#readyOps
                if ((ski.nioReadyOps() & ski.nioInterestOps()) != 0) { // 与操作 readOps是根据rOps来的 interestOps是channel.register()的时候传进来的肯定不是0 也就是说决定socket时候返回给客户端的因素在于这个rOps
                    this.selectedKeys.add(ski); // 处于就绪事件的selectionKey
                    return 1;
                }
            }
        }
        return 0;
    }

    /**
     * Invoked by interestOps to ensure the interest ops are updated at the
     * next selection operation.
     */
    protected abstract void setEventOps(SelectionKeyImpl ski);
}
