package com.eldrinn.foreman.gui.widget;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;

import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.data.TaskStatus;
import com.eldrinn.foreman.gui.ForemanGui;
import com.eldrinn.foreman.gui.ForemanGuiData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskRowWidget extends ToggleButton {

    public TaskRowWidget(Task task, ForemanGuiData data) {
        size(ROW_WIDTH, 20);

        TextWidget normalLabel = new TextWidget(buildLabel(task));
        normalLabel.size(ROW_WIDTH, 20);
        normalLabel.alignment(Alignment.CenterLeft);
        normalLabel.padding(4, 0, 0, 0);

        TextWidget activeLabel = new TextWidget(buildLabel(task));
        activeLabel.size(ROW_WIDTH, 20);
        activeLabel.alignment(Alignment.CenterLeft);
        activeLabel.padding(4, 0, 0, 0);
        activeLabel.color(0xFFFFFF);

        value(new BoolValue.Dynamic(() -> task.id.equals(data.selectedTaskId), selected -> {
            if (selected) {
                data.selectTask(task.id);
                ForemanGui.open(data);
            }
        }));
        child(false, normalLabel);
        child(true, activeLabel);
    }

    private static final int LEFT_WIDTH = com.eldrinn.foreman.gui.ForemanGui.LEFT_WIDTH;
    private static final int ROW_WIDTH = LEFT_WIDTH - 2 * com.eldrinn.foreman.gui.ForemanGui.PADDING;

    private static String buildLabel(Task task) {
        return statusIcon(task.status) + " " + truncate(task.title, 28) + "  " + buildAssigneeText(task);
    }

    private static String statusIcon(TaskStatus status) {
        switch (status) {
            case OPEN:
                return "○"; // ○
            case IN_PROGRESS:
                return "~";
            case DONE:
                return "✔"; // ✔
            default:
                return "?";
        }
    }

    private static String buildAssigneeText(Task task) {
        if (task.assignees.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (UUID uuid : task.assignees) {
            if (shown >= 2) {
                sb.append(
                    String.format(
                        net.minecraft.util.StatCollector.translateToLocal("foreman.gui.row.more"),
                        task.assignees.size() - 2));
                break;
            }
            String name = resolveName(uuid);
            if (sb.length() > 0) sb.append(" ");
            sb.append("[")
                .append(name)
                .append("]");
            shown++;
        }
        return sb.toString();
    }

    private static String resolveName(UUID uuid) {
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().thePlayer.sendQueue;
        for (GuiPlayerInfo info : netHandler.playerInfoList) {
            net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().theWorld
                .getPlayerEntityByName(info.name);
            if (player != null && uuid.equals(
                player.getGameProfile()
                    .getId()))
                return info.name;
        }
        return uuid.toString()
            .substring(0, 8);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}
