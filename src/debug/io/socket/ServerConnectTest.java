package debug.io.socket;

import java.io.IOException;
import java.net.ServerSocket;

/**
 *
 * @since 2022/5/21
 * @author dingrui
 */
public class ServerConnectTest {

    public static final int PORT = 9993;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT, 3);
        System.out.println("new了一个服务端");
        System.in.read();
    }
}
