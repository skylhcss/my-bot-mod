package name.modid.client.screen;

import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.UI;
import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 快捷键配置界面（minihud 风格）
 * 支持组合键：按住 Ctrl/Shift/Alt + 主键
 */
public class KeybindConfigScreen extends Screen {

    private final Screen parent;
    private final ModConfig config;
    private int panelX, panelY, panelWidth, panelHeight;
    private boolean capturing = false;

    public KeybindConfigScreen(Screen parent) {
        super(Component.literal("快捷键配置"));
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
        int cx = panelX + panelWidth / 2;

        int captureBtnY = panelY + 60;
        this.addRenderableWidget(new ModernButton(
            cx - 60, captureBtnY, 120, btnH,
            Component.literal(getKeybindDisplay()),
            button -> {
                capturing = true;
                button.setMessage(Component.literal("按下按键..."));
            }
        ));

        this.addRenderableWidget(new ModernButton(
            cx - 40, captureBtnY + btnH + 8, 80, btnH,
            Component.literal("重置"),
            button -> {
                config.configMenuKey = "key.keyboard.b";
                config.save();
                updateCaptureButton();
            }
        ));

        int doneY = panelY + panelHeight - btnH - 4;
        this.addRenderableWidget(new ModernButton(
            cx - btnW / 2, doneY, btnW, btnH,
            Component.literal("完成"),
            button -> this.minecraft.setScreen(parent)
        ));
    }

    private String getKeybindDisplay() {
        return formatKeybind(config.configMenuKey);
    }

    private static String formatKeybind(String keybind) {
        if (keybind == null || keybind.isEmpty()) return "未设置";
        return keybind
            .replace("key.keyboard.", "")
            .replace("LEFT_", "")
            .replace("RIGHT_", "")
            .toUpperCase();
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

        UI.drawScaledCentered(graphics, this.font, Component.literal("快捷键配置"), cx, y, sc, DesignTokens.HEADER_TEXT_COLOR);
        y += lineH + 10;
        graphics.fill(panelX + 6, y, panelX + panelWidth - 6, y + 1, DesignTokens.HEADER_DIVIDER_COLOR);
        y += 8;

        UI.drawScaledCentered(graphics, this.font, Component.literal("打开配置菜单"), cx, y, sc, DesignTokens.ITEM_TEXT_COLOR);
        y += lineH + 12;

        if (capturing) {
            UI.drawScaledCentered(graphics, this.font, Component.literal("按下任意键设置快捷键"), cx, y + 42, sc, 0xFFFF55);
            UI.drawScaledCentered(graphics, this.font, Component.literal("支持组合键: 按住 Ctrl/Shift/Alt + 主键"), cx, y + 54, sc, 0xFFAAAAAA);
            UI.drawScaledCentered(graphics, this.font, Component.literal("按 ESC 取消"), cx, y + 66, sc, 0xFFAAAAAA);
        } else {
            UI.drawScaledCentered(graphics, this.font, Component.literal("点击按钮后按下新快捷键"), cx, y + 42, sc, 0xFFAAAAAA);
            UI.drawScaledCentered(graphics, this.font, Component.literal("支持组合键: Ctrl/Shift/Alt + 主键"), cx, y + 54, sc, 0xFF808080);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing) {
            if (keyCode == 256) { // ESC
                capturing = false;
                updateCaptureButton();
                return true;
            }

            String keyName = org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, scanCode);
            if (keyName == null) keyName = getSpecialKeyName(keyCode);

            if (keyName != null) {
                StringBuilder sb = new StringBuilder();
                if ((modifiers & 2) != 0) sb.append("ctrl+");
                if ((modifiers & 1) != 0) sb.append("shift+");
                if ((modifiers & 4) != 0) sb.append("alt+");
                sb.append(keyName);

                config.configMenuKey = "key.keyboard." + sb.toString().toLowerCase();
                config.save();
                capturing = false;
                updateCaptureButton();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String getSpecialKeyName(int keyCode) {
        return switch (keyCode) {
            case 290 -> "f1"; case 291 -> "f2"; case 292 -> "f3";
            case 293 -> "f4"; case 294 -> "f5"; case 295 -> "f6";
            case 296 -> "f7"; case 297 -> "f8"; case 298 -> "f9";
            case 299 -> "f10"; case 300 -> "f11"; case 301 -> "f12";
            case 32 -> "space"; case 257 -> "enter"; case 258 -> "tab";
            case 259 -> "backspace"; case 261 -> "delete";
            case 265 -> "up"; case 264 -> "down";
            case 263 -> "left"; case 262 -> "right";
            default -> null;
        };
    }

    private void updateCaptureButton() {
        this.children().stream()
            .filter(w -> w instanceof ModernButton)
            .findFirst()
            .ifPresent(w -> ((ModernButton) w).setMessage(Component.literal(getKeybindDisplay())));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
