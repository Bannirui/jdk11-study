package debug.concurrentlinkedqueue;


import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @since 2022/5/11
 * @author dingrui
 */
public class ConcurrentLinkedQueueTest {

    public static void main(String[] args) {
        ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
        q.offer(1);
        System.out.println(q);
    }
}
