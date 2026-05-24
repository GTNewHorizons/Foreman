package com.eldrinn.foreman.network;

import java.io.IOException;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.storage.ForemanWorldData;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

public class CreateTaskPacket implements IPacket {

    private Task task;

    public CreateTaskPacket() {}

    public CreateTaskPacket(Task task) {
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
        if (data.getTask(task.id) != null) {
            // Ignore duplicate — client sent a create for an already-existing ID.
            return null;
        }
        data.addTask(task);
        ForemanNetwork.CHANNEL.sendToAll(new SyncAllTasksPacket(data.getAllTasks()));
        return null;
    }
}
