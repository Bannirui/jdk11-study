package debug.io.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * <p>多线程</p>
 * <p>为每个连接都创建一个线程 每个线程中的<tt>read</tt>操作是阻塞点<ul>
 *     <li>假使读操作这样的一个阻塞近乎于不阻塞，也就是一个线程创建后，拿到cpu执行时间片后可以立马执行，执行完后进行线程销毁</li>
 *     <li>假使读操作近乎于无限阻塞，就是一个线程创建后，一直被阻塞</li>
 * </ul>
 * 上面是两个极限情况，实际情况即使没那么糟糕也明显存在的问题就是<ul>
 *     <li>线程创建和销毁都是比较重的os开销</li>
 *     <li>线程创建过多占用内存资源很大</li>
 *     <li>线程之间上下文切换占用os资源</li>
 * </ul>
 * </p>
 * @since 2022/5/21
 * @author dingrui
 */
public class BIOModelByMultipleThread {

    private static final int PORT = 9991;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        while (true) {
            Socket socket = server.accept();
            new Thread(() -> {
                try {
                    InputStream in = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    while (true) {
                        String msg = reader.readLine();
                        // TODO: 2022/5/21 业务逻辑
                    }
                } catch (Exception ignored) {
                }
            }).start();
        }
    }
}
