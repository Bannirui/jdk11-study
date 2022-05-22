package debug.io.model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <p>NIO非阻塞下 多路复用模型</p>
 *
 * @since 2022/5/21
 * @author dingrui
 */
public class NIOModelMultiple {

    private static final List<Socket> SOCKETS = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel channel = ServerSocketChannel.open();
        // 非阻塞模式
        channel.configureBlocking(false);
        ServerSocket server = channel.socket();
        server.bind(new InetSocketAddress(9090));
        channel.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
            selector.select();
            // 多路复用器
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectionKeys.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                if (key.isAcceptable()) {
                    // TODO: 2022/5/21
                } else if (key.isReadable()) {
                    // TODO: 2022/5/21
                } else if (key.isWritable()) {
                    // TODO: 2022/5/21
                }
            }
        }
    }
}
