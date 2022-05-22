package debug.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>{@link Selector}就是复用器 在java nio下的一种对多路复用的具体实现<ul>
 *     <li>select</li>
 *     <li>poll</li>
 *     <li>epoll</li>
 *     <li>kqueue</li>
 * </ul></p>
 * @since 2022/5/21
 * @author dingrui
 */
public class SelectorTest {

    public static void main(String[] args) throws IOException {
        // 服务端创建 监听端口
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress(9001));

        // 创建一个复用器
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_ACCEPT); // 把channel注册到复用器
        int ret = selector.select();// 阻塞调用
        Set<SelectionKey> selectionKeys = selector.selectedKeys(); // 拿到事件就绪的channel

        // 以下就是根据业务展开了
        Iterator<SelectionKey> it = selectionKeys.iterator();
        while (it.hasNext()){
            SelectionKey sk = it.next();
            it.remove();
            sk.isValid();
            sk.isWritable();
            sk.isReadable();
            sk.isAcceptable();
        }
    }
}
