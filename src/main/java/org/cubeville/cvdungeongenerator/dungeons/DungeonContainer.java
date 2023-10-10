package org.cubeville.cvdungeongenerator.dungeons;

import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.cubeville.cvgames.models.GameRegion;
import io.lumine.mythic.bukkit.BukkitAdapter;

import java.util.Objects;

public class DungeonContainer {

    Block location;
    Vector relativePosition;
    double lootSlotChance;

    public DungeonContainer(Block location, double lootSlotChance) {
        this.location = location;
        this.lootSlotChance = lootSlotChance;
    }

    public void setRelativePosition(GameRegion pieceRegion) {
        relativePosition = location.getLocation().toVector().subtract(pieceRegion.getMin().toVector());
    }

    public void populateContainer(Location pieceLocation, int pieceRotation) {
        Vector rotationVector = RotationUtils.getRotationVector(pieceRotation);
        Vector relativeVector;
        if (pieceRotation % 180 == 0) {
            relativeVector = new Vector(relativePosition.getX(), relativePosition.getY(), relativePosition.getZ());
        } else {
            relativeVector = new Vector(relativePosition.getZ(), relativePosition.getY(), relativePosition.getX());
        }
        relativeVector.multiply(rotationVector);
        Location containerLocation = pieceLocation.clone().add(relativeVector);
        Block block = Objects.requireNonNull(containerLocation.getWorld()).getBlockAt(containerLocation);
        if (!(block.getState() instanceof Container)) {
            Bukkit.getLogger().info("Error populating container for dungeon: Could not find a container at location " + containerLocation);
            return;
        }
        Container container = (Container) block.getState();
        for (int i = 0; i < container.getInventory().getSize(); i++) {
            if (RandomManager.getRandom().nextDouble() > lootSlotChance) { continue; }
            container.getInventory().setItem(i, new ItemStack(Material.APPLE));
        }
    }
}

