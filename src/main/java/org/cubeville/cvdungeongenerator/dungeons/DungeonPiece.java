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
import java.util.List;
import java.util.Objects;

public class DungeonPiece {

    private final String name;
    private final Clipboard clipboard;
    private final GameRegion pieceRegion;
    private final Integer weight;
    private List<DungeonExit> exits = new ArrayList<>();

    private final CardinalDirection entranceDirection;
    private final Vector relativeEntranceMin, relativeEntranceMax;

    public DungeonPiece(String name, GameRegion gameRegion) {
        this(name, gameRegion, null, null, null);
    }

    public DungeonPiece(String name, GameRegion pieceRegion, @Nullable GameRegion entranceRegion, @Nullable CardinalDirection entranceDirection, @Nullable Integer weight) {
        this.name = name;
        this.pieceRegion = pieceRegion;
        Location min = pieceRegion.getMin();
        Location max = pieceRegion.getMax();
        // Fill out the pieces with structure voids so we can check corners on generation instead of checking all blocks.
        this.relativeEntranceMin = entranceRegion != null ? entranceRegion.getMin().toVector().subtract(min.toVector()) : null;
        this.relativeEntranceMax = entranceRegion != null ? entranceRegion.getMax().toVector().subtract(min.toVector()) : null;
        this.entranceDirection = entranceDirection;
        this.weight = weight;
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
        int pieceRotation = RotationUtils.getRotationFrom(entranceDirection, dei.getDirection());
        Vector rotatedRelativeMin = RotationUtils.getRotatedRelativeMin(relativeEntranceMin, relativeEntranceMax, pieceRotation);
        Location pasteLocation = dei.getShiftedMinLocation().subtract(rotatedRelativeMin);
        paste(pasteLocation, pieceRotation);
        return new PasteAt(pasteLocation, pieceRotation);
    }

    public GameRegion getPasteRegion(DungeonExitInstance dei) {
        int pieceRotation = RotationUtils.getRotationFrom(entranceDirection, dei.getDirection());
        Vector rotatedRelativeMin = RotationUtils.getRotatedRelativeMin(relativeEntranceMin, relativeEntranceMax, pieceRotation);
        Location pasteLocation = dei.getShiftedMinLocation().subtract(rotatedRelativeMin);
        Vector rotationVector = RotationUtils.getRotationVector(pieceRotation);
        Vector sizeVector;
        if (pieceRotation % 180 == 0) {
            sizeVector = new Vector(getXSize(), getYSize(), getZSize());
        } else {
            sizeVector = new Vector(getZSize(), getYSize(), getXSize());
        }
        sizeVector.multiply(rotationVector);
        return gameRegionFromLocations(pasteLocation, pasteLocation.clone().add(sizeVector));
    }

    private GameRegion gameRegionFromLocations(Location minY, Location maxY) {
        // We will already know which Y is min / max since it can never be subtracted
        Location min = new Location(minY.getWorld(), 0, minY.getY(), 0);
        Location max = new Location(minY.getWorld(), 0, maxY.getY(), 0);

        if (maxY.getBlockX() > minY.getBlockX()) {
            min.setX(minY.getBlockX());
            max.setX(maxY.getBlockX());
        } else {
            min.setX(maxY.getBlockX());
            max.setX(minY.getBlockX());
        }

        if (maxY.getBlockZ() > minY.getBlockZ()) {
            min.setZ(minY.getBlockZ());
            max.setZ(maxY.getBlockZ());
        } else {
            min.setZ(maxY.getBlockZ());
            max.setZ(minY.getBlockZ());
        }

        return new GameRegion(min, max);
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

    public Integer getWeight() {
        return weight;
    }
}
