package org.cubeville.cvdungeongenerator.dungeons;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.cubeville.cvgames.enums.CardinalDirection;
import org.cubeville.cvgames.models.GameRegion;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class DungeonPiece {

    private final String name;
    private final Clipboard clipboard;
    private final GameRegion pieceRegion;
    private List<DungeonExit> exits = new ArrayList<>();

    private final CardinalDirection entranceDirection;
    private final Vector relativeEntranceMin, relativeEntranceMax;

    public DungeonPiece(String name, GameRegion gameRegion) {
        this(name, gameRegion, null, null);
    }

    public DungeonPiece(String name, GameRegion pieceRegion, @Nullable GameRegion entranceRegion, @Nullable CardinalDirection entranceDirection) {
        this.name = name;
        this.pieceRegion = pieceRegion;
        Location min = pieceRegion.getMin();
        Location max = pieceRegion.getMax();
        this.relativeEntranceMin = entranceRegion != null ? entranceRegion.getMin().toVector().subtract(min.toVector()) : null;
        this.relativeEntranceMax = entranceRegion != null ? entranceRegion.getMax().toVector().subtract(min.toVector()) : null;
        this.entranceDirection = entranceDirection;
        CuboidRegion region = new CuboidRegion(BlockVector3.at(min.getX(), min.getY(), min.getZ()), BlockVector3.at(max.getX(), max.getY(), max.getZ()));
        clipboard = new BlockArrayClipboard(region);
        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                BukkitAdapter.adapt(Objects.requireNonNull(min.getWorld())), region, clipboard, region.getMinimumPoint()
        );
        try {
            Operations.complete(forwardExtentCopy);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
    }

    public void paste(Location location) {
        paste(location, 0);
    }

    public void paste(Location location, int rotation) {
        System.out.println("pasting at " + location + " with rotation " + rotation);
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(Objects.requireNonNull(location.getWorld())))) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            AffineTransform transform = new AffineTransform();
            transform = transform.rotateY(-1 * rotation);
            holder.setTransform(transform);
            Operation operation = holder
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
    }

    public PasteAt paste(DungeonExitInstance dei) {
        if (entranceDirection == null || relativeEntranceMax == null || relativeEntranceMin == null) { return null; }
        // Move 1 away from where the exit is defined
        Location pasteLocation = dei.getMin().clone().add(RotationUtils.getExitDirectionOffset(dei.getDirection()));
        // Find what rotation this piece should be
        int pieceRotation = RotationUtils.getRotationFrom(entranceDirection, dei.getDirection());
        Vector rotatedRelativeMin = RotationUtils.getRotatedRelativeMin(relativeEntranceMin, relativeEntranceMax, pieceRotation);
        System.out.println("start paste loc " + pasteLocation);
        System.out.println("rel ent min " + relativeEntranceMin);
        System.out.println("rel ent max " + relativeEntranceMax);
        System.out.println("rot rel min " + rotatedRelativeMin);
        System.out.println("rot " + pieceRotation);

        pasteLocation.subtract(rotatedRelativeMin);
        paste(pasteLocation, pieceRotation);
        return new PasteAt(pasteLocation, pieceRotation);
    }

    public int getXSize() {
        return pieceRegion.getMax().getBlockX() - pieceRegion.getMin().getBlockX();
    }

    public int getYSize() {
        return pieceRegion.getMax().getBlockY() - pieceRegion.getMin().getBlockY();
    }

    public int getZSize() {
        return pieceRegion.getMax().getBlockZ() - pieceRegion.getMin().getBlockZ();
    }

    public Location getMin() {
        return pieceRegion.getMin();
    }

    public void setExits(List<DungeonExit> exits) {
        exits.forEach(exit -> exit.setRelativePosition(pieceRegion));
        this.exits = exits;
    }

    public List<DungeonExitInstance> createExitInstances(PasteAt pasteAt) {
        List<DungeonExitInstance> newInstances = new ArrayList<>();
        for (DungeonExit de : exits) {
            newInstances.add(new DungeonExitInstance(de, pasteAt));
        }
        return newInstances;
    }

    public String getName() {
        return name;
    }

    public GameRegion getPieceRegion() {
        return pieceRegion;
    }
}
