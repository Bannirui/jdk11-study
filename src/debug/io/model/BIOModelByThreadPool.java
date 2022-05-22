package debug.io.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * <p>线程池</p>
 * <p>线程池的方案解决了多线程版本的几个问题<ul>
 *     <li>线程的创建和销毁开销</li>
 *     <li>线程复用</li>
 * </ul>
 * 但是因为每个线程里面的任务天然阻塞，这一点没法规避就会带来问题<ul>
 *     <li>任务阻塞 导致创建大量的任务或者线程</li>
 *     <li>线程上下文切换</li>
 * </ul>
 * </p>
 *
 * @since 2022/5/21
 * @author dingrui
 */
public class BIOModelByThreadPool {

    private static final int PORT = 9991;

    private static final ExecutorService myTP = new ThreadPoolExecutor(2, 5, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(Integer.MAX_VALUE));

    /**
     * 任务对象
     */
    private static class MyTask implements Runnable {

        private Socket socket;

        public MyTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            InputStream in = null;
            try {
                in = this.socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                while (true) {
                    String msg = reader.readLine();
                    // TODO: 2022/5/21 业务逻辑
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        while (true) {
            Socket socket = server.accept();
            // 封装成任务丢进线程池
            myTP.submit(new MyTask(socket));
        }
    }
}
