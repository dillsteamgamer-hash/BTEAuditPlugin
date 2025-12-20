package mplugin.net.bTEAuditPlugin.commands;

import mplugin.net.bTEAuditPlugin.resources.DatabaseManager;
import mplugin.net.bTEAuditPlugin.resources.RegionData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jnbt.*;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.InflaterInputStream;

public class reloadDatabaseWithAutodelete implements CommandExecutor {

    private DatabaseManager databaseManager;
    private Connection databaseConnection;
    private static JavaPlugin plugin;

    public reloadDatabaseWithAutodelete(JavaPlugin plugin) {
        reloadDatabaseWithAutodelete.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if(!(args.length == 1 && args[0].equals("yes"))) {
            commandSender.sendMessage("Invalid arguments {yes}!");
            return false;
        }

        // Run the heavy process asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

            databaseManager = new DatabaseManager(plugin);
            databaseManager.initDatabase();
            databaseConnection = databaseManager.getConnection();

            ArrayList<String> overworldRegionFiles = new ArrayList<>();
            File worldContainer = Bukkit.getWorldContainer();
            File regionFolder = new File(worldContainer,
                    Objects.requireNonNull(plugin.getConfig().getString("Earth-World-Name")) + "/region");

            if (!regionFolder.exists() || !regionFolder.isDirectory()) {
                plugin.getLogger().severe("§4Region folder not found at: " + regionFolder.getAbsolutePath());
                return;
            }

            File[] files = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));
            if (files == null) return;

            for (File file : files) overworldRegionFiles.add(file.getName());

            ArrayList<RegionData> regionDataList = new ArrayList<>();
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    commandSender.sendMessage("§2Found region files: " + overworldRegionFiles.size()));

            for (String file : overworldRegionFiles) {
                String name = file;
                String stripped = file.substring(2); // Remove "r." prefix

                int xPos = Integer.parseInt(stripped.substring(0, stripped.indexOf(".")));
                stripped = stripped.substring(stripped.indexOf(".") + 1);
                int zPos = Integer.parseInt(stripped.substring(0, stripped.indexOf(".")));

                regionDataList.add(new RegionData(name, xPos, zPos, "Unchecked"));
            }

            plugin.getServer().getScheduler().runTask(plugin, () ->
                    commandSender.sendMessage("§2Found all regions! Adding regions to database."));

            for (RegionData regionData : regionDataList) {
                try {
                    String sql;
                    if (databaseConnection.getMetaData().getDriverName().equalsIgnoreCase("sqlite jdbc")) {
                        sql = "INSERT OR IGNORE INTO regions (name, x, z, status) VALUES (?, ?, ?, ?)";
                    } else {
                        sql = "INSERT IGNORE INTO regions (name, x, z, status) VALUES (?, ?, ?, ?)";
                    }

                    PreparedStatement ps = databaseConnection.prepareStatement(sql);
                    ps.setString(1, regionData.getName());
                    ps.setInt(2, regionData.getX());
                    ps.setInt(3, regionData.getZ());

                    File regionFile = new File(worldContainer,
                            plugin.getConfig().getString("Earth-World-Name") + "/region/" + regionData.getName());

                    long total = getCombinedInhabitedTime(regionFile, regionData.getName());

                    if (total == 0) {
                        ps.setString(4, "AutoDeleted");
                        if(plugin.getConfig().getBoolean("Testing-Mode")){
                            plugin.getLogger().info("Region " + regionData.getName() + " would be autodeleted!");
                        }else{
                            regionFile.delete();
                        }
                    } else if(total > 0){
                        ps.setString(4, "Unchecked");
                        if(plugin.getConfig().getBoolean("Testing-Mode")){
                            plugin.getLogger().info("Region " + regionData.getName() + " has time " + total);
                        }
                    } else if(total == -1){
                        ps.setString(4, "Error");
                        plugin.getLogger().severe("Unknown region format!");
                    }else{
                        ps.setString(4, "Error");
                        plugin.getLogger().warning("Unknown error in getting total inhabited time!");
                    }
                    ps.executeUpdate();

                } catch (SQLException | IOException e) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            commandSender.sendMessage("§4Error processing region " + regionData.getName() + ", see console."));
                    e.printStackTrace();
                }
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                commandSender.sendMessage("§2Successfully added all regions to the database!");
                commandSender.sendMessage("§2Finished reloading the database!");
            });

            databaseManager.closeDatabase();
        });

        return true;
    }

    private static final int CHUNK_COUNT = 32;
    public static long getCombinedInhabitedTime(File regionFile, String regionName) throws IOException {
        long totalInhabitedTime = 0;

        if (!regionFile.exists()) {
            plugin.getLogger().severe("Region file does not exist: " + regionFile.getAbsolutePath());
            return -1;
        }

        try (RandomAccessFile raf = new RandomAccessFile(regionFile, "r")) {
            byte[] header = new byte[8192];
            raf.readFully(header);

            for (int x = 0; x < CHUNK_COUNT; x++) {
                for (int z = 0; z < CHUNK_COUNT; z++) {
                    int index = 4 * (x + z * CHUNK_COUNT);
                    int offset = ((header[index] & 0xFF) << 16)
                            | ((header[index + 1] & 0xFF) << 8)
                            | (header[index + 2] & 0xFF);
                    int sectors = header[index + 3] & 0xFF;

                    if (offset == 0 || sectors == 0) continue;

                    raf.seek(offset * 4096L);
                    int length = raf.readInt();
                    byte compression = raf.readByte();

                    if (compression != 2) {
                        plugin.getLogger().severe("Unsupported compression type " + compression + " in region " + regionName);
                        return -1;
                    }

                    byte[] chunkData = new byte[length - 1];
                    raf.readFully(chunkData);

                    try (ByteArrayInputStream bais = new ByteArrayInputStream(chunkData);
                         InflaterInputStream is = new InflaterInputStream(bais);
                         NBTInputStream nbtStream = new NBTInputStream(is)) {

                        CompoundTag root = (CompoundTag) nbtStream.readTag();
                        Map<String, Tag> rootMap = root.getValue();

                        long inhabitedTime = 0;

                        if (rootMap.containsKey("InhabitedTime")) {
                            inhabitedTime = ((LongTag) rootMap.get("InhabitedTime")).getValue();
                        } else if (rootMap.containsKey("Level")) {
                            CompoundTag level = (CompoundTag) rootMap.get("Level");
                            Map<String, Tag> levelMap = level.getValue();
                            if (levelMap.containsKey("InhabitedTime")) {
                                inhabitedTime = ((LongTag) levelMap.get("InhabitedTime")).getValue();
                            } else {
                                plugin.getLogger().severe("Chunk missing InhabitedTime in legacy Level tag in region " + regionName);
                                return -1;
                            }
                        } else {
                            plugin.getLogger().severe("Chunk missing Level tag in region " + regionName);
                            return -1;
                        }

                        totalInhabitedTime += inhabitedTime;
                    }
                }
            }
        }
        return totalInhabitedTime;
    }
}