package mplugin.net.bTEAuditPlugin;

import mplugin.net.bTEAuditPlugin.commands.*;
import mplugin.net.bTEAuditPlugin.resources.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.Objects;


public final class BTEAuditPlugin extends JavaPlugin {

    DatabaseManager database;

    @Override
    public void onEnable() {
        database = new DatabaseManager(this);
        database.initDatabase();
        getLogger().info("Creating void world...");


        //Creates the voidWorld
        WorldCreator creator = new WorldCreator("audit_world");
        creator.generator(new VoidWorldGenerator());
        World world = creator.createWorld();
        if (world != null) {
            getLogger().info("Void world created successfully: " + world.getName());
        } else {
            getLogger().warning("Failed to create void world!");
        }

        //Reads from config file
        BlockPoint returnPoint = new BlockPoint(getConfig().getInt("Mark-For-Deletion-TP-X"), getConfig().getInt("Mark-For-Deletion-TP-Y"), getConfig().getInt("Mark-For-Deletion-TP-Z"));
        String markAsHavingProgressMessage = getConfig().getString("Mark-Progress-Message");


        //Command Set-Up
        //Tester Commands
        Objects.requireNonNull(getCommand("tpToWorld")).setExecutor(new TPToWorld());
        Objects.requireNonNull(getCommand("auditHelp")).setExecutor(new auditHelp());
        //Admin Commands
        Objects.requireNonNull(getCommand("reloadDatabase")).setExecutor(new reloadDatabase(this));
        Objects.requireNonNull(getCommand("resetDatabase")).setExecutor(new resetDatabase(this));
        Objects.requireNonNull(getCommand("nextDeleteRegion")).setExecutor(new nextDeleteRegion(this));
        Objects.requireNonNull(getCommand("deleteRegion")).setExecutor(new deleteRegion(this, returnPoint));
        //Auditor Commands
        Objects.requireNonNull(getCommand("nextRegion")).setExecutor(new nextRegion(this));
        Objects.requireNonNull(getCommand("markRegion")).setExecutor(new markRegion(this, returnPoint, markAsHavingProgressMessage));
        //Other Commands
        Objects.requireNonNull(getCommand("regionStats")).setExecutor(new regionStats(this));
    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
        database.closeDatabase();
    }


    @Override
    public void onLoad(){
        saveDefaultConfig();
    }
}
