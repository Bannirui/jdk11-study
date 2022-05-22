package debug.io.model.reactor.singlereactormultiplethread;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

/**
 * <p>单reactor多线程</p>
 * <p>相较于{@link debug.io.model.reactor.singlereactorsinglethread.Reactor}版本 仅仅是在处理读写请求的时候加了个线程池</p>
 * @since 2022/5/22
 * @author dingrui
 */
public class Reactor implements Runnable {

    ServerSocketChannel serverSocketChannel;
    Selector selector;

    public Reactor(int port) {
        try {
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.bind(new InetSocketAddress(port));
            this.serverSocketChannel.configureBlocking(false);

            this.selector = Selector.open();
            this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT, new Acceptor(this.serverSocketChannel, this.selector));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.selector.select();
                Iterator<SelectionKey> it = this.selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey sk = it.next();
                    it.remove();
                    this.dispatch(sk);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void dispatch(SelectionKey sk) {
        Runnable r = (Runnable) sk.attachment();
        r.run();
    }
}
