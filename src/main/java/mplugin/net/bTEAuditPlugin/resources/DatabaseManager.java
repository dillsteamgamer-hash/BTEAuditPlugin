package mplugin.net.bTEAuditPlugin.resources;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {


    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    public void initDatabase() {
        try {
            // Ensure plugin folder exists
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

            File dbFile = new File(plugin.getDataFolder(), "regions.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS regions (
                        name VARCHAR(16) PRIMARY KEY,
                        x INTEGER,
                        z INTEGER,
                        status VARCHAR(16),
                        deleted1 UUID,
                        deleted2 UUID
                    )
                """);
            }

            plugin.getLogger().info("SQLite database initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database!", e);
        }
    }


    public void closeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("SQLite connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close database connection!", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
