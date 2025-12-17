package mplugin.net.bTEAuditPlugin;

import mplugin.net.bTEAuditPlugin.commands.*;
import mplugin.net.bTEAuditPlugin.resources.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;


public final class BTEAuditPlugin extends JavaPlugin {

    DatabaseManager database;

    @Override
    public void onEnable() {
        database = new DatabaseManager(this);
        database.initDatabase();

        //Deletes the files in audit_world_xyz folders
        deleteAuditWorlds();


        /*
        //Creates the voidWorld
        WorldCreator creator = new WorldCreator("audit_world");
        creator.generator(new VoidWorldGenerator());
        World world = creator.createWorld();
        if (world != null) {
            getLogger().info("Void world created successfully: " + world.getName());
        } else {
            getLogger().warning("Failed to create void world!");
        }
        */

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
        Objects.requireNonNull(getCommand("reloadDatabaseWithAutodelete")).setExecutor(new reloadDatabaseWithAutodelete(this));
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


    private void deleteAuditWorlds() {
        File worldContainer = Bukkit.getWorldContainer();

        File[] files = worldContainer.listFiles();
        if (files == null) {
            getLogger().warning("World container is empty or inaccessible.");
            return;
        }

        for (File file : files) {
            if (!file.isDirectory()) continue;

            String name = file.getName();
            if (!name.startsWith("audit_world")) continue;

            // Safety check: make sure it's not loaded
            if (Bukkit.getWorld(name) != null) {
                getLogger().warning("World " + name + " is loaded — skipping deletion.");
                continue;
            }

            try {
                deleteRecursively(file.toPath());
                getLogger().info("Deleted audit world: " + name);
            } catch (IOException e) {
                getLogger().severe("Failed to delete world " + name);
                e.printStackTrace();
            }
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed deleting " + p, e);
                    }
                });
    }
}
