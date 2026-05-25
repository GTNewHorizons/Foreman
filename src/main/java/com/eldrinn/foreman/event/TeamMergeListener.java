package com.eldrinn.foreman.event;

import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.SyncAllTasksPacket;
import com.eldrinn.foreman.storage.ForemanWorldData;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamMergeEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class TeamMergeListener {

    @SubscribeEvent
    public void onTeamMerge(TeamMergeEvent event) {
        ForemanWorldData data = ForemanWorldData.get();
        data.mergeTasks(event.consumed.getTeamId(), event.surviving.getTeamId());
        ForemanNetwork.sendToTeamMembers(
            event.surviving.getMembers(),
            new SyncAllTasksPacket(data.getTeamTasks(event.surviving.getTeamId())));
    }
}
