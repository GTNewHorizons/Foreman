package com.eldrinn.foreman.network;

import com.gtnewhorizon.gtnhlib.network.base.NetworkChannel;

public class ForemanNetwork {

    public static final NetworkChannel CHANNEL = new NetworkChannel("foreman");

    public static void init() {
        CHANNEL.toClient(new SyncAllTasksPacket());
        CHANNEL.toClient(new SyncTeamMembersPacket());
        CHANNEL.toServer(new CreateTaskPacket());
        CHANNEL.toServer(new UpdateTaskPacket());
        CHANNEL.toServer(new DeleteTaskPacket());
    }
}
