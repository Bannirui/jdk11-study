package debug.object.jol;

import debug.object.jol.bean.Bean;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;

/**
 * <p>java中一个对象包含3个部分<ul>
 *     <li>对象头 12byte<ul>
 *         <li>MarkWord 8byte</li>
 *         <li>KlassWord 4byte 压缩指针后的大小</li>
 *     </ul></li>
 *     <li>实例数据</li>
 *     <li>对齐填充</li>
 * </ul></p>
 * @since 2022/5/27
 * @author dingrui
 */
public class JOLTest {

    public static void main(String[] args) throws InterruptedException {
        for (; ; ) {
        }
    }

    /**
     * <p>VM参数-XX:-UseCompressedOops 关闭指针压缩</p>
     * <pre>
     *     +--------+------+------------------------+---------------------------------------+
     *     | OFFSET | SIZE |          DES           |                VALUE                  |
     *     +--------+------+------------------------+---------------------------------------+
     *     |    0   |   8  | (object header: mark)  | 0x0000000000000005 (biasable; age: 0) |
     *     +--------+------+------------------------+---------------------------------------+
     *     |    8   |   8  | (object header: class) |          0x000000010e1e5d98           |
     *     +--------+------+------------------------+---------------------------------------+
     * </pre>
     * <p>Instance size: 16 bytes</p>
     * <p>Space losses: 0 bytes internal + 0 bytes external = 0 bytes total</p>
     */
    @Test
    public void testLayout00() {
        Bean b = new Bean();
        System.out.println(ClassLayout.parseInstance(b).toPrintable());
    }

    /**
     * <p>VM参数-XX:+UseCompressedOops 或者使用默认参数 打开指针压缩</p>
     * <pre>
     *     +--------+------+------------------------+---------------------------------------+
     *     | OFFSET | SIZE |          DES           |                VALUE                  |
     *     +--------+------+------------------------+---------------------------------------+
     *     |    0   |   8  | (object header: mark)  | 0x0000000000000005 (biasable; age: 0) |
     *     +--------+------+------------------------+---------------------------------------+
     *     |    8   |   4  | (object header: class) |              0x0016a9f0               |
     *     +--------+------+------------------------+---------------------------------------+
     *     |   12   |   4  | (object alignment gap) |                                       |
     *     +--------+------+------------------------+---------------------------------------+
     * </pre>
     * <p>Instance size: 16 bytes</p>
     * <p>Space losses: 0 bytes internal + 4 bytes external = 4 bytes total</p>
     * <p>指针压缩 字面意思 压缩了一个指针的大小<ul>
     *     <li>32位处理器每次能处理32bit=4byte的单位->指针保存的就是4字节的一条内存地址</li>
     *     <li>64位处理器每次能处理64bit=8byte的单位->指针保存的就是8字节的一条内存地址</li>
     * </ul> 一片内存都是从低位到高位 如果总是用8byte来标识一个指针位置也就意味着在一定阈值之内64位的高位会浪费掉 完全可以用低位表示这个指针</p>
     * <p>对齐填充 总和为8bytes的倍数</p>
     */
    @Test
    public void testLayout01() {
        Bean b = new Bean();
        System.out.println(ClassLayout.parseInstance(b).toPrintable());
    }

    /**
     * <p>jdk11的JVM默认启动参数没有使用偏向延迟技术</p>
     * <p>查看jvm的参数 jinfo -flags {pid}</p>
     * <p>通过启动VM添加参数 -XX:BiasedLockingStartupDelay=4000 启动偏向锁延迟 4秒钟</p>
     * <p>延迟偏向之前创建的对象 对象头MarkWord的值0x0000000000000001 即16进制的0000000000000001 对应的二进制为1 标识的状态是无锁状态</p>
     * <p>延迟偏向之后创建的对象 对象头MarkWord的值0x0000000000000005 即16进制的0000000000000005 对应的二进制为101 标识的状态是未偏向的偏向锁</p>
     */
    @Test
    public void testLayout02() throws InterruptedException {
        Bean b = new Bean();
        System.out.println(ClassLayout.parseInstance(b).toPrintable());

        Thread.sleep(4_000);
        b = new Bean();
        System.out.println(ClassLayout.parseInstance(b).toPrintable());
    }

    /**
     * <p>偏向延迟之前创建的无锁对象 经过synchronized之后直接变成轻量级锁</p>
     */
    @Test
    public void testLayout03() throws InterruptedException {
        Object o = new Object();
        /**
         * <p>0x0000000000000001->1->无锁</p>
         */
        System.out.println(ClassLayout.parseInstance(o).toPrintable());
        synchronized (o) {
            /**
             * <p>0x000070000a7a2fc8->11100000000000000001010011110100010111111001000->轻量级锁</p>
             */
            System.out.println(ClassLayout.parseInstance(o).toPrintable());
        }
    }

    /**
     * <p>没有开启偏向延迟或者偏向延迟之后创建的对象 状态是没有偏向的偏向锁 经过synchronized之后就变成偏向当前线程的偏向锁</p>
     */
    @Test
    public void testLayout04() {
        Object o = new Object();
        /**
         * <p>0x0000000000000005->101->没有偏向线程的偏向锁</p>
         */
        System.out.println(ClassLayout.parseInstance(o).toPrintable());
        synchronized (o) {
            /**
             * <p>0x00007f8f0a80e805->11111111000111100001010100000001110100000000101->有偏向线程的偏向锁</p>
             */
            System.out.println(ClassLayout.parseInstance(o).toPrintable());
        }
    }

    /**
     * <p>当对象处于偏向锁状态时mark word总共64bits 除去低2位锁标识 1位偏向标识就剩61位 这61位中还得留高54位用于记录线程id 那就还剩7位(2位epoch 1位不用 1位gc年龄)</p>
     * <p>显然 在偏向锁状态下 是没有地方存储对象的hashcode的 也就是说hashcode值跟偏向锁状态互斥<ul>
     *     <li>第一种情况 当前线程持有对象的锁 锁状态为偏向锁 在synchronized代码块内调用hashcode之后 锁升级为轻量级锁</li>
     *     <li>第二种情况 当前对象无锁 调用hashcode之后 synchronized获取到的是轻量级锁 跳过偏向锁</li>
     * </ul></p>
     */
    @Test
    public void testLayout05() {
        Object o = new Object();
        // 0x0000000000000005->101->无偏向的偏向锁
        System.out.println(ClassLayout.parseInstance(o).toPrintable());
        synchronized (o) {
            // 0x00007ffac1019805->11111111111101011000001000000011001100000000101->偏向当前线程的偏向锁
            System.out.println(ClassLayout.parseInstance(o).toPrintable());
            o.hashCode();
            // 0x00007ffabff13702->11111111111101010111111111100010011011100000010
            System.out.println(ClassLayout.parseInstance(o).toPrintable());
        }
    }

    /**
     * <p>完整的锁升级过程 锁的膨胀升级肯定跟竞争有关系 但是升级之后的锁状态还跟是否进行了一次hashcode方法求值有关系</p>
     */
    @Test
    public void testLayout06() {
        Object o = new Object();
        // 0x0000000000000005->101->匿名偏向锁
        System.out.println(ClassLayout.parseInstance(o).toPrintable());
        synchronized (o) {
            // 0x00007f94bf00d005->...101->偏向锁
            System.out.println(ClassLayout.parseInstance(o).toPrintable());
        }
        // 0x00007f94bf00d005->...101->偏向锁
        System.out.println(ClassLayout.parseInstance(o).toPrintable());
        o.hashCode();
        // 0x0000001da2cb7701->///...001->无锁
        System.out.println(ClassLayout.parseInstance(o).toPrintable());
        synchronized (o) {
            // 0x0000700002f10fc0->...000->轻量级锁
            System.out.println(ClassLayout.parseInstance(o).toPrintable());
            o.hashCode();
            // 0x0000700002f10fc0->///...000->轻量级锁
            System.out.println(ClassLayout.parseInstance(o).toPrintable());
        }
    }
}
