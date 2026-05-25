package com.eldrinn.foreman.cache;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.network.PacketBuffer;

public class PlayerEntry {

    public final UUID id;
    public final String name;

    public PlayerEntry(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public void writeToBuf(PacketBuffer buf) throws IOException {
        buf.writeLong(id.getMostSignificantBits());
        buf.writeLong(id.getLeastSignificantBits());
        buf.writeStringToBuffer(name);
    }

    public static PlayerEntry readFromBuf(PacketBuffer buf) throws IOException {
        UUID id = new UUID(buf.readLong(), buf.readLong());
        String name = buf.readStringFromBuffer(64);
        return new PlayerEntry(id, name);
    }
}
