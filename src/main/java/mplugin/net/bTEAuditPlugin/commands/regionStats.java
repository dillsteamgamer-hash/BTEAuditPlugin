package mplugin.net.bTEAuditPlugin.commands;

import mplugin.net.bTEAuditPlugin.resources.DatabaseManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class regionStats implements CommandExecutor {
    DatabaseManager databaseManager;
    Connection databaseConnection;
    private final JavaPlugin plugin;

    public regionStats(JavaPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        databaseManager = new DatabaseManager(plugin);
        databaseManager.initDatabase();
        databaseConnection = databaseManager.getConnection();

        int totalRegions = 0;
        int totalUnchecked = 0;
        int totalHP = 0;
        int totalMFD1 = 0;
        int totalMFD2 = 0;
        int totalDeleted = 0;

        try{
            PreparedStatement ps = databaseConnection.prepareStatement("SELECT COUNT(*) AS count FROM regions");
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                totalRegions = rs.getInt("count");
            }
        } catch (SQLException e) {
            databaseConnection = databaseManager.getConnection();
            throw new RuntimeException(e);
        }
        try{
            PreparedStatement ps = databaseConnection.prepareStatement("SELECT COUNT(*) AS count FROM regions WHERE (deleted1 IS NOT NULL AND deleted2 IS NULL AND status='MFD')");
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                totalMFD1 = rs.getInt("count");
            }
        } catch (SQLException e) {
            databaseConnection = databaseManager.getConnection();
            throw new RuntimeException(e);
        }
        try{
            PreparedStatement ps = databaseConnection.prepareStatement("SELECT COUNT(*) AS count FROM regions WHERE (deleted1 IS NOT NULL AND deleted2 IS NOT NULL AND status='MFD')");
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                totalMFD2 = rs.getInt("count");
            }
        } catch (SQLException e) {
            databaseConnection = databaseManager.getConnection();
            throw new RuntimeException(e);
        }

        totalDeleted = count("deleted");
        totalUnchecked = count("Unchecked");
        totalHP = count("HP");

        commandSender.sendMessage("§6-----Region Stats-----");
        commandSender.sendMessage("§eTotal Regions: "+totalRegions);
        commandSender.sendMessage("§eUnchecked: "+totalUnchecked);
        commandSender.sendMessage("§eHas Progress: "+totalHP);
        commandSender.sendMessage("§eMFD 1: "+totalMFD1);
        commandSender.sendMessage("§eMFD 2: "+totalMFD2);
        commandSender.sendMessage("§eDeleted: "+totalDeleted);
        commandSender.sendMessage("§6----------------------");

        databaseManager.closeDatabase();

        return false;
    }

    public int count(String type){
        try{
            PreparedStatement ps = databaseConnection.prepareStatement("SELECT COUNT(*) AS count FROM regions WHERE status=?");
            ps.setString(1,type);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return  rs.getInt("count");
            }
        } catch (SQLException e) {
            databaseConnection = databaseManager.getConnection();
            throw new RuntimeException(e);
        }
        return 0;
    }
}
