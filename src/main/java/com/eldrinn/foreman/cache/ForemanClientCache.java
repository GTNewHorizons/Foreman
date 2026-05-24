package com.eldrinn.foreman.cache;

import com.eldrinn.foreman.data.Task;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Client-side in-memory task store. Replaced wholesale on each SyncAllTasksPacket.
 * Only safe to access from the client thread.
 */
public class ForemanClientCache {

    private static Map<UUID, Task> tasks = new LinkedHashMap<>();

    public static void update(List<Task> incoming) {
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
