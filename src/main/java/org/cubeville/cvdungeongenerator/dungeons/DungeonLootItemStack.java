package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.inventory.ItemStack;

public class DungeonLootItemStack {

    private final ItemStack item;
    private final int minItems, maxItems;
    private final int weight;

    public DungeonLootItemStack(ItemStack item, int weight, int minItems, int maxItems) {
        this.item = item;
        this.weight = weight;
        this.minItems = minItems;
        this.maxItems = maxItems;
    }

    public ItemStack getItem() {
        int amount;
        if (minItems == maxItems) {
            amount = minItems;
        } else {
            amount = RandomManager.getRandom().nextInt(maxItems - minItems + 1) + minItems;
        }
        ItemStack newItem = item.clone();
        newItem.setAmount(amount);
        return newItem;
    }

    public Integer getWeight() {
        return weight;
    }
}
