package debug.io.model.reactor.multiplereactormultiplethread;

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

    SocketChannel s;

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);

    public Handler(SocketChannel s) {
        this.s = s;
    }

    @Override
    public void run() {
        try {
            // 读
            ByteBuffer b = ByteBuffer.allocate(1024);
            this.s.read(b);
            // 封装任务提交线程池
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
                String msg = new String(this.b.array(), StandardCharsets.UTF_8);
                System.out.println("handler-> 收到来自" + this.s.getRemoteAddress() + "的消息=" + msg);
                // 写
                this.s.write(ByteBuffer.wrap("handler-> 来自handler的回写".getBytes(StandardCharsets.UTF_8)));
            } catch (Exception ignored) {
            }
        }
    }
}
