package name.modid.client.screen;

import name.modid.client.screen.pages.*;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.TabButton;
import name.modid.client.screen.widget.UI;
import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 现代化配置界面主屏幕（全屏布局）
 *
 * 布局自上而下：
 *   顶部标题栏 → Tab 栏 → 滚动内容区（Section 卡片）→ 底部完成按钮
 *
 * 所有文字使用 {@link DesignTokens#TEXT_SCALE} 小号字体，整体更紧凑。
 */
public class ModernConfigScreen extends Screen {

    private final Screen parent;
    private final ModConfig config;

    // 全屏面板布局
    private int panelX, panelY, panelWidth, panelHeight;

    // 各区域
    private int tabBarY;
    private int contentY;
    private int contentHeight;
    private int footerY;

    // Tab 栏
    private final List<TabButton> tabButtons = new ArrayList<>();
    /** 当前页面索引（static，在子屏幕往返后保持选中状态） */
    private static int currentPageIndex = 0;

    // 页面
    private final List<ConfigPage> pages = new ArrayList<>();
    private ConfigPage currentPage;

    public ModernConfigScreen(Screen parent) {
        super(Component.literal("My Bot Mod - 配置"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        tabButtons.clear();
        pages.clear();

        // ===== 全屏面板（仅留少量边距） =====
        panelX = DesignTokens.PANEL_H_MARGIN;
        panelY = DesignTokens.PANEL_TOP_MARGIN;
        panelWidth = this.width - DesignTokens.PANEL_H_MARGIN * 2;
        panelHeight = this.height - DesignTokens.PANEL_TOP_MARGIN - DesignTokens.PANEL_BOTTOM_MARGIN;

        // 自上而下分配各区域 Y 坐标（无标题栏，Tab 紧贴面板顶部）
        tabBarY = panelY + DesignTokens.SCROLL_AREA_PADDING;
        footerY = panelY + panelHeight - DesignTokens.FOOTER_HEIGHT;
        contentY = tabBarY + DesignTokens.TAB_BAR_HEIGHT + DesignTokens.TAB_BAR_BOTTOM_GAP;
        contentHeight = footerY - DesignTokens.FOOTER_TOP_GAP - contentY;

        // 创建页面
        pages.add(new GeneralPage(this, config));
        pages.add(new CombatPage(this, config));
        pages.add(new SurvivalPage(this, config));
        pages.add(new AdvancedPage(this, config));

        // 创建 Tab 栏（均匀分布，留出卡片左右内边距宽度）
        int tabCount = pages.size();
        int tabBarLeft = panelX + DesignTokens.SCROLL_AREA_PADDING;
        int tabBarWidth = panelWidth - DesignTokens.SCROLL_AREA_PADDING * 2;
        int tabWidth = (tabBarWidth - DesignTokens.TAB_BAR_GAP * (tabCount - 1)) / tabCount;

        for (int i = 0; i < tabCount; i++) {
            final int index = i;
            int tabX = tabBarLeft + i * (tabWidth + DesignTokens.TAB_BAR_GAP);

            TabButton tab = new TabButton(tabX, tabBarY, tabWidth, DesignTokens.TAB_BAR_HEIGHT,
                pages.get(i).getTitle(), btn -> switchPage(index));
            tabButtons.add(tab);
            this.addRenderableWidget(tab);
        }

        // 激活之前选中的 Tab
        if (!tabButtons.isEmpty()) {
            int activeIndex = Math.min(currentPageIndex, tabButtons.size() - 1);
            for (int i = 0; i < tabButtons.size(); i++) {
                tabButtons.get(i).setActive(i == activeIndex);
            }
            currentPageIndex = activeIndex;
        }

        // 初始化当前页面
        initCurrentPage();

        // 完成按钮（底部居中）
        int doneWidth = DesignTokens.DONE_BUTTON_WIDTH;
        int doneHeight = DesignTokens.DONE_BUTTON_HEIGHT;
        int doneX = panelX + (panelWidth - doneWidth) / 2;
        int doneY = footerY + (DesignTokens.FOOTER_HEIGHT - doneHeight) / 2;
        this.addRenderableWidget(
            new ModernButton(doneX, doneY, doneWidth, doneHeight,
                Component.literal("完成"),
                button -> { config.save(); this.minecraft.setScreen(parent); }
            )
        );
    }

    private void initCurrentPage() {
        currentPage = pages.get(currentPageIndex);

        // 滚动内容区：Tab 栏下方到页脚上方，左右对齐面板内边界
        int areaX = panelX + DesignTokens.SCROLL_AREA_PADDING;
        int areaWidth = panelWidth - DesignTokens.SCROLL_AREA_PADDING * 2;

        currentPage.init(areaX, contentY, areaWidth, contentHeight, this.minecraft, this.font);
    }

    private void switchPage(int index) {
        if (index < 0 || index >= pages.size()) return;

        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).setActive(i == index);
        }

        currentPageIndex = index;
        initCurrentPage();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制背景
        this.renderBackground(graphics);

        // 绘制全屏面板背景
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, DesignTokens.PANEL_BG);
        drawBorder(graphics, panelX, panelY, panelWidth, panelHeight, DesignTokens.PANEL_BORDER);

        // 页脚上分隔线
        int footerDividerY = footerY - DesignTokens.FOOTER_TOP_GAP / 2;
        graphics.fill(panelX + DesignTokens.SCROLL_AREA_PADDING, footerDividerY,
                     panelX + panelWidth - DesignTokens.SCROLL_AREA_PADDING, footerDividerY + 1,
                     DesignTokens.HEADER_DIVIDER_COLOR);

        // 绘制当前页面（在面板内部）
        if (currentPage != null) {
            currentPage.render(graphics, mouseX, mouseY, partialTick);
        }

        // 绘制 Tab 和按钮（由父类渲染）
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (currentPage != null) return currentPage.mouseClicked(mouseX, mouseY, button);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 优先交给滚动条拖动
        if (currentPage != null && currentPage.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (currentPage != null && currentPage.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentPage != null && currentPage.mouseScrolled(mouseX, mouseY, 0, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        config.save();
        this.minecraft.setScreen(parent);
    }
}
