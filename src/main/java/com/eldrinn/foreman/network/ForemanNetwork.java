package com.eldrinn.foreman.network;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.network.base.NetworkChannel;

public class ForemanNetwork {

    public static final NetworkChannel CHANNEL = new NetworkChannel("foreman");

    public static void init() {
        CHANNEL.toClient(new SyncAllTasksPacket());
        CHANNEL.toClient(new OpenGuiPacket());
        CHANNEL.toServer(new CreateTaskPacket());
        CHANNEL.toServer(new UpdateTaskPacket());
        CHANNEL.toServer(new DeleteTaskPacket());
    }

    /**
     * Sends a packet to all online members of the given team.
     * Members who are offline are skipped silently.
     */
    @SuppressWarnings("unchecked")
    public static void sendToTeamMembers(Set<UUID> memberUuids, IPacket packet) {
        List<EntityPlayerMP> online = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : online) {
            if (memberUuids.contains(player.getUniqueID())) {
                CHANNEL.sendTo(packet, player);
            }
        }
    }
}
