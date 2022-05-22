package debug.io.socket;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @since 2022/5/20
 * @author dingrui
 */
public class ClientTest {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(ServerTest.PORT));
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        String msg = "hello, this is from: " + Thread.currentThread().getId();
        writer.println(msg);
        writer.flush();

        System.in.read();

        writer.close();
        socket.close();
    }
}
