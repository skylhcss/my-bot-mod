package name.modid.client.screen.pages;

import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.SectionCard;
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
 * 使用 SectionCard 系统组织配置项，使用 pose translate 实现滚动
 */
public abstract class ConfigPage {
    
    protected final ModConfig config;
    protected Minecraft minecraft;
    protected Font font;
    
    // Section 卡片列表
    protected final List<SectionCard> sections = new ArrayList<>();
    
    // 布局参数（由主屏幕设置）
    protected int scrollAreaX;
    protected int scrollAreaY;
    protected int scrollAreaWidth;
    protected int scrollAreaHeight;
    protected int contentWidth; // 内容区宽度（面板内减去边距）
    
    // 滚动状态
    public double scrollOffset = 0;
    protected double maxScrollOffset = 0;
    protected int virtualContentHeight = 0;
    
    public ConfigPage(ModConfig config) {
        this.config = config;
    }
    
    /**
     * 初始化页面（由主屏幕调用）
     */
    public void init(int areaX, int areaY, int areaWidth, int areaHeight, Minecraft minecraft, Font font) {
        this.scrollAreaX = areaX;
        this.scrollAreaY = areaY;
        this.scrollAreaWidth = areaWidth;
        this.scrollAreaHeight = areaHeight;
        this.contentWidth = areaWidth - DesignTokens.SCROLL_AREA_PADDING * 2;
        this.minecraft = minecraft;
        this.font = font;
        
        this.sections.clear();
        this.scrollOffset = 0;
        
        // 子类构建 Section 卡片
        buildPage();
        
        // 布局所有卡片并计算虚拟高度
        layoutSections();
    }
    
    /**
     * 子类实现：构建 Section 卡片和配置项
     */
    protected abstract void buildPage();
    
    /**
     * 获取页面标题
     */
    public abstract Component getTitle();
    
    /**
     * 添加一个 Section 卡片
     */
    protected SectionCard addSection(String title) {
        SectionCard card = new SectionCard(title);
        sections.add(card);
        return card;
    }
    
    /**
     * 布局所有 Section 卡片，计算虚拟内容高度
     */
    private void layoutSections() {
        int cardX = scrollAreaX + DesignTokens.SCROLL_AREA_PADDING;
        int currentY = scrollAreaY + DesignTokens.CONTENT_TOP;
        
        for (int i = 0; i < sections.size(); i++) {
            SectionCard card = sections.get(i);
            int cardHeight = card.layout(cardX, currentY, contentWidth);
            currentY += cardHeight;
            if (i < sections.size() - 1) {
                currentY += DesignTokens.CARD_GAP;
            }
        }
        
        this.virtualContentHeight = currentY - scrollAreaY;
        this.maxScrollOffset = Math.max(0, virtualContentHeight - scrollAreaHeight);
    }
    
    /**
     * 渲染页面
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 裁剪区域（屏幕绝对坐标，不受 pose 影响）
        graphics.enableScissor(scrollAreaX, scrollAreaY, scrollAreaX + scrollAreaWidth, scrollAreaY + scrollAreaHeight);
        
        // 使用 pose translate 偏移渲染
        graphics.pose().pushPose();
        graphics.pose().translate(0, -scrollOffset, 0);
        
        // 调整鼠标 Y 坐标以匹配虚拟坐标
        double adjustedMouseY = mouseY + scrollOffset;
        
        for (SectionCard card : sections) {
            card.render(graphics, mouseX, (int) adjustedMouseY, partialTick);
        }
        
        graphics.pose().popPose();
        graphics.disableScissor();
        
        // 绘制滚动条（不受 translate 影响）
        if (maxScrollOffset > 0) {
            renderScrollbar(graphics);
        }
    }
    
    private void renderScrollbar(GuiGraphics graphics) {
        int sbX = scrollAreaX + scrollAreaWidth - DesignTokens.SCROLLBAR_WIDTH - 2;
        int sbY = scrollAreaY;
        int sbH = scrollAreaHeight;
        
        graphics.fill(sbX, sbY, sbX + DesignTokens.SCROLLBAR_WIDTH, sbY + sbH, DesignTokens.SCROLLBAR_TRACK);
        
        double ratio = (double) sbH / virtualContentHeight;
        int thumbH = Math.max(DesignTokens.SCROLLBAR_MIN_THUMB, (int) (sbH * ratio));
        int thumbY = sbY + (int) ((sbH - thumbH) * (scrollOffset / maxScrollOffset));
        
        graphics.fill(sbX, thumbY, sbX + DesignTokens.SCROLLBAR_WIDTH, thumbY + thumbH, DesignTokens.SCROLLBAR_THUMB);
    }
    
    /**
     * 处理鼠标滚轮
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= scrollAreaX && mouseX <= scrollAreaX + scrollAreaWidth
            && mouseY >= scrollAreaY && mouseY <= scrollAreaY + scrollAreaHeight) {
            if (maxScrollOffset > 0) {
                scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - scrollY * 15));
                return true;
            }
        }
        return false;
    }
    
    /**
     * 处理鼠标点击（虚拟坐标）
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < scrollAreaX || mouseX > scrollAreaX + scrollAreaWidth) return false;
        
        double adjustedMouseY = mouseY + scrollOffset;
        
        for (SectionCard card : sections) {
            for (AbstractWidget widget : card.getAllWidgets()) {
                if (widget.isMouseOver(mouseX, adjustedMouseY) && widget.mouseClicked(mouseX, adjustedMouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 处理鼠标拖动
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double adjustedMouseY = mouseY + scrollOffset;
        
        for (SectionCard card : sections) {
            for (AbstractWidget widget : card.getAllWidgets()) {
                if (widget.mouseDragged(mouseX, adjustedMouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 处理鼠标释放
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double adjustedMouseY = mouseY + scrollOffset;
        
        for (SectionCard card : sections) {
            for (AbstractWidget widget : card.getAllWidgets()) {
                if (widget.mouseReleased(mouseX, adjustedMouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }
}
