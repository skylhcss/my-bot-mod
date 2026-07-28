package name.modid.client.screen;

import name.modid.client.screen.pages.BehaviorPage;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.UI;
import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 单个假人的行为管理界面（右键假人面板 → 行为管理，每假人各具一个）
 *
 * - 内容复用 BehaviorPage（播放列表/启动停止/可用行为/错误）
 * - 打开即请求，之后每 40 tick 轮询一次；收到 BEHAVIOR_LIST 包立即重建（无需退出重进）
 */
public class BotBehaviorScreen extends Screen {

    /** 轮询间隔（tick）：行为运行状态、外部文件变化的兜底刷新 */
    private static final int POLL_INTERVAL = 40;

    private final String botName;
    private final Screen parent;
    private final BehaviorPage page;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentY;
    private int contentHeight;
    private int tickCounter;

    public BotBehaviorScreen(String botName, Screen parent) {
        super(Component.translatable("gui.my-bot-mod.behavior.screen_title", botName));
        this.botName = botName;
        this.parent = parent;
        this.page = new BehaviorPage(botName, ModConfig.getInstance());
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(360, this.width - 40);
        panelHeight = Math.min(240, this.height - 40);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        contentY = panelY + 30;
        contentHeight = panelHeight - 30 - 26;

        int pad = DesignTokens.SCROLL_AREA_PADDING;
        page.init(panelX + pad, contentY, panelWidth - pad * 2, contentHeight, this.minecraft, this.font);
        page.sendRequest();

        // 底部：返回面板
        this.addRenderableWidget(new ModernButton(
            panelX + (panelWidth - DesignTokens.DONE_BUTTON_WIDTH) / 2,
            panelY + panelHeight - 20,
            DesignTokens.DONE_BUTTON_WIDTH, DesignTokens.DONE_BUTTON_HEIGHT,
            CommonComponents.GUI_BACK, b -> onClose()));
    }

    /** 收到 BEHAVIOR_LIST 包时由客户端接收器调用：就地重建内容 */
    public void refresh() {
        int pad = DesignTokens.SCROLL_AREA_PADDING;
        page.init(panelX + pad, contentY, panelWidth - pad * 2, contentHeight, this.minecraft, this.font);
    }

    @Override
    public void tick() {
        super.tick();
        if (++tickCounter % POLL_INTERVAL == 0) {
            page.sendRequest();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if >=1.20.2 {
        /*this.renderBackground(graphics, mouseX, mouseY, partialTick);
        *///?} else {
        this.renderBackground(graphics);
        //?}
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, DesignTokens.PANEL_BG);
        UI.border(graphics, panelX, panelY, panelWidth, panelHeight, DesignTokens.PANEL_BORDER);

        float sc = DesignTokens.TEXT_SCALE;
        UI.drawScaledCentered(graphics, this.font, this.title,
            panelX + panelWidth / 2, panelY + 8, sc * 1.15F, DesignTokens.HEADER_TEXT_COLOR);
        // 运行状态徽标
        boolean running = page.isRunning();
        UI.drawScaledCentered(graphics, this.font,
            Component.translatable(running
                ? "gui.my-bot-mod.behavior.state.running" : "gui.my-bot-mod.behavior.state.idle"),
            panelX + panelWidth / 2, panelY + 19, sc * 0.9F,
            running ? DesignTokens.ACCENT : 0xFF888888);

        page.render(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (page.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (page.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    //? if >=1.20.2 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        if (page.mouseScrolled(mouseX, mouseY, 0, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (page.mouseScrolled(mouseX, mouseY, 0, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    //?}

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
