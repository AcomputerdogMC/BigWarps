package net.acomputerdog.bigwarps.util;

import java.util.HashMap;
import java.util.Map;

public class BiMap<A, B> {

    private final Map<A, B> map1;
    private final Map<B, A> map2;

    public BiMap() {
        map2 = new HashMap<>();
        map1 = new HashMap<>();
    }

    public void put(A a, B b) {
        if (map1.containsKey(a)) {
            //remove existing mapping for a
            map2.remove(map1.remove(a));
        }

        if (map2.containsKey(b)) {
            //remove existing mapping for b
            map1.remove(map2.remove(b));
        }

        map1.put(a, b);
        map2.put(b, a);
    }

    public Object get(Object obj) {
        if (map1.containsKey(obj)) {
            return map1.get(obj);
        }
        if (map2.containsKey(obj)) {
            return map2.get(obj);
        }
        return null;
    }

    public B getA(A a) {
        return map1.get(a);
    }

    public A getB(B b) {
        return map2.get(b);
    }

    public boolean contains(Object obj) {
        return map1.containsKey(obj) || map2.containsKey(obj);
    }

    public boolean containsA(A a) {
        return map1.containsKey(a);
    }

    public boolean containsB(B b) {
        return map2.containsKey(b);
    }

    public Object remove(Object obj) {
        if (map1.containsKey(obj)) {
            return removeA((A) obj);
        }
        if (map2.containsKey(obj)) {
            return removeB((B) obj);
        }
        return null;
    }

    public B removeA(A a) {
        B b = map1.remove(a);
        if (b != null) {
            map2.remove(b);
        }
        return b;
    }

    public A removeB(B b) {
        A a = map2.remove(b);
        if (a != null) {
            map1.remove(a);
        }
        return a;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BiMap)) return false;

        BiMap<?, ?> biMap = (BiMap<?, ?>) o;

        return map1.equals(biMap.map1);

    }

    @Override
    public int hashCode() {
        return map1.hashCode();
    }
}
