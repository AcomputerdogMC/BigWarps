import net.acomputerdog.bigwarps.util.BiMap;
import org.junit.Assert;
import org.junit.Test;

public class TestBiMap {

    @Test
    public void testPutGet() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Integer one = 1;
        Integer two = 2;
        Integer three = 3;

        Assert.assertEquals(one, map.getA("one"));
        Assert.assertEquals("one", map.getB(one));
        Assert.assertEquals(two, map.getA("two"));
        Assert.assertEquals("two", map.getB(two));
        Assert.assertEquals(three, map.getA("three"));
        Assert.assertEquals("three", map.getB(three));
    }

    @Test
    public void testRemoves() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        map.removeA("one");
        map.removeB(3);

        Assert.assertFalse(map.containsA("one"));
        Assert.assertFalse(map.containsB(1));
        Assert.assertTrue(map.containsA("two"));
        Assert.assertTrue(map.containsB(2));
        Assert.assertFalse(map.containsA("three"));
        Assert.assertFalse(map.containsB(3));
    }

    @Test
    public void testGenericGet() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Integer one = 1;
        Integer two = 2;
        Integer three = 3;

        Assert.assertEquals(one, map.get("one"));
        Assert.assertEquals("one", map.get(one));
        Assert.assertEquals(two, map.get("two"));
        Assert.assertEquals("two", map.get(two));
        Assert.assertEquals(three, map.get("three"));
        Assert.assertEquals("three", map.get(three));
    }

    @Test
    public void testGenericRemove() {
        BiMap<String, Integer> map = new BiMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        map.remove("one");
        map.remove(3);

        Assert.assertFalse(map.containsA("one"));
        Assert.assertFalse(map.containsB(1));
        Assert.assertTrue(map.containsA("two"));
        Assert.assertTrue(map.containsB(2));
        Assert.assertFalse(map.containsA("three"));
        Assert.assertFalse(map.containsB(3));
    }
}
