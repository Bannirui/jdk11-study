package debug.io.model.reactor;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 *
 * @since 2022/5/22
 * @author dingrui
 */
public class ClientTest {

    public static final int PORT = 9991;

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("localhost", PORT));
        // 读
        new Thread(() -> {
            while (true) {
                try {
                    InputStream in = socket.getInputStream();
                    byte[] bytes = new byte[1024];
                    in.read(bytes);
                    System.out.println("->客户端 收到服务端的消息=" + new String(bytes, StandardCharsets.UTF_8));
                } catch (Exception ignored) {
                }
            }
        }).start();

        // 写
        while (true) {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String s = scanner.nextLine();
                socket.getOutputStream().write(s.getBytes());
            }
        }
    }
}
