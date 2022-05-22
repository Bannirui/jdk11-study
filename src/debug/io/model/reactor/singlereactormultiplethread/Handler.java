package debug.io.model.reactor.singlereactormultiplethread;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @since 2022/5/22
 * @author dingrui
 */
public class Handler implements Runnable {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);

    SocketChannel s;

    public Handler(SocketChannel s) {
        this.s = s;
    }

    @Override
    public void run() {
        try {
            ByteBuffer b = ByteBuffer.allocate(1024);
            this.s.read(b);
            EXECUTOR.execute(new Process(this.s, b));
        } catch (Exception ignored) {
        }
    }

    private static class Process implements Runnable {

        SocketChannel s;
        ByteBuffer b;

        public Process(SocketChannel s, ByteBuffer b) {
            this.s = s;
            this.b = b;
        }

        @Override
        public void run() {
            try {
                SocketAddress remoteAddress = this.s.getRemoteAddress();
                String msg = new String(b.array(), StandardCharsets.UTF_8);
                System.out.println("->服务端 收到来自客户端的消息=" + msg);
                String echo = "->客户端 服务端处理业务的线程=" + Thread.currentThread().getName();
                this.s.write(ByteBuffer.wrap(echo.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception ignored) {
            }
        }
    }
}
