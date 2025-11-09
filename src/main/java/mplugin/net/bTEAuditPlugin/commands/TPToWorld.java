package mplugin.net.bTEAuditPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class TPToWorld implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        Player player = (Player) commandSender;

        World voidWorld = Bukkit.getWorld("audit_world");
        assert voidWorld != null;
        player.teleport(voidWorld.getSpawnLocation());

        return false;
    }
}