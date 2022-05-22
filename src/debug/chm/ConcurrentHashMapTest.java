package debug.chm;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @since 2022/2/21
 * @author dingrui
 */
public class ConcurrentHashMapTest {

    public static void main(String[] args) {
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        Integer a = chm.put("A", 1);
        System.out.println(chm);
    }
}
