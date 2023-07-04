package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.cubeville.cvgames.enums.CardinalDirection;

public class DungeonExitInstance {

    private final DungeonExit dungeonExit;
    private final int pieceRotationPasted;
    private final CardinalDirection direction;
    private final Location min;

    DungeonExitInstance(DungeonExit dungeonExit, PasteAt pasteAt) {
        this.dungeonExit = dungeonExit;
        this.pieceRotationPasted = pasteAt.rotation;

        Vector rotatedRelativeMin = RotationUtils.getRotatedRelativeMin(dungeonExit.getRelativeMin(), dungeonExit.getRelativeMax(), pieceRotationPasted);

        this.direction = RotationUtils.applyRotationToCardinalDirection(dungeonExit.getDirection(), pieceRotationPasted);
        this.min = pasteAt.location.clone().add(rotatedRelativeMin);
    }

    public Location getMin() {
        return min;
    }

    public CardinalDirection getDirection() {
        return direction;
    }
}