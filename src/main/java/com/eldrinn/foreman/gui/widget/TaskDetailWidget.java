package com.eldrinn.foreman.gui.widget;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.foreman.cache.ForemanClientCache;
import com.eldrinn.foreman.data.Subtask;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.data.TaskLocation;
import com.eldrinn.foreman.data.TaskStatus;
import com.eldrinn.foreman.gui.ForemanGui;
import com.eldrinn.foreman.gui.ForemanGuiData;
import com.eldrinn.foreman.network.CreateTaskPacket;
import com.eldrinn.foreman.network.DeleteTaskPacket;
import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.UpdateTaskPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskDetailWidget extends Flow {

    // Row height for rows containing 20px interactive elements
    private static final int ROW_H = 24;
    // Height of interactive elements (buttons, fields)
    private static final int EL_H = 20;

    private final ForemanGuiData data;
    private final Task task;
    private final boolean isNew;

    public TaskDetailWidget(ForemanGuiData data) {
        super(com.cleanroommc.modularui.api.GuiAxis.Y);
        this.data = data;
        size(ForemanGui.RIGHT_WIDTH, ForemanGui.HEIGHT);
        padding(ForemanGui.PADDING);

        if (data.createMode) {
            this.task = new Task(UUID.randomUUID(), "", "", TaskStatus.OPEN);
            this.isNew = true;
            buildForm();
        } else if (data.selectedTaskId != null) {
            Task found = ForemanClientCache.get(data.selectedTaskId);
            this.task = found;
            this.isNew = false;
            if (found != null) {
                buildForm();
            } else {
                child(new TextWidget(t("foreman.gui.not_found")));
            }
        } else {
            this.task = null;
            this.isNew = false;
            child(new TextWidget(t("foreman.gui.placeholder")));
        }
    }

    private void buildForm() {
        final int W = ForemanGui.RIGHT_WIDTH - 2 * ForemanGui.PADDING;

        // Header: [icon 20] [title fills rest] [delete 20]
        // No margins — sizes sum exactly to W
        final int titleW = isNew ? W - EL_H : W - EL_H * 2;
        Flow header = Flow.row()
            .size(W, ROW_H);
        ItemStack iconStack = parseIconItem(task.iconItem);
        header.child(
            new ButtonWidget<>().size(EL_H, EL_H)
                .overlay(new ItemDrawable(iconStack))
                .onMousePressed(btn -> {
                    if (btn == 0) {
                        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
                        task.iconItem = held != null
                            ? Item.itemRegistry.getNameForObject(held.getItem()) + ":" + held.getItemDamage()
                            : null;
                    } else if (btn == 1) {
                        task.iconItem = null;
                    } else {
                        return false;
                    }
                    sendUpdate();
                    ForemanGui.open(data);
                    return true;
                }));
        PlainTextField titleField = new PlainTextField();
        titleField.size(titleW, EL_H);
        titleField.setTextColor(0xFFFFFF);
        titleField.value(new StringValue.Dynamic(() -> task.title, val -> {
            task.title = val;
            sendUpdate();
        }));
        header.child(titleField);
        if (!isNew) {
            header.child(
                new ButtonWidget<>().size(EL_H, EL_H)
                    .overlay(com.eldrinn.foreman.gui.ForemanIcons.DELETE)
                    .onMousePressed(btn -> {
                        if (btn != 0) return false;
                        ForemanNetwork.CHANNEL.sendToServer(new DeleteTaskPacket(task.id));
                        data.clear();
                        ForemanGui.open(data);
                        return true;
                    }));
        }
        child(header);

        // Description
        TextWidget descLabel = new TextWidget(t("foreman.gui.detail.description"));
        descLabel.size(W, 14);
        child(descLabel);
        PlainTextField descField = new PlainTextField();
        descField.size(W, EL_H);
        descField.setTextColor(0xFFFFFF);
        descField.value(new StringValue.Dynamic(() -> task.description, val -> {
            task.description = val;
            sendUpdate();
        }));
        child(descField);

        // Status buttons
        TextWidget statusLabel = new TextWidget(t("foreman.gui.detail.status"));
        statusLabel.size(W, 14);
        child(statusLabel);
        Flow statusRow = Flow.row()
            .size(W, ROW_H);
        for (TaskStatus s : TaskStatus.values()) {
            TaskStatus status = s;
            TextWidget normalLabel = new TextWidget(s.displayName());
            normalLabel.size(120, EL_H);
            normalLabel.alignment(Alignment.Center);
            normalLabel.color(0xFFFFFF);
            TextWidget activeLabel = new TextWidget(s.displayName());
            activeLabel.size(120, EL_H);
            activeLabel.alignment(Alignment.Center);
            activeLabel.color(0xFFFFFF);
            statusRow.child(
                new ToggleButton().size(120, EL_H)
                    .value(new BoolValue.Dynamic(() -> task.status == status, selected -> {
                        if (selected) {
                            task.status = status;
                            sendUpdate();
                            ForemanGui.open(data);
                        }
                    }))
                    .child(false, normalLabel)
                    .child(true, activeLabel));
        }
        child(statusRow);

        // Assignees
        TextWidget assigneesLabel = new TextWidget(t("foreman.gui.detail.assignees"));
        assigneesLabel.size(W, 14);
        child(assigneesLabel);
        child(new AssigneePickerWidget(task, data));

        // Location header: [label fills rest] [show-on-map label 56] [4px gap] [toggle 36]
        final int TOGGLE_W = 36;
        final int MAP_LABEL_W = 56;
        final int MAP_GAP = 4;
        Flow locationHeader = Flow.row()
            .size(W, ROW_H);
        TextWidget locationLabel = new TextWidget(t("foreman.gui.detail.location"));
        locationLabel.size(W - MAP_LABEL_W - MAP_GAP - TOGGLE_W, EL_H);
        locationLabel.alignment(Alignment.CenterLeft);
        locationHeader.child(locationLabel);
        TextWidget showMapLabel = new TextWidget(t("foreman.gui.detail.show_on_map"));
        showMapLabel.size(MAP_LABEL_W, EL_H);
        showMapLabel.alignment(Alignment.CenterRight);
        locationHeader.child(showMapLabel);
        TextWidget mapSpacer = new TextWidget("");
        mapSpacer.size(MAP_GAP, EL_H);
        locationHeader.child(mapSpacer);
        TextWidget mapOff = new TextWidget(t("foreman.gui.detail.show_on_map.off"));
        mapOff.size(TOGGLE_W, EL_H);
        mapOff.alignment(Alignment.Center);
        mapOff.color(0xAAAAAA);
        TextWidget mapOn = new TextWidget(t("foreman.gui.detail.show_on_map.on"));
        mapOn.size(TOGGLE_W, EL_H);
        mapOn.alignment(Alignment.Center);
        mapOn.color(0x55FF55);
        locationHeader.child(
            new ToggleButton().size(TOGGLE_W, EL_H)
                .value(new BoolValue.Dynamic(() -> task.showOnMap, val -> {
                    task.showOnMap = val;
                    sendUpdate();
                }))
                .child(false, mapOff)
                .child(true, mapOn));
        child(locationHeader);
        child(buildLocationRow());

        // Subtasks
        TextWidget subtasksLabel = new TextWidget(t("foreman.gui.detail.subtasks"));
        subtasksLabel.size(W, 14);
        child(subtasksLabel);
        child(buildSubtaskList());
    }

    private Flow buildLocationRow() {
        final int W = ForemanGui.RIGHT_WIDTH - 2 * ForemanGui.PADDING;
        // Layout: [x: 10][field][y: 10][field][z: 10][field][Pos 44]
        // No margins — all sizes sum to W exactly
        final int LABEL_W = 10;
        final int POS_W = 44;
        final int FIELD_W = (W - 3 * LABEL_W - POS_W) / 3;
        Flow row = Flow.row()
            .size(W, ROW_H);

        TextWidget xLabel = new TextWidget("x:");
        xLabel.size(LABEL_W, EL_H);
        xLabel.alignment(Alignment.CenterLeft);
        row.child(xLabel);
        PlainTextField xField = new PlainTextField();
        xField.size(FIELD_W, EL_H);
        xField.setTextColor(0xFFFFFF);
        xField.padding(4, 0, 0, 0);
        xField.value(new StringValue.Dynamic(() -> String.valueOf(task.location != null ? task.location.x : 0), val -> {
            ensureLocation();
            try {
                task.location.x = Integer.parseInt(val.trim());
                sendUpdate();
            } catch (NumberFormatException ignored) {}
        }));
        row.child(xField);

        TextWidget yLabel = new TextWidget("y:");
        yLabel.size(LABEL_W, EL_H);
        yLabel.alignment(Alignment.CenterLeft);
        yLabel.padding(4, 0, 0, 0);
        row.child(yLabel);
        PlainTextField yField = new PlainTextField();
        yField.size(FIELD_W, EL_H);
        yField.setTextColor(0xFFFFFF);
        yField.padding(4, 0, 0, 0);
        yField.value(new StringValue.Dynamic(() -> String.valueOf(task.location != null ? task.location.y : 0), val -> {
            ensureLocation();
            try {
                task.location.y = Integer.parseInt(val.trim());
                sendUpdate();
            } catch (NumberFormatException ignored) {}
        }));
        row.child(yField);

        TextWidget zLabel = new TextWidget("z:");
        zLabel.size(LABEL_W, EL_H);
        zLabel.alignment(Alignment.CenterLeft);
        zLabel.padding(4, 0, 0, 0);
        row.child(zLabel);
        PlainTextField zField = new PlainTextField();
        zField.size(FIELD_W, EL_H);
        zField.setTextColor(0xFFFFFF);
        zField.padding(4, 0, 0, 0);
        zField.value(new StringValue.Dynamic(() -> String.valueOf(task.location != null ? task.location.z : 0), val -> {
            ensureLocation();
            try {
                task.location.z = Integer.parseInt(val.trim());
                sendUpdate();
            } catch (NumberFormatException ignored) {}
        }));
        row.child(zField);

        // Pos button fills remaining space: W - 3*LABEL_W - 3*FIELD_W
        final int actualPosW = W - 3 * LABEL_W - 3 * FIELD_W;
        TextWidget posLabel = new TextWidget(t("foreman.gui.detail.pos"));
        posLabel.size(actualPosW, EL_H);
        posLabel.alignment(Alignment.Center);
        posLabel.color(0xFFFFFF);
        row.child(
            new ButtonWidget<>().size(actualPosW, EL_H)
                .child(posLabel)
                .onMousePressed(btn -> {
                    if (btn != 0) return false;
                    EntityPlayer p = Minecraft.getMinecraft().thePlayer;
                    ensureLocation();
                    task.location.x = (int) p.posX;
                    task.location.y = (int) p.posY;
                    task.location.z = (int) p.posZ;
                    task.location.dimension = p.worldObj.provider.dimensionId;
                    sendUpdate();
                    ForemanGui.open(data);
                    return true;
                }));

        return row;
    }

    private Flow buildSubtaskList() {
        final int W = ForemanGui.RIGHT_WIDTH - 2 * ForemanGui.PADDING;
        Flow col = Flow.column();
        col.size(W, ROW_H);
        col.coverChildrenHeight(ROW_H);

        for (Subtask sub : task.subtasks) {
            Subtask s = sub;
            TextWidget checkLabel = new TextWidget(s.checked ? "x" : " ");
            checkLabel.size(EL_H, EL_H);
            checkLabel.alignment(Alignment.Center);
            TextWidget subtaskTitle = new TextWidget(s.title);
            subtaskTitle.size(W - EL_H * 2, EL_H);
            subtaskTitle.alignment(Alignment.CenterLeft);
            subtaskTitle.padding(4, 0, 0, 0);
            col.child(
                Flow.row()
                    .size(W, ROW_H)
                    .child(
                        new ButtonWidget<>().size(EL_H, EL_H)
                            .child(checkLabel)
                            .onMousePressed(btn -> {
                                if (btn != 0) return false;
                                s.checked = !s.checked;
                                sendUpdate();
                                ForemanGui.open(data);
                                return true;
                            }))
                    .child(subtaskTitle)
                    .child(
                        new ButtonWidget<>().size(EL_H, EL_H)
                            .overlay(com.eldrinn.foreman.gui.ForemanIcons.DELETE)
                            .onMousePressed(btn -> {
                                if (btn != 0) return false;
                                task.subtasks.remove(s);
                                sendUpdate();
                                ForemanGui.open(data);
                                return true;
                            })));
        }

        // Add subtask row
        String[] newTitle = { "" };
        PlainTextField addField = new PlainTextField();
        addField.size(W - EL_H, EL_H);
        addField.setTextColor(0xFFFFFF);
        addField.autoUpdateOnChange(true);
        addField.value(new StringValue.Dynamic(() -> newTitle[0], val -> newTitle[0] = val));
        col.child(
            Flow.row()
                .size(W, ROW_H)
                .child(addField)
                .child(
                    new ButtonWidget<>().size(EL_H, EL_H)
                        .overlay(com.eldrinn.foreman.gui.ForemanIcons.PLUS)
                        .onMousePressed(btn -> {
                            if (btn != 0) return false;
                            String title = newTitle[0].trim();
                            if (!title.isEmpty()) {
                                task.subtasks.add(new Subtask(UUID.randomUUID(), title, false));
                                sendUpdate();
                                ForemanGui.open(data);
                            }
                            return true;
                        })));
        return col;
    }

    private static ItemStack parseIconItem(String iconItem) {
        if (iconItem == null) return null;
        try {
            int lastColon = iconItem.lastIndexOf(':');
            String itemName = iconItem.substring(0, lastColon);
            int meta = Integer.parseInt(iconItem.substring(lastColon + 1));
            Item item = (Item) Item.itemRegistry.getObject(itemName);
            if (item == null) return null;
            return new ItemStack(item, 1, meta);
        } catch (Exception e) {
            return null;
        }
    }

    private void ensureLocation() {
        if (task.location == null) {
            task.location = new TaskLocation(0, 0, 0, 0, "");
        }
    }

    private static String t(String key) {
        return net.minecraft.util.StatCollector.translateToLocal(key);
    }

    private void sendUpdate() {
        if (isNew) {
            if (!task.title.isEmpty()) {
                ForemanNetwork.CHANNEL.sendToServer(new CreateTaskPacket(task));
                data.selectTask(task.id);
            }
        } else {
            ForemanNetwork.CHANNEL.sendToServer(new UpdateTaskPacket(task));
        }
    }
}
