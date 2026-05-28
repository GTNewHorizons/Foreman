package com.eldrinn.foreman.gui.widget;

import java.util.Collection;

import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
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
        final int HEIGHT = ForemanGui.getHeight();
        size(ForemanGui.LEFT_WIDTH, HEIGHT);
        padding(ForemanGui.PADDING);

        final int P = ForemanGui.PADDING;
        final int W = ForemanGui.LEFT_WIDTH - 2 * P;
        final int H = HEIGHT - 2 * P;

        // Tabs — each tab takes exactly 1/3 of the available width
        final int TAB_W = W / 3;
        child(
            Flow.row()
                .size(W, 24)
                .child(
                    tabButton(
                        net.minecraft.util.StatCollector.translateToLocal("foreman.gui.tab.open"),
                        TaskStatus.OPEN,
                        data,
                        TAB_W))
                .child(
                    tabButton(
                        net.minecraft.util.StatCollector.translateToLocal("foreman.gui.tab.in_progress"),
                        TaskStatus.IN_PROGRESS,
                        data,
                        TAB_W))
                .child(
                    tabButton(
                        net.minecraft.util.StatCollector.translateToLocal("foreman.gui.tab.done"),
                        TaskStatus.DONE,
                        data,
                        TAB_W)));

        // Search: icon button toggles field; live search on keystroke
        final int SEARCH_BTN_W = 20;
        Flow searchRow = Flow.row()
            .size(W, 20);
        searchRow.child(
            new ButtonWidget<>().size(SEARCH_BTN_W, 20)
                .overlay(com.eldrinn.foreman.gui.ForemanIcons.SEARCH)
                .onMousePressed(btn -> {
                    if (btn != 0) return false;
                    data.searchExpanded = !data.searchExpanded;
                    if (!data.searchExpanded) {
                        data.searchQuery = "";
                    }
                    ForemanGui.open(data);
                    return true;
                }));
        if (data.searchExpanded) {
            PlainTextField searchField = new PlainTextField();
            searchField.size(W - SEARCH_BTN_W, 20);
            searchField.setTextColor(0xFFFFFF);
            searchField.autoUpdateOnChange(true);
            searchField.value(new StringValue.Dynamic(() -> data.searchQuery, val -> {
                data.searchQuery = val;
                ForemanGui.open(data);
            }));
            searchRow.child(searchField);
        }
        child(searchRow);

        // Task list filtered by active tab and search query
        @SuppressWarnings("rawtypes")
        ListWidget list = new ListWidget();
        list.scrollDirection(new VerticalScrollData(false, TaskRowWidget.SCROLLBAR_W));
        list.size(W, H - 24 - 20 - 28);
        Collection<Task> all = ForemanClientCache.getAll();
        String query = data.searchQuery.toLowerCase();
        for (Task task : all) {
            if (task.status != data.activeTab) continue;
            if (!query.isEmpty() && !task.title.toLowerCase()
                .contains(query)
                && !task.description.toLowerCase()
                    .contains(query))
                continue;
            list.child(new TaskRowWidget(task, data));
        }
        child(list);

        // Bottom bar: New Task + theme toggle
        final int THEME_BTN_W = 28;
        final int NEW_TASK_W = W - THEME_BTN_W - 4;

        TextWidget newTaskLabel = new TextWidget(
            net.minecraft.util.StatCollector.translateToLocal("foreman.gui.new_task"));
        newTaskLabel.size(NEW_TASK_W, 24);
        newTaskLabel.alignment(Alignment.Center);

        TextWidget themeLabel = new TextWidget("☀");
        themeLabel.size(THEME_BTN_W, 24);
        themeLabel.alignment(Alignment.Center);

        child(
            Flow.row()
                .size(W, 24)
                .child(
                    new ButtonWidget<>().size(NEW_TASK_W, 24)
                        .child(newTaskLabel)
                        .onMousePressed(btn -> {
                            if (btn != 0) return false;
                            data.enterCreateMode();
                            ForemanGui.open(data);
                            return true;
                        }))
                .child(
                    new ButtonWidget<>().size(THEME_BTN_W, 24)
                        .child(themeLabel)
                        .onMousePressed(btn -> {
                            if (btn != 0) return false;
                            ForemanGui.toggleTheme();
                            ForemanGui.open(data);
                            return true;
                        })));
    }

    private static ToggleButton tabButton(String label, TaskStatus status, ForemanGuiData data, int width) {
        TextWidget normalLabel = new TextWidget(label);
        normalLabel.size(width, 22);
        normalLabel.alignment(Alignment.Center);

        TextWidget activeLabel = new TextWidget(label);
        activeLabel.size(width, 22);
        activeLabel.alignment(Alignment.Center);

        return new ToggleButton().size(width, 22)
            .value(new BoolValue.Dynamic(() -> data.activeTab == status, selected -> {
                if (selected) {
                    data.activeTab = status;
                    data.clear();
                    ForemanGui.open(data);
                }
            }))
            .child(false, normalLabel)
            .child(true, activeLabel);
    }
}
