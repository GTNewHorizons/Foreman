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

    public AssigneePickerWidget(Task task, ForemanGuiData data, int width) {
        super(com.cleanroommc.modularui.api.GuiAxis.Y);
        size(width, 20);
        coverChildrenHeight(20);
        final int HEAD_SIZE = 8;
        final int GAP = 4;
        final int CHECK_W = net.minecraft.client.Minecraft.getMinecraft().fontRenderer.getStringWidth("[x]");
        final int NAME_W = width - GAP - CHECK_W - GAP - HEAD_SIZE - GAP;

        for (PlayerEntry player : resolveAvailablePlayers()) {
            boolean assigned = task.assignees.contains(player.id());

            var checkMark = new TextWidget<>(assigned ? "[x]" : "[ ]");
            checkMark.size(CHECK_W, 20);
            checkMark.textAlign(Alignment.CenterLeft);
            checkMark.marginLeft(GAP);

            PlayerHeadWidget head = new PlayerHeadWidget(player.name());
            head.size(HEAD_SIZE, HEAD_SIZE)
                .marginTop(6)
                .marginLeft(GAP);

            var nameLabel = new TextWidget<>(player.name());
            nameLabel.size(NAME_W, 20);
            nameLabel.textAlign(Alignment.CenterLeft);
            nameLabel.marginLeft(GAP);

            Flow row = Flow.row()
                .size(width, 20);
            row.child(checkMark);
            row.child(head);
            row.child(nameLabel);

            child(
                new ButtonWidget<>().size(width, 20)
                    .child(row)
                    .onMousePressed(btn -> {
                        if (btn != 0) return false;
                        if (task.assignees.contains(player.id())) {
                            task.assignees.remove(player.id());
                        } else {
                            task.assignees.add(player.id());
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
