package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Location;
import org.cubeville.cvgames.enums.CardinalDirection;

public class DungeonExitInstance {

    private final DungeonExit dungeonExit;
    private final CardinalDirection directionPlaced;
    private Location min, max;

    DungeonExitInstance(DungeonExit dungeonExit, CardinalDirection directionPlaced, Location locationPasted) {
        this.dungeonExit = dungeonExit;
        this.directionPlaced = directionPlaced;
    }

    public CardinalDirection nextPasteDirection(CardinalDirection newAttemptedDirection) {
        return CardinalDirectionMap.combineRotations(directionPlaced, newAttemptedDirection);
    }

    public DungeonExit getDungeonExit() {
        return dungeonExit;
    }
}
