package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.cubeville.cvgames.enums.CardinalDirection;

public class DungeonExitInstance {

    private final DungeonExit dungeonExit;
    private final CardinalDirection direction;
    private final Location min, max;

    DungeonExitInstance(DungeonExit dungeonExit, PasteAt pasteAt) {
        this.dungeonExit = dungeonExit;
        int pieceRotationPasted = pasteAt.rotation;

        Vector rotatedRelativeMin = RotationUtils.getRotatedRelativeMin(dungeonExit.getRelativeMin(), dungeonExit.getRelativeMax(), pieceRotationPasted);

        this.direction = RotationUtils.applyRotationToCardinalDirection(dungeonExit.getDirection(), pieceRotationPasted);
        this.min = pasteAt.location.clone().add(rotatedRelativeMin);
        this.max = min.clone().add(dungeonExit.getSizeVector());
    }

    public CardinalDirection getDirection() {
        return direction;
    }

    public Location getShiftedMinLocation() {
        // Move 1 away from where the exit is defined
        return min.clone().add(RotationUtils.getExitDirectionOffset(direction));
    }

    public void fill() {
        WorldEditUtils.setAsync(min, max, dungeonExit.getFillMaterial());
    }
}