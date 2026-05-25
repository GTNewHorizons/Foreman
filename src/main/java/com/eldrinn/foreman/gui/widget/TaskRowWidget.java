package com.eldrinn.foreman.gui.widget;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.eldrinn.foreman.cache.ForemanClientCache;
import com.eldrinn.foreman.cache.PlayerEntry;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.data.TaskStatus;
import com.eldrinn.foreman.gui.ForemanGui;
import com.eldrinn.foreman.gui.ForemanGuiData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskRowWidget extends ButtonWidget<TaskRowWidget> {

    public TaskRowWidget(Task task, ForemanGuiData data) {
        size(LEFT_WIDTH - 8, 20);
        child(new TextWidget(buildLabel(task)));
        onMousePressed(mouseButton -> {
            if (mouseButton != 0) return false;
            data.selectTask(task.id);
            ForemanGui.open(data);
            return true;
        });
    }

    private static final int LEFT_WIDTH = com.eldrinn.foreman.gui.ForemanGui.LEFT_WIDTH;

    private static String buildLabel(Task task) {
        return statusIcon(task.status) + " " + truncate(task.title, 28) + "  " + buildAssigneeText(task);
    }

    private static String statusIcon(TaskStatus status) {
        switch (status) {
            case OPEN:        return "*";
            case IN_PROGRESS: return "~";
            case DONE:        return "+";
            default:          return "?";
        }
    }

    private static String buildAssigneeText(Task task) {
        if (task.assignees.isEmpty()) return "";
        List<PlayerEntry> members = ForemanClientCache.getTeamMembers();
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (UUID uuid : task.assignees) {
            if (shown >= 2) {
                sb.append(" +").append(task.assignees.size() - 2).append(" more");
                break;
            }
            String name = resolveName(uuid, members);
            if (sb.length() > 0) sb.append(" ");
            sb.append("[").append(name).append("]");
            shown++;
        }
        return sb.toString();
    }

    private static String resolveName(UUID uuid, List<PlayerEntry> members) {
        for (PlayerEntry e : members) {
            if (e.id.equals(uuid)) return e.name;
        }
        NetworkPlayerInfo info = Minecraft.getMinecraft().getNetHandler().getPlayerInfo(uuid);
        return info != null ? info.getGameProfile().getName() : uuid.toString().substring(0, 8);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}
