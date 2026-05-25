package com.eldrinn.foreman.gui.widget;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
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
                child(new TextWidget("Task not found."));
            }
        } else {
            this.task = null;
            this.isNew = false;
            child(new TextWidget("Select a task or press 'New Task'."));
        }
    }

    private void buildForm() {
        final int W = ForemanGui.RIGHT_WIDTH - 2 * ForemanGui.PADDING;
        // Header: title field + delete button
        Flow header = Flow.row()
            .size(W, 24);
        PlainTextField titleField = new PlainTextField();
        titleField.size(W - 28, 22);
        titleField.value(new StringValue.Dynamic(() -> task.title, val -> {
            task.title = val;
            sendUpdate();
        }));
        header.child(titleField);
        if (!isNew) {
            TextWidget deleteLabel = new TextWidget("D");
            deleteLabel.size(24, 22);
            deleteLabel.alignment(Alignment.Center);
            header.child(
                new ButtonWidget<>().size(24, 22)
                    .child(deleteLabel)
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
        child(new TextWidget("Description:"));
        PlainTextField descField = new PlainTextField();
        descField.size(W, 22);
        descField.value(new StringValue.Dynamic(() -> task.description, val -> {
            task.description = val;
            sendUpdate();
        }));
        child(descField);

        // Status buttons
        child(new TextWidget("Status:"));
        Flow statusRow = Flow.row()
            .size(W, 22);
        for (TaskStatus s : TaskStatus.values()) {
            TaskStatus status = s;
            TextWidget statusLabel = new TextWidget(s.displayName());
            statusLabel.size(120, 20);
            statusLabel.alignment(Alignment.Center);
            statusRow.child(
                new ButtonWidget<>().size(120, 20)
                    .child(statusLabel)
                    .onMousePressed(btn -> {
                        if (btn != 0) return false;
                        task.status = status;
                        sendUpdate();
                        ForemanGui.open(data);
                        return true;
                    }));
        }
        child(statusRow);

        // Assignees
        child(new TextWidget("Assignees:"));
        child(new AssigneePickerWidget(task, data));

        // Location
        child(new TextWidget("Location:"));
        child(buildLocationRow());

        // Subtasks
        child(new TextWidget("Subtasks:"));
        child(buildSubtaskList());
    }

    private Flow buildLocationRow() {
        final int W = ForemanGui.RIGHT_WIDTH - 2 * ForemanGui.PADDING;
        Flow row = Flow.row()
            .size(W, 22);

        row.child(new TextWidget("x:"));
        PlainTextField xField = new PlainTextField();
        xField.size(60, 20);
        xField.value(new StringValue.Dynamic(() -> String.valueOf(task.location != null ? task.location.x : 0), val -> {
            ensureLocation();
            try {
                task.location.x = Integer.parseInt(val.trim());
                sendUpdate();
            } catch (NumberFormatException ignored) {}
        }));
        row.child(xField);

        row.child(new TextWidget("y:"));
        PlainTextField yField = new PlainTextField();
        yField.size(60, 20);
        yField.value(new StringValue.Dynamic(() -> String.valueOf(task.location != null ? task.location.y : 0), val -> {
            ensureLocation();
            try {
                task.location.y = Integer.parseInt(val.trim());
                sendUpdate();
            } catch (NumberFormatException ignored) {}
        }));
        row.child(yField);

        row.child(new TextWidget("z:"));
        PlainTextField zField = new PlainTextField();
        zField.size(60, 20);
        zField.value(new StringValue.Dynamic(() -> String.valueOf(task.location != null ? task.location.z : 0), val -> {
            ensureLocation();
            try {
                task.location.z = Integer.parseInt(val.trim());
                sendUpdate();
            } catch (NumberFormatException ignored) {}
        }));
        row.child(zField);

        TextWidget posLabel = new TextWidget("Pos");
        posLabel.size(36, 20);
        posLabel.alignment(Alignment.Center);
        row.child(
            new ButtonWidget<>().size(36, 20)
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
        col.size(W, 18);
        col.coverChildrenHeight(18);

        for (Subtask sub : task.subtasks) {
            Subtask s = sub;
            TextWidget checkLabel = new TextWidget(s.checked ? "x" : " ");
            checkLabel.size(18, 16);
            checkLabel.alignment(Alignment.Center);
            TextWidget subtaskTitle = new TextWidget(s.title);
            subtaskTitle.size(W - 36, 16);
            TextWidget deleteLabel = new TextWidget("D");
            deleteLabel.size(18, 16);
            deleteLabel.alignment(Alignment.Center);
            col.child(
                Flow.row()
                    .size(W, 18)
                    .child(
                        new ButtonWidget<>().size(18, 16)
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
                        new ButtonWidget<>().size(18, 16)
                            .child(deleteLabel)
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
        addField.size(W - 28, 18);
        addField.autoUpdateOnChange(true);
        addField.value(new StringValue.Dynamic(() -> newTitle[0], val -> newTitle[0] = val));
        TextWidget addLabel = new TextWidget("+");
        addLabel.size(24, 18);
        addLabel.alignment(Alignment.Center);
        col.child(
            Flow.row()
                .size(W, 20)
                .child(addField)
                .child(
                    new ButtonWidget<>().size(24, 18)
                        .child(addLabel)
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

    private void ensureLocation() {
        if (task.location == null) {
            task.location = new TaskLocation(0, 0, 0, 0, "");
        }
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
