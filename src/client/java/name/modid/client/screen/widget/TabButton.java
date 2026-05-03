package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 选项卡按钮组件
 * 用于顶部的选项卡切换
 */
public class TabButton extends Button {
    
    private static final int NORMAL_COLOR = 0x20FFFFFF;
    private static final int HOVER_COLOR = 0x30FFFFFF;
    private static final int ACTIVE_COLOR = 0x40FFFFFF;
    private static final int BORDER_COLOR = 0x40FFFFFF;
    private static final int ACTIVE_BORDER_COLOR = 0xFF55FF55;
    
    private boolean isActive;
    
    public TabButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.isActive = false;
    }
    
    /**
     * 设置是否为激活状态
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    /**
     * 获取是否为激活状态
     */
    public boolean isActive() {
        return this.isActive;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 确定背景颜色
        int backgroundColor;
        if (this.isActive) {
            backgroundColor = ACTIVE_COLOR;
        } else if (this.isHovered()) {
            backgroundColor = HOVER_COLOR;
        } else {
            backgroundColor = NORMAL_COLOR;
        }
        
        // 绘制背景
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, backgroundColor);
        
        // 绘制边框
        int borderColor = this.isActive ? ACTIVE_BORDER_COLOR : BORDER_COLOR;
        drawBorder(graphics, this.getX(), this.getY(), this.width, this.height, borderColor);
        
        // 如果是激活状态，绘制底部高亮线
        if (this.isActive) {
            graphics.fill(this.getX(), this.getY() + this.height - 2, 
                         this.getX() + this.width, this.getY() + this.height, 
                         ACTIVE_BORDER_COLOR);
        }
        
        // 绘制文本
        int textColor = this.isActive ? 0x55FF55 : 0xFFFFFF;
        graphics.drawCenteredString(
            Minecraft.getInstance().font,
            this.getMessage(),
            this.getX() + this.width / 2,
            this.getY() + (this.height - 8) / 2,
            textColor
        );
    }
    
    /**
     * 绘制边框
     */
    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
