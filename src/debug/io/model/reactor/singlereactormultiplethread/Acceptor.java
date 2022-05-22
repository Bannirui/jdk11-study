package debug.io.model.reactor.singlereactormultiplethread;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 *
 * @since 2022/5/22
 * @author dingrui
 */
public class Acceptor implements Runnable {

    ServerSocketChannel ss;
    Selector sl;

    public Acceptor(ServerSocketChannel ss, Selector sl) {
        this.ss = ss;
        this.sl = sl;
    }

    @Override
    public void run() {
        try {
            SocketChannel s = this.ss.accept();
            s.configureBlocking(false);
            s.register(this.sl, SelectionKey.OP_READ, new Handler(s));
        } catch (Exception ignored) {
        }
    }
}
