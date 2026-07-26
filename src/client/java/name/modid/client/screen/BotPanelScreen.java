package name.modid.client.screen;

import name.modid.bot.BotSettings;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.UI;
import name.modid.net.BotNetworking;
import name.modid.net.BotPanelData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
    /** 与 BotSettings.KEYS 对齐的三态值（0=继承 1=开 2=关） */
    private final int[] overrides = new int[BotSettings.KEYS.length];
    private final ModernButton[] settingButtons = new ModernButton[BotSettings.KEYS.length];

    private int panelX, panelY, panelWidth, panelHeight;
    private ModernButton deleteButton;
    private boolean confirmingDelete = false;

    public BotPanelScreen(BotPanelData data) {
        this(data, null);
    }

    public BotPanelScreen(BotPanelData data, Screen parent) {
        super(Component.translatable("gui.my-bot-mod.panel.title", data.name()));
        this.data = data;
        this.parent = parent;
        this.gameMode = data.gameMode();
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
        int rowH = DesignTokens.ROW_HEIGHT;
        int step = rowH + 3;

        // ===== 左侧：当前功能 =====
        this.addRenderableWidget(new ModernButton(leftX, buttonsTop, colW, rowH,
            Component.translatable("gui.my-bot-mod.panel.open_inventory"), b -> run("bot " + data.name() + " inventory")));
        this.addRenderableWidget(new ModernButton(leftX, buttonsTop + step, colW, rowH,
            Component.translatable("gui.my-bot-mod.panel.open_enderchest"), b -> run("bot " + data.name() + " enderchest")));

        // 游戏模式（4 个按钮）
        int gmY = buttonsTop + step * 2;
        int gmGap = 4;
        int gmW = (colW - gmGap * 3) / 4;
        String[] gmIds = {"survival", "creative", "adventure", "spectator"};
        for (int i = 0; i < 4; i++) {
            final int mode = i;
            this.addRenderableWidget(new ModernButton(leftX + i * (gmW + gmGap), gmY, gmW, rowH,
                Component.translatable("gameMode." + gmIds[i]), b -> {
                    this.gameMode = mode;
                    run("bot " + data.name() + " gamemode " + gmIds[mode]);
                }));
        }

        this.addRenderableWidget(new ModernButton(leftX, buttonsTop + step * 3, colW, rowH,
            Component.translatable("gui.my-bot-mod.panel.stop"), b -> run("bot " + data.name() + " stop")));
        this.addRenderableWidget(new ModernButton(leftX, buttonsTop + step * 4, colW, rowH,
            Component.translatable("gui.my-bot-mod.panel.tphere"), b -> run("bot " + data.name() + " tphere")));
        deleteButton = new ModernButton(leftX, buttonsTop + step * 5, colW, rowH,
            Component.translatable("gui.my-bot-mod.panel.delete"), b -> onDeleteClicked());
        this.addRenderableWidget(deleteButton);

        // ===== 右侧：个人配置（优先于全局） =====
        for (int i = 0; i < BotSettings.KEYS.length; i++) {
            final int idx = i;
            final String key = BotSettings.KEYS[i];
            ModernButton btn = new ModernButton(rightX, buttonsTop + step * i, colW, rowH,
                settingLabel(key, overrides[i]), b -> cycleOverride(idx));
            settingButtons[i] = btn;
            this.addRenderableWidget(btn);
        }

        // 底部完成按钮
        this.addRenderableWidget(new ModernButton(
            panelX + (panelWidth - DesignTokens.DONE_BUTTON_WIDTH) / 2,
            panelY + panelHeight - DesignTokens.DONE_BUTTON_HEIGHT - 4,
            DesignTokens.DONE_BUTTON_WIDTH, DesignTokens.DONE_BUTTON_HEIGHT,
            net.minecraft.network.chat.CommonComponents.GUI_DONE, b -> this.onClose()));
    }

    private void cycleOverride(int idx) {
        overrides[idx] = (overrides[idx] + 1) % 3;
        String key = BotSettings.KEYS[idx];
        if (settingButtons[idx] != null) {
            settingButtons[idx].setMessage(settingLabel(key, overrides[idx]));
        }
        // 发送 C2S 更新个人配置
        FriendlyByteBuf buf = BotNetworking.c2s();
        buf.writeUtf(data.name());
        buf.writeUtf(key);
        buf.writeVarInt(overrides[idx]);
        ClientPlayNetworking.send(BotNetworking.UPDATE_SETTING, buf);
    }

    private void run(String command) {
        ClientPacketListener connection = this.minecraft != null ? this.minecraft.getConnection() : null;
        if (connection != null) {
            connection.sendCommand(command);
        }
    }

    /** 删除假人：二次确认（首次点击仅提示，再次点击才执行） */
    private void onDeleteClicked() {
        if (!confirmingDelete) {
            confirmingDelete = true;
            deleteButton.setMessage(Component.translatable("gui.my-bot-mod.panel.confirm_delete"));
            return;
        }
        run("bot " + data.name() + " kill");
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if >=1.20.2 {
        /*this.renderBackground(graphics, mouseX, mouseY, partialTick);
        *///?} else {
        this.renderBackground(graphics);
        //?}

        // 面板背景 + 边框（全局配置界面风格）
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, DesignTokens.PANEL_BG);
        UI.border(graphics, panelX, panelY, panelWidth, panelHeight, DesignTokens.PANEL_BORDER);

        float sc = DesignTokens.TEXT_SCALE;
        int cx = panelX + panelWidth / 2;
        int y = panelY + 8;
        UI.drawScaledCentered(graphics, this.font, this.title, cx, y, sc * 1.2F, DesignTokens.HEADER_TEXT_COLOR);
        y += 16;
        UI.drawScaledCentered(graphics, this.font, Component.translatable(
            "gui.my-bot-mod.panel.status_food",
            Component.translatable(statusKey(data.status())),
            data.food()), cx, y, sc, statusColor(data.status()));
        y += 10;
        UI.drawScaledCentered(graphics, this.font, Component.translatable(
            "gui.my-bot-mod.panel.dim_pos", shortDimension(data.dimension()),
            String.format("%.0f, %.0f, %.0f", data.x(), data.y(), data.z())), cx, y, sc, 0xFFAAAAAA);
        y += 10;
        UI.drawScaledCentered(graphics, this.font, Component.translatable(
            "gui.my-bot-mod.panel.health_gamemode",
            String.format("%.1f/%.1f", data.health(), data.maxHealth()),
            gameModeName(gameMode)), cx, y, sc, 0xFFAAAAAA);

        // 分隔线
        int dividerY = panelY + 54;
        graphics.fill(panelX + 6, dividerY, panelX + panelWidth - 6, dividerY + 1, DesignTokens.HEADER_DIVIDER_COLOR);

        // 列标题
        int pad = 12;
        int colGap = 12;
        int colW = (panelWidth - pad * 2 - colGap) / 2;
        int leftX = panelX + pad;
        int rightX = leftX + colW + colGap;
        UI.drawScaled(graphics, this.font, Component.translatable("gui.my-bot-mod.panel.actions_col"), leftX, panelY + 60, sc, DesignTokens.CARD_TITLE_COLOR);
        UI.drawScaled(graphics, this.font, Component.translatable("gui.my-bot-mod.panel.settings_col"), rightX, panelY + 60, sc, DesignTokens.CARD_TITLE_COLOR);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Component settingLabel(String key, int stateId) {
        return Component.literal(BotSettings.displayName(key) + ": ").append(stateLabel(stateId));
    }

    private static Component stateLabel(int id) {
        return switch (id) {
            case 1 -> Component.translatable("gui.my-bot-mod.state.on");
            case 2 -> Component.translatable("gui.my-bot-mod.state.off");
            default -> Component.translatable("gui.my-bot-mod.state.inherit");
        };
    }

    private static Component gameModeName(int id) {
        return switch (id) {
            case 0 -> Component.translatable("gameMode.survival");
            case 1 -> Component.translatable("gameMode.creative");
            case 2 -> Component.translatable("gameMode.adventure");
            case 3 -> Component.translatable("gameMode.spectator");
            default -> Component.translatable("gui.my-bot-mod.panel.unknown");
        };
    }

    private static String shortDimension(String dim) {
        int idx = dim.indexOf(':');
        return idx >= 0 ? dim.substring(idx + 1) : dim;
    }

    /** 活动状态对应的翻译键 */
    private static String statusKey(int status) {
        return switch (status) {
            case 1 -> "gui.my-bot-mod.status.combat";
            case 2 -> "gui.my-bot-mod.status.pathfinding";
            case 3 -> "gui.my-bot-mod.status.following";
            default -> "gui.my-bot-mod.status.idle";
        };
    }

    /** 活动状态对应的显示颜色 */
    private static int statusColor(int status) {
        return switch (status) {
            case 1 -> 0xFFFF6B6B;
            case 2 -> 0xFFFFD93D;
            case 3 -> 0xFF6BCB77;
            default -> 0xFF9E9E9E;
        };
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
