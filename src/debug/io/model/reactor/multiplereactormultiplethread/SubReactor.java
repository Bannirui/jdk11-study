package debug.io.model.reactor.multiplereactormultiplethread;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 *
 * @since 2022/5/22
 * @author dingrui
 */
public class SubReactor implements Runnable {

    // sub编号
    int id;
    Selector sl;

    public SubReactor(int id, Selector sl) {
        this.id = id;
        this.sl = sl;
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
