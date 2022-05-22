package debug.io.bio.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * <p>BIO客户端</p>
 * @since 2022/5/20
 * @author dingrui
 */
public class SocketTest {

    private static final String IP = "127.0.0.1";
    private static final int PORT = 9992;

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(IP, PORT));
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        Scanner scanner = new Scanner(System.in);
        String readLine = "";
        // 线程负责读
        Thread t = new Thread(() -> {
            int size = -1;
            byte[] bytes = new byte[1024];
            StringBuilder sb = new StringBuilder(1024);
            try {
                while ((size = socket.getInputStream().read(bytes, 0, bytes.length)) > 0) {
                    String msg = new String(bytes, 0, size, "UTF-8");
                    sb.append(msg);
                    if (msg.lastIndexOf("\n") > 0) {
                        System.out.println(sb.toString());
                        sb.delete(0, sb.length());
                    }
                    if (Thread.currentThread().isInterrupted()) break;
                }
            } catch (Exception ignored) {
            }
        });
        t.start();

        // main线程写
        while (!readLine.equalsIgnoreCase("bye")) {
            readLine = scanner.nextLine();
            writer.println(readLine);
            writer.flush();
        }

        scanner.close();
        writer.close();
        socket.close();
        t.interrupt();
    }
}
