package com.eldrinn.foreman.gui.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.foreman.cache.ForemanClientCache;
import com.eldrinn.foreman.cache.PlayerEntry;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.gui.ForemanGui;
import com.eldrinn.foreman.gui.ForemanGuiData;
import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.UpdateTaskPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AssigneePickerWidget extends ListWidget<Flow, AssigneePickerWidget> {

    public AssigneePickerWidget(Task task, ForemanGuiData data) {
        size(ForemanGui.RIGHT_WIDTH - 8, 100);
        List<PlayerEntry> players = resolveAvailablePlayers();
        for (PlayerEntry player : players) {
            boolean assigned = task.assignees.contains(player.id);
            PlayerEntry p = player;
            child(Flow.row()
                .size(ForemanGui.RIGHT_WIDTH - 12, 18)
                .child(new ButtonWidget<>()
                    .size(ForemanGui.RIGHT_WIDTH - 12, 16)
                    .child(new TextWidget((assigned ? "[x] " : "[ ] ") + p.name))
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
        List<PlayerEntry> members = ForemanClientCache.getTeamMembers();
        if (!members.isEmpty()) return members;
        List<PlayerEntry> online = new ArrayList<>();
        Collection<NetworkPlayerInfo> infos = Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap();
        for (NetworkPlayerInfo info : infos) {
            online.add(new PlayerEntry(info.getGameProfile().getId(), info.getGameProfile().getName()));
        }
        return online;
    }
}
