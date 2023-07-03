package org.cubeville.cvdungeongenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvgames.CVGames;
import org.cubeville.cvdungeongenerator.dungeons.Dungeons;

public class CVDungeonGenerator extends JavaPlugin {
    private static CVDungeonGenerator instance;


    public void onEnable() {
        instance = this;
        CVGames.gameManager().registerGame("dungeons", Dungeons::new);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static CVDungeonGenerator getInstance() { return instance; }

}
