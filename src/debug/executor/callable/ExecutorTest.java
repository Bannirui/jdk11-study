package debug.executor.callable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author dingrui
 * @since 2022/2/10
 * @description
 */
public class ExecutorTest {

    public static void main(String[] args) {
        ExecutorService executor = new ThreadPoolExecutor(
                3,
                4,
                2_000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(5_00)
        );
        List<Future<String>> ret = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Future<String> f = executor.submit(new MyTask(i));
            ret.add(f);
        }
        for (Future<String> f : ret) {
            try {
                while (!f.isDone()) ;
                System.out.println("获取任务执行结果=" + f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } finally {
                executor.shutdown();
            }
        }
    }
}
