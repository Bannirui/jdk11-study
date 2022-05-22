package debug.aqs;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @since 2022/2/21
 * @author dingrui
 */
public class ReentrantReadWriteLockTest {

    private Object data;

    private volatile boolean cacheValid;

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    public void processCachedData() {
        rwl.readLock().lock(); // 获取读锁
        if (!this.cacheValid) {
            rwl.readLock().unlock(); // 释放读锁
            rwl.writeLock().lock(); // 获取写锁
            try {
                if (!this.cacheValid) {
                    // TODO: 2022/2/21 操作缓存数据
                    System.out.println("把数据缓存上");
                    this.cacheValid = true;
                }
                rwl.readLock().lock(); // 获取读锁 持有写锁的情况下允许获取读锁(锁降级)
            } finally {
                rwl.writeLock().unlock(); // 释放写锁
            }
        }

        try{
            this.use(this.data);
        }finally {
            rwl.readLock().unlock(); // 释放读锁
        }
    }

    private void use(Object data){
        // TODO: 2022/2/21 读数据
        System.out.println("读取数据");
    }

    public static void main(String[] args) {
        new ReentrantReadWriteLockTest().processCachedData();
    }
}
