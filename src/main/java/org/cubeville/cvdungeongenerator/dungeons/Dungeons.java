package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    private int maxPieceGeneration;
    private Player player;
    private GameRegion dungeonRegion;
    private final Set<DungeonPiece> dungeonPieces = new HashSet<>();
    private final Set<DungeonPiece> deadEnds = new HashSet<>();
    private final Stack<DungeonExitInstance> currentExits = new Stack<>();
    private final int DUNGEON_PIECE_BATCH_SIZE = 10;
    private final int DUNGEON_PIECE_BATCHES_PER_SECOND = 5;


    public Dungeons(String id, String arenaName) {
        super(id, arenaName);
        addGameVariable("start-piece", new GameVariableRegion("The piece that a user starts within"));
        addGameVariable("start-location", new GameVariableLocation("The location a player starts at (please set this location on the start piece)"));
        //TODO -- addGameVariable("end-piece", new GameVariableRegion("The piece that a user starts within"));
        //TODO -- addGameVariable("end-region", new GameVariableRegion("The location a player gets teleported out of the dungeon at"));
        addGameVariable("paste-block", new GameVariableBlock("Places you can exit a dungeon piece from"));
        addGameVariable("dungeon-region", new GameVariableRegion("The region that dungeon generation can happen in"));
        addGameVariable("piece-generation", new GameVariableInt("The number of pieces generated before it caps the rest of them off with dead ends"));
        addGameVariable("generation-room", new GameVariableRegion("The room the player sits in while the dungeon generates"));
        addGameVariable("generation-location", new GameVariableLocation("The location the player spawn in the generation room"));
        addGameVariable("loot-slot-chance", new GameVariableDouble("The percentage chance that a specific slot in a container has an item or not"), .3);

        addGameVariableObjectList("dungeon-pieces", new HashMap(){{
            put("name", new GameVariableString("The name of the dungeon piece (used for entrances and exits)"));
            put("piece-region", new GameVariableRegion("The region that contains the piece that will be pasted in"));
            put("entrance-region", new GameVariableRegion("The region that is used to define where the entrance is within the piece"));
            put("entrance-direction", new GameVariableCardinalDirection("The direction you are entering from (facing from outside into the piece)"));
            put("weight", new GameVariableInt("The weight of the piece"));
        }}, "Pieces that are used to make a dungeon");

        addGameVariableObjectList("dungeon-exits", new HashMap(){{
            put("piece-name", new GameVariableString("The name of the dungeon piece this exit belongs to"));
            put("exit-region", new GameVariableRegion("The region that is used to define where the exit is within the piece"));
            put("exit-direction", new GameVariableCardinalDirection("The direction that you are exiting"));
            put("fill", new GameVariableMaterial("If not used, the fill for this dungeon"));
        }}, "Places you can exit a dungeon piece at");

        addGameVariableObjectList("dungeon-spawns", new HashMap(){{
            put("piece-name", new GameVariableString("The name of the dungeon piece this spawn belongs to"));
            put("location", new GameVariableLocation("The location within the piece the mob spawns"));
            put("mob-name", new GameVariableString("The name of the mob you are spawning"));
            put("level", new GameVariableInt("The level of mob that is spawned in"));
        }}, "Mob spawns in the dungeon");

        addGameVariableObjectList("dungeon-containers", new HashMap(){{
            put("piece-name", new GameVariableString("The name of the dungeon piece this container belongs to"));
            put("location", new GameVariableBlock("The block that a container exists on"));
        }}, "Dungeon containers");

        addGameVariableObjectList("dungeon-loot-table", new HashMap(){{
            put("item", new GameVariableItem("The name of the dungeon piece this chest belongs to"));
//            put("quantity-min", new GameVariableItem("The minimum number of items that spawn when this is selected"));
//            put("quantity-max", new GameVariableItem("The maximum number of items that spawn when this is selected"));
            put("weight", new GameVariableInt("The weight of this item being used"));
        }}, "Dungeon loot table");
    }

    @Override
    public void onGameStart(Player player) {
        this.player = player;
        state.put(player, new DungeonState());
        dungeonRegion = (GameRegion) getVariable("dungeon-region");
        maxPieceGeneration = (int) getVariable("piece-generation");
        RandomManager.setCurrentRandom();
        // First, take all the variables coming in and process them, so we can easily calculate info using them.
        processDungeonPieces();
    }

    private void processDungeonPieces() {
        // First, generate a map that will take the name of the piece and
        Map<String, List<DungeonExit>> nameToExits = new HashMap<>();
        for (HashMap<String, Object> dungeonExit : (List<HashMap<String, Object>>) getVariable("dungeon-exits")) {
            String pieceName = (String) dungeonExit.get("piece-name");
            DungeonExit de = new DungeonExit(
                (GameRegion) dungeonExit.get("exit-region"),
                (CardinalDirection) dungeonExit.get("exit-direction"),
                (Material) dungeonExit.get("fill")
            );
            if (nameToExits.containsKey(pieceName)) {
                nameToExits.get(pieceName).add(de);
            } else {
                nameToExits.put(pieceName, new ArrayList<>(){{ add(de); }});
            }
        }

        Map<String, List<DungeonSpawn>> nameToSpawns = new HashMap<>();
        for (HashMap<String, Object> dungeonSpawn : (List<HashMap<String, Object>>) getVariable("dungeon-spawns")) {
            String pieceName = (String) dungeonSpawn.get("piece-name");
            DungeonSpawn ds = new DungeonSpawn(
                    (Location) dungeonSpawn.get("location"),
                    (String) dungeonSpawn.get("mob-name"),
                    (Integer) dungeonSpawn.get("level")
            );
            if (nameToSpawns.containsKey(pieceName)) {
                nameToSpawns.get(pieceName).add(ds);
            } else {
                nameToSpawns.put(pieceName, new ArrayList<>(){{ add(ds); }});
            }
        }

        Map<String, List<DungeonContainer>> nameToContainers = new HashMap<>();
        for (HashMap<String, Object> dungeonContainer : (List<HashMap<String, Object>>) getVariable("dungeon-containers")) {
            String pieceName = (String) dungeonContainer.get("piece-name");
            DungeonContainer dc = new DungeonContainer(
                    (Block) dungeonContainer.get("location"),
                    (double) getVariable("loot-slot-chance")
            );
            if (nameToContainers.containsKey(pieceName)) {
                nameToContainers.get(pieceName).add(dc);
            } else {
                nameToContainers.put(pieceName, new ArrayList<>(){{ add(dc); }});
            }
        }

        for (HashMap<String, Object> dungeonPiece : (List<HashMap<String, Object>>) getVariable("dungeon-pieces")) {
            Integer weight = (Integer) dungeonPiece.get("weight");
            DungeonPiece dp = new DungeonPiece(
                (String) dungeonPiece.get("name"),
                (GameRegion) dungeonPiece.get("piece-region"),
                (GameRegion) dungeonPiece.get("entrance-region"),
                (CardinalDirection) dungeonPiece.get("entrance-direction"),
                weight
             );
            dungeonPieces.add(dp);
            if (nameToExits.containsKey(dp.getName())) {
                dp.setExits(nameToExits.get(dp.getName()));
            } else {
                deadEnds.add(dp);
            }
            if (nameToSpawns.containsKey(dp.getName())) {
                dp.setMobSpawns(nameToSpawns.get(dp.getName()));
            }
            if (nameToContainers.containsKey(dp.getName())) {
                dp.setContainers(nameToContainers.get(dp.getName()));
            }
        }

        DungeonPiece startPiece = new DungeonPiece("start-piece", (GameRegion) getVariable("start-piece"));
        Location startLocationOffset = ((Location) getVariable("start-location")).clone().subtract(startPiece.getMin());
        Block block = (Block) getVariable("paste-block");
        Location blockLocation = block.getLocation().clone();
        blockLocation.setYaw(startLocationOffset.getYaw());
        blockLocation.setPitch(startLocationOffset.getPitch());
        for (DungeonExit exit : nameToExits.get("start-region")) {
            exit.setRelativePosition(startPiece.getPieceRegion());
            currentExits.add(new DungeonExitInstance(exit, new PasteAt(blockLocation, 0)));
        }
        startGenerateCountdownTask(startPiece, blockLocation, startLocationOffset);
    }

    private void startGenerateCountdownTask(DungeonPiece startPiece, Location blockLocation, Location startLocationOffset) {
        // IntelliJ made me do this idk why
        int timeToGenerate = (maxPieceGeneration / (DUNGEON_PIECE_BATCH_SIZE * DUNGEON_PIECE_BATCHES_PER_SECOND)) + 3;
        final int[] i = { timeToGenerate };
        generationCountdownTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(CVDungeonGenerator.getInstance(), () -> {
            if (i[0] == timeToGenerate) {
                WorldEditUtils.setAsync(dungeonRegion.getMin(), dungeonRegion.getMax(), Material.AIR);
            }

            if (i[0] == timeToGenerate - 1) {
                createGenerationRoom();
                startPiece.paste(blockLocation);
                generateDungeon();
            }

            if (i[0] == 0) {
                player.teleport(blockLocation.add(startLocationOffset));
                endGenerateCountdownTask();
            }
            player.sendTitle("§b§lGenerating Map", "§eReady in " + i[0] + "...", 2, 20, 2);
            player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1.0F, 1.0F);
            i[0]--;
        }, 0L, 20L);
    }

    private void createGenerationRoom() {
        DungeonPiece generationRoom = new DungeonPiece("generation-room", (GameRegion) getVariable("generation-room"));
        Location playerLocationOffset = ((Location) getVariable("generation-location")).clone().subtract(generationRoom.getMin());
        Block block = (Block) getVariable("paste-block");
        Location blockLocation = block.getLocation().clone().subtract(0, 80, 0);
        generationRoom.paste(blockLocation);
        player.teleport(blockLocation.add(playerLocationOffset));
    }

    private void endGenerateCountdownTask() {
        Bukkit.getScheduler().cancelTask(generationCountdownTask);
        generationCountdownTask = -1;
    }

    private void generateDungeon() {
        Set<DungeonPiece> excluding = new HashSet<>();
        while (maxPieceGeneration > 0 && currentExits.size() > 0) {
            // Let's use a depth first search so there's a single long path to the end.
            DungeonExitInstance exitInstance = currentExits.pop();
            DungeonPiece selectedPiece = null;
            for (int i = 0; i < dungeonPieces.size(); i++) {
                DungeonPiece piece = getRandomPiece(excluding);
                if (piece == null) { break; }
                GameRegion pasteRegion = piece.getPasteRegion(exitInstance);
                // If the paste region won't be in the dungeon region, continue to the next piece
                if (!dungeonRegion.containsLocation(pasteRegion.getMin()) || !dungeonRegion.containsLocation(pasteRegion.getMax())) {
                    excluding.add(piece);
                    continue;
                }
                if (!WorldEditUtils.isRegionEmpty(pasteRegion.getMin(), pasteRegion.getMax())) {
                    excluding.add(piece);
                    continue;
                }
                selectedPiece = piece;
                break;
            }

            if (selectedPiece == null) {
                // We can just fill in the exit with the specified material
                exitInstance.fill();
            } else {
                PasteAt pa = selectedPiece.paste(exitInstance);
                currentExits.addAll(selectedPiece.createExitInstances(pa));
                maxPieceGeneration--;
                if (maxPieceGeneration % DUNGEON_PIECE_BATCH_SIZE == 0) {
                    // Continue generating the dungeon after waiting for a bit so we don't overwhelm the system
                    Bukkit.getScheduler().scheduleSyncDelayedTask(CVDungeonGenerator.getInstance(), this::generateDungeon, (20 / DUNGEON_PIECE_BATCHES_PER_SECOND));
                    break;
                }
            }
            excluding.clear();
        }

        // Only do the post dungeon generation stuff if the while loop condition was actually met instead of being broken out of.
        if (maxPieceGeneration <= 0 || currentExits.size() <= 0) {
            afterGenerateDungeon();
        }
    }

    private void afterGenerateDungeon() {
        //TODO -- add the exit to the end of the chain here
        for (DungeonExitInstance exitInstance : currentExits) {
            // Fill in the remaining exits so people can't escape >:D
            exitInstance.fill();
        }
    }

    private DungeonPiece getRandomPiece(Set<DungeonPiece> excluding) {
        // Get a random piece based on the weight attached to each piece
        int totalWeight = 0;
        List<DungeonPiece> allowedPieces = new ArrayList<>();
        for (DungeonPiece dp : dungeonPieces) {
            if (excluding.contains(dp)) { continue; }
            // Don't include dead ends if we are on our last bit of dungeon plx
            if (deadEnds.contains(dp) && currentExits.isEmpty()) { continue; }
            totalWeight += dp.getWeight();
            allowedPieces.add(dp);
        }
        if (totalWeight <= 0) { return null; }
        int randomValue = RandomManager.getRandom().nextInt(totalWeight);
        int currentWeight = 0;
        for (DungeonPiece dp : allowedPieces) {
            if (randomValue <= currentWeight + dp.getWeight()) {
                return dp;
            }
            currentWeight += dp.getWeight();
        }
        return null;
    }

    @Override
    protected DungeonState getState() {
        return (DungeonState) state.get(player);
    }

    @Override
    public void onPlayerLeave(Player player) {
        endGenerateCountdownTask();
        finishGame();
    }

    @Override
    protected PlayerState getState(Player player) {
        return null;
    }

    @Override
    public void onGameFinish() {
        WorldEditUtils.setAsync(dungeonRegion.getMin(), dungeonRegion.getMax(), Material.AIR);
        dungeonPieces.forEach(DungeonPiece::clearActiveMobs);
        // Reset the state of the game
        dungeonPieces.clear();
        deadEnds.clear();
        currentExits.clear();
        dungeonRegion = null;
        player = null;
        maxPieceGeneration = 0;
    }
}
