package org.cubeville.cvdungeongenerator.dungeons;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
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
    private final Set<Item> droppedItems = new HashSet<>();
    private DungeonPiece endPiece;
    private MythicMob finalBoss;
    private final Stack<DungeonExitInstance> currentExits = new Stack<>();
    private final int DUNGEON_PIECE_BATCH_SIZE = 10;
    private final int DUNGEON_PIECE_BATCHES_PER_SECOND = 5;
    private float playerExp;
    private int playerLevel;


    public Dungeons(String id, String arenaName) {
        super(id, arenaName);
        addGameVariable("start-piece", new GameVariableRegion("The piece that a user starts within"));
        addGameVariable("start-location", new GameVariableLocation("The location a player starts at (please set this location on the start piece)"));
        addGameVariable("end-boss", new GameVariableString("The mob that the player needs to kill in order to win"));
        addGameVariable("paste-block", new GameVariableBlock("Places you can exit a dungeon piece from"));
        addGameVariable("dungeon-region", new GameVariableRegion("The region that dungeon generation can happen in"));
        addGameVariable("win-command", new GameVariableString("The command run when the player wins the game"));
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
            put("quantity-min", new GameVariableInt("The minimum number of items that spawn when this is selected"));
            put("quantity-max", new GameVariableInt("The maximum number of items that spawn when this is selected"));
            put("weight", new GameVariableInt("The weight of this item being used"));
        }}, "Dungeon loot table");
    }

    @Override
    public void onGameStart(Player player) {
        this.player = player;
        player.setFireTicks(0);
        player.getActivePotionEffects().clear();
        player.setHealth(20);
        player.setSaturation(20);
        playerExp = player.getExp();
        playerLevel = player.getLevel();
        player.setExp(0);
        player.setLevel(0);
        state.put(player, new DungeonState());
        dungeonRegion = (GameRegion) getVariable("dungeon-region");
        maxPieceGeneration = (int) getVariable("piece-generation");
        String finalBossName = (String) getVariable("end-boss");
        finalBoss = MythicBukkit.inst().getMobManager().getMythicMob(finalBossName).orElse(null);
        if (finalBoss == null) {
            player.sendMessage("§cError Generating Dungeon -- Mob with name " + finalBossName + " does not exist! Please report this error to a staff member.");
            finishGame();
            return;
        }
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

        DungeonLootTable lootTable = new DungeonLootTable();
        for (HashMap<String, Object> dungeonLootItem : (List<HashMap<String, Object>>) getVariable("dungeon-loot-table")) {
            lootTable.addItem(new DungeonLootItemStack(
                    (ItemStack) dungeonLootItem.get("item"),
                    (int) dungeonLootItem.get("weight"),
                    (int) dungeonLootItem.get("quantity-min"),
                    (int) dungeonLootItem.get("quantity-max")
            ));
        }

        Map<String, List<DungeonContainer>> nameToContainers = new HashMap<>();
        for (HashMap<String, Object> dungeonContainer : (List<HashMap<String, Object>>) getVariable("dungeon-containers")) {
            String pieceName = (String) dungeonContainer.get("piece-name");
            DungeonContainer dc = new DungeonContainer(
                    (Block) dungeonContainer.get("location"),
                    (double) getVariable("loot-slot-chance"),
                    lootTable
            );
            if (nameToContainers.containsKey(pieceName)) {
                nameToContainers.get(pieceName).add(dc);
            } else {
                nameToContainers.put(pieceName, new ArrayList<>(){{ add(dc); }});
            }
        }

        for (HashMap<String, Object> dungeonPiece : (List<HashMap<String, Object>>) getVariable("dungeon-pieces")) {
            DungeonPiece dp = new DungeonPiece(
                (String) dungeonPiece.get("name"),
                (GameRegion) dungeonPiece.get("piece-region"),
                (GameRegion) dungeonPiece.get("entrance-region"),
                (CardinalDirection) dungeonPiece.get("entrance-direction"),
                (Integer) dungeonPiece.get("weight")
             );
            if (dp.getName().equals("end-piece")) {
                endPiece = dp;
            } else {
                dungeonPieces.add(dp);
            }
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
        if (nameToContainers.containsKey("start-piece")) {
            startPiece.setContainers(nameToContainers.get("start-piece"));
        }
        Location startLocationOffset = ((Location) getVariable("start-location")).clone().subtract(startPiece.getMin());
        Block block = (Block) getVariable("paste-block");
        Location blockLocation = block.getLocation().clone();
        blockLocation.setYaw(startLocationOffset.getYaw());
        blockLocation.setPitch(startLocationOffset.getPitch());
        for (DungeonExit exit : nameToExits.get("start-piece")) {
            exit.setRelativePosition(startPiece.getPieceRegion());
            currentExits.add(new DungeonExitInstance(exit, new PasteAt(blockLocation, 0)));
        }
        startGenerateCountdownTask(startPiece, blockLocation, startLocationOffset);
    }

    private void startGenerateCountdownTask(DungeonPiece startPiece, Location blockLocation, Location startLocationOffset) {
        // IntelliJ made me do this idk why
        int timeToGenerate = (maxPieceGeneration / (DUNGEON_PIECE_BATCH_SIZE * DUNGEON_PIECE_BATCHES_PER_SECOND)) + 3;
        final int[] i = { (timeToGenerate * 2) };
        generationCountdownTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(CVDungeonGenerator.getInstance(), () -> {
            if (i[0] == (timeToGenerate * 2)) {
                WorldEditUtils.setAsync(dungeonRegion.getMin(), dungeonRegion.getMax(), Material.AIR);
            }

            if (i[0] == ((timeToGenerate * 2) - 1)) {
                createGenerationRoom();
                startPiece.paste(blockLocation);
                startPiece.populateContainers(blockLocation, 0);
                generateDungeon();
            }

            if (i[0] == 0) {
                player.teleport(blockLocation.add(startLocationOffset));
                endGenerateCountdownTask();
            }

            if (i[0] % 2 == 0) {
                player.sendTitle("§b§lGenerating Map", "§eReady in " + (i[0] / 2) + "...", 2, 20, 2);
                player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1.0F, 1.0F);
            }
            i[0]--;
        }, 0L, 10L);
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
        if (generationCountdownTask != -1) { Bukkit.getScheduler().cancelTask(generationCountdownTask); }
        generationCountdownTask = -1;
    }

    private void generateDungeon() {
        while (maxPieceGeneration > 1 && currentExits.size() > 0) {
            runStep();

            if (maxPieceGeneration % DUNGEON_PIECE_BATCH_SIZE == 0) {
                // Continue generating the dungeon after waiting for a bit so we don't overwhelm the system
                Bukkit.getScheduler().scheduleSyncDelayedTask(CVDungeonGenerator.getInstance(), this::generateDungeon, (20 / DUNGEON_PIECE_BATCHES_PER_SECOND));
                break;
            }
        }

        // If we hit this, the dungeon did not generate fully
        if (currentExits.size() == 0) {
            player.sendMessage("§cError Generating Dungeon -- The dungeon cannot generate completely. Please try again, and report to staff if this is happening regularly.");
            finishGame();
            return;
        }

        // Only do the post dungeon generation stuff if the while loop condition was actually met instead of being broken out of.
        if (maxPieceGeneration <= 1) {
            afterGenerateDungeon();
        }
    }

    private void runStep() {
        runStep(null);
    }

    private void runStep(DungeonPiece requiredPiece) {
        // Let's use a depth first search so there's a single long path to the end.
        Set<DungeonPiece> excluding = new HashSet<>();
        DungeonExitInstance exitInstance = currentExits.pop();
        DungeonPiece selectedPiece = null;
        for (int i = 0; i < dungeonPieces.size(); i++) {
            DungeonPiece piece;

            if (requiredPiece != null) {
                if (excluding.contains(requiredPiece)) { break; }
                piece = requiredPiece;

            } else {
                piece = getRandomPiece(excluding);
                if (piece == null) { break; }
            }

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
        }
        excluding.clear();
    }

    private void afterGenerateDungeon() {

        while (maxPieceGeneration > 0 && currentExits.size() > 0) {
            runStep(endPiece);
        }

        // If we hit this, the generated dungeon is impossible, and we need to kick the player out
        if (maxPieceGeneration == 1) {
            player.sendMessage("§cError Generating Dungeon -- Cannot find a valid location for the end piece. Please try again, and report to staff if this is happening regularly.");
            finishGame();
            return;
        }

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
        finishGame();
    }

    @Override
    protected PlayerState getState(Player player) {
        return null;
    }

    @EventHandler
    protected void onMythicMobDeath(MythicMobDeathEvent mde) {
        if (player == null) { return; }
        if (mde.getMob().getType().equals(finalBoss)) {
            if ((mde.getKiller() != null && mde.getKiller().equals(player)) || mde.getEntity().getLocation().distance(player.getLocation()) < 100) {
                // THE PLAYER WINS
                player.sendTitle("§a§lYou Win!", "You survived the dungeon!", 5, 90, 5);
                String winCommand = (String) getVariable("win-command");
                winCommand = winCommand.replaceAll("%player%", player.getDisplayName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), winCommand);
                finishGame();
            }
        }
    }

    @EventHandler
    protected void onPlayerDeath(PlayerDeathEvent deathEvent) {
        if (deathEvent.getEntity().equals(player)) {
            // make sure the player drops nothing on death
            deathEvent.setKeepInventory(true);
            deathEvent.getDrops().clear();
            deathEvent.setDroppedExp(0);
            player.sendTitle("§c§lYou Died!", "You succumbed to the dungeon...", 5, 90, 5);

            finishGame();
        }
    }

    // Prevent the case where someone throws a bunch of stuff on the ground in the spawn room and regenerates
    @EventHandler
    protected void onPlayerDropItem(PlayerDropItemEvent dropItemEvent) {
        if (dropItemEvent.getPlayer().equals(player)) {
            droppedItems.add(dropItemEvent.getItemDrop());
        }
    }

    @EventHandler
    protected void onItemDespawn(ItemDespawnEvent itemDespawnEvent) {
        droppedItems.remove(itemDespawnEvent.getEntity());
    }

    @Override
    public void onGameFinish() {
        endGenerateCountdownTask();
        dungeonPieces.forEach(DungeonPiece::clearActiveMobs);
        endPiece.clearActiveMobs();
        // Reset the state of the game
        dungeonPieces.clear();
        deadEnds.clear();
        currentExits.clear();
        dungeonRegion = null;
        player.setFireTicks(0);
        player.getActivePotionEffects().clear();
        player.setHealth(20);
        player.setSaturation(20);
        player.setExp(playerExp);
        player.setLevel(playerLevel);
        player.teleport((Location) getVariable("exit"));
        droppedItems.forEach(Entity::remove);
        droppedItems.clear();

        player = null;
        maxPieceGeneration = 0;
    }
}
