package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Bukkit;

import java.util.Random;

public class RandomManager {

    private static Random random;

    public static Random getRandom() {
        return random;
    }

    // Hopefully this means I can reproduce specific dungeon instances and make it easier to find bugs?
    public static void setCurrentRandom() {
        random = new Random();
        long seed = random.nextLong();
        random.setSeed(seed);
        Bukkit.getLogger().info("Generating a dungeon with the random seed: " + seed);
    }

}
