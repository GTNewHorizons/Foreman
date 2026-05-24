package com.eldrinn.foreman.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;

public class Comment {

    public String author; // player name at time of writing (display only)
    public long timestamp;
    public String text;

    public Comment(String author, long timestamp, String text) {
        this.author = author;
        this.timestamp = timestamp;
        this.text = text;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("author", author);
        tag.setLong("timestamp", timestamp);
        tag.setString("text", text);
        return tag;
    }

    public static Comment fromNBT(NBTTagCompound tag) {
        return new Comment(
            tag.getString("author"),
            tag.getLong("timestamp"),
            tag.getString("text")
        );
    }

    public void writeToBuf(PacketBuffer buf) throws IOException {
        buf.writeString(author);
        buf.writeLong(timestamp);
        buf.writeString(text);
    }

    public static Comment readFromBuf(PacketBuffer buf) throws IOException {
        return new Comment(
            buf.readStringFromBuffer(64),
            buf.readLong(),
            buf.readStringFromBuffer(2048)
        );
    }
}
