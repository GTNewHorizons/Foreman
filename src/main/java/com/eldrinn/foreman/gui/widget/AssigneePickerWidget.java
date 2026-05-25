package com.eldrinn.foreman.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.foreman.cache.PlayerEntry;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.gui.ForemanGui;
import com.eldrinn.foreman.gui.ForemanGuiData;
import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.UpdateTaskPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AssigneePickerWidget extends Flow {

    public AssigneePickerWidget(Task task, ForemanGuiData data) {
        super(com.cleanroommc.modularui.api.GuiAxis.Y);
        final int W = ForemanGui.RIGHT_WIDTH - 2 * ForemanGui.PADDING;
        size(W, 20);
        coverChildrenHeight(20);
        for (PlayerEntry player : resolveAvailablePlayers()) {
            boolean assigned = task.assignees.contains(player.id);
            PlayerEntry p = player;
            TextWidget label = new TextWidget((assigned ? "[x] " : "[ ] ") + p.name);
            label.size(W, 20);
            label.alignment(Alignment.CenterLeft);
            label.padding(6, 0, 0, 0);
            child(
                new ButtonWidget<>().size(W, 20)
                    .child(label)
                    .onMousePressed(btn -> {
                        if (btn != 0) return false;
                        if (task.assignees.contains(p.id)) {
                            task.assignees.remove(p.id);
                        } else {
                            task.assignees.add(p.id);
                        }
                        ForemanNetwork.CHANNEL.sendToServer(new UpdateTaskPacket(task));
                        ForemanGui.open(data);
                        return true;
                    }));
        }
    }

    private static List<PlayerEntry> resolveAvailablePlayers() {
        List<PlayerEntry> result = new ArrayList<>();

        // Always include local player first
        EntityPlayer local = Minecraft.getMinecraft().thePlayer;
        UUID localId = local.getGameProfile()
            .getId();
        result.add(new PlayerEntry(localId, local.getCommandSenderName()));

        // Add other online players visible in the tab list
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().thePlayer.sendQueue;
        for (GuiPlayerInfo info : netHandler.playerInfoList) {
            EntityPlayer p = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(info.name);
            if (p != null && !p.getGameProfile()
                .getId()
                .equals(localId)) {
                result.add(
                    new PlayerEntry(
                        p.getGameProfile()
                            .getId(),
                        info.name));
            }
        }
        return result;
    }
}
