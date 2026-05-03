package name.modid.client.screen.pages;

import name.modid.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置页面基类
 * 所有配置页面都继承此类
 */
public abstract class ConfigPage {
    
    protected final ModConfig config;
    public final List<AbstractWidget> widgets = new ArrayList<>();
    protected Minecraft minecraft;
    protected Font font;
    
    protected int width;
    protected int height;
    protected int contentX;
    protected int contentY;
    protected int contentWidth;
    protected int contentHeight;
    
    // 滚动相关
    public double scrollOffset = 0;
    protected double maxScrollOffset = 0;
    
    // 布局常量
    protected static final int ITEM_HEIGHT = 24;
    protected static final int ITEM_SPACING = 6;
    protected static final int GROUP_SPACING = 20;
    
    public ConfigPage(ModConfig config) {
        this.config = config;
    }
    
    /**
     * 初始化页面
     */
    public void init(int screenWidth, int screenHeight, Minecraft minecraft, Font font) {
        this.width = screenWidth;
        this.height = screenHeight;
        this.minecraft = minecraft;
        this.font = font;
        
        // 计算内容区域（从主屏幕获取）
        int sidebarWidth = 80;
        int padding = 10;
        int titleHeight = 30;
        int buttonHeight = 20;
        
        this.contentX = sidebarWidth + padding * 2;
        this.contentY = titleHeight + padding;
        this.contentWidth = screenWidth - sidebarWidth - padding * 4;
        this.contentHeight = screenHeight - titleHeight - padding * 3 - buttonHeight;
        
        // 清空组件
        widgets.clear();
        scrollOffset = 0;
        
        // 子类实现具体初始化
        initPage();
        
        // 计算最大滚动偏移
        calculateMaxScroll();
    }
    
    /**
     * 子类实现具体的页面初始化
     */
    protected abstract void initPage();
    
    /**
     * 获取页面标题
     */
    public abstract Component getTitle();
    
    /**
     * 渲染页面
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 启用裁剪
        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
        
        // 渲染所有组件
        for (AbstractWidget widget : widgets) {
            // 计算组件在屏幕上的实际位置
            int screenY = (int)(widget.getY() - scrollOffset);
            
            // 只渲染可见的组件
            if (screenY + widget.getHeight() >= contentY && screenY <= contentY + contentHeight) {
                // 临时设置组件位置
                int originalY = widget.getY();
                widget.setY(screenY);
                
                // 渲染组件
                widget.render(graphics, mouseX, mouseY, partialTick);
                
                // 恢复原始位置
                widget.setY(originalY);
            }
        }
        
        // 禁用裁剪
        graphics.disableScissor();
        
        // 绘制滚动条
        if (maxScrollOffset > 0) {
            renderScrollbar(graphics);
        }
    }
    
    /**
     * 渲染滚动条
     */
    private void renderScrollbar(GuiGraphics graphics) {
        int scrollbarX = contentX + contentWidth - 6;
        int scrollbarY = contentY;
        int scrollbarHeight = contentHeight;
        
        // 绘制滚动条背景
        graphics.fill(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarHeight, 0x40FFFFFF);
        
        // 计算滚动条滑块的大小和位置
        double contentRatio = (double) contentHeight / (contentHeight + maxScrollOffset);
        int thumbHeight = Math.max(20, (int)(scrollbarHeight * contentRatio));
        int thumbY = scrollbarY + (int)((scrollbarHeight - thumbHeight) * (scrollOffset / maxScrollOffset));
        
        // 绘制滚动条滑块
        graphics.fill(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0x80FFFFFF);
    }
    
    /**
     * 处理鼠标滚动
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 检查鼠标是否在内容区域内
        if (mouseX >= contentX && mouseX <= contentX + contentWidth &&
            mouseY >= contentY && mouseY <= contentY + contentHeight) {
            
            if (maxScrollOffset > 0) {
                scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - scrollY * 15));
                return true;
            }
        }
        return false;
    }
    
    /**
     * 处理鼠标点击
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 应用滚动偏移
        double adjustedMouseY = mouseY + scrollOffset;
        
        for (AbstractWidget widget : widgets) {
            if (widget.isMouseOver(mouseX, adjustedMouseY)) {
                return widget.mouseClicked(mouseX, adjustedMouseY, button);
            }
        }
        return false;
    }
    
    /**
     * 计算最大滚动偏移
     */
    protected void calculateMaxScroll() {
        if (widgets.isEmpty()) {
            maxScrollOffset = 0;
            return;
        }
        
        // 找到最底部的组件
        int maxY = 0;
        for (AbstractWidget widget : widgets) {
            int widgetBottom = widget.getY() + widget.getHeight();
            if (widgetBottom > maxY) {
                maxY = widgetBottom;
            }
        }
        
        // 计算需要滚动的距离
        int contentBottom = contentY + contentHeight;
        maxScrollOffset = Math.max(0, maxY - contentBottom + 20);
    }
    
    /**
     * 添加组件
     */
    protected void addWidget(AbstractWidget widget) {
        widgets.add(widget);
    }
    
    /**
     * 绘制分组标题
     */
    protected void drawGroupTitle(GuiGraphics graphics, String title, int y) {
        int screenY = (int)(y - scrollOffset);
        if (screenY >= contentY - 20 && screenY <= contentY + contentHeight) {
            graphics.drawString(font, Component.literal("§e§l" + title), contentX + 10, screenY, 0xFFFF55);
        }
    }
    
    /**
     * 绘制描述文本
     */
    protected void drawDescription(GuiGraphics graphics, String description, int y) {
        int screenY = (int)(y - scrollOffset);
        if (screenY >= contentY - 20 && screenY <= contentY + contentHeight) {
            graphics.drawString(font, Component.literal("§7" + description), contentX + 10, screenY, 0xAAAAAA);
        }
    }
}

