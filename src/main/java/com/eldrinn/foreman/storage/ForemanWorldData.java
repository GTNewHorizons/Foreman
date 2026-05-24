package com.eldrinn.foreman.storage;

import com.eldrinn.foreman.data.Task;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.*;

public class ForemanWorldData extends WorldSavedData {

    private static final String DATA_NAME = "ForemanTasks";

    private static final org.apache.logging.log4j.Logger LOG =
        org.apache.logging.log4j.LogManager.getLogger("foreman");

    // Insertion-ordered map for deterministic list output.
    private final Map<UUID, Task> tasks = new LinkedHashMap<>();

    public ForemanWorldData() {
        super(DATA_NAME);
    }

    // Required by WorldSavedData — called by MapStorage via reflection.
    @SuppressWarnings("unused")
    public ForemanWorldData(String name) {
        super(name);
    }

    /** Load (or create) the data from the overworld's global map storage. */
    public static ForemanWorldData get() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) throw new IllegalStateException("ForemanWorldData.get() called on client side");
        WorldServer overworld = server.worldServerForDimension(0);
        MapStorage storage = overworld.mapStorage;
        ForemanWorldData data = (ForemanWorldData) storage.loadData(ForemanWorldData.class, DATA_NAME);
        if (data == null) {
            data = new ForemanWorldData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public Collection<Task> getAllTasks() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    @Nullable
    public Task getTask(UUID id) {
        return tasks.get(id);
    }

    public void addTask(Task task) {
        tasks.put(task.id, task);
        markDirty();
    }

    public void updateTask(Task task) {
        tasks.put(task.id, task);
        markDirty();
    }

    /** Returns true if a task with the given id existed and was removed. */
    public boolean deleteTask(UUID id) {
        if (tasks.remove(id) != null) {
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        tasks.clear();
        NBTTagList list = compound.getTagList("tasks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            try {
                Task task = Task.fromNBT(list.getCompoundTagAt(i));
                tasks.put(task.id, task);
            } catch (Exception e) {
                LOG.warn("Skipping corrupt task entry at index {}: {}", i, e.getMessage());
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (Task task : tasks.values()) {
            list.appendTag(task.toNBT());
        }
        compound.setTag("tasks", list);
    }
}
