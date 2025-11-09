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

        int totalRegions;
        int totalUnchecked;
        int totalHP;
        int totalMFD1;
        int totalMFD2;
        int totalDeleted;

        try{
            PreparedStatement ps = databaseConnection.prepareStatement("SELECT COUNT(*) AS count FROM regions");
            ResultSet rs = ps.executeQuery();
            totalRegions = rs.getInt("count");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try{
            PreparedStatement ps = databaseConnection.prepareStatement("SELECT COUNT(*) AS count FROM regions WHERE (deleted1 IS NOT NULL AND deleted2 IS NULL AND status='MFD')");
            ResultSet rs = ps.executeQuery();
            totalMFD1 = rs.getInt("count");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try{
            PreparedStatement ps = databaseConnection.prepareStatement("SELECT COUNT(*) AS count FROM regions WHERE (deleted1 IS NOT NULL AND deleted2 IS NOT NULL AND status='MFD')");
            ResultSet rs = ps.executeQuery();
            totalMFD2 = rs.getInt("count");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        totalUnchecked = count("Unchecked");
        totalHP = count("HP");
        totalDeleted = count("Deleted");

        commandSender.sendMessage("-----Region Stats-----");
        commandSender.sendMessage("Total Regions: "+totalRegions);
        commandSender.sendMessage("Unchecked: "+totalUnchecked);
        commandSender.sendMessage("Has Progress: "+totalHP);
        commandSender.sendMessage("MFD 1: "+totalMFD1);
        commandSender.sendMessage("MFD 2: "+totalMFD2);
        commandSender.sendMessage("Deleted: "+totalDeleted);
        commandSender.sendMessage("----------------------");

        databaseManager.closeDatabase();

        return false;
    }

    public int count(String type){
        try{
            PreparedStatement ps = databaseConnection.prepareStatement("SELECT COUNT(*) AS count FROM regions WHERE status=?");
            ps.setString(1,type);
            ResultSet rs = ps.executeQuery();
            return  rs.getInt("count");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
