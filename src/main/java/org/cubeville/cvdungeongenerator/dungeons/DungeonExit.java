package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.util.Vector;
import org.cubeville.cvgames.enums.CardinalDirection;
import org.cubeville.cvgames.models.GameRegion;

import java.util.Map;

public class DungeonExit {
    GameRegion region;
    private Vector relativeMin, relativeMax;
    private CardinalDirection direction;



    public DungeonExit(GameRegion region, CardinalDirection direction) {
        this.region = region;
        this.direction = direction;
    }

    public void setRelativePosition(GameRegion pieceRegion) {
        relativeMin = region.getMin().toVector().subtract(pieceRegion.getMin().toVector());
        relativeMax = region.getMax().toVector().subtract(pieceRegion.getMin().toVector());
    }

    public CardinalDirection getDirection() {
        return direction;
    }

    public Vector getRelativeMin() {
        return relativeMin;
    }

    public Vector getRelativeMax() {
        return relativeMax;
    }


}
