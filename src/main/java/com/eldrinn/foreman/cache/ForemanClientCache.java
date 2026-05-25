package com.eldrinn.foreman.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
    private static List<PlayerEntry> teamMembers = new ArrayList<>();

    public static void update(Collection<Task> incoming) {
        tasks.clear();
        for (Task t : incoming) {
            tasks.put(t.id, t);
        }
    }

    public static void updateTeamMembers(List<PlayerEntry> incoming) {
        teamMembers = new ArrayList<>(incoming);
    }

    public static Collection<Task> getAll() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    public static List<PlayerEntry> getTeamMembers() {
        return Collections.unmodifiableList(teamMembers);
    }

    @Nullable
    public static Task get(UUID id) {
        return tasks.get(id);
    }
}
