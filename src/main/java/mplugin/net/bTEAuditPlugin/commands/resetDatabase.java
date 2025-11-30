package mplugin.net.bTEAuditPlugin.commands;

import mplugin.net.bTEAuditPlugin.resources.DatabaseManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class resetDatabase implements CommandExecutor {
    private DatabaseManager databaseManager;
    private Connection databaseConnection;

    private final JavaPlugin plugin;

    public resetDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        databaseManager = new DatabaseManager(plugin);
        databaseManager.initDatabase();
        databaseConnection = databaseManager.getConnection();

        String sql = "DROP TABLE regions";
        try {
            Statement statement = databaseConnection.createStatement();
            statement.execute(sql);
            commandSender.sendMessage("§2Successfully dropped the regions database!");
        } catch (SQLException e) {
            databaseConnection = databaseManager.getConnection();
            throw new RuntimeException(e);
        }


        databaseManager.closeDatabase();
        return false;
    }
}
