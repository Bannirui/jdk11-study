package debug.aqs;

import java.io.FileFilter;
import java.util.Objects;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 *
 * @since 2022/2/17
 * @author dingrui
 */
public class MyLock {

    public static void main(String[] args) {
        long tid = Thread.currentThread().getId();
        MyLock myLock = new MyLock();
        myLock.lock();
        try {
            System.out.println("[" + tid + "]上锁");
            System.out.println("[" + tid + "] thread running...");
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
            }
        } finally {
            System.out.println("[" + tid + "]去锁");
            myLock.unLock();
        }
    }

    private final MySync mySync = new MySync();

    private static final int step = 1;

    public void lock() {
        this.mySync.acquire(step);
    }

    public void unLock() {
        this.mySync.release(step);
    }

    private static class MySync extends AbstractQueuedSynchronizer {
        /**
         * 尝试获取资源
         * @param arg
         * @return
         */
        @Override
        protected boolean tryAcquire(int arg) {
            int c = super.getState();
            Thread curThread = Thread.currentThread();
            if (c < 0) throw new IllegalArgumentException();
            if (c == 0) {
                // 多线程cas
                if (!super.compareAndSetState(0, arg)) return false;
                else {
                    // 抢锁成功
                    super.setExclusiveOwnerThread(curThread);
                    return true;
                }
            } else {
                // 锁已经被抢占 不支持重入
                if (Objects.equals(curThread, super.getExclusiveOwnerThread()))
                    throw new UnsupportedOperationException();
                else return false;
            }
        }

        /**
         * 尝试释放资源
         * @param arg
         * @return
         */
        @Override
        protected boolean tryRelease(int arg) {
            // 执行线程
            Thread curThread = Thread.currentThread();
            if (!Objects.equals(curThread, super.getExclusiveOwnerThread())) throw new UnsupportedOperationException();
            int c = super.getState();
            if (c == 0 || c - arg != 0) throw new IllegalArgumentException();
            if (!super.compareAndSetState(c, 0)) return false;
            super.setExclusiveOwnerThread(null);
            return true;
        }
    }
}
