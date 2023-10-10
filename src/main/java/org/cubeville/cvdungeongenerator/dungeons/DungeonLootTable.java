package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DungeonLootTable {
    private Set<DungeonLootItemStack> items = new HashSet<>();

    public void addItem(DungeonLootItemStack itemStack) {
        items.add(itemStack);
    }

    public ItemStack getRandomItem() {
        // Get a random piece based on the weight attached to each piece
        int totalWeight = 0;
        for (DungeonLootItemStack itemStack : items) {
            totalWeight += itemStack.getWeight();
        }
        if (totalWeight <= 0) { return null; }
        int randomValue = RandomManager.getRandom().nextInt(totalWeight);
        int currentWeight = 0;
        for (DungeonLootItemStack itemStack : items) {
            if (randomValue <= currentWeight + itemStack.getWeight()) {
                return itemStack.getItem();
            }
            currentWeight += itemStack.getWeight();
        }
        return null;
    }
}
