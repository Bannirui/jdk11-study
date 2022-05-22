package debug.executor.callable;

import java.util.concurrent.Callable;

/**
 * @author dingrui
 * @since 2022/2/10
 * @description
 */
public class MyTask implements Callable<String> {

    private final Integer idx;

    public MyTask(Integer idx) {
        this.idx = idx;
    }

    @Override
    public String call() throws Exception {
        System.out.println("线程=" + Thread.currentThread().getId() + ", 正在执行任务, idx=" + this.idx);
        Thread.sleep(5_000);
        return ("idx=" + this.idx).intern();
    }
}
