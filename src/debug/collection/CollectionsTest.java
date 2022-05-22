package debug.collection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>仅仅是在现有已经实现的数据结构基础上重写方法并且返回{@link UnsupportedOperationException}</p>
 * @author dingrui
 * @since 2022/2/9
 * @description
 */
public class CollectionsTest {

    public static void main(String[] args) {
        Set<Integer> s = new HashSet<Integer>() {{
            add(1);
            add(2);
        }};
        Set<Integer> us = Collections.unmodifiableSet(s);
        us.add(3);
        System.out.println(us);
    }
}
