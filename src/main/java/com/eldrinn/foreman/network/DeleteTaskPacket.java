package com.eldrinn.foreman.network;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.foreman.storage.ForemanWorldData;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

public class DeleteTaskPacket implements IPacket {

    private UUID taskId;

    public DeleteTaskPacket() {}

    public DeleteTaskPacket(UUID taskId) {
        this.taskId = taskId;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeLong(taskId.getMostSignificantBits());
        buf.writeLong(taskId.getLeastSignificantBits());
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        taskId = new UUID(buf.readLong(), buf.readLong());
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        EntityPlayerMP sender = handler.playerEntity;
        Team team = TeamManager.getTeamByPlayer(sender.getUniqueID());
        if (team == null) return null;

        ForemanWorldData data = ForemanWorldData.get();
        data.deleteTask(team.getTeamId(), taskId);
        ForemanNetwork
            .sendToTeamMembers(team.getMembers(), new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
        return null;
    }
}
