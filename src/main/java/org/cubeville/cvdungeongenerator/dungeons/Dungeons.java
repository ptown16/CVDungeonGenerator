package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.cubeville.cvgames.models.GameRegion;
import org.cubeville.cvgames.models.PlayerState;
import org.cubeville.cvgames.models.SoloGame;
import org.cubeville.cvgames.vartypes.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dungeons extends SoloGame {

    Player player;
    List<DungeonPiece> dungeonPieces = new ArrayList<>();

    public Dungeons(String id, String arenaName) {
        super(id, arenaName);
        addGameVariable("start-piece", new GameVariableRegion("The piece that a user starts within"));
        addGameVariable("start-location", new GameVariableLocation("The location a player starts at (please set this location on the start piece)"));
        addGameVariable("paste-block", new GameVariableBlock("Places you can exit a dungeon piece from"));
        addGameVariable("dungeon-region", new GameVariableRegion("The region that dungeon generation can happen in"));
        addGameVariableObjectList("dungeon-pieces", new HashMap(){{
            put("name", new GameVariableString("The name of the dungeon piece (used for entrances and exits)"));
            put("piece-region", new GameVariableRegion("The region that contains the piece that will be pasted in"));
        }}, "Pieces that are used to make a dungeon");

        addGameVariableObjectList("dungeon-entrances", new HashMap(){{
            put("name", new GameVariableString("The name of the dungeon piece this entrance belongs to"));
            put("entrance-region", new GameVariableRegion("The region that is used to define where the exit is within the piece"));
            put("direction", new GameVariableCardinalDirection("The direction you are entering from"));
        }}, "Places you can enter into a dungeon piece from");

        addGameVariableObjectList("dungeon-exits", new HashMap(){{
            put("name", new GameVariableString("The name of the dungeon piece this exit belongs to"));
            put("exit-region", new GameVariableRegion("The region that is used to define where the exit is within the piece"));
            put("direction", new GameVariableCardinalDirection("The direction that you are exiting"));
        }}, "Places you can exit a dungeon piece at");
    }

    @Override
    public void onGameStart(Player player) {
        this.player = player;
        state.put(player, new DungeonState());
        Block block = (Block) getVariable("paste-block");
        Location blockLocation = block.getLocation().clone();
        DungeonPiece startPiece = new DungeonPiece((GameRegion) getVariable("start-piece"));
        // We want to get the location relative to the place it's pasted.
        Location startLocationOffset = ((Location) getVariable("start-location")).clone().subtract(startPiece.getMin());
        // Start the player at the correct location at the start.


        startPiece.paste(blockLocation);
        player.teleport(blockLocation.add(startLocationOffset));
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
