package name.modid.client.screen;

import name.modid.client.screen.pages.*;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.TabButton;
import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 现代化配置界面主屏幕
 * 居中面板 + 顶部 Tab 栏 + Section 卡片分组
 */
public class ModernConfigScreen extends Screen {
    
    private final Screen parent;
    private final ModConfig config;
    
    // 面板布局
    private int panelX, panelY, panelWidth, panelHeight;
    
    // Tab 栏
    private final List<TabButton> tabButtons = new ArrayList<>();
    private int currentPageIndex = 0;
    
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
        
        // 计算居中面板
        panelWidth = Math.min(DesignTokens.PANEL_MAX_WIDTH, this.width - DesignTokens.PANEL_H_PADDING * 2);
        panelX = (this.width - panelWidth) / 2;
        panelY = DesignTokens.PANEL_TOP_MARGIN;
        panelHeight = this.height - DesignTokens.PANEL_TOP_MARGIN - DesignTokens.PANEL_BOTTOM_MARGIN;
        
        // 创建页面
        pages.add(new GeneralPage(this, config));
        pages.add(new CombatPage(this, config));
        pages.add(new SurvivalPage(this, config));
        pages.add(new AdvancedPage(this, config));
        
        // 创建 Tab 栏
        int tabCount = pages.size();
        int tabWidth = (panelWidth - DesignTokens.TAB_BAR_GAP * (tabCount - 1)) / tabCount;
        int tabY = panelY;
        
        for (int i = 0; i < tabCount; i++) {
            final int index = i;
            int tabX = panelX + i * (tabWidth + DesignTokens.TAB_BAR_GAP);
            
            TabButton tab = new TabButton(tabX, tabY, tabWidth, DesignTokens.TAB_BAR_HEIGHT,
                pages.get(i).getTitle(), btn -> switchPage(index));
            tabButtons.add(tab);
            this.addRenderableWidget(tab);
        }
        
        // 激活第一个 Tab
        if (!tabButtons.isEmpty()) {
            tabButtons.get(0).setActive(true);
        }
        
        // 初始化当前页面
        initCurrentPage();
        
        // 完成按钮
        int doneX = (this.width - DesignTokens.DONE_BUTTON_WIDTH) / 2;
        int doneY = panelY + panelHeight + 8;
        this.addRenderableWidget(
            new name.modid.client.screen.widget.ModernButton(
                doneX, doneY, DesignTokens.DONE_BUTTON_WIDTH, DesignTokens.DONE_BUTTON_HEIGHT,
                Component.literal("完成"),
                button -> { config.save(); this.minecraft.setScreen(parent); }
            )
        );
    }
    
    private void initCurrentPage() {
        currentPage = pages.get(currentPageIndex);
        
        // 滚动区域：Tab 栏下方到面板底部
        int areaX = panelX;
        int areaY = panelY + DesignTokens.TAB_BAR_HEIGHT + DesignTokens.CONTENT_TOP;
        int areaWidth = panelWidth;
        int areaHeight = panelY + panelHeight - areaY;
        
        currentPage.init(areaX, areaY, areaWidth, areaHeight, this.minecraft, this.font);
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
        
        // 绘制面板背景
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, DesignTokens.PANEL_BG);
        drawBorder(graphics, panelX, panelY, panelWidth, panelHeight, DesignTokens.PANEL_BORDER);
        
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
