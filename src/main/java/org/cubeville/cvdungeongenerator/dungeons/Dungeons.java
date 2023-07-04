package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.cubeville.cvdungeongenerator.CVDungeonGenerator;
import org.cubeville.cvgames.enums.CardinalDirection;
import org.cubeville.cvgames.models.GameRegion;
import org.cubeville.cvgames.models.PlayerState;
import org.cubeville.cvgames.models.SoloGame;
import org.cubeville.cvgames.vartypes.*;

import java.util.*;

public class Dungeons extends SoloGame {

    private int generationCountdownTask = -1;
    private int maxPieceGeneration = 4;

    private Player player;
    private List<DungeonPiece> dungeonPieces = new ArrayList<>();
    private List<DungeonPiece> deadEnds = new ArrayList<>();
    private Queue<DungeonExitInstance> currentExits = new PriorityQueue<>();

    public Dungeons(String id, String arenaName) {
        super(id, arenaName);
        addGameVariable("start-piece", new GameVariableRegion("The piece that a user starts within"));
        addGameVariable("start-location", new GameVariableLocation("The location a player starts at (please set this location on the start piece)"));
        addGameVariable("paste-block", new GameVariableBlock("Places you can exit a dungeon piece from"));
        addGameVariable("dungeon-region", new GameVariableRegion("The region that dungeon generation can happen in"));
        addGameVariableObjectList("dungeon-pieces", new HashMap(){{
            put("name", new GameVariableString("The name of the dungeon piece (used for entrances and exits)"));
            put("piece-region", new GameVariableRegion("The region that contains the piece that will be pasted in"));
            put("entrance-region", new GameVariableRegion("The region that is used to define where the entrance is within the piece"));
            put("entrance-direction", new GameVariableCardinalDirection("The direction you are entering from (facing from outside into the piece)"));
        }}, "Pieces that are used to make a dungeon");

        addGameVariableObjectList("dungeon-exits", new HashMap(){{
            put("piece-name", new GameVariableString("The name of the dungeon piece this exit belongs to"));
            put("exit-region", new GameVariableRegion("The region that is used to define where the exit is within the piece"));
            put("exit-direction", new GameVariableCardinalDirection("The direction that you are exiting"));
        }}, "Places you can exit a dungeon piece at");
    }

    @Override
    public void onGameStart(Player player) {
        this.player = player;
        state.put(player, new DungeonState());
        Block block = (Block) getVariable("paste-block");
        Location blockLocation = block.getLocation().clone();
        // First, take all the variables coming in and process them so we can easily calculate info using them.
        processDungeonPieces();
    }

    private void processDungeonPieces() {
        // First, generate a map that will take the name of the piece and
        Map<String, List<DungeonExit>> nameToExits = new HashMap<>();
        for (HashMap<String, Object> dungeonExit : (List<HashMap<String, Object>>) getVariable("dungeon-exits")) {
            String pieceName = (String) dungeonExit.get("piece-name");
            DungeonExit de = new DungeonExit((GameRegion) dungeonExit.get("exit-region"), (CardinalDirection) dungeonExit.get("exit-direction"));
            if (nameToExits.containsKey(pieceName)) {
                nameToExits.get(pieceName).add(de);
            } else {
                nameToExits.put(pieceName, new ArrayList<>(){{ add(de); }});
            }
        }

        for (HashMap<String, Object> dungeonPiece : (List<HashMap<String, Object>>) getVariable("dungeon-pieces")) {
            DungeonPiece dp = new DungeonPiece(
                (String) dungeonPiece.get("name"),
                (GameRegion) dungeonPiece.get("piece-region"),
                (GameRegion) dungeonPiece.get("entrance-region"),
                (CardinalDirection) dungeonPiece.get("entrance-direction")
            );
            dungeonPieces.add(dp);
            if (nameToExits.containsKey(dp.getName())) {
                dp.setExits(nameToExits.get(dp.getName()));
            } else {
                deadEnds.add(dp);
            }
        }

        DungeonPiece startPiece = new DungeonPiece("start-piece", (GameRegion) getVariable("start-piece"));
        Location startLocationOffset = ((Location) getVariable("start-location")).clone().subtract(startPiece.getMin());
        Block block = (Block) getVariable("paste-block");
        Location blockLocation = block.getLocation().clone();
        for (DungeonExit exit : nameToExits.get("start-region")) {
            currentExits.add(new DungeonExitInstance(exit, exit.getDirection()));
        }
        startPiece.paste(blockLocation);
        startGenerateCountdownTask(blockLocation.add(startLocationOffset));
    }

    private void startGenerateCountdownTask(Location startLocation) {
        // IntelliJ made me do this idk why
        final int[] i = {3};
        generationCountdownTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(CVDungeonGenerator.getInstance(), () -> {
            if (i[0] == 0) {
                player.teleport(startLocation);
                endGenerateCountdownTask();
            }
            player.sendTitle("§bGenerating Map", "§Ready in " + i[0] + "...", 2, 18, 2);
            i[0]--;
        }, 0L, 20L);
    }

    private void endGenerateCountdownTask() {
        Bukkit.getScheduler().cancelTask(generationCountdownTask);
        generationCountdownTask = -1;
    }

    private void generateDungeon() {
        while (maxPieceGeneration > 0 && currentExits.size() > 0) {
            // Let's use a breadth first search rn
            DungeonExitInstance exitInstance = currentExits.poll();

        }
    }

    @Override
    protected DungeonState getState() {
        return (DungeonState) state.get(player);
    }

    @Override
    public void onPlayerLeave(Player player) {
        endGenerateCountdownTask();
    }

    @Override
    protected PlayerState getState(Player player) {
        return null;
    }

    @Override
    public void onGameFinish() {

    }
}
