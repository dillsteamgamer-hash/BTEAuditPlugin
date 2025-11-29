package mplugin.net.bTEAuditPlugin.commands;

import mplugin.net.bTEAuditPlugin.resources.BlockPoint;
import mplugin.net.bTEAuditPlugin.resources.DatabaseManager;
import mplugin.net.bTEAuditPlugin.resources.RegionData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class deleteRegion implements CommandExecutor, TabCompleter {
    DatabaseManager databaseManager;
    Connection databaseConnection;

    RegionData regionData;

    BlockPoint blockPoint;


    private final JavaPlugin plugin;


    public deleteRegion(JavaPlugin plugin, BlockPoint blockPoint){
        this.plugin = plugin;
        this.blockPoint = blockPoint;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        databaseManager = new DatabaseManager(plugin);
        databaseManager.initDatabase();
        databaseConnection = databaseManager.getConnection();
        Player player = (Player) commandSender;
        String senderUUID = String.valueOf(player.getUniqueId());

        int xPosition = player.getLocation().getBlockX();
        int zPosition = player.getLocation().getBlockZ();


        // Convert block coordinates to chunk coordinates
        int chunkX = xPosition >> 4;
        int chunkZ = zPosition >> 4;

        // Convert chunk coordinates to region coordinates
        int regionX = Math.floorDiv(chunkX, 32);
        int regionZ = Math.floorDiv(chunkZ, 32);

        String regionName = "r." + regionX + "." + regionZ + ".mca";
        commandSender.sendMessage("§3Current Region: " + regionName);

        //Just creates an object of regionData from the database
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
            databaseConnection = databaseManager.getConnection();
            e.printStackTrace();
        } catch (Exception e) {
            databaseConnection = databaseManager.getConnection();
            throw new RuntimeException(e);
        }

        World world = Bukkit.getWorld("world");
        assert world != null;

        //Makes sure the command sender defo has permission to delete the region
        //Will then update the necessary database files and delete the region I.A.
        if(!regionData.getDeleted1().equals(senderUUID) && !regionData.getDeleted2().equals(senderUUID)){
            player.sendMessage("§2Has command access!");
            if(args.length == 1){
                if(args[0].equals("yes")){
                    Boolean regionDeleteWorked = initDeleteRegion();
                    Boolean recordDeleteWorked = updateRecordDeleted();
                    if(regionDeleteWorked && recordDeleteWorked){
                        player.sendMessage("§2Region Deleted and Record successfully updated!");
                        player.teleport(new Location(world, blockPoint.getxPos(), blockPoint.getyPos(), blockPoint.getzPos()));
                    }else{
                        player.sendMessage("§4Region did not delete successfully!");
                    }
                }else if(args[0].equals("no")){
                    Boolean dataBaseUpdated = removeDeletionTag();
                    if(dataBaseUpdated){
                        player.sendMessage("§2Success in resetting record, region now set as unchecked!");
                        player.teleport(new Location(world, blockPoint.getxPos(), blockPoint.getyPos(), blockPoint.getzPos()));
                    }
                }
            }else{
                player.sendMessage("§4Invalid argument, either use yes(deletes the region) or no(does not delete the region)!");
            }
        }


        databaseManager.closeDatabase();


        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

        ArrayList<String> validArgs = new ArrayList<>();
        if(args.length == 1){
            StringUtil.copyPartialMatches(args[0], List.of("yes", "no"), validArgs);
        }

        return validArgs;
    }


    private Boolean initDeleteRegion(){
        File regionFile = new File("world/region/" + regionData.getName());
        if(!regionFile.exists()){
            System.out.println("Error in finding region file in world!");
            return false;
        }else{
            return regionFile.delete();
        }
    }

    //When admin decides not to delete the region
    private Boolean removeDeletionTag(){
        try{
            PreparedStatement ps = databaseConnection.prepareStatement("UPDATE regions SET status='Unchecked', deleted1=null, deleted2=null WHERE name=?");
            ps.setString(1, regionData.getName());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            databaseConnection = databaseManager.getConnection();
            throw new RuntimeException(e);
        }
    }

    private Boolean updateRecordDeleted(){
        try{
            PreparedStatement ps = databaseConnection.prepareStatement("UPDATE regions SET status='deleted' WHERE name=?");
            ps.setString(1, regionData.getName());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            databaseConnection = databaseManager.getConnection();
            throw new RuntimeException(e);
        }
    }
}
