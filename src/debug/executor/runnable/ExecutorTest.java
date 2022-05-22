package debug.executor.runnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author dingrui
 * @since 2022/2/10
 * @description
 */
public class ExecutorTest {

    private static class MyTask implements Runnable {

        private Integer id;

        public MyTask(Integer id) {
            this.id = id;
        }

        @Override
        public void run() {
            final String msg = ("线程=" + Thread.currentThread().getId() + ", 执行任务=" + this.id).intern();
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(msg);
        }
    }

    public static void main(String[] args) {
        ExecutorService executor = new ThreadPoolExecutor(
                2,
                4,
                2_000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1_00)
        );
        for (int i = 0; i < 10; i++) {
            executor.execute(new MyTask(i));
        }
        executor.shutdown();
    }
}
