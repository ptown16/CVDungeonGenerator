package org.cubeville.cvdungeongenerator.dungeons;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.cubeville.cvdungeongenerator.CVDungeonGenerator;
import org.cubeville.cvgames.models.GameRegion;
import io.lumine.mythic.bukkit.BukkitAdapter;

import java.util.ArrayList;
import java.util.List;

public class DungeonSpawn {

    Location location;
    Vector relativePosition;
    MythicMob mob;
    Integer level;

    public DungeonSpawn(Location location, String mobName, Integer level) {
        this.location = location;
        this.mob = MythicBukkit.inst().getMobManager().getMythicMob(mobName).orElse(null);
        this.level = level;
    }

    public void setRelativePosition(GameRegion pieceRegion) {
        relativePosition = location.toVector().subtract(pieceRegion.getMin().toVector());
    }

    public ActiveMob spawn(Location pieceLocation, int pieceRotation) {
        Vector rotationVector = RotationUtils.getRotationVector(pieceRotation);
        Vector relativeVector;
        if (pieceRotation % 180 == 0) {
            relativeVector = new Vector(relativePosition.getX(), relativePosition.getY(), relativePosition.getZ());
        } else {
            relativeVector = new Vector(relativePosition.getZ(), relativePosition.getY(), relativePosition.getX());
        }
        relativeVector.multiply(rotationVector);
        Location mobSpawnLocation = pieceLocation.clone().add(relativeVector);
        //TODO -- mob spawn direction rotation based on piece loc
        return mob.spawn(BukkitAdapter.adapt(mobSpawnLocation), level);
    }
}

