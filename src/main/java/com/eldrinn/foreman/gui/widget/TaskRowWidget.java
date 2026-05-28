package com.eldrinn.foreman.gui.widget;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.data.TaskStatus;
import com.eldrinn.foreman.gui.ForemanGui;
import com.eldrinn.foreman.gui.ForemanGuiData;
import com.eldrinn.foreman.network.ForemanNetwork;
import com.eldrinn.foreman.network.UpdateTaskPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskRowWidget extends Flow {

    private static final int LEFT_WIDTH = ForemanGui.LEFT_WIDTH;
    public static final int SCROLLBAR_W = 4;
    private static final int ROW_WIDTH = LEFT_WIDTH - 2 * ForemanGui.PADDING - SCROLLBAR_W;
    private static final int STATUS_BTN_W = 20;
    private static final int ICON_W = 20;
    private static final int LABEL_W = ROW_WIDTH - ICON_W - STATUS_BTN_W - 2;

    public TaskRowWidget(Task task, ForemanGuiData data) {
        super(com.cleanroommc.modularui.api.GuiAxis.X);
        size(ROW_WIDTH, 20);

        child(new TaskIconWidget(task.iconItem, task.status).size(ICON_W, 20));

        TextWidget normalLabel = new TextWidget(buildLabel(task));
        normalLabel.size(LABEL_W, 20);
        normalLabel.alignment(Alignment.CenterLeft);
        normalLabel.padding(4, 0, 0, 0);

        TextWidget activeLabel = new TextWidget(buildLabel(task));
        activeLabel.size(LABEL_W, 20);
        activeLabel.alignment(Alignment.CenterLeft);
        activeLabel.padding(4, 0, 0, 0);

        ToggleButton selectBtn = new ToggleButton();
        selectBtn.size(LABEL_W, 20);
        selectBtn.value(new BoolValue.Dynamic(() -> task.id.equals(data.selectedTaskId), selected -> {
            if (selected) {
                data.selectTask(task.id);
                ForemanGui.open(data);
            }
        }));
        selectBtn.child(false, normalLabel);
        selectBtn.child(true, activeLabel);

        TaskStatus next = nextStatus(task.status);
        TextWidget nextLabel = new TextWidget(statusIcon(next));
        nextLabel.size(STATUS_BTN_W, 20);
        nextLabel.alignment(Alignment.Center);

        ButtonWidget<?> statusBtn = new ButtonWidget<>();
        statusBtn.size(STATUS_BTN_W, 20);
        statusBtn.child(nextLabel);
        statusBtn.onMousePressed(btn -> {
            if (btn != 0) return false;
            task.status = next;
            ForemanNetwork.CHANNEL.sendToServer(new UpdateTaskPacket(task));
            ForemanGui.open(data);
            return true;
        });

        child(selectBtn);
        child(statusBtn);
    }

    private static TaskStatus nextStatus(TaskStatus current) {
        switch (current) {
            case OPEN:
                return TaskStatus.IN_PROGRESS;
            case IN_PROGRESS:
                return TaskStatus.DONE;
            case DONE:
                return TaskStatus.OPEN;
            default:
                return TaskStatus.OPEN;
        }
    }

    private static String buildLabel(Task task) {
        return truncate(task.title, 28) + "  " + buildAssigneeText(task);
    }

    private static String statusIcon(TaskStatus status) {
        switch (status) {
            case OPEN:
                return "○";
            case IN_PROGRESS:
                return "~";
            case DONE:
                return "✔";
            default:
                return "?";
        }
    }

    private static String buildAssigneeText(Task task) {
        if (task.assignees.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (UUID uuid : task.assignees) {
            if (shown >= 2) {
                sb.append(
                    String.format(
                        net.minecraft.util.StatCollector.translateToLocal("foreman.gui.row.more"),
                        task.assignees.size() - 2));
                break;
            }
            String name = resolveName(uuid);
            if (sb.length() > 0) sb.append(" ");
            sb.append("[")
                .append(name)
                .append("]");
            shown++;
        }
        return sb.toString();
    }

    private static String resolveName(UUID uuid) {
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().thePlayer.sendQueue;
        for (GuiPlayerInfo info : netHandler.playerInfoList) {
            net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().theWorld
                .getPlayerEntityByName(info.name);
            if (player != null && uuid.equals(
                player.getGameProfile()
                    .getId()))
                return info.name;
        }
        return uuid.toString()
            .substring(0, 8);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }

    @SideOnly(Side.CLIENT)
    private static class TaskIconWidget extends Widget<TaskIconWidget> {

        private final @Nullable String iconItem;
        private final TaskStatus status;

        TaskIconWidget(@Nullable String iconItem, TaskStatus status) {
            this.iconItem = iconItem;
            this.status = status;
        }

        @Override
        protected WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
            return theme.getPanelTheme();
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            ItemStack stack = IconSlotWidget.parseIconItem(iconItem);
            if (stack != null) {
                int pad = 2;
                GuiDraw.drawItem(
                    stack,
                    pad,
                    pad,
                    getArea().width - 2 * pad,
                    getArea().height - 2 * pad,
                    context.getCurrentDrawingZ());
            } else {
                // fallback to status symbol
                String symbol = statusFallback(status);
                int x = getArea().width / 2 - 3;
                int y = getArea().height / 2 - 4;
                Minecraft.getMinecraft().fontRenderer.drawString(symbol, x, y, 0xAAAAAA);
            }
        }

        private static String statusFallback(TaskStatus status) {
            switch (status) {
                case OPEN:
                    return "○";
                case IN_PROGRESS:
                    return "~";
                case DONE:
                    return "✔";
                default:
                    return "?";
            }
        }
    }
}
