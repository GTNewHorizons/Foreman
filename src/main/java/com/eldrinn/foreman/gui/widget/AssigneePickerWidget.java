package com.eldrinn.foreman.gui.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;

import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.foreman.cache.PlayerEntry;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.gui.ForemanGui;
import com.eldrinn.foreman.gui.ForemanGuiData;
import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.UpdateTaskPacket;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManagerClient;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AssigneePickerWidget extends ListWidget<Flow, AssigneePickerWidget> {

    public AssigneePickerWidget(Task task, ForemanGuiData data) {
        final int W = ForemanGui.RIGHT_WIDTH - 2 * ForemanGui.PADDING;
        size(W, 18);
        coverChildrenHeight(18);
        List<PlayerEntry> players = resolveAvailablePlayers();
        for (PlayerEntry player : players) {
            boolean assigned = task.assignees.contains(player.id);
            PlayerEntry p = player;
            TextWidget assigneeLabel = new TextWidget((assigned ? "[x] " : "[ ] ") + p.name);
            assigneeLabel.size(W, 16);
            assigneeLabel.alignment(Alignment.CenterLeft);
            child(
                Flow.row()
                    .size(W, 18)
                    .child(
                        new ButtonWidget<>().size(W, 16)
                            .child(assigneeLabel)
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
                            })));
        }
    }

    private static List<PlayerEntry> resolveAvailablePlayers() {
        Team team = TeamManagerClient.GetTeam();
        if (team == null) return new ArrayList<>();

        // Build UUID -> name map from the online player list
        Map<UUID, String> onlineNames = new HashMap<>();
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().thePlayer.sendQueue;
        for (GuiPlayerInfo info : netHandler.playerInfoList) {
            net.minecraft.entity.player.EntityPlayer p = Minecraft.getMinecraft().theWorld
                .getPlayerEntityByName(info.name);
            if (p != null) {
                onlineNames.put(
                    p.getGameProfile()
                        .getId(),
                    info.name);
            }
        }

        List<PlayerEntry> result = new ArrayList<>();
        for (UUID uuid : team.getMembers()) {
            String name = onlineNames.getOrDefault(
                uuid,
                uuid.toString()
                    .substring(0, 8));
            result.add(new PlayerEntry(uuid, name));
        }
        return result;
    }
}
