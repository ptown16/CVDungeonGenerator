package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Location;
import org.cubeville.cvgames.enums.CardinalDirection;
import org.cubeville.cvgames.models.GameRegion;

public class DungeonExit {
    GameRegion region;
    private Location relativeMin, relativeMax;
    private CardinalDirection direction;

    public DungeonExit(GameRegion region, CardinalDirection direction) {
        this.region = region;
        this.direction = direction;
    }

    public void setRelativePosition(GameRegion pieceRegion) {
        relativeMin = region.getMin().clone().subtract(pieceRegion.getMin());
        relativeMax = region.getMax().clone().subtract(pieceRegion.getMin());
    }

    public CardinalDirection getDirection() {
        return direction;
    }

    public int getHeight() {
        return relativeMax.getBlockY() - relativeMin.getBlockY();
    }

    public int getWidth() {
        // Why is Minecraft like this...
        // Going NORTH is -Z
        // Going EAST is +X
        // Going SOUTH is +Z
        // Going WEST is -X
        // If the exit is facing NORTH or SOUTH we are going along the Z axis which means X is the width
        // If the exit is facing EAST or WEST we are going along the X axis which means Z is the width
        switch (direction) {
            case NORTH:
            case SOUTH:
                return relativeMax.getBlockX() - relativeMin.getBlockX();
            case EAST:
            case WEST:
                return relativeMax.getBlockZ() - relativeMin.getBlockZ();
        }
        return 0;
    }
}
