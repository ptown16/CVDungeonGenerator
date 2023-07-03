package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.cubeville.cvgames.models.GameRegion;
import org.cubeville.cvgames.models.PlayerState;
import org.cubeville.cvgames.models.SoloGame;
import org.cubeville.cvgames.vartypes.GameVariableBlock;
import org.cubeville.cvgames.vartypes.GameVariableList;
import org.cubeville.cvgames.vartypes.GameVariableRegion;

import java.util.ArrayList;
import java.util.List;

public class Dungeons extends SoloGame {

    Player player;
    List<DungeonPiece> dungeonPieces = new ArrayList<>();

    public Dungeons(String id, String arenaName) {
        super(id, arenaName);
        addGameVariable("test-rgs", new GameVariableList<>(GameVariableRegion.class, "A region to test clipboard functionality"));
        addGameVariable("start-block", new GameVariableBlock("The place the dungeon begins its generation."));
    }

    @Override
    public void onGameStart(Player player) {
        this.player = player;
        state.put(player, new DungeonState());
        List<GameRegion> testRegions = (List<GameRegion>) getVariable("test-rgs");
        Block block = (Block) getVariable("start-block");
        Location buildLocation = block.getLocation().clone();

        for (GameRegion gameRegion : testRegions) {
            DungeonPiece dp = new DungeonPiece(gameRegion);
            dp.paste(buildLocation);
            buildLocation.add(dp.getXSize(), 0, 0);
        }
    }

    @Override
    protected DungeonState getState() {
        return (DungeonState) state.get(player);
    }

    @Override
    public void onPlayerLeave(Player player) {

    }

    @Override
    protected PlayerState getState(Player player) {
        return null;
    }

    @Override
    public void onGameFinish() {

    }
}
