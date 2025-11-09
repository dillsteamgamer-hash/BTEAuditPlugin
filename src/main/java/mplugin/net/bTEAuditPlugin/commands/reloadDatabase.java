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


public class reloadDatabase implements CommandExecutor {
    private DatabaseManager databaseManager;
    private Connection databaseConnection;

    private final JavaPlugin plugin;

    public reloadDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        databaseManager = new DatabaseManager(plugin);
        databaseManager.initDatabase();
        databaseConnection = databaseManager.getConnection();


        ArrayList<String> overworldRegionFiles = new ArrayList<>();
        File worldContainer = Bukkit.getWorldContainer();
        File regionFolder = new File(worldContainer, "world/region");

        if (!regionFolder.exists() || !regionFolder.isDirectory()) {
            System.out.println("Region folder not found at: " + regionFolder.getAbsolutePath());
            return false;
        }

        File[] files = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));
        if (files == null) return false;

        // Add all filenames to the ArrayList
        for (File file : files) {
            overworldRegionFiles.add(file.getName());
        }

        ArrayList<RegionData> regionDataList = new ArrayList<>();

        commandSender.sendMessage("Found filepath! Finding regions!");
        for(String file : overworldRegionFiles){
            commandSender.sendMessage("New Region Found: " + file);
            String name = file;
            file = file.substring(2);

            int xPos = Integer.parseInt(file.substring(0, file.indexOf(".")));
            file = file.substring(file.indexOf(".") + 1);
            int zPos = Integer.parseInt(file.substring(0, file.indexOf(".")));


            regionDataList.add(new RegionData(name, xPos, zPos, "Unchecked"));
        }

        commandSender.sendMessage("Found all regions! Adding regions to Database!");
        for(RegionData regionData : regionDataList){
            String sql = "INSERT OR IGNORE INTO regions (name, x, z, status) VALUES (?, ?, ?, ?)";
            try {
                PreparedStatement ps = databaseConnection.prepareStatement(sql);
                ps.setString(1, regionData.getName());
                ps.setInt(2, regionData.getX());
                ps.setInt(3, regionData.getZ());
                ps.setString(4, "Unchecked");
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Fail to add region to database!" + e);
                commandSender.sendMessage("Error in adding a region to the database, see console for more info");
            }
        }

        commandSender.sendMessage("Successfully added all regions to the database!");
        commandSender.sendMessage("Finished reloading the database!");



        databaseManager.closeDatabase();
        return false;
    }
}

