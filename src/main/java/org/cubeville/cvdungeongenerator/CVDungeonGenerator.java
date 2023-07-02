package org.cubeville.cvdungeongenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class CVDungeonGenerator extends JavaPlugin {
    private static CVDungeonGenerator instance;


    public void onEnable() {
        instance = this;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static CVDungeonGenerator getInstance() { return instance; }

}
