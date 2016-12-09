package net.acomputerdog.bigwarps.warp;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps "simple" warp names to "real" warp names for easy warping.
 *  Simple warp names are just the names that were assigned at creation, like "house" or "castle".
 *  Real warp names are the owner's *current* name, followed by a dot, then the simple name.
 *  The owner's name is updated at each startup
 * A particular simple -> real mapping is only valid if there is only one warp with the same real name.
 *  If two or more players all have a public warp named "house", then quickwarps will be disabled.
 *  Those players will still be able to easily warp, however, because their own house will be considered a private
 *    warp and take priority.
 *  Other players will have to specify a real name.
 * Example: player1 has a public warp called "lit_house".
 *  player1 can use the simple name "lit_house" because their own private warp takes priority.
 *  Normally other players would have to use the "real name" of "player1.lit_house", however
 *  quickwarps will map "lit_house" to "player1.lit_house" automatically.
 *  However if player2 also builds a "lit_house" then quickwarps will be disabled because of the conflict.
 */
public class QuickWarps {
    //map of simple names to real names
    private final Map<String, String> nameMap;
    //map of simple names to the number of times it is being used
    private final Map<String, Integer> nameCount;

    public QuickWarps() {
        nameMap = new HashMap<>();
        nameCount = new HashMap<>();
    }

    public String getRealName(String shortName) {
        return nameMap.get(shortName);
    }

    /**
     * Increase the usage of a simple name
     */
    public void increaseCount(String shortName, String realName) {
        int count = getNameCount(shortName) + 1;
        checkNameCount(count, shortName, realName);
    }

    /**
     * Decrease the usage of a simple name
     */
    public void decreaseCount(String shortName, String realName) {
        int count = getNameCount(shortName) - 1;
        checkNameCount(count, shortName, realName);
    }

    /**
     * Removes all references to a simple name
     */
    public void removeName(String shortName) {
        nameCount.remove(shortName);
        nameMap.remove(shortName);
    }

    /**
     * Checks if there is or is not a name collision, and updates the mappings as needed.
     */
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
