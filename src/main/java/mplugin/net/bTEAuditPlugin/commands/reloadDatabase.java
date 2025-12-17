package mplugin.net.bTEAuditPlugin.commands;

import mplugin.net.bTEAuditPlugin.resources.DatabaseManager;
import mplugin.net.bTEAuditPlugin.resources.RegionData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

public class reloadDatabase implements CommandExecutor {

    private final JavaPlugin plugin;

    public reloadDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            DatabaseManager databaseManager = new DatabaseManager(plugin);
            databaseManager.initDatabase();
            Connection databaseConnection = databaseManager.getConnection();

            ArrayList<String> overworldRegionFiles = new ArrayList<>();

            File worldContainer = Bukkit.getWorldContainer();
            File regionFolder = new File(
                    worldContainer,
                    Objects.requireNonNull(plugin.getConfig().getString("Earth-World-Name")) + "/region"
            );

            if (!regionFolder.exists() || !regionFolder.isDirectory()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getLogger().severe("§4Region folder not found at: " + regionFolder.getAbsolutePath())
                );
                databaseManager.closeDatabase();
                return;
            }

            File[] files = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));
            if (files == null) {
                databaseManager.closeDatabase();
                return;
            }

            for (File file : files) {
                overworldRegionFiles.add(file.getName());
            }

            ArrayList<RegionData> regionDataList = new ArrayList<>();

            Bukkit.getScheduler().runTask(plugin, () ->
                    commandSender.sendMessage("§2Found filepath! Finding regions!")
            );

            for (String file : overworldRegionFiles) {
                String name = file;
                file = file.substring(2);

                int xPos = Integer.parseInt(file.substring(0, file.indexOf(".")));
                file = file.substring(file.indexOf(".") + 1);
                int zPos = Integer.parseInt(file.substring(0, file.indexOf(".")));

                regionDataList.add(new RegionData(name, xPos, zPos, "Unchecked"));
            }

            Bukkit.getScheduler().runTask(plugin, () ->
                    commandSender.sendMessage("§2Found all regions! Adding regions to Database!")
            );

            for (RegionData regionData : regionDataList) {
                String sql;

                try {
                    if (databaseConnection.getMetaData().getDriverName().equalsIgnoreCase("sqlite jdbc")) {
                        sql = "INSERT OR IGNORE INTO regions (name, x, z, status) VALUES (?, ?, ?, ?)";
                    } else {
                        sql = "INSERT IGNORE INTO regions (name, x, z, status) VALUES (?, ?, ?, ?)";
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                try (PreparedStatement ps = databaseConnection.prepareStatement(sql)) {
                    ps.setString(1, regionData.getName());
                    ps.setInt(2, regionData.getX());
                    ps.setInt(3, regionData.getZ());
                    ps.setString(4, "Unchecked");
                    ps.executeUpdate();
                } catch (SQLException e) {
                    databaseConnection = databaseManager.getConnection();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLogger().severe("§4Fail to add region to database!" + e);
                        commandSender.sendMessage("§4Error in adding a region to the database, see console for more info");
                    });
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                commandSender.sendMessage("§2Successfully added all regions to the database!");
                commandSender.sendMessage("§2Finished reloading the database!");
            });

            databaseManager.closeDatabase();
        });

        return false;
    }
}