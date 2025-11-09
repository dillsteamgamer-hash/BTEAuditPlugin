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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class markRegion implements CommandExecutor {
    DatabaseManager databaseManager;
    Connection databaseConnection;

    RegionData regionData;


    private final JavaPlugin plugin;

    public markRegion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        Player player = (Player) commandSender;

        World currentWorld = player.getWorld();
        if (currentWorld.getName().equals("audit_world")) {
            databaseManager = new DatabaseManager(plugin);
            databaseManager.initDatabase();
            databaseConnection = databaseManager.getConnection();

            String senderUUID = String.valueOf(player.getUniqueId());

            int xPosition = player.getLocation().getBlockX();
            int yPosition = player.getLocation().getBlockY();
            int zPosition = player.getLocation().getBlockZ();


            // Convert block coordinates to chunk coordinates
            int chunkX = xPosition >> 4;
            int chunkZ = zPosition >> 4;

            // Convert chunk coordinates to region coordinates
            int regionX = Math.floorDiv(chunkX, 32);
            int regionZ = Math.floorDiv(chunkZ, 32);

            String regionName = "r." + regionX + "." + regionZ + ".mca";

            commandSender.sendMessage("§3Current Region: " + regionName);

            //Finds and loads the data of the region currently being stood in
            //Probably should change so that it checks if the status is unchecked as well -- make sure auditor hasn't moved to a new region
            try (PreparedStatement ps = databaseConnection.prepareStatement("SELECT * FROM regions WHERE name=?")) {
                ps.setString(1, regionName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    regionData = new RegionData(
                            rs.getString("name"),
                            rs.getInt("x"),
                            rs.getInt("z"),
                            rs.getString("status"));
                    regionData.setDeleted1(rs.getString("deleted1"));
                    regionData.setDeleted2(rs.getString("deleted2"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            World world = Bukkit.getWorld("world");
            assert world != null;

            //Updates the regionData object to reflect the new attributes
            if (args.length == 1) {
                if (args[0].equals("MFD")) {
                    commandSender.sendMessage("§3Sending to build world!");
                    player.teleport(new Location(world, -2082, 1, -2002));
                    regionData.setStatus("MFD");
                    if (regionData.getDeleted1() == null) {
                        regionData.setDeleted1(senderUUID);
                    } else if (regionData.getDeleted2() == null) {
                        regionData.setDeleted2(senderUUID);
                    } else {
                        commandSender.sendMessage("§4An error in marking the region for deletion has occurred!");
                        System.out.println("§4Error in marking region as deleted, already has 2 mark for deletions!");
                    }
                } else if (args[0].equals("HP")) {
                    regionData.setStatus("HP");
                    regionData.setDeleted1(null);
                    regionData.setDeleted2(null);


                    commandSender.sendMessage("§3Teleporting to build world");
                    player.teleport(new Location(world, xPosition, yPosition, zPosition));
                    commandSender.sendMessage("§3Please run \"/build\" add too add any new builds not already in the build counter!");
                }

                //Updates the database using the new attributes defined in the regionData object
                try (PreparedStatement ps = databaseConnection.prepareStatement("UPDATE regions SET status = ?, deleted1 = ?, deleted2 = ? WHERE name = ?")) {
                    ps.setString(1, regionData.getStatus());
                    ps.setString(2, regionData.getDeleted1());
                    ps.setString(3, regionData.getDeleted2());
                    ps.setString(4, regionData.getName());
                    ps.executeUpdate();
                    commandSender.sendMessage("§2Success in updating database!");
                } catch (SQLException e) {
                    commandSender.sendMessage("§4Error in updating database!");
                    e.printStackTrace();
                } catch (Exception e) {
                    commandSender.sendMessage("§4Error in updating database!");
                    throw new RuntimeException(e);
                }
                Boolean deletionSuccessful = deleteVoidRegion();
                if (deletionSuccessful) {
                    player.sendMessage("§2Success in deleting the region copy!");
                } else {
                    player.sendMessage("§4And error has occurred in deleting the region copy!");
                }
            } else {
                commandSender.sendMessage("§4Invalid argument! Select from 'MFD'(Marked For Deletion) or 'HP'(Has Progress)");
            }

        }
        databaseManager.closeDatabase();
        return false;
    }

    //Deletes the copy of the region in the void/audit world
     private Boolean deleteVoidRegion(){
        File regionFile = new File("audit_world/region/" + regionData.getName());
        if(!regionFile.exists()){
            System.out.println("§4Error in finding region file in audit_world!");
            return false;
        }else{
            return regionFile.delete();
        }
    }
}
