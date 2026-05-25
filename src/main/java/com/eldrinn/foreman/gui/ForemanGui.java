package com.eldrinn.foreman.gui;

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

    public static final int WIDTH = 900;
    public static final int HEIGHT = 580;
    public static final int LEFT_WIDTH = 380;
    public static final int RIGHT_WIDTH = 480;

    public static void open() {
        open(new ForemanGuiData());
    }

    public static void open(ForemanGuiData data) {
        ModularPanel panel = ModularPanel.defaultPanel("foreman_main", WIDTH, HEIGHT);

        panel.child(Flow.row()
            .size(WIDTH, HEIGHT)
            .child(new TaskListWidget(data))
            .child(new TaskDetailWidget(data)));

        ClientGUI.open(new ModularScreen("foreman", panel));
    }
}
