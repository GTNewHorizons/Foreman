package com.eldrinn.foreman.gui.widget;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.eldrinn.foreman.cache.ForemanClientCache;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.gui.ForemanGui;
import com.eldrinn.foreman.gui.ForemanGuiData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskRowWidget extends Flow {

    private static final int LEFT_WIDTH = ForemanGui.LEFT_WIDTH;
    public static final int SCROLLBAR_W = 4;
    private static final int ROW_WIDTH = LEFT_WIDTH - 2 * ForemanGui.PADDING - SCROLLBAR_W;
    private static final int ICON_W = 20;
    private static final int PIN_BTN_W = 20;
    private static final int SELECT_BTN_W = ROW_WIDTH - PIN_BTN_W;

    public TaskRowWidget(Task task, ForemanGuiData data) {
        super(com.cleanroommc.modularui.api.GuiAxis.X);
        size(ROW_WIDTH, 20);

        ToggleButton selectBtn = new ToggleButton();
        selectBtn.size(SELECT_BTN_W, 20);
        selectBtn.value(new BoolValue.Dynamic(() -> task.id.equals(data.selectedTaskId), selected -> {
            if (selected) {
                data.selectTask(task.id);
                ForemanGui.open(data);
            }
        }));
        selectBtn.child(false, buildRowContent(task, SELECT_BTN_W));
        selectBtn.child(true, buildRowContent(task, SELECT_BTN_W));

        boolean pinned = ForemanClientCache.isPinned(task.id);
        boolean canPin = ForemanClientCache.canPin();
        IDrawable pinIcon;
        if (pinned) {
            pinIcon = GuiTextures.FAVORITE.withColorOverride(0xFFF0C040);
        } else if (canPin) {
            pinIcon = GuiTextures.FAVORITE_OUTLINE;
        } else {
            pinIcon = GuiTextures.FAVORITE_OUTLINE.withColorOverride(0xFF555555);
        }
        ButtonWidget<?> pinBtn = new ButtonWidget<>();
        pinBtn.size(PIN_BTN_W, 20);
        pinBtn.overlay(pinIcon);
        pinBtn.onMousePressed(btn -> {
            if (btn != 0) return false;
            if (ForemanClientCache.isPinned(task.id)) {
                ForemanClientCache.unpin(task.id);
            } else {
                ForemanClientCache.pin(task.id);
            }
            ForemanGui.open(data);
            return true;
        });

        child(selectBtn);
        child(pinBtn);
    }

    private static final int TEXT_PAD = 4;
    private static final int HEAD_SIZE = 8;
    private static final int HEAD_GAP = 2;

    private static Flow buildRowContent(Task task, int width) {
        ItemStack stack = IconSlotWidget.parseIconItem(task.iconItem);
        Flow row = Flow.row()
            .size(width, 20);
        int used = 0;

        if (stack != null) {
            row.child(new InlineIconWidget(stack).size(ICON_W, 20));
            used += ICON_W;
        }

        String title = truncate(task.title, 22);
        int leftPad = stack == null ? TEXT_PAD : 0;
        int assigneeW = assigneeBlockWidth(task);
        int maxTitleW = width - used - leftPad - assigneeW;
        int titlePixelW = Minecraft.getMinecraft().fontRenderer.getStringWidth(title) + 4;
        var titleLabel = new TextWidget<>(title);
        titleLabel.textAlign(Alignment.CenterLeft);
        titleLabel.marginLeft(leftPad);
        titleLabel.size(Math.min(titlePixelW, maxTitleW), 20);
        row.child(titleLabel);

        // assignee heads + names
        int shown = 0;
        for (UUID uuid : task.assignees) {
            if (shown >= 2) {
                String more = String.format(
                    net.minecraft.util.StatCollector.translateToLocal("foreman.gui.row.more"),
                    task.assignees.size() - 2);
                var moreLabel = new TextWidget<>(more);
                moreLabel.size(30, 20);
                moreLabel.textAlign(Alignment.CenterLeft);
                row.child(moreLabel);
                break;
            }
            String name = resolveName(uuid);
            if (name != null) {
                row.child(
                    new PlayerHeadWidget(name).size(HEAD_SIZE, HEAD_SIZE)
                        .marginTop(6)
                        .marginLeft(HEAD_GAP));
                var nameLabel = new TextWidget<>("[" + name + "]");
                nameLabel.size(nameTextWidth(name), 20);
                nameLabel.textAlign(Alignment.CenterLeft);
                nameLabel.marginLeft(HEAD_GAP);
                row.child(nameLabel);
            }
            shown++;
        }

        return row;
    }

    private static int assigneeBlockWidth(Task task) {
        if (task.assignees.isEmpty()) return 0;
        int w = 0;
        int shown = 0;
        for (UUID uuid : task.assignees) {
            if (shown >= 2) {
                w += 30;
                break;
            }
            String name = resolveName(uuid);
            if (name != null) {
                w += HEAD_GAP + HEAD_SIZE + nameTextWidth(name);
            }
            shown++;
        }
        return w;
    }

    private static int nameTextWidth(String name) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth("[" + name + "]") + 2;
    }

    private static @Nullable String resolveName(UUID uuid) {
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().thePlayer.sendQueue;
        for (GuiPlayerInfo info : netHandler.playerInfoList) {
            net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().theWorld
                .getPlayerEntityByName(info.name);
            if (player != null && uuid.equals(
                player.getGameProfile()
                    .getId()))
                return info.name;
        }
        return null;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }

    @SideOnly(Side.CLIENT)
    private static class InlineIconWidget extends Widget<InlineIconWidget> {

        private final ItemStack stack;

        InlineIconWidget(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        protected WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
            return theme.getFallback();
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            int pad = 2;
            GuiDraw.drawItem(
                stack,
                pad,
                pad,
                getArea().width - 2 * pad,
                getArea().height - 2 * pad,
                context.getCurrentDrawingZ());
        }
    }
}
