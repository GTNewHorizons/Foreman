package com.eldrinn.foreman.network;

import java.io.IOException;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.storage.ForemanWorldData;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

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
        ForemanWorldData data = ForemanWorldData.get();
        if (data.getTask(task.id) == null) {
            // Ignore update for non-existent task.
            return null;
        }
        data.updateTask(task);
        ForemanNetwork.CHANNEL.sendToAll(new SyncAllTasksPacket(data.getAllTasks()));
        return null;
    }
}
