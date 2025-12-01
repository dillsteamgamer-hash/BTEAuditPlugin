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

public class nextRegion implements CommandExecutor {

    private final JavaPlugin plugin;

    public nextRegion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull[] strings) {

        if (!(commandSender instanceof Player player)) return false;

        DatabaseManager databaseManager = new DatabaseManager(plugin);
        databaseManager.initDatabase();
        Connection connection = databaseManager.getConnection();

        RegionData regionData;

        String senderUUID = player.getUniqueId().toString();

        // Fetch the next region
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM regions WHERE status='Unchecked' OR " +
                        "(deleted1 IS NOT NULL AND deleted2 IS NULL AND deleted1 != ? AND status='MFD') LIMIT 1")) {
            ps.setString(1, senderUUID);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                player.sendMessage("§cNo regions available.");
                databaseManager.closeDatabase();
                return true;
            }

            regionData = new RegionData(
                    rs.getString("name"),
                    rs.getInt("x"),
                    rs.getInt("z"),
                    rs.getString("status")
            );
        } catch (SQLException e) {
            databaseManager.closeDatabase();
            e.printStackTrace();
            return true;
        }
        player.sendMessage("§3Next Region: " + regionData.getName());


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
            player.sendMessage("§cFailed to copy region file!");
            databaseManager.closeDatabase();
            e.printStackTrace();
            return true;
        }
        player.sendMessage("§3Copied Region Data to Audit World");

        // Teleport player to the center of the region
        int blockX = regionData.getX() * 512 + 256;
        int blockZ = regionData.getZ() * 512 + 256;
        assert world != null;
        int y = world.getHighestBlockYAt(blockX, blockZ);

        player.teleport(new Location(world, blockX, y, blockZ));
        player.sendMessage("§3Teleported to region: " + regionData.getName());

        player.setAllowFlight(true);
        player.setFlying(true);

        databaseManager.closeDatabase();
        return true;
    }
}