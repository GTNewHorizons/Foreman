package com.eldrinn.foreman.network;

import com.eldrinn.foreman.storage.ForemanWorldData;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;
import java.util.UUID;

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
        ForemanWorldData data = ForemanWorldData.get();
        data.deleteTask(taskId);
        ForemanNetwork.CHANNEL.sendToAll(new SyncAllTasksPacket(data.getAllTasks()));
        return null;
    }
}
