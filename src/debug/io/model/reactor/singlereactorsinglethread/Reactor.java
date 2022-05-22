package debug.io.model.reactor.singlereactorsinglethread;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>单reactor单线程</p>
 * <p>弊端<ul>
 *     <li>服务端只能同时处理一个客户端的请求</li>
 * </ul></p>
 * @since 2022/5/22
 * @author dingrui
 */
public class Reactor implements Runnable {

    // 服务端socket
    ServerSocketChannel serverSocketChannel;
    // 复用器
    Selector selector;

    public Reactor(int port) {
        try {
            // 创建 非阻塞 绑定 监听
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.bind(new InetSocketAddress(port));
            // 注册复用器
            this.selector = Selector.open();
            // att用来处理连接请求
            this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT, new Acceptor(this.serverSocketChannel, this.selector));
        } catch (Exception ignored) {
        }
    }

    /**
     * <p>一个reactor收到请求后 不管是连接请求还是读写请求 一股脑处理<ul>
     *     <li>如果请求是连接请求 处理的会很快 交给acceptor处理器</li>
     *     <li>如果请求是读写请求 可能会包含着比较重的业务逻辑 分发给一个个的handler单独处理</li>
     * </ul></p>
     */
    @Override
    public void run() {
        while (true) {
            try {
                // os kqueue 收到的都是连接请求
                this.selector.select();
                Set<SelectionKey> selectionKeys = this.selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                while (it.hasNext()) {
                    SelectionKey sk = it.next();
                    it.remove();
                    // 请求交给dispatch进行派发 此时进来的可能是连接请求 也有可能是读请求
                    this.dispatch(sk);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void dispatch(SelectionKey sk) {
        // 不管是连接请求还是读请求 他们两个处理器的顶层都是Runnable Acceptor收到的是连接请求 Handler收到的是读请求
        Runnable r = (Runnable) sk.attachment();
        r.run();
    }
}
