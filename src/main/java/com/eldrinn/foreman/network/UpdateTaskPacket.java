package com.eldrinn.foreman.network;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.storage.ForemanWorldData;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

public class UpdateTaskPacket implements IPacket {

    private Task task;

    public UpdateTaskPacket() {}

    public UpdateTaskPacket(Task task) {
        this.task = task;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        task.writeToBuf(buf);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        task = Task.readFromBuf(buf);
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        EntityPlayerMP sender = handler.playerEntity;
        Team team = TeamManager.getTeamByPlayer(sender.getUniqueID());
        if (team == null) return null;

        ForemanWorldData data = ForemanWorldData.get();
        if (data.getTask(team.getTeamId(), task.id) == null) return null; // unknown task
        data.updateTask(team.getTeamId(), task);
        ForemanNetwork
            .sendToTeamMembers(team.getMembers(), new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
        return null;
    }
}
