package debug.io.model.reactor.multiplereactormultiplethread;

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

    private int idx;

    private final int SUB_REACTOR_CNT = 5;
    private SubReactor[] subReactors = new SubReactor[SUB_REACTOR_CNT];
    private Thread[] threads = new Thread[SUB_REACTOR_CNT];
    private final Selector[] selectors = new Selector[SUB_REACTOR_CNT];

    public Acceptor(ServerSocketChannel ss) {
        this.ss = ss;

        for (int i = 0; i < SUB_REACTOR_CNT; i++) {
            try {
                this.selectors[i] = Selector.open();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.subReactors[i] = new SubReactor(i, this.selectors[i]);
            this.threads[i] = new Thread(this.subReactors[i]);
            this.threads[i].start();
        }
    }

    /**
     * mainReactor将连接请求都分发到acceptor中
     */
    @Override
    public void run() {
        try {
            SocketChannel s = this.ss.accept();
            s.configureBlocking(false);
            this.selectors[this.idx].wakeup();
            // 让subReactor中携带的复用器关注读请求
            s.register(this.selectors[this.idx], SelectionKey.OP_READ, new Handler(s));
            if (++this.idx == SUB_REACTOR_CNT) this.idx = 0;
        } catch (Exception ignored) {
        }
    }
}
