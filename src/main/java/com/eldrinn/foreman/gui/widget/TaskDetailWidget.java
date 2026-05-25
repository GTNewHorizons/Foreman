package com.eldrinn.foreman.gui.widget;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
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
        // Header: title field + delete button
        Flow header = Flow.row().size(ForemanGui.RIGHT_WIDTH - 4, 24);
        header.child(new TextFieldWidget()
            .size(ForemanGui.RIGHT_WIDTH - 32, 22)
            .value(new StringValue.Dynamic(
                () -> task.title,
                val -> { task.title = val; sendUpdate(); })));
        if (!isNew) {
            header.child(new ButtonWidget<>()
                .size(24, 22)
                .child(new TextWidget("[D]"))
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
        child(new TextWidget("Description:").size(ForemanGui.RIGHT_WIDTH - 4, 12));
        child(new TextFieldWidget()
            .size(ForemanGui.RIGHT_WIDTH - 4, 48)
            .value(new StringValue.Dynamic(
                () -> task.description,
                val -> { task.description = val; sendUpdate(); })));

        // Status buttons
        child(new TextWidget("Status:").size(ForemanGui.RIGHT_WIDTH - 4, 12));
        Flow statusRow = Flow.row().size(ForemanGui.RIGHT_WIDTH - 4, 22);
        for (TaskStatus s : TaskStatus.values()) {
            TaskStatus status = s;
            statusRow.child(new ButtonWidget<>()
                .size(120, 20)
                .child(new TextWidget(s.displayName()))
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
        child(new TextWidget("Assignees:").size(ForemanGui.RIGHT_WIDTH - 4, 12));
        child(new AssigneePickerWidget(task, data));

        // Location
        child(new TextWidget("Location:").size(ForemanGui.RIGHT_WIDTH - 4, 12));
        child(buildLocationRow());

        // Subtasks
        child(new TextWidget("Subtasks:").size(ForemanGui.RIGHT_WIDTH - 4, 12));
        child(buildSubtaskList());
    }

    private Flow buildLocationRow() {
        int locX = task.location != null ? task.location.x : 0;
        int locY = task.location != null ? task.location.y : 0;
        int locZ = task.location != null ? task.location.z : 0;

        Flow row = Flow.row().size(ForemanGui.RIGHT_WIDTH - 4, 22);

        row.child(new TextWidget("x:").size(12, 20));
        row.child(new TextFieldWidget()
            .size(60, 20)
            .value(new StringValue.Dynamic(
                () -> String.valueOf(task.location != null ? task.location.x : 0),
                val -> { ensureLocation(); try { task.location.x = Integer.parseInt(val.trim()); sendUpdate(); } catch (NumberFormatException ignored) {} })));

        row.child(new TextWidget("y:").size(12, 20));
        row.child(new TextFieldWidget()
            .size(60, 20)
            .value(new StringValue.Dynamic(
                () -> String.valueOf(task.location != null ? task.location.y : 0),
                val -> { ensureLocation(); try { task.location.y = Integer.parseInt(val.trim()); sendUpdate(); } catch (NumberFormatException ignored) {} })));

        row.child(new TextWidget("z:").size(12, 20));
        row.child(new TextFieldWidget()
            .size(60, 20)
            .value(new StringValue.Dynamic(
                () -> String.valueOf(task.location != null ? task.location.z : 0),
                val -> { ensureLocation(); try { task.location.z = Integer.parseInt(val.trim()); sendUpdate(); } catch (NumberFormatException ignored) {} })));

        row.child(new ButtonWidget<>()
            .size(32, 20)
            .child(new TextWidget("[Pos]"))
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
        Flow col = Flow.column().size(ForemanGui.RIGHT_WIDTH - 4, 150);
        for (Subtask sub : task.subtasks) {
            Subtask s = sub;
            col.child(Flow.row()
                .size(ForemanGui.RIGHT_WIDTH - 4, 18)
                .child(new ButtonWidget<>()
                    .size(18, 16)
                    .child(new TextWidget(s.checked ? "[x]" : "[ ]"))
                    .onMousePressed(btn -> {
                        if (btn != 0) return false;
                        s.checked = !s.checked;
                        sendUpdate();
                        ForemanGui.open(data);
                        return true;
                    }))
                .child(new TextWidget(s.title).size(ForemanGui.RIGHT_WIDTH - 50, 16))
                .child(new ButtonWidget<>()
                    .size(18, 16)
                    .child(new TextWidget("[D]"))
                    .onMousePressed(btn -> {
                        if (btn != 0) return false;
                        task.subtasks.remove(s);
                        sendUpdate();
                        ForemanGui.open(data);
                        return true;
                    })));
        }

        // Add subtask row
        String[] newTitle = {""};
        col.child(Flow.row()
            .size(ForemanGui.RIGHT_WIDTH - 4, 20)
            .child(new TextFieldWidget()
                .size(ForemanGui.RIGHT_WIDTH - 40, 18)
                .value(new StringValue.Dynamic(() -> newTitle[0], val -> newTitle[0] = val)))
            .child(new ButtonWidget<>()
                .size(24, 18)
                .child(new TextWidget("[+]"))
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
