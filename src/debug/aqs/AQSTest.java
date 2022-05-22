package debug.aqs;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author dingrui
 * @since 2022/2/17
 * @description
 */
public class AQSTest {

    // 公平锁
    private static ReentrantLock fairLock = new ReentrantLock(true);
    // 非公平锁
    private static ReentrantLock nonfairLock = new ReentrantLock(false);

    public static void main(String[] args) {

    }

    public void testFairLock() {
        // 上锁
        fairLock.lock();
        try {
            // TODO: 2022/2/17
        } finally {
            // 去锁
            fairLock.unlock();
        }
    }

    public void testNonfairLock() {
        // 上锁
        nonfairLock.lock();
        try {
            // TODO: 2022/2/17
        } finally {
            // 去锁
            nonfairLock.unlock();
        }
    }
}
