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

        final boolean customDB = plugin.getConfig().getBoolean("Use-Custom-DB");

        if(!customDB) {

            try {
                // Ensure plugin folder exists
                if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

                File dbFile = new File(plugin.getDataFolder(), "regions.db");
                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

                connection = DriverManager.getConnection(url);

                plugin.getLogger().info("SQLite database initialized successfully.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database!", e);
            }
        }else{

            final String dbURL = plugin.getConfig().getString("Custom-DB-URL");
            final String dbUsername = plugin.getConfig().getString("Custom-DB-Username");
            final String dbPassword = plugin.getConfig().getString("Custom-DB-Password");

            try {
                connection = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
            } catch(SQLException e){
                e.printStackTrace();
            }
        }


        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                                CREATE TABLE IF NOT EXISTS regions (
                                    name VARCHAR(25) PRIMARY KEY,
                                    x INTEGER,
                                    z INTEGER,
                                    status VARCHAR(16),
                                    deleted1 VARCHAR(36),
                                    deleted2 VARCHAR(36)
                                )
                            """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


    public void closeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close database connection!", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
