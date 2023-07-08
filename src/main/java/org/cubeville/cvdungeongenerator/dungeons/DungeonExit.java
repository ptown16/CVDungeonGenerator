package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.cubeville.cvgames.enums.CardinalDirection;
import org.cubeville.cvgames.models.GameRegion;

public class DungeonExit {

    private final GameRegion region;
    private Vector relativeMin, relativeMax;
    private final CardinalDirection direction;
    private final Material fillMaterial;



    public DungeonExit(GameRegion region, CardinalDirection direction, Material fillMaterial) {
        this.region = region;
        this.direction = direction;
        this.fillMaterial = fillMaterial;
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

    public Vector getSizeVector() { return relativeMax.clone().subtract(relativeMin); }


    public Material getFillMaterial() {
        return fillMaterial;
    }
}
