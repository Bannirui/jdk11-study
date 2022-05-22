package debug.io.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @since 2022/5/20
 * @author dingrui
 */
public class ServerTest {

    public static final int PORT = 9993;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT, 3);
        System.out.println("new了一个服务端");

        System.in.read();

        while (true) {
            // 接收客户端连接 阻塞
            Socket socket = server.accept();
            new Thread(() -> {
                try {
                    InputStream in = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    while (true) {
                        // 读取 阻塞
                        System.out.println(reader.readLine());
                    }
                } catch (Exception ignored) {
                }
            }).start();
        }
    }
}
