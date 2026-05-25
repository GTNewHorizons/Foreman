package com.eldrinn.foreman.gui.widget;

import java.util.Collection;

import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.foreman.cache.ForemanClientCache;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.data.TaskStatus;
import com.eldrinn.foreman.gui.ForemanGui;
import com.eldrinn.foreman.gui.ForemanGuiData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskListWidget extends Flow {

    public TaskListWidget(ForemanGuiData data) {
        super(com.cleanroommc.modularui.api.GuiAxis.Y);
        size(ForemanGui.LEFT_WIDTH, ForemanGui.HEIGHT);

        // Tabs
        child(Flow.row()
            .size(ForemanGui.LEFT_WIDTH, 24)
            .child(tabButton("To do", TaskStatus.OPEN, data))
            .child(tabButton("Doing", TaskStatus.IN_PROGRESS, data))
            .child(tabButton("Done", TaskStatus.DONE, data)));

        // Search placeholder (non-functional, reserved for Phase 4)
        child(new TextWidget("Search...").size(ForemanGui.LEFT_WIDTH - 8, 18));

        // Task list
        ListWidget<TaskRowWidget, ?> list = new ListWidget<>();
        list.size(ForemanGui.LEFT_WIDTH - 4, ForemanGui.HEIGHT - 24 - 18 - 28);
        Collection<Task> all = ForemanClientCache.getAll();
        for (Task task : all) {
            if (task.status == data.activeTab) {
                list.child(new TaskRowWidget(task, data));
            }
        }
        child(list);

        // New Task button
        child(new ButtonWidget<>()
            .size(ForemanGui.LEFT_WIDTH - 8, 24)
            .child(new TextWidget("New Task"))
            .onMousePressed(btn -> {
                if (btn != 0) return false;
                data.enterCreateMode();
                ForemanGui.open(data);
                return true;
            }));
    }

    private static ButtonWidget<?> tabButton(String label, TaskStatus status, ForemanGuiData data) {
        return new ButtonWidget<>()
            .size(120, 22)
            .child(new TextWidget(label))
            .onMousePressed(btn -> {
                if (btn != 0) return false;
                data.activeTab = status;
                data.clear();
                ForemanGui.open(data);
                return true;
            });
    }
}
