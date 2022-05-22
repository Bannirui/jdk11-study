package debug.io.model.reactor.singlereactorsinglethread;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * <p>用于处理{@link Reactor}中的连接请求</p>
 * @since 2022/5/22
 * @author dingrui
 */
public class Acceptor implements Runnable {

    ServerSocketChannel serverSocketChannel;
    Selector selector;

    public Acceptor(ServerSocketChannel serverSocketChannel, Selector selector) {
        this.serverSocketChannel = serverSocketChannel;
        this.selector = selector;
    }

    /**
     * <p>处理连接请求 进来的全部都是连接请求</p>
     */
    @Override
    public void run() {
        try {
            SocketChannel socketChannel = this.serverSocketChannel.accept();
            System.out.println("->服务端 来自" + socketChannel.getRemoteAddress()+"的连接请求");
            // 读写非阻塞 注册到复用器
            socketChannel.configureBlocking(false);
            socketChannel.register(this.selector, SelectionKey.OP_READ, new Handler(socketChannel));
        } catch (Exception ignored) {

        }
    }
}
