package debug.io.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * <p>一个线程</p>
 * <p>功能实现就是获取请求的连接，获取该连接发送的消息，开展业务逻辑</p>
 * <p>设计到的阻塞操作是<tt>accept</tt>和<tt>read</tt></p>
 * <p>在整个轮询中，假使获取到了一个<tt>socket</tt>之后，后续的操作整个被阻塞住，并且与此同时有源源不断的连接进来，但是因为main线程一直阻塞，导致请求无法处理</p>
 * @since 2022/5/21
 * @author dingrui
 */
public class BIOModelByMainThread {

    private static final int PORT = 9991;

    public static void main(String[] args) throws IOException {
        // 在os底层做了bind和listen
        ServerSocket server = new ServerSocket(PORT);
        while (true) {
            /**
             * 第1个阻塞点 拿到连接的socket
             */
            Socket socket = server.accept();
            InputStream in = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while (true) {
                /**
                 * 第2个阻塞点
                 */
                String msg = reader.readLine();
                // TODO: 2022/5/21 业务逻辑
            }
        }
    }
}
