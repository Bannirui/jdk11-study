package debug.thread;

import java.util.concurrent.locks.LockSupport;

/**
 * <p>{@link Thread#interrupt()}仅仅是设置了{@link Thread}实例的中断状态
 * 至于收到某个线程收到了中断状态后的执行逻辑需要用户自定义
 * <p>如果某个线程处于阻塞状态中 被中断 会抛出{@link InterruptedException}异常 但是并不影响后续的执行</p>
 * <p>阻塞状态
 * <ul>
 *     <li>被{@link LockSupport#park()}挂起</li>
 *     <li>{@link Object#wait()}</li>
 *     <li>{@link Thread#join()}</li>
 *     <li>{@link Thread#sleep(long)}</li>
 * </ul>
 *
 * @since 2022/2/18
 * @author dingrui
 */
public class ThreadInterruptTest {

    public static void main(String[] args) throws InterruptedException {

        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (; ; ) {
                try {
                    if (Thread.currentThread().isInterrupted())
                        System.out.println(System.currentTimeMillis() + " 线程被中断...");
                    else System.out.println(System.currentTimeMillis() + " 线程正常执行...");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t1.start();

        Thread.sleep(3_000);
        // 设置中断状态
        t1.interrupt();
        // 检查中断状态
        boolean status = t1.isInterrupted();
        Thread.sleep(3_000);
        System.out.println();
    }
}
