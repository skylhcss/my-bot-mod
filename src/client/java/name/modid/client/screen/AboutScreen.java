package name.modid.client.screen;

import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.UI;
import name.modid.config.ModConfig;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 关于界面（匹配配置面板风格）
 */
public class AboutScreen extends Screen {

    private final Screen parent;
    private final ModConfig config;
    private int panelX, panelY, panelWidth, panelHeight;

    public AboutScreen(Screen parent) {
        super(Component.literal("关于"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = this.width - DesignTokens.PANEL_H_MARGIN * 2;
        panelX = (this.width - panelWidth) / 2;
        panelY = DesignTokens.PANEL_TOP_MARGIN;
        panelHeight = this.height - DesignTokens.PANEL_TOP_MARGIN - DesignTokens.PANEL_BOTTOM_MARGIN;

        int btnW = DesignTokens.DONE_BUTTON_WIDTH;
        int btnH = DesignTokens.DONE_BUTTON_HEIGHT;
        int cx = panelX + (panelWidth - btnW) / 2;

        int githubBtnY = panelY + panelHeight - DesignTokens.FOOTER_HEIGHT - btnH - 4;
        this.addRenderableWidget(new ModernButton(cx, githubBtnY, btnW, btnH,
            Component.literal("GitHub 仓库"),
            button -> {
                try { Util.getPlatform().openUri(config.githubRepo); }
                catch (Exception e) { /* ignore */ }
            }));

        int doneY = panelY + panelHeight - btnH - 4;
        this.addRenderableWidget(new ModernButton(cx, doneY, btnW, btnH,
            Component.literal("完成"),
            button -> this.minecraft.setScreen(parent)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, DesignTokens.PANEL_BG);
        drawBorder(graphics, panelX, panelY, panelWidth, panelHeight, DesignTokens.PANEL_BORDER);

        float sc = DesignTokens.TEXT_SCALE;
        int lineH = UI.lineHeight(sc);
        int cx = panelX + panelWidth / 2;
        int y = panelY + 14;
        int padX = panelX + DesignTokens.CARD_H_PADDING;

        UI.drawScaledCentered(graphics, this.font, Component.literal("关于"), cx, y, sc, DesignTokens.HEADER_TEXT_COLOR);
        y += lineH + 10;
        graphics.fill(panelX + 6, y, panelX + panelWidth - 6, y + 1, DesignTokens.HEADER_DIVIDER_COLOR);
        y += 6;

        UI.drawScaledCentered(graphics, this.font, Component.literal(config.modName), cx, y, sc * 1.2F, 0xFFFF55);
        y += (int) (lineH * 1.4F) + 4;
        UI.drawScaledCentered(graphics, this.font, "v" + config.modVersion, cx, y, sc, 0xFFAAAAAA);
        y += lineH + 2;
        UI.drawScaledCentered(graphics, this.font, config.author, cx, y, sc, 0xFFE0E0E0);
        y += lineH + 2;
        UI.drawScaledCentered(graphics, this.font, config.license, cx, y, sc, 0xFFAAAAAA);
        y += lineH + 10;

        graphics.fill(panelX + 6, y, panelX + panelWidth - 6, y + 1, DesignTokens.HEADER_DIVIDER_COLOR);
        y += 6;
        UI.drawScaled(graphics, this.font, config.description, padX, y, sc, 0xFFE0E0E0);
        y += lineH + 4;
        UI.drawScaled(graphics, this.font, "Minecraft 1.20.1 · Fabric Loader", padX, y, sc, 0xFFAAAAAA);
        y += lineH + 10;

        graphics.fill(panelX + 6, y, panelX + panelWidth - 6, y + 1, DesignTokens.HEADER_DIVIDER_COLOR);
        y += 6;
        UI.drawScaled(graphics, this.font, "邮箱: " + config.email, padX, y, sc, 0xFFAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
