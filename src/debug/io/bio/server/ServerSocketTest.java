package debug.io.bio.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

/**
 * <p>BIO的服务端</p>
 * @since 2022/5/20
 * @author dingrui
 */
public class ServerSocketTest {

    private static final int PORT = 9992;

    public static void main(String[] args) throws IOException {
        // 创建服务端
        ServerSocket server = new ServerSocket();
        // 绑定端口
        server.bind(new InetSocketAddress(PORT));
        // 监听端口
        Socket socket = server.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println("服务端 -> 连接ok");
        writer.flush();

        String msg;
        while (!"bye".equalsIgnoreCase(msg = reader.readLine()) && Objects.nonNull(msg)) {
            System.out.println("客户端 <- " + msg);
            writer.println("服务端 -> " + msg);
            writer.flush();
            msg = null;
        }
        reader.close();
        writer.close();
        socket.close();
        server.close();
    }
}
