package com.eldrinn.foreman.gui;

import net.minecraft.client.Minecraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.foreman.gui.widget.TaskDetailWidget;
import com.eldrinn.foreman.gui.widget.TaskListWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ForemanGui {

    private static final Logger LOG = LogManager.getLogger("ForemanGui");

    public static final int WIDTH = 900;
    public static final int HEIGHT = 580;
    public static final int LEFT_WIDTH = 380;
    public static final int RIGHT_WIDTH = 480;
    public static final int PADDING = 6;

    private static volatile ForemanGuiData pendingOpen = null;
    /** Last data used to open the GUI — used to refresh on server sync. */
    private static ForemanGuiData activeData = null;

    public static void open() {
        open(new ForemanGuiData());
    }

    public static void open(ForemanGuiData data) {
        LOG.info(
            "[ForemanGui] open() queued — tab={} selected={} createMode={}",
            data.activeTab,
            data.selectedTaskId,
            data.createMode);
        pendingOpen = data;
    }

    /**
     * Called by SyncAllTasksPacket after cache is updated.
     * If the Foreman GUI is currently open, schedules a rebuild with fresh data.
     */
    @SideOnly(Side.CLIENT)
    public static void notifySyncReceived() {
        if (activeData == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof IMuiScreen)) {
            activeData = null; // GUI was closed
            return;
        }
        if (pendingOpen == null) { // don't overwrite a user-triggered open
            open(activeData);
        }
    }

    /** Called from ClientProxy.onClientTick to drain pending open request. */
    public static void tick() {
        ForemanGuiData data = pendingOpen;
        if (data == null) return;
        pendingOpen = null;
        LOG.info("[ForemanGui] tick() draining — tab={}", data.activeTab);
        try {
            LOG.info("[ForemanGui] tick() building panel");
            ModularPanel panel = ModularPanel.defaultPanel("foreman_main", WIDTH, HEIGHT);
            LOG.info("[ForemanGui] tick() building widgets");
            panel.child(
                Flow.row()
                    .size(WIDTH, HEIGHT)
                    .child(new TaskListWidget(data))
                    .child(new TaskDetailWidget(data)));
            LOG.info("[ForemanGui] tick() calling ClientGUI.open()");
            ClientGUI.open(new ModularScreen("foreman", panel));
            activeData = data;
            LOG.info("[ForemanGui] tick() done");
        } catch (Exception e) {
            LOG.error("[ForemanGui] tick() FAILED", e);
        }
    }
}
