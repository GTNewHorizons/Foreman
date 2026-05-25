package com.eldrinn.foreman.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.data.TaskStatus;
import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.SyncAllTasksPacket;
import com.eldrinn.foreman.storage.ForemanWorldData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

public class ForemanCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "foreman";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/foreman <list|reload|gui|create <title>|assign <id> <player>|unassign <id> <player>|done <id>|export [name]|import <name>>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // subcommand-level checks handle OP restriction for reload
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(
                args,
                "list",
                "reload",
                "gui",
                "create",
                "assign",
                "unassign",
                "done",
                "export",
                "import");
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
            case "create": {
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("Usage: /foreman create <title>"));
                    return;
                }
                if (!(sender instanceof EntityPlayerMP)) {
                    sender.addChatMessage(new ChatComponentText("Must be run by a player."));
                    return;
                }
                EntityPlayerMP player = (EntityPlayerMP) sender;
                Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
                if (team == null) {
                    sender.addChatMessage(new ChatComponentText("You are not in a team."));
                    return;
                }
                String title = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Task task = new Task(UUID.randomUUID(), title, "", TaskStatus.OPEN);
                data.addTask(team.getTeamId(), task);
                ForemanNetwork
                    .sendToTeamMembers(team.getMembers(), new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
                sender.addChatMessage(
                    new ChatComponentText(
                        "Created task [" + task.id.toString()
                            .substring(0, 8) + "] " + title));
                break;
            }
            case "assign": {
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentText("Usage: /foreman assign <id> <player>"));
                    return;
                }
                if (!(sender instanceof EntityPlayerMP)) {
                    sender.addChatMessage(new ChatComponentText("Must be run by a player."));
                    return;
                }
                Task task = findTaskByShortId(data, sender, args[1]);
                if (task == null) return;
                Team senderTeam = TeamManager.getTeamByPlayer(((EntityPlayerMP) sender).getUniqueID());
                if (senderTeam == null) return;
                EntityPlayerMP target = CommandBase.getPlayer(sender, args[2]);
                if (!task.assignees.contains(target.getUniqueID())) {
                    task.assignees.add(target.getUniqueID());
                    data.updateTask(senderTeam.getTeamId(), task);
                    ForemanNetwork.sendToTeamMembers(
                        senderTeam.getMembers(),
                        new SyncAllTasksPacket(data.getTeamTasks(senderTeam.getTeamId())));
                    sender.addChatMessage(new ChatComponentText("Assigned " + args[2] + " to " + task.title));
                    target.addChatMessage(new ChatComponentTranslation("foreman.chat.assigned", task.title));
                }
                break;
            }
            case "unassign": {
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentText("Usage: /foreman unassign <id> <player>"));
                    return;
                }
                if (!(sender instanceof EntityPlayerMP)) {
                    sender.addChatMessage(new ChatComponentText("Must be run by a player."));
                    return;
                }
                Task task = findTaskByShortId(data, sender, args[1]);
                if (task == null) return;
                Team senderTeam = TeamManager.getTeamByPlayer(((EntityPlayerMP) sender).getUniqueID());
                if (senderTeam == null) return;
                EntityPlayerMP target = CommandBase.getPlayer(sender, args[2]);
                task.assignees.remove(target.getUniqueID());
                data.updateTask(senderTeam.getTeamId(), task);
                ForemanNetwork.sendToTeamMembers(
                    senderTeam.getMembers(),
                    new SyncAllTasksPacket(data.getTeamTasks(senderTeam.getTeamId())));
                sender.addChatMessage(new ChatComponentText("Unassigned " + args[2] + " from " + task.title));
                break;
            }
            case "done": {
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("Usage: /foreman done <id>"));
                    return;
                }
                Task task = findTaskByShortId(data, sender, args[1]);
                if (task == null) return;
                task.status = TaskStatus.DONE;
                if (sender instanceof EntityPlayerMP) {
                    Team team = TeamManager.getTeamByPlayer(((EntityPlayerMP) sender).getUniqueID());
                    if (team != null) {
                        data.updateTask(team.getTeamId(), task);
                        ForemanNetwork.sendToTeamMembers(
                            team.getMembers(),
                            new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
                    }
                }
                sender.addChatMessage(new ChatComponentText("Marked done: " + task.title));
                break;
            }
            case "export": {
                Collection<Task> tasks = getSenderTasks(sender, data);
                if (tasks == null) {
                    sender.addChatMessage(new ChatComponentText("You are not in a team."));
                    return;
                }
                Gson gson = new GsonBuilder().setPrettyPrinting()
                    .create();
                JsonArray arr = new JsonArray();
                for (Task t : tasks) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", t.id.toString());
                    obj.addProperty("title", t.title);
                    obj.addProperty("description", t.description);
                    obj.addProperty("status", t.status.name());
                    if (t.iconItem != null) obj.addProperty("iconItem", t.iconItem);
                    obj.addProperty("showOnMap", t.showOnMap);
                    if (t.location != null) {
                        JsonObject loc = new JsonObject();
                        loc.addProperty("x", t.location.x);
                        loc.addProperty("y", t.location.y);
                        loc.addProperty("z", t.location.z);
                        loc.addProperty("dimension", t.location.dimension);
                        loc.addProperty("label", t.location.label);
                        obj.add("location", loc);
                    }
                    JsonArray subtasks = new JsonArray();
                    for (com.eldrinn.foreman.data.Subtask s : t.subtasks) {
                        JsonObject so = new JsonObject();
                        so.addProperty("title", s.title);
                        so.addProperty("checked", s.checked);
                        subtasks.add(so);
                    }
                    obj.add("subtasks", subtasks);
                    arr.add(obj);
                }
                File dir = new File(
                    MinecraftServer.getServer()
                        .getEntityWorld()
                        .getSaveHandler()
                        .getWorldDirectory(),
                    "foreman");
                dir.mkdirs();
                String filename = args.length >= 2 ? args[1] : "export";
                File out = new File(dir, filename + ".json");
                try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
                    gson.toJson(arr, w);
                    sender.addChatMessage(
                        new ChatComponentText("Exported " + tasks.size() + " tasks to " + out.getName()));
                } catch (IOException e) {
                    sender.addChatMessage(new ChatComponentText("Export failed: " + e.getMessage()));
                }
                break;
            }
            case "import": {
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("Usage: /foreman import <filename>"));
                    return;
                }
                if (!(sender instanceof EntityPlayerMP)) {
                    sender.addChatMessage(new ChatComponentText("Must be run by a player."));
                    return;
                }
                EntityPlayerMP player = (EntityPlayerMP) sender;
                Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
                if (team == null) {
                    sender.addChatMessage(new ChatComponentText("You are not in a team."));
                    return;
                }
                File dir = new File(
                    MinecraftServer.getServer()
                        .getEntityWorld()
                        .getSaveHandler()
                        .getWorldDirectory(),
                    "foreman");
                File in = new File(dir, args[1] + ".json");
                if (!in.exists()) {
                    sender.addChatMessage(new ChatComponentText("File not found: " + in.getName()));
                    return;
                }
                try (Reader r = new InputStreamReader(new FileInputStream(in), StandardCharsets.UTF_8)) {
                    JsonArray arr = new Gson().fromJson(r, JsonArray.class);
                    int count = 0;
                    for (JsonElement el : arr) {
                        JsonObject obj = el.getAsJsonObject();
                        Task t = new Task(
                            UUID.randomUUID(),
                            obj.get("title")
                                .getAsString(),
                            obj.has("description") ? obj.get("description")
                                .getAsString() : "",
                            TaskStatus.valueOf(
                                obj.get("status")
                                    .getAsString()));
                        if (obj.has("iconItem")) t.iconItem = obj.get("iconItem")
                            .getAsString();
                        if (obj.has("showOnMap")) t.showOnMap = obj.get("showOnMap")
                            .getAsBoolean();
                        if (obj.has("location")) {
                            JsonObject loc = obj.getAsJsonObject("location");
                            t.location = new com.eldrinn.foreman.data.TaskLocation(
                                loc.get("x")
                                    .getAsInt(),
                                loc.get("y")
                                    .getAsInt(),
                                loc.get("z")
                                    .getAsInt(),
                                loc.get("dimension")
                                    .getAsInt(),
                                loc.has("label") ? loc.get("label")
                                    .getAsString() : "");
                        }
                        if (obj.has("subtasks")) {
                            for (JsonElement se : obj.getAsJsonArray("subtasks")) {
                                JsonObject so = se.getAsJsonObject();
                                t.subtasks.add(
                                    new com.eldrinn.foreman.data.Subtask(
                                        UUID.randomUUID(),
                                        so.get("title")
                                            .getAsString(),
                                        so.has("checked") && so.get("checked")
                                            .getAsBoolean()));
                            }
                        }
                        data.addTask(team.getTeamId(), t);
                        count++;
                    }
                    ForemanNetwork.sendToTeamMembers(
                        team.getMembers(),
                        new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
                    sender.addChatMessage(new ChatComponentText("Imported " + count + " tasks."));
                } catch (Exception e) {
                    sender.addChatMessage(new ChatComponentText("Import failed: " + e.getMessage()));
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

    /** Finds a task by the first 8 chars of its UUID. Sends error to sender if not found. */
    private Task findTaskByShortId(ForemanWorldData data, ICommandSender sender, String shortId) {
        Collection<Task> all = getSenderTasks(sender, data);
        if (all == null) {
            sender.addChatMessage(new ChatComponentText("You are not in a team."));
            return null;
        }
        for (Task t : all) {
            if (t.id.toString()
                .startsWith(shortId)) return t;
        }
        sender.addChatMessage(new ChatComponentText("Task not found: " + shortId));
        return null;
    }
}
