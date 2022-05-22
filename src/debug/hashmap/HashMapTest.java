package debug.hashmap;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @since 2022/2/21
 * @author dingrui
 */
public class HashMapTest {

    public static void main(String[] args) {
        Map<String, Integer> m = new HashMap<>();
        Integer a = m.put("a", 1);

        System.out.println(m);
    }
}
