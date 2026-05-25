package com.eldrinn.foreman.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.eldrinn.foreman.data.Task;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-side in-memory store. Replaced wholesale on each sync packet.
 * Only safe to access from the client thread.
 */
@SideOnly(Side.CLIENT)
public class ForemanClientCache {

    private static Map<UUID, Task> tasks = new LinkedHashMap<>();

    public static void update(Collection<Task> incoming) {
        tasks.clear();
        for (Task t : incoming) {
            tasks.put(t.id, t);
        }
    }

    public static Collection<Task> getAll() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    @Nullable
    public static Task get(UUID id) {
        return tasks.get(id);
    }
}
