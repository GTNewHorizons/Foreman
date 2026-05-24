package com.eldrinn.foreman.command;

import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.SyncAllTasksPacket;
import com.eldrinn.foreman.storage.ForemanWorldData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import java.util.Collection;

public class ForemanCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "foreman";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/foreman <list|reload>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // subcommand-level checks handle OP restriction for reload
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
                Collection<Task> all = data.getAllTasks();
                if (all.isEmpty()) {
                    sender.addChatMessage(new ChatComponentText("No tasks."));
                } else {
                    for (Task t : all) {
                        // Show shortened UUID (first 8 chars) for readability.
                        String shortId = t.id.toString().substring(0, 8);
                        sender.addChatMessage(new ChatComponentText(
                            String.format("[%s] %s (%s)", shortId, t.title, t.status.name())
                        ));
                    }
                }
                break;
            }
            case "reload": {
                if (!isOp(sender)) {
                    sender.addChatMessage(new ChatComponentText("You need to be OP to use this command."));
                    return;
                }
                // Syncs current in-memory state to all connected clients.
                ForemanNetwork.CHANNEL.sendToAll(new SyncAllTasksPacket(data.getAllTasks()));
                sender.addChatMessage(new ChatComponentText(
                    "Synced " + data.getAllTasks().size() + " tasks to all players."
                ));
                break;
            }
            default:
                sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
        }
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
