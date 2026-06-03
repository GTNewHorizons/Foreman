package com.eldrinn.foreman.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.eldrinn.foreman.cache.ForemanClientCache;
import com.eldrinn.foreman.config.PinnedTasksConfig;
import com.eldrinn.foreman.data.Subtask;
import com.eldrinn.foreman.data.Task;
import com.eldrinn.foreman.data.TaskStatus;
import com.eldrinn.foreman.gui.widget.IconSlotWidget;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HudRenderer {

    private static final RenderItem RENDER_ITEM = new RenderItem();

    private static final int MAX_SUBTASKS_SHOWN = 3;
    private static final int LINE_H = 10;
    private static final int ICON_SIZE = 10; // item icon scaled to match line height
    private static final int ICON_GAP = 2;
    private static final int BLOCK_GAP = 4;
    static final int PADDING = 4;

    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final int COLOR_GRAY = 0xAAAAAA;
    private static final int COLOR_YELLOW = 0xF0C040;
    private static final int COLOR_GREEN = 0x8BC34A;

    @SubscribeEvent
    public void onRenderHud(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null && !(mc.currentScreen instanceof HudSettingsScreen)) return;

        PinnedTasksConfig cfg = ForemanClientCache.getPinConfig();
        if (!cfg.isHudVisible()) return;

        List<Task> pinned = ForemanClientCache.getPinnedTasks();
        if (pinned.isEmpty()) return;

        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = res.getScaledWidth();
        int sh = res.getScaledHeight();

        int[] pos = computeHudPosition(cfg, sw, sh, mc.fontRenderer, pinned);
        int startX = pos[0];
        int startY = pos[1];
        int blockW = pos[2];
        int totalHeight = pos[3];

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glScaled(cfg.getScale(), cfg.getScale(), 1.0);

        // Coordinates must be divided by scale because GL matrix already scaled up
        double s = cfg.getScale();
        int sx = (int) (startX / s);
        int sy = (int) (startY / s);

        if (cfg.isShowBackground()) {
            net.minecraft.client.gui.Gui
                .drawRect(sx - PADDING, sy - PADDING, sx + blockW + PADDING, sy + totalHeight + PADDING, 0x88000000);
        }

        int y = sy;
        for (Task task : pinned) {
            y = drawTaskBlock(mc.fontRenderer, task, sx, y);
            y += BLOCK_GAP;
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    /**
     * Returns [startX, startY, blockW, totalH] for the HUD block in screen coordinates.
     * Used by HudSettingsScreen to position the drag handle.
     */
    public static int[] computeHudPosition(PinnedTasksConfig cfg, int sw, int sh, FontRenderer fr, List<Task> pinned) {
        int blockW = maxBlockWidth(pinned, fr);
        int totalH = totalHeight(pinned);
        int x = anchorX(cfg.getAnchor(), sw, blockW) + cfg.getOffsetX();
        int y = anchorY(cfg.getAnchor(), sh, totalH) + cfg.getOffsetY();
        return new int[] { x, y, blockW, totalH };
    }

    private int drawTaskBlock(FontRenderer fr, Task task, int x, int y) {
        String statusText = "[" + task.status.displayName()
            .toUpperCase() + "]";
        fr.drawStringWithShadow(statusText, x, y, statusColor(task.status));
        y += LINE_H;

        ItemStack iconStack = IconSlotWidget.parseIconItem(task.iconItem);
        if (iconStack != null) {
            drawItemIcon(iconStack, x, y);
            fr.drawStringWithShadow(task.title, x + ICON_SIZE + ICON_GAP, y, COLOR_WHITE);
        } else {
            fr.drawStringWithShadow(task.title, x, y, COLOR_WHITE);
        }
        y += LINE_H;

        if (!task.subtasks.isEmpty()) {
            int shown = 0;
            for (Subtask st : task.subtasks) {
                if (shown >= MAX_SUBTASKS_SHOWN) break;
                int color = st.checked ? COLOR_GRAY : COLOR_WHITE;
                String label = st.checked ? "§m- " + st.title + "§r" : "- " + st.title;
                fr.drawStringWithShadow(label, x + PADDING, y, color);
                y += LINE_H;
                shown++;
            }
            int remaining = task.subtasks.size() - shown;
            if (remaining > 0) {
                fr.drawStringWithShadow(
                    StatCollector.translateToLocalFormatted("foreman.gui.row.more", remaining),
                    x + PADDING,
                    y,
                    COLOR_GRAY);
                y += LINE_H;
            }
        }

        return y;
    }

    static int totalHeight(List<Task> pinned) {
        int h = 0;
        for (Task t : pinned) {
            h += LINE_H * 2; // status + title
            int subtaskLines = Math.min(t.subtasks.size(), MAX_SUBTASKS_SHOWN);
            h += LINE_H * subtaskLines;
            if (t.subtasks.size() > MAX_SUBTASKS_SHOWN) h += LINE_H; // "+N more"
            h += BLOCK_GAP;
        }
        return h;
    }

    static int maxBlockWidth(List<Task> pinned, FontRenderer fr) {
        int max = 80;
        for (Task t : pinned) {
            max = Math.max(
                max,
                fr.getStringWidth(
                    "[" + t.status.displayName()
                        .toUpperCase() + "]"));
            int titlePrefix = t.iconItem != null ? ICON_SIZE + ICON_GAP : 0;
            max = Math.max(max, titlePrefix + fr.getStringWidth(t.title));
            int shown = 0;
            for (Subtask st : t.subtasks) {
                if (shown >= MAX_SUBTASKS_SHOWN) break;
                max = Math.max(max, PADDING + fr.getStringWidth("- " + st.title));
                shown++;
            }
        }
        return max + PADDING * 2;
    }

    static int anchorX(PinnedTasksConfig.Anchor anchor, int sw, int blockW) {
        return switch (anchor) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> 2;
            case TOP_CENTER, MIDDLE_CENTER, BOTTOM_CENTER -> (sw - blockW) / 2;
            default -> sw - blockW - 2; // RIGHT
        };
    }

    static int anchorY(PinnedTasksConfig.Anchor anchor, int sh, int totalH) {
        return switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 2;
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT -> (sh - totalH) / 2;
            default -> sh - totalH - 2; // BOTTOM
        };
    }

    private void drawItemIcon(ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        // Scale 16x16 item down to ICON_SIZE
        float s = ICON_SIZE / 16.0f;
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(s, s, s);
        RENDER_ITEM.renderItemIntoGUI(mc.fontRenderer, mc.renderEngine, stack, 0, 0);
        GL11.glPopMatrix();

        RenderHelper.disableStandardItemLighting();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private int statusColor(TaskStatus status) {
        return switch (status) {
            case IN_PROGRESS -> COLOR_YELLOW;
            case DONE -> COLOR_GREEN;
            default -> COLOR_GRAY;
        };
    }
}
