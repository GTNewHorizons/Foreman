package com.eldrinn.foreman.command;

import java.util.Collection;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.SyncAllTasksPacket;
import com.eldrinn.foreman.storage.ForemanWorldData;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

public class ForemanCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "foreman";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/foreman <list|reload|gui>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // subcommand-level checks handle OP restriction for reload
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(args, "list", "reload", "gui");
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }

        ForemanWorldData data = ForemanWorldData.get();

        switch (args[0]) {
            case "list": {
                Collection<Task> tasks = getSenderTasks(sender, data);
                if (tasks == null) {
                    sender.addChatMessage(new ChatComponentText("You are not in a team."));
                    return;
                }
                if (tasks.isEmpty()) {
                    sender.addChatMessage(new ChatComponentText("No tasks."));
                } else {
                    for (Task t : tasks) {
                        String shortId = t.id.toString()
                            .substring(0, 8);
                        sender.addChatMessage(
                            new ChatComponentText(String.format("[%s] %s (%s)", shortId, t.title, t.status.name())));
                    }
                }
                break;
            }
            case "reload": {
                if (!isOp(sender)) {
                    sender.addChatMessage(new ChatComponentText("You need to be OP to use this command."));
                    return;
                }
                // Re-sends each online player their team's current task list.
                @SuppressWarnings("unchecked")
                List<EntityPlayerMP> online = MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList;
                for (EntityPlayerMP player : online) {
                    Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
                    if (team == null) continue;
                    ForemanNetwork.CHANNEL.sendTo(new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())), player);
                }
                sender.addChatMessage(new ChatComponentText("Synced tasks to all online players."));
                break;
            }
            case "gui": {
                if (sender instanceof EntityPlayerMP) {
                    ForemanNetwork.CHANNEL
                        .sendTo(new com.eldrinn.foreman.network.OpenGuiPacket(), (EntityPlayerMP) sender);
                } else {
                    sender.addChatMessage(new ChatComponentText("GUI is client-only."));
                }
                break;
            }
            default:
                sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
        }
    }

    /** Returns the sender's team tasks, or null if sender is not a player / has no team. */
    private Collection<Task> getSenderTasks(ICommandSender sender, ForemanWorldData data) {
        if (!(sender instanceof EntityPlayerMP)) return null;
        Team team = TeamManager.getTeamByPlayer(((EntityPlayerMP) sender).getUniqueID());
        if (team == null) return null;
        return data.getTeamTasks(team.getTeamId());
    }

    /** Console sender is always considered OP. */
    private boolean isOp(ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP)) return true;
        EntityPlayerMP player = (EntityPlayerMP) sender;
        return MinecraftServer.getServer()
            .getConfigurationManager()
            .func_152596_g(player.getGameProfile());
    }
}
