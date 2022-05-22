package debug.io.model.reactor.singlereactorsinglethread;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * <p>接收{@link Reactor}的读写请求</p>
 * @since 2022/5/22
 * @author dingrui
 */
public class Handler implements Runnable {

    SocketChannel socketChannel;

    public Handler(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    /**
     * <p>处理读写请求</p>
     */
    @Override
    public void run() {
        ByteBuffer b = ByteBuffer.allocate(1024);
        try {
            // 客户端信息
            SocketAddress remoteAddress = this.socketChannel.getRemoteAddress();
            this.socketChannel.read(b);
            // 收到了读请求的内容
            String msg = new String(b.array(), StandardCharsets.UTF_8);
            // TODO: 2022/5/22 业务处理
            System.out.println("->服务端 来自" + remoteAddress + "的一条消息=" + msg);
            // 回写数据给客户端
            String echo = "->客户端 服务端已经收到了你的信息=" + msg;
            this.socketChannel.write(ByteBuffer.wrap(echo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
        }
    }
}
