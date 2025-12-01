package mplugin.net.bTEAuditPlugin.commands;

import mplugin.net.bTEAuditPlugin.resources.DatabaseManager;
import mplugin.net.bTEAuditPlugin.resources.RegionData;
import mplugin.net.bTEAuditPlugin.resources.VoidWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.Objects;

public class nextDeleteRegion implements CommandExecutor {
    DatabaseManager databaseManager;
    Connection databaseConnection;
    RegionData regionData;


    private final JavaPlugin plugin;

    public nextDeleteRegion(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        databaseManager = new DatabaseManager(plugin);
        databaseManager.initDatabase();
        databaseConnection = databaseManager.getConnection();

        Player player = (Player) commandSender;
        String senderUUID = String.valueOf(player.getUniqueId());

        //Same code as in nextRegion, with an extended SQL query to make sure that they are going to a region they can delete
        try (PreparedStatement ps = databaseConnection.prepareStatement("SELECT * FROM regions WHERE (status='MFD' AND deleted1 IS NOT NULL AND deleted2 IS NOT NULL AND deleted1 != ? AND deleted2 != ?) LIMIT 1")) {
            ps.setString(1, senderUUID);
            ps.setString(2, senderUUID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                regionData = new RegionData(
                        rs.getString("name"),
                        rs.getInt("x"),
                        rs.getInt("z"),
                        rs.getString("status")
                );
            }else{
                player.sendMessage("§3There are no regions which need the final go-ahead!");
                return true;
            }
        } catch (SQLException e) {
            databaseConnection = databaseManager.getConnection();
            e.printStackTrace();
        } catch (Exception e) {
            databaseConnection = databaseManager.getConnection();
            throw new RuntimeException(e);
        }
        commandSender.sendMessage("§3Next Region: " + regionData.getName());

        World auditWorld = Bukkit.getWorld("audit_world");
        //Teleport players out of audit_world
        if (auditWorld != null) {
            for (Player p : auditWorld.getPlayers()) {
                p.teleport(new Location(Bukkit.getWorld(Objects.requireNonNull(plugin.getConfig().getString("Earth-World-Name"))), p.getX(), p.getY(), p.getZ()));
                p.sendMessage("World has been emptied, only 1 person can audit at a time!");
            }

            // Unload the world
            Bukkit.unloadWorld(auditWorld, false);
        }


        // Reload the audit world
        WorldCreator creator = new WorldCreator("audit_world");
        creator.generator(new VoidWorldGenerator());
        World world = creator.createWorld();
        if (world != null) {
            plugin.getLogger().info("Void world created successfully: " + world.getName());
        } else {
            plugin.getLogger().warning("Failed to create void world!");
        }

        //Copy region file to audit_world
        File source = new File(Bukkit.getWorldContainer(), plugin.getConfig().getString("Earth-World-Name") + "/region/" + regionData.getName());
        File targetDir = new File(Bukkit.getWorldContainer(), "audit_world/region/");
        targetDir.mkdirs();
        File target = new File(targetDir, source.getName());
        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage("§cFailed to copy region file!");
            databaseManager.closeDatabase();
            return true;
        }
        player.sendMessage("§3Copied Region Data to Audit World");

        //TPs player to the centre of the copy of the region in the audit world
        int blockX = regionData.getX() * 512 + 256;
        int blockZ = regionData.getZ() * 512 + 256;

        World voidWorld = Bukkit.getWorld("audit_world");
        assert voidWorld != null;

        int y = voidWorld.getHighestBlockYAt(blockX, blockZ);

        player.teleport(new Location(voidWorld, blockX, y, blockZ));
        player.sendMessage("§3Teleported to region: " + regionData.getName());

        player.setAllowFlight(true);
        player.setFlying(true);

        databaseManager.closeDatabase();
        return false;
    }
}
