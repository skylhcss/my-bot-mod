package name.modid.client.screen;

import name.modid.bot.BotSettings;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.UI;
import name.modid.net.BotNetworking;
import name.modid.net.BotPanelData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/**
 * 假人设置面板（全局配置界面风格）
 *
 * 左侧：当前功能（打开背包/末影箱、游戏模式、手持槽位、停止、传送、删除）。
 * 右侧：假人个人配置（三态，优先于全局配置）。
 * 由服务端在玩家右键假人时下发数据打开。
 */
public class BotPanelScreen extends Screen {

    private final BotPanelData data;
    private final Screen parent;
    private int gameMode;
    private int selectedSlot;
    /** 与 BotSettings.KEYS 对齐的三态值（0=继承 1=开 2=关） */
    private final int[] overrides = new int[BotSettings.KEYS.length];
    private final ModernButton[] settingButtons = new ModernButton[BotSettings.KEYS.length];

    private int panelX, panelY, panelWidth, panelHeight;
    private int slotRowCenterX, slotRowY;

    public BotPanelScreen(BotPanelData data) {
        this(data, null);
    }

    public BotPanelScreen(BotPanelData data, Screen parent) {
        super(Component.literal(data.name() + " 的设置"));
        this.data = data;
        this.parent = parent;
        this.gameMode = data.gameMode();
        this.selectedSlot = data.selectedSlot();
        for (int i = 0; i < BotSettings.KEYS.length; i++) {
            this.overrides[i] = data.overrideId(BotSettings.KEYS[i]);
        }
    }

    @Override
    protected void init() {
        super.init();
        panelX = DesignTokens.PANEL_H_MARGIN;
        panelY = DesignTokens.PANEL_TOP_MARGIN;
        panelWidth = this.width - DesignTokens.PANEL_H_MARGIN * 2;
        panelHeight = this.height - DesignTokens.PANEL_TOP_MARGIN - DesignTokens.PANEL_BOTTOM_MARGIN;

        int pad = 12;
        int colGap = 12;
        int colW = (panelWidth - pad * 2 - colGap) / 2;
        int leftX = panelX + pad;
        int rightX = leftX + colW + colGap;

        int contentTop = panelY + 60;
        int buttonsTop = contentTop + 14;
        int rowH = DesignTokens.DONE_BUTTON_HEIGHT;
        int step = rowH + 5;

        // ===== 左侧：当前功能 =====
        this.addRenderableWidget(new ModernButton(leftX, buttonsTop, colW, rowH,
            Component.literal("打开背包"), b -> run("bot " + data.name() + " inventory")));
        this.addRenderableWidget(new ModernButton(leftX, buttonsTop + step, colW, rowH,
            Component.literal("打开末影箱"), b -> run("bot " + data.name() + " enderchest")));

        // 游戏模式（4 个按钮）
        int gmY = buttonsTop + step * 2;
        int gmGap = 4;
        int gmW = (colW - gmGap * 3) / 4;
        String[] gmNames = {"生存", "创造", "冒险", "旁观"};
        String[] gmIds = {"survival", "creative", "adventure", "spectator"};
        for (int i = 0; i < 4; i++) {
            final int mode = i;
            this.addRenderableWidget(new ModernButton(leftX + i * (gmW + gmGap), gmY, gmW, rowH,
                Component.literal(gmNames[i]), b -> {
                    this.gameMode = mode;
                    run("bot " + data.name() + " gamemode " + gmIds[mode]);
                }));
        }

        // 手持槽位 ◀ N ▶
        slotRowY = buttonsTop + step * 3;
        slotRowCenterX = leftX + colW / 2;
        this.addRenderableWidget(new ModernButton(leftX, slotRowY, 28, rowH,
            Component.literal("◀"), b -> changeSlot(-1)));
        this.addRenderableWidget(new ModernButton(leftX + colW - 28, slotRowY, 28, rowH,
            Component.literal("▶"), b -> changeSlot(1)));

        this.addRenderableWidget(new ModernButton(leftX, buttonsTop + step * 4, colW, rowH,
            Component.literal("停止动作"), b -> run("bot " + data.name() + " stop")));
        this.addRenderableWidget(new ModernButton(leftX, buttonsTop + step * 5, colW, rowH,
            Component.literal("传送到我"), b -> run("bot " + data.name() + " tphere")));
        this.addRenderableWidget(new ModernButton(leftX, buttonsTop + step * 6, colW, rowH,
            Component.literal("删除假人"), b -> {
                run("bot " + data.name() + " kill");
                this.onClose();
            }));

        // ===== 右侧：个人配置（优先于全局） =====
        for (int i = 0; i < BotSettings.KEYS.length; i++) {
            final int idx = i;
            final String key = BotSettings.KEYS[i];
            ModernButton btn = new ModernButton(rightX, buttonsTop + step * i, colW, rowH,
                Component.literal(settingLabel(key, overrides[i])), b -> cycleOverride(idx));
            settingButtons[i] = btn;
            this.addRenderableWidget(btn);
        }

        // 底部完成按钮
        this.addRenderableWidget(new ModernButton(
            panelX + (panelWidth - DesignTokens.DONE_BUTTON_WIDTH) / 2,
            panelY + panelHeight - DesignTokens.DONE_BUTTON_HEIGHT - 4,
            DesignTokens.DONE_BUTTON_WIDTH, DesignTokens.DONE_BUTTON_HEIGHT,
            Component.literal("完成"), b -> this.onClose()));
    }

    private void cycleOverride(int idx) {
        overrides[idx] = (overrides[idx] + 1) % 3;
        String key = BotSettings.KEYS[idx];
        if (settingButtons[idx] != null) {
            settingButtons[idx].setMessage(Component.literal(settingLabel(key, overrides[idx])));
        }
        // 发送 C2S 更新个人配置
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(data.name());
        buf.writeUtf(key);
        buf.writeVarInt(overrides[idx]);
        ClientPlayNetworking.send(BotNetworking.UPDATE_SETTING, buf);
    }

    private void changeSlot(int delta) {
        this.selectedSlot = Math.floorMod(this.selectedSlot + delta, 9);
        run("bot " + data.name() + " slot " + this.selectedSlot);
    }

    private void run(String command) {
        ClientPacketListener connection = this.minecraft != null ? this.minecraft.getConnection() : null;
        if (connection != null) {
            connection.sendCommand(command);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // 面板背景 + 边框（全局配置界面风格）
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, DesignTokens.PANEL_BG);
        drawBorder(graphics, panelX, panelY, panelWidth, panelHeight, DesignTokens.PANEL_BORDER);

        float sc = DesignTokens.TEXT_SCALE;
        int cx = panelX + panelWidth / 2;
        int y = panelY + 8;
        UI.drawScaledCentered(graphics, this.font, this.title, cx, y, sc * 1.2F, DesignTokens.HEADER_TEXT_COLOR);
        y += 16;
        UI.drawScaledCentered(graphics, this.font, Component.literal(
            "维度 " + shortDimension(data.dimension())
            + "  坐标 " + String.format("%.0f, %.0f, %.0f", data.x(), data.y(), data.z())), cx, y, sc, 0xFFAAAAAA);
        y += 10;
        UI.drawScaledCentered(graphics, this.font, Component.literal(
            "生命 " + String.format("%.1f/%.1f", data.health(), data.maxHealth())
            + "  游戏模式 " + gameModeName(gameMode)), cx, y, sc, 0xFFAAAAAA);

        // 分隔线
        int dividerY = panelY + 54;
        graphics.fill(panelX + 6, dividerY, panelX + panelWidth - 6, dividerY + 1, DesignTokens.HEADER_DIVIDER_COLOR);

        // 列标题
        int pad = 12;
        int colGap = 12;
        int colW = (panelWidth - pad * 2 - colGap) / 2;
        int leftX = panelX + pad;
        int rightX = leftX + colW + colGap;
        UI.drawScaled(graphics, this.font, Component.literal("操作"), leftX, panelY + 60, sc, DesignTokens.CARD_TITLE_COLOR);
        UI.drawScaled(graphics, this.font, Component.literal("个人配置（优先于全局）"), rightX, panelY + 60, sc, DesignTokens.CARD_TITLE_COLOR);

        // 手持槽位显示
        UI.drawScaledCentered(graphics, this.font, Component.literal("手持: " + (selectedSlot + 1)),
            slotRowCenterX, slotRowY + (DesignTokens.DONE_BUTTON_HEIGHT - UI.lineHeight(sc)) / 2, sc, 0xFFFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static String settingLabel(String key, int stateId) {
        return BotSettings.displayName(key) + ": " + stateLabel(stateId);
    }

    private static String stateLabel(int id) {
        return switch (id) {
            case 1 -> "开";
            case 2 -> "关";
            default -> "继承";
        };
    }

    private static String gameModeName(int id) {
        return switch (id) {
            case 0 -> "生存";
            case 1 -> "创造";
            case 2 -> "冒险";
            case 3 -> "旁观";
            default -> "未知";
        };
    }

    private static String shortDimension(String dim) {
        int idx = dim.indexOf(':');
        return idx >= 0 ? dim.substring(idx + 1) : dim;
    }

    @Override
    public void onClose() {
        if (parent != null && this.minecraft != null) {
            this.minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
