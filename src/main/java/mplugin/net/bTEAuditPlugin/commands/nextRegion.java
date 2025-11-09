package mplugin.net.bTEAuditPlugin.commands;

import mplugin.net.bTEAuditPlugin.resources.DatabaseManager;
import mplugin.net.bTEAuditPlugin.resources.RegionData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

public class nextRegion implements CommandExecutor {
    DatabaseManager databaseManager;
    Connection databaseConnection;
    RegionData regionData;


    private final JavaPlugin plugin;

    public nextRegion(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        databaseManager = new DatabaseManager(plugin);
        databaseManager.initDatabase();
        databaseConnection = databaseManager.getConnection();

        Player player = (Player) commandSender;
        String senderUUID = String.valueOf(player.getUniqueId());

        try (PreparedStatement ps = databaseConnection.prepareStatement("SELECT * FROM regions WHERE status='Unchecked' OR (deleted1 IS NOT NULL AND deleted2 IS NULL AND deleted1 != ? AND status='MFD') LIMIT 1")) {
            ps.setString(1, senderUUID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                regionData = new RegionData(
                        rs.getString("name"),
                        rs.getInt("x"),
                        rs.getInt("z"),
                        rs.getString("status"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        commandSender.sendMessage("Next Region: " + regionData.getName());

        File source = new File(Bukkit.getWorldContainer(), "world/region/r." + regionData.getX() + "." + regionData.getZ() + ".mca");
        File targetDir = new File(Bukkit.getWorldContainer(), "audit_world/region/");
        targetDir.mkdirs();

        File target = new File(targetDir, source.getName());


        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        commandSender.sendMessage("Copied Region Data to Audit World");


        int blockX = regionData.getX() * 512 + 256;
        int blockZ = regionData.getZ() * 512 + 256;

        World voidWorld = Bukkit.getWorld("audit_world");
        assert voidWorld != null;

        int y = voidWorld.getHighestBlockYAt(blockX, blockZ);

        player.teleport(new Location(voidWorld, blockX, y, blockZ));
        player.sendMessage("§aTeleported to region: §e" + regionData.getName());

        databaseManager.closeDatabase();
        return false;
    }
}
