package debug.io.model.reactor.multiplereactormultiplethread;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

/**
 *
 * @since 2022/5/22
 * @author dingrui
 */
public class MainReactor implements Runnable {

    ServerSocketChannel ss;
    Selector sl;

    public MainReactor(int port) {
        try {
            this.ss = ServerSocketChannel.open();
            this.sl = Selector.open();
            this.ss.configureBlocking(false);
            this.ss.bind(new InetSocketAddress(port));
            this.ss.register(this.sl, SelectionKey.OP_ACCEPT, new Acceptor(this.ss));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.sl.select();
                Iterator<SelectionKey> it = this.sl.selectedKeys().iterator();
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
