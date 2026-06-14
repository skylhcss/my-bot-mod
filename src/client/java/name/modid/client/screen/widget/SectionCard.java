package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Section 卡片组件
 * 带圆角背景、边框和标题的容器，用于组织配置项
 */
public class SectionCard {
    
    private final String title;
    private int x, y;
    private int width;
    private int height;
    private final List<AbstractWidget> items = new ArrayList<>();
    private final List<ResetButton> resets = new ArrayList<>();
    
    public SectionCard(String title) {
        this.title = title;
    }
    
    /**
     * 添加一个配置项和可选的重置按钮
     */
    public void addItem(AbstractWidget widget, ResetButton reset) {
        items.add(widget);
        resets.add(reset);
    }
    
    public void addItem(AbstractWidget widget) {
        items.add(widget);
        resets.add(null);
    }
    
    /**
     * 计算并布局所有子项，返回卡片总高度
     */
    public int layout(int cardX, int cardY, int cardWidth) {
        this.x = cardX;
        this.y = cardY;
        this.width = cardWidth;
        
        int currentY = cardY + DesignTokens.CARD_V_PADDING + DesignTokens.CARD_TITLE_HEIGHT + DesignTokens.CARD_TITLE_GAP;
        int itemWidth = cardWidth - DesignTokens.CARD_H_PADDING * 2 - DesignTokens.RESET_SIZE - DesignTokens.RESET_SIZE;
        
        for (int i = 0; i < items.size(); i++) {
            AbstractWidget item = items.get(i);
            item.setX(cardX + DesignTokens.CARD_H_PADDING);
            item.setY(currentY);
            item.setWidth(itemWidth);
            
            ResetButton reset = resets.get(i);
            if (reset != null) {
                reset.setX(cardX + cardWidth - DesignTokens.CARD_H_PADDING - DesignTokens.RESET_SIZE);
                reset.setY(currentY + (DesignTokens.ROW_HEIGHT - DesignTokens.RESET_SIZE) / 2);
            }
            
            currentY += DesignTokens.ROW_HEIGHT + DesignTokens.ROW_GAP;
        }
        
        // 移除末尾多余间距
        currentY -= DesignTokens.ROW_GAP;
        currentY += DesignTokens.CARD_V_PADDING;
        
        this.height = currentY - cardY;
        return this.height;
    }
    
    /**
     * 渲染卡片及其子项
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制圆角背景
        drawRoundedRect(graphics, x, y, width, height, DesignTokens.CARD_BG);
        
        // 绘制边框
        drawRoundedBorder(graphics, x, y, width, height, DesignTokens.CARD_BORDER);
        
        // 绘制标题
        graphics.drawString(
            Minecraft.getInstance().font,
            Component.literal(title),
            x + DesignTokens.CARD_H_PADDING,
            y + DesignTokens.CARD_V_PADDING,
            DesignTokens.CARD_TITLE_COLOR
        );
        
        // 渲染子项
        for (AbstractWidget item : items) {
            item.render(graphics, mouseX, mouseY, partialTick);
        }
        for (ResetButton reset : resets) {
            if (reset != null) {
                reset.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }
    
    /**
     * 获取所有子 widget（含 ResetButton）
     */
    public List<AbstractWidget> getAllWidgets() {
        List<AbstractWidget> all = new ArrayList<>(items);
        for (ResetButton r : resets) {
            if (r != null) all.add(r);
        }
        return all;
    }
    
    public int getHeight() { return height; }
    public int getY() { return y; }
    
    // ========== 圆角矩形绘制 ==========
    
    private void drawRoundedRect(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        int r = DesignTokens.CARD_RADIUS;
        // 顶部缩进
        graphics.fill(x + r, y, x + w - r, y + 1, color);
        for (int i = 1; i < r; i++) {
            graphics.fill(x + r - i, y + i, x + w - r + i, y + i + 1, color);
        }
        // 中间满宽
        graphics.fill(x, y + r, x + w, y + h - r, color);
        // 底部缩进
        for (int i = 1; i < r; i++) {
            graphics.fill(x + r - i, y + h - r + i - 1, x + w - r + i, y + h - r + i, color);
        }
        graphics.fill(x + r, y + h - 1, x + w - r, y + h, color);
    }
    
    private void drawRoundedBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        int r = DesignTokens.CARD_RADIUS;
        // 顶部边框
        graphics.fill(x + r, y, x + w - r, y + 1, color);
        // 底部边框
        graphics.fill(x + r, y + h - 1, x + w - r, y + h, color);
        // 左侧边框
        graphics.fill(x, y + r, x + 1, y + h - r, color);
        // 右侧边框
        graphics.fill(x + w - 1, y + r, x + w, y + h - r, color);
        // 圆角过渡
        for (int i = 1; i < r; i++) {
            graphics.fill(x + r - i, y + i, x + r - i + 1, y + i + 1, color);
            graphics.fill(x + w - r + i - 1, y + i, x + w - r + i, y + i + 1, color);
            graphics.fill(x + r - i, y + h - i - 1, x + r - i + 1, y + h - i, color);
            graphics.fill(x + w - r + i - 1, y + h - i - 1, x + w - r + i, y + h - i, color);
        }
    }
}
