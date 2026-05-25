package com.eldrinn.foreman.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.foreman.cache.ForemanClientCache;
import com.eldrinn.foreman.cache.PlayerEntry;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SyncTeamMembersPacket implements IPacket {

    private List<PlayerEntry> members = new ArrayList<>();

    public SyncTeamMembersPacket() {}

    public SyncTeamMembersPacket(List<PlayerEntry> members) {
        this.members = members;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(members.size());
        for (PlayerEntry e : members) {
            e.writeToBuf(buf);
        }
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        int count = buf.readInt();
        if (count < 0 || count > 200) throw new IOException("Invalid member count: " + count);
        members = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            members.add(PlayerEntry.readFromBuf(buf));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        ForemanClientCache.updateTeamMembers(members);
        return null;
    }
}
