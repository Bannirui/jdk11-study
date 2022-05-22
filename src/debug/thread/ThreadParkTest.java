package debug.thread;

import debug.varhandle.VarHandleTest;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

/**
 * <p><h4>LockSupport操作线程的挂起和唤醒</h4></p>
 *
 * <p>park
 * <p>谁执行{@link LockSupport#park()}方法 就将谁挂起 所谓的挂起本质就是利用{@link sun.misc.Unsafe}执行park方法
 * 如果park(...)方法调用的时候传参了 参数作为线程阻塞的原因记录
 *
 * <p>unpark</p>
 * <p>{@link LockSupport#unpark(Thread)}执行的时候传入要唤醒的线程实例
 *
 * <p>被park的线程被唤醒的情况</p>
 * <ul>
 *     <li>通过其他线程调用unpark</li>
 *     <li>其他线程设置了被阻塞线程的中断状态 {@link Thread#interrupt()}</li>
 *     <li>其他...</li>
 * </ul>
 *
 * @since 2022/2/18
 * @author dingrui
 */
public class ThreadParkTest {

    public void test() {
        // park t1
        Thread t1 = new Thread(() -> {
            int i = 0;
            for (; ; ) {
                System.out.println("t1 running");
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 挂起
                if (i == 0) LockSupport.park(this);
                i++;
            }
        });

        // unpark t1
        Thread t2 = new Thread(() -> {
            System.out.println("t2 running");
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LockSupport.unpark(t1);
        });

        // interrupt t1
        Thread t3 = new Thread(() -> {
            System.out.println("t3 running");
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            t1.interrupt();
        });

        t1.start();
        //t2.start();
        t3.start();

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ThreadParkTest().test();
    }
}
