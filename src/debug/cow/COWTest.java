package debug.cow;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @since 2022/5/18
 * @author dingrui
 */
public class COWTest {

    public static void main(String[] args) {
        CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>();
        cow.add(1);
        Integer ret = cow.get(0);
        Integer remove = cow.remove(0);
        System.out.println(cow);
    }
}
