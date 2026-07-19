package name.modid.client.baton;

import name.modid.client.BotClientData;
import name.modid.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 指挥棒手持 HUD：仅在屏幕左上与右上显示信息，避免与物品栏/准星重合。
 * - 左上：标题、当前模式、操作说明、传送权限、选中假人的详细信息
 * - 右上：假人列表（高亮选中项）
 */
public class BatonHudOverlay {

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFAAAAAA;
    private static final int COLOR_DIM = 0xFFCCCCCC;
    private static final int COLOR_GREEN = 0xFF55FF55;
    private static final int COLOR_GOLD = 0xFFFFAA00;
    private static final int COLOR_RED = 0xFFFF5555;
    private static final int COLOR_AQUA = 0xFF55FFFF;

    private static final int MARGIN = 4;
    private static final int LINE = 11;
    private static final int GAP = 5;

    public static void register() {
        HudRenderCallback.EVENT.register(BatonHudOverlay::onHudRender);
    }

    private static void onHudRender(GuiGraphics g, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null || mc.options.hideGui) return;
        if (!BatonClientState.isHoldingBaton(mc.player)) return;

        renderLeft(g, mc.font, mc);
        renderRight(g, mc.font, mc, g.guiWidth());
    }

    // ==================== 左上：模式 + 操作 + 选中信息 ====================

    private static void renderLeft(GuiGraphics g, Font font, Minecraft mc) {
        int x = MARGIN;
        int y = MARGIN;

        BatonClientState.Mode mode = BatonClientState.getMode();
        boolean teleport = mode == BatonClientState.Mode.TELEPORT;
        int modeColor = teleport ? COLOR_GOLD : COLOR_GREEN;

        y = line(g, font, x, y, Component.translatable("gui.my-bot-mod.baton.title"), COLOR_WHITE);
        y = line(g, font, x, y,
            Component.translatable("gui.my-bot-mod.baton.mode_label")
                .append(Component.translatable(mode.translationKey)), modeColor);

        y += GAP;
        y = line(g, font, x, y, Component.translatable("gui.my-bot-mod.baton.switch_hint"), COLOR_GRAY);
        y = line(g, font, x, y, Component.translatable("gui.my-bot-mod.baton.select_hint"), COLOR_GRAY);
        y = line(g, font, x, y, Component.translatable(teleport
            ? "gui.my-bot-mod.baton.action.teleport"
            : "gui.my-bot-mod.baton.action.pathfind"), COLOR_GRAY);

        if (teleport) {
            boolean allowed = mc.player.getAbilities().instabuild
                || ModConfig.getInstance().allowBatonTeleportNonCreative;
            if (!allowed) {
                y = line(g, font, x, y, Component.translatable("gui.my-bot-mod.baton.tp_warn"), COLOR_RED);
            }
        }

        y += GAP;
        String selected = BatonClientState.getSelectedBotName();
        if (selected == null) {
            line(g, font, x, y, Component.translatable("gui.my-bot-mod.baton.no_selection"), COLOR_GOLD);
            return;
        }

        y = line(g, font, x, y,
            Component.translatable("gui.my-bot-mod.baton.selected").append(Component.literal(": " + selected)),
            COLOR_AQUA);

        AbstractClientPlayer bot = BatonClientState.findSelectedClientPlayer();
        if (bot != null) {
            y = line(g, font, x, y, labeled("gui.my-bot-mod.baton.health",
                String.format("%.0f/%.0f", bot.getHealth(), bot.getMaxHealth())), COLOR_WHITE);
            y = line(g, font, x, y, labeled("gui.my-bot-mod.baton.distance",
                String.format("%.1f", mc.player.distanceTo(bot))), COLOR_WHITE);
            y = line(g, font, x, y, labeled("gui.my-bot-mod.baton.pos",
                bot.getBlockX() + ", " + bot.getBlockY() + ", " + bot.getBlockZ()), COLOR_WHITE);
            y = line(g, font, x, y, Component.translatable("gui.my-bot-mod.baton.state_label")
                .append(Component.translatable(bot.isInWater()
                    ? "gui.my-bot-mod.baton.state_swim"
                    : "gui.my-bot-mod.baton.state_near")), COLOR_GREEN);
        } else {
            String dim = dimensionOf(selected);
            if (dim != null) {
                y = line(g, font, x, y, labeled("gui.my-bot-mod.baton.dimension", dim), COLOR_GRAY);
            }
            line(g, font, x, y, Component.translatable("gui.my-bot-mod.baton.not_nearby"), COLOR_GRAY);
        }
    }

    // ==================== 右上：假人列表 ====================

    private static void renderRight(GuiGraphics g, Font font, Minecraft mc, int w) {
        List<BotClientData.Entry> bots = BotClientData.get();
        int y = MARGIN;

        Component header = Component.translatable("gui.my-bot-mod.baton.bots")
            .append(Component.literal(" (" + bots.size() + ")"));
        drawRight(g, font, header, w, y, COLOR_WHITE);
        y += LINE;

        if (bots.isEmpty()) {
            drawRight(g, font, Component.translatable("gui.my-bot-mod.baton.no_bots"), w, y, COLOR_GRAY);
            return;
        }

        String selected = BatonClientState.getSelectedBotName();
        int max = Math.min(bots.size(), 12);
        for (int i = 0; i < max; i++) {
            String name = bots.get(i).name();
            boolean sel = name.equals(selected);
            drawRight(g, font, Component.literal((sel ? "\u25B6 " : "   ") + name), w, y, sel ? COLOR_GREEN : COLOR_DIM);
            y += LINE;
        }
        if (bots.size() > max) {
            drawRight(g, font, Component.literal("\u2026"), w, y, COLOR_GRAY);
        }
    }

    // ==================== 辅助 ====================

    private static Component labeled(String key, String value) {
        return Component.translatable(key).append(Component.literal(": " + value));
    }

    private static int line(GuiGraphics g, Font font, int x, int y, Component c, int color) {
        g.drawString(font, c, x, y, color);
        return y + LINE;
    }

    private static void drawRight(GuiGraphics g, Font font, Component c, int w, int y, int color) {
        g.drawString(font, c, w - MARGIN - font.width(c), y, color);
    }

    private static String dimensionOf(String botName) {
        for (BotClientData.Entry e : BotClientData.get()) {
            if (e.name().equals(botName)) return e.dimension();
        }
        return null;
    }
}
