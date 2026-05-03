package name.modid.client.screen;

import name.modid.client.screen.pages.*;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.SidebarButton;
import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 现代化配置界面主屏幕
 * 采用左侧边栏 + 右侧内容的布局
 */
public class ModernConfigScreen extends Screen {
    
    private final Screen parent;
    private final ModConfig config;
    
    // 侧边栏按钮
    private final List<SidebarButton> sidebarButtons = new ArrayList<>();
    private int currentPageIndex = 0;
    
    // 页面
    private final List<ConfigPage> pages = new ArrayList<>();
    private ConfigPage currentPage;
    
    // 布局常量
    private static final int SIDEBAR_WIDTH = 80;
    private static final int PADDING = 10;
    private static final int TITLE_HEIGHT = 30;
    private static final int BUTTON_HEIGHT = 20;
    
    public ModernConfigScreen(Screen parent) {
        super(Component.literal("My Bot Mod - 配置"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 清空之前的数据
        sidebarButtons.clear();
        pages.clear();
        
        // 创建页面
        pages.add(new GeneralPage(this, config));
        pages.add(new AttackPage(this, config));
        pages.add(new MountPage(this, config));
        pages.add(new SurvivalPage(this, config));
        pages.add(new AdvancedPage(this, config));
        
        // 创建侧边栏按钮
        int buttonHeight = 24;
        int buttonSpacing = 4;
        int startY = TITLE_HEIGHT + PADDING;
        
        for (int i = 0; i < pages.size(); i++) {
            final int index = i;
            ConfigPage page = pages.get(i);
            
            SidebarButton button = new SidebarButton(
                PADDING,
                startY + i * (buttonHeight + buttonSpacing),
                SIDEBAR_WIDTH - PADDING * 2,
                buttonHeight,
                page.getTitle(),
                btn -> switchPage(index)
            );
            
            sidebarButtons.add(button);
            this.addRenderableWidget(button);
        }
        
        // 设置第一个按钮为激活状态
        if (!sidebarButtons.isEmpty()) {
            sidebarButtons.get(0).setActive(true);
            currentPage = pages.get(0);
            currentPage.init(this.width, this.height, this.minecraft, this.font);
        }
        
        // 添加底部按钮
        int bottomButtonWidth = 100;
        int bottomButtonHeight = 20;
        int bottomY = this.height - PADDING - bottomButtonHeight;
        
        // 重置按钮
        this.addRenderableWidget(
            new ModernButton(
                PADDING,
                bottomY,
                bottomButtonWidth,
                bottomButtonHeight,
                Component.literal("重置为默认"),
                button -> {
                    config.reset();
                    this.minecraft.setScreen(new ModernConfigScreen(parent));
                }
            )
        );
        
        // 完成按钮
        this.addRenderableWidget(
            new ModernButton(
                this.width - PADDING - bottomButtonWidth,
                bottomY,
                bottomButtonWidth,
                bottomButtonHeight,
                Component.literal("完成"),
                button -> {
                    config.save();
                    this.minecraft.setScreen(parent);
                }
            )
        );
    }
    
    /**
     * 切换页面
     */
    private void switchPage(int index) {
        if (index < 0 || index >= pages.size()) {
            return;
        }
        
        // 更新侧边栏按钮状态
        for (int i = 0; i < sidebarButtons.size(); i++) {
            sidebarButtons.get(i).setActive(i == index);
        }
        
        // 切换页面
        currentPageIndex = index;
        currentPage = pages.get(index);
        currentPage.init(this.width, this.height, this.minecraft, this.font);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制背景
        this.renderBackground(graphics);
        
        // 绘制侧边栏背景
        graphics.fill(0, 0, SIDEBAR_WIDTH, this.height, 0xE0000000);
        
        // 绘制标题
        graphics.drawString(this.font, "配置", PADDING, PADDING, 0xFFFFFF);
        
        // 绘制内容区域背景
        int contentX = SIDEBAR_WIDTH + PADDING;
        int contentY = TITLE_HEIGHT;
        int contentWidth = this.width - SIDEBAR_WIDTH - PADDING * 2;
        int contentHeight = this.height - TITLE_HEIGHT - PADDING - BUTTON_HEIGHT - PADDING;
        
        graphics.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, 0x90000000);
        
        // 绘制当前页面
        if (currentPage != null) {
            currentPage.render(graphics, mouseX, mouseY, partialTick);
        }
        
        // 绘制所有组件（包括侧边栏按钮和底部按钮）
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先让页面内的组件处理点击
        if (currentPage != null) {
            // 调整鼠标Y坐标以匹配滚动偏移
            for (var widget : currentPage.widgets) {
                int screenY = (int)(widget.getY() - currentPage.scrollOffset);
                int originalY = widget.getY();
                widget.setY(screenY);
                
                if (widget.isMouseOver(mouseX, mouseY) && widget.mouseClicked(mouseX, mouseY, button)) {
                    widget.setY(originalY);
                    return true;
                }
                
                widget.setY(originalY);
            }
        }
        
        // 然后让父类处理（侧边栏按钮和底部按钮）
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 传递拖动事件到页面内的组件
        if (currentPage != null) {
            for (var widget : currentPage.widgets) {
                int screenY = (int)(widget.getY() - currentPage.scrollOffset);
                int originalY = widget.getY();
                widget.setY(screenY);
                
                if (widget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                    widget.setY(originalY);
                    return true;
                }
                
                widget.setY(originalY);
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 传递释放事件到页面内的组件
        if (currentPage != null) {
            for (var widget : currentPage.widgets) {
                int screenY = (int)(widget.getY() - currentPage.scrollOffset);
                int originalY = widget.getY();
                widget.setY(screenY);
                
                if (widget.mouseReleased(mouseX, mouseY, button)) {
                    widget.setY(originalY);
                    return true;
                }
                
                widget.setY(originalY);
            }
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 传递滚动事件到当前页面
        if (currentPage != null && currentPage.mouseScrolled(mouseX, mouseY, 0, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public void onClose() {
        config.save();
        this.minecraft.setScreen(parent);
    }
    
    /**
     * 获取内容区域的边界
     */
    public int getContentX() {
        return SIDEBAR_WIDTH + PADDING * 2;
    }
    
    public int getContentY() {
        return TITLE_HEIGHT + PADDING;
    }
    
    public int getContentWidth() {
        return this.width - SIDEBAR_WIDTH - PADDING * 4;
    }
    
    public int getContentHeight() {
        return this.height - TITLE_HEIGHT - PADDING * 3 - BUTTON_HEIGHT;
    }
}
