package debug.io.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>NIO非阻塞下 单路模型</p>
 *
 * <p>逆向理解多路复用 当前连接的获取不存在阻塞 也就是说可以源源不断获取大量的连接 但是连接的读写状态我们并不知道
 * <p>现在有个集合 里面全是socket<ul>
 *     <li>用户层可以轮询挨个向os发送sc 问它这个socket的状态 拿到读写状态后进行操作 这个时候发生了一次系统调用 向知道整个集合的socket状态就得发生N次系统调用</li>
 *     <li>os提供一个函数 入参是集合 我们一次性将所有socket发给os os告诉用户这些连接的读写状态 发生一次系统调用</li>
 * </ul></p>
 *
 * <p>如上的这种方式就叫多路复用 实现三剑客<ul>
 *     <li>select</li>
 *     <li>poll</li>
 *     <li>epoll</li>
 * </ul></p>
 * @since 2022/5/21
 * @author dingrui
 */
public class NIOModelSingle {

    private static final List<Socket> SOCKETS = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        // 非阻塞模式
        channel.configureBlocking(false);
        ServerSocket server = channel.socket();
        server.bind(new InetSocketAddress(9090));
        while (true) {
            Socket socket = server.accept();
            if (Objects.isNull(socket)) continue;
            SOCKETS.add(socket);
            for (Socket s : SOCKETS) {
                InputStream in = s.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String msg = reader.readLine();
                if(Objects.isNull(msg)) continue;
                // TODO: 2022/5/21 业务逻辑
            }
        }
    }
}
