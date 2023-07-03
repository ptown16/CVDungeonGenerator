package org.cubeville.cvdungeongenerator.dungeons;

import org.cubeville.cvgames.models.PlayerState;

public class DungeonState extends PlayerState {
    @Override
    public int getSortingValue() {
        return -1;
    }
}
