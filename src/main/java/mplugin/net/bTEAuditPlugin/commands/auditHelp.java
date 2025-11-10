package mplugin.net.bTEAuditPlugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class auditHelp implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        commandSender.sendMessage("§6----------------------");
        commandSender.sendMessage("§eGeneral Commands");
        commandSender.sendMessage("§e/regionstats: Used to show the status of all regions");
        commandSender.sendMessage("§e/tptoworld: Used to teleport to the audit world, only used for testing");
        commandSender.sendMessage("§e/audithelp: Display the help messages (this)");
        commandSender.sendMessage("§eAuditor Commands");
        commandSender.sendMessage("§e/nextregion: Teleports to the next region that needs to be reviewed");
        commandSender.sendMessage("§e/markRegion HP/MFD: Used to mark the status of the region currently stood in (providing currently in the audit world)");
        commandSender.sendMessage("§eAdmin Level Commands");
        commandSender.sendMessage("§e/nextdeleteregion: Similar to /nextregion, but will only teleport to regions with 2 (non executor) approvals");
        commandSender.sendMessage("§e/deleteregion yes/no: Used to give approval to delete a region or to reset its status to 'Unchecked'");
        commandSender.sendMessage("§e/reloaddatabase: Used to reload the database with all new regions, VERY LAGGY");
        commandSender.sendMessage("§e/resetdatabase: Used to completely reset the database, only used in extreme scenarios");
        commandSender.sendMessage("§6----------------------");


        return false;
    }
}
