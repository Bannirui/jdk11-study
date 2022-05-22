package debug.io.nio;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @since 2022/5/21
 * @author dingrui
 */
public class UngrowableSetTest {

    public static void main(String[] args) {
        Set<Integer> set = new HashSet<Integer>(){{
            add(1);
        }};
        Set<Integer> uSet = ungrowableSet(set);
        System.out.println(uSet);
    }

    static <E> Set<E> ungrowableSet(final Set<E> s) {
        return new Set<E>() {

            public int size()                 { return s.size(); }
            public boolean isEmpty()          { return s.isEmpty(); }
            public boolean contains(Object o) { return s.contains(o); }
            public Object[] toArray()         { return s.toArray(); }
            public <T> T[] toArray(T[] a)     { return s.toArray(a); }
            public String toString()          { return s.toString(); }
            public Iterator<E> iterator()     { return s.iterator(); }
            public boolean equals(Object o)   { return s.equals(o); }
            public int hashCode()             { return s.hashCode(); }
            public void clear()               { s.clear(); }
            public boolean remove(Object o)   { return s.remove(o); }

            public boolean containsAll(Collection<?> coll) {
                return s.containsAll(coll);
            }
            public boolean removeAll(Collection<?> coll) {
                return s.removeAll(coll);
            }
            public boolean retainAll(Collection<?> coll) {
                return s.retainAll(coll);
            }

            public boolean add(E o){
                throw new UnsupportedOperationException();
            }
            public boolean addAll(Collection<? extends E> coll) {
                throw new UnsupportedOperationException();
            }

        };
    }
}
