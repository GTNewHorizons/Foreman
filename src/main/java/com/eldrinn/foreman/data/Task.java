package com.eldrinn.foreman.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Task {

    public UUID id;
    public String title;
    public String description;
    public TaskStatus status;
    public List<UUID> assignees; // UUIDs, display names resolved on client
    @Nullable public TaskLocation location;
    public List<Subtask> subtasks;
    public List<Comment> comments; // soft limit enforced on add: max 50

    public Task(UUID id, String title, String description, TaskStatus status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignees = new ArrayList<>();
        this.location = null;
        this.subtasks = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("idMost", id.getMostSignificantBits());
        tag.setLong("idLeast", id.getLeastSignificantBits());
        tag.setString("title", title);
        tag.setString("description", description);
        tag.setString("status", status.name());

        NBTTagList assigneeList = new NBTTagList();
        for (UUID uuid : assignees) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong("most", uuid.getMostSignificantBits());
            entry.setLong("least", uuid.getLeastSignificantBits());
            assigneeList.appendTag(entry);
        }
        tag.setTag("assignees", assigneeList);

        tag.setBoolean("hasLocation", location != null);
        if (location != null) tag.setTag("location", location.toNBT());

        NBTTagList subtaskList = new NBTTagList();
        for (Subtask s : subtasks) subtaskList.appendTag(s.toNBT());
        tag.setTag("subtasks", subtaskList);

        NBTTagList commentList = new NBTTagList();
        for (Comment c : comments) commentList.appendTag(c.toNBT());
        tag.setTag("comments", commentList);

        return tag;
    }

    public static Task fromNBT(NBTTagCompound tag) {
        Task task = new Task(
            new UUID(tag.getLong("idMost"), tag.getLong("idLeast")),
            tag.getString("title"),
            tag.getString("description"),
            TaskStatus.fromNBT(tag.getString("status"))
        );

        NBTTagList assigneeList = tag.getTagList("assignees", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < assigneeList.tagCount(); i++) {
            NBTTagCompound entry = assigneeList.getCompoundTagAt(i);
            task.assignees.add(new UUID(entry.getLong("most"), entry.getLong("least")));
        }

        if (tag.getBoolean("hasLocation")) {
            task.location = TaskLocation.fromNBT(tag.getCompoundTag("location"));
        }

        NBTTagList subtaskList = tag.getTagList("subtasks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < subtaskList.tagCount(); i++) {
            task.subtasks.add(Subtask.fromNBT(subtaskList.getCompoundTagAt(i)));
        }

        NBTTagList commentList = tag.getTagList("comments", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < commentList.tagCount(); i++) {
            task.comments.add(Comment.fromNBT(commentList.getCompoundTagAt(i)));
        }

        return task;
    }

    public void writeToBuf(PacketBuffer buf) throws IOException {
        buf.writeLong(id.getMostSignificantBits());
        buf.writeLong(id.getLeastSignificantBits());
        buf.writeString(title);
        buf.writeString(description);
        buf.writeInt(status.ordinal());

        buf.writeInt(assignees.size());
        for (UUID uuid : assignees) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }

        buf.writeBoolean(location != null);
        if (location != null) location.writeToBuf(buf);

        buf.writeInt(subtasks.size());
        for (Subtask s : subtasks) s.writeToBuf(buf);

        buf.writeInt(comments.size());
        for (Comment c : comments) c.writeToBuf(buf);
    }

    public static Task readFromBuf(PacketBuffer buf) throws IOException {
        UUID id = new UUID(buf.readLong(), buf.readLong());
        String title = buf.readStringFromBuffer(256);
        String description = buf.readStringFromBuffer(4096);
        int ordinal = buf.readInt();
        TaskStatus[] statuses = TaskStatus.values();
        if (ordinal < 0 || ordinal >= statuses.length) throw new IOException("Invalid TaskStatus ordinal: " + ordinal);
        TaskStatus status = statuses[ordinal];

        Task task = new Task(id, title, description, status);

        int assigneeCount = buf.readInt();
        for (int i = 0; i < assigneeCount; i++) {
            task.assignees.add(new UUID(buf.readLong(), buf.readLong()));
        }

        if (buf.readBoolean()) {
            task.location = TaskLocation.readFromBuf(buf);
        }

        int subtaskCount = buf.readInt();
        for (int i = 0; i < subtaskCount; i++) {
            task.subtasks.add(Subtask.readFromBuf(buf));
        }

        int commentCount = buf.readInt();
        for (int i = 0; i < commentCount; i++) {
            task.comments.add(Comment.readFromBuf(buf));
        }

        return task;
    }
}
