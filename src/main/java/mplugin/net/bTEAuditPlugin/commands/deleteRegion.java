package mplugin.net.bTEAuditPlugin.commands;

import mplugin.net.bTEAuditPlugin.resources.DatabaseManager;
import mplugin.net.bTEAuditPlugin.resources.RegionData;
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

public class deleteRegion implements CommandExecutor {
    DatabaseManager databaseManager;
    Connection databaseConnection;

    RegionData regionData;


    private final JavaPlugin plugin;


    public deleteRegion(JavaPlugin plugin){
        this.plugin = plugin;
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


        // Convert block coords to chunk coords
        int chunkX = xPosition >> 4;
        int chunkZ = zPosition >> 4;

        // Convert chunk coords to region coords
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
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Makes sure the command sender defo has permission to delete the region
        //Will then update the necessary database files and delete the region I.A.
        if(!regionData.getDeleted1().equals(senderUUID) && !regionData.getDeleted2().equals(senderUUID)){
            player.sendMessage("§2Has command access!");
            if(args.length == 1){
                if(args[0].equals("yes")){
                    Boolean regionDeleteWorked = initDeleteRegion();
                    Boolean recordDeleteWorked = removeRecord();
                    if(regionDeleteWorked && recordDeleteWorked){
                        player.sendMessage("§2Region and Record successfully deleted!");
                    }else{
                        player.sendMessage("§4Region did not delete successfully!");
                    }
                }else if(args[0].equals("no")){
                    Boolean dataBaseUpdated = removeDeletionTag();
                    if(dataBaseUpdated){
                        player.sendMessage("§2Success in resetting record, region now set as unchecked!");
                    }
                }
            }else{
                player.sendMessage("§4Invalid args, either use yes(deletes the region) or no(does not delete the region)!");
            }
        }


        databaseManager.closeDatabase();


        return false;
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

    private Boolean removeDeletionTag(){
        try{
            PreparedStatement ps = databaseConnection.prepareStatement("UPDATE regions SET status='Unchecked', deleted1=null, deleted2=null WHERE name=?");
            ps.setString(1, regionData.getName());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean removeRecord(){
        try{
            PreparedStatement ps = databaseConnection.prepareStatement("DELETE FROM regions WHERE name=?");
            ps.setString(1, regionData.getName());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
