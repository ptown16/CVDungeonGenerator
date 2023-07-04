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
import org.cubeville.cvgames.enums.CardinalDirection;
import org.cubeville.cvgames.models.GameRegion;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DungeonPiece {

    private final String name;
    private final Clipboard clipboard;
    private final GameRegion gameRegion;
    private List<DungeonExit> exits = new ArrayList<>();

    private final GameRegion entranceRegion;
    private final CardinalDirection entranceDirection;

    public DungeonPiece(String name, GameRegion gameRegion) {
        this(name, gameRegion, null, null);
    }

    public DungeonPiece(String name, GameRegion gameRegion, @Nullable GameRegion entranceRegion, @Nullable CardinalDirection entranceDirection) {
        this.name = name;
        this.gameRegion = gameRegion;
        this.entranceRegion = entranceRegion;
        this.entranceDirection = entranceDirection;
        Location min = gameRegion.getMin();
        Location max = gameRegion.getMax();
        CuboidRegion region = new CuboidRegion(BlockVector3.at(min.getX(), min.getY(), min.getZ()), BlockVector3.at(max.getX(), max.getY(), max.getZ()));
        clipboard = new BlockArrayClipboard(region);
        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
            BukkitAdapter.adapt(Objects.requireNonNull(min.getWorld())) , region, clipboard, region.getMinimumPoint()
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
            transform = transform.rotateY(rotation);
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

    public void paste(DungeonExitInstance dei) {
        // my brain is melting please help
    }

    public int getXSize() {
        return gameRegion.getMax().getBlockX() - gameRegion.getMin().getBlockX();
    }

    public int getYSize() {
        return gameRegion.getMax().getBlockY() - gameRegion.getMin().getBlockY();
    }

    public int getZSize() {
        return gameRegion.getMax().getBlockZ() - gameRegion.getMin().getBlockZ();
    }

    public Location getMin() { return gameRegion.getMin(); }

    public Location getMax() { return gameRegion.getMax(); }

    public List<DungeonExit> getExits() {
        return exits;
    }

    public void setExits(List<DungeonExit> exits) {
        exits.forEach(exit -> exit.setRelativePosition(gameRegion));
        this.exits = exits;
    }

    public String getName() {
        return name;
    }

    public GameRegion getEntranceRegion() {
        return entranceRegion;
    }

    public CardinalDirection getEntranceDirection() {
        return entranceDirection;
    }
}
