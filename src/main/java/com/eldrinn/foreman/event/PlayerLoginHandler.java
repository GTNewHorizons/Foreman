package com.eldrinn.foreman.event;

import net.minecraft.entity.player.EntityPlayerMP;

import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.SyncAllTasksPacket;
import com.eldrinn.foreman.storage.ForemanWorldData;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class PlayerLoginHandler {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return; // GTNHTeams hasn't initialized yet — rare edge case

        ForemanWorldData data = ForemanWorldData.get();
        ForemanNetwork.CHANNEL.sendTo(new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())), player);
    }
}
