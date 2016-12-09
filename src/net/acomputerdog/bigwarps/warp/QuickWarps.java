package net.acomputerdog.bigwarps.warp;

import java.util.HashMap;
import java.util.Map;

public class QuickWarps {
    private final Map<String, String> nameMap;
    private final Map<String, Integer> nameCount;

    public QuickWarps() {
        nameMap = new HashMap<>();
        nameCount = new HashMap<>();
    }

    public String getRealName(String shortName) {
        return nameMap.get(shortName);
    }

    public void increaseCount(String shortName, String realName) {
        int count = getNameCount(shortName) + 1;
        checkNameCount(count, shortName, realName);
    }

    public void decreaseCount(String shortName, String realName) {
        int count = getNameCount(shortName) - 1;
        checkNameCount(count, shortName, realName);
    }

    public void removeName(String shortName) {
        nameCount.remove(shortName);
        nameMap.remove(shortName);
    }

    private void checkNameCount(int count, String shortName, String realName) {
        nameCount.put(shortName, count);
        if (count <= 1) {
            nameMap.put(shortName, realName);
        } else {
            nameMap.remove(shortName);
        }
    }

    private int getNameCount(String shortName) {
        Integer i = nameCount.get(shortName);
        if (i == null) {
            nameCount.put(shortName, 0);
            i = 0;
        }
        return i;
    }
}
