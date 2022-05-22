package debug.io.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 *
 * @since 2022/5/21
 * @author dingrui
 */
public class ClientConnectTest {

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < 51; i++) {
            Socket socket = new Socket();
            // 轮询向服务请求连接 当达到服务端设置的backlog阈值后 阻塞住
            socket.connect(new InetSocketAddress(ServerTest.PORT));
            System.out.println("申请向服务端的连接");
        }
    }
}
