package org.cubeville.cvdungeongenerator.dungeons;

import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Objects;

public class WorldEditUtils {

    public static void setAsync(Location min, Location max, Material material) {
        TaskManager.taskManager().async(() -> set(min, max, material));
    }

    public static void set(Location min, Location max, Material material) {
        CuboidRegion selection = getCuboidRegion(min, max);
        try (EditSession editSession = getEditSession(Objects.requireNonNull(min.getWorld()))) {
            Pattern pattern = new BaseBlock(BukkitAdapter.adapt(material.createBlockData()));
            editSession.setBlocks((Region) selection, pattern);
            editSession.flushQueue();
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
        }
    }

    public static void replace(Location min, Location max, Material from, Material to) {
        CuboidRegion selection = getCuboidRegion(min, max);
        try (EditSession editSession = getEditSession(Objects.requireNonNull(min.getWorld()))) {
            Pattern pattern = new BaseBlock(BukkitAdapter.adapt(to.createBlockData()));
            Mask mask = new BlockMask(editSession.getExtent(), new BaseBlock(BukkitAdapter.adapt(from.createBlockData())));
            editSession.replaceBlocks(selection, mask, pattern);
            editSession.flushQueue();
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
        }
    }

    public static void asyncReplace(Location min, Location max, Material from, Material to) {
        TaskManager.taskManager().async(() -> replace(min, max, from, to));
    }

    public static boolean isRegionEmpty(Location min, Location max) {
        CuboidRegion selection = getCuboidRegion(min, max);
        try (EditSession editSession = getEditSession(Objects.requireNonNull(min.getWorld()))) {
            Mask mask = new BlockMask(editSession.getExtent(), new BaseBlock(BukkitAdapter.adapt(Material.AIR.createBlockData())));
            boolean isEmpty = editSession.countBlocks(selection, mask.inverse()) == 0;
            editSession.flushQueue();
            return isEmpty;
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static CuboidRegion getCuboidRegion(Location min, Location max) {
        World world = BukkitAdapter.adapt(Objects.requireNonNull(min.getWorld()));
        BlockVector3 minBV = BlockVector3.at(min.getBlockX(), min.getBlockY(), min.getBlockZ());
        BlockVector3 maxBV = BlockVector3.at(max.getBlockX(), max.getBlockY(), max.getBlockZ());
        return new CuboidRegion(world, minBV, maxBV);
    }

    private static EditSession getEditSession(org.bukkit.World world) {
        return WorldEdit.getInstance().newEditSessionBuilder()
            .world(FaweAPI.getWorld(world.getName()))
            .fastMode(true)
            .maxBlocks(-1)
            .build();
    }
}
