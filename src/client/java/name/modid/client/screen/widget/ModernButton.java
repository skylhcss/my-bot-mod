package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 现代化按钮组件
 * 模仿 Sodium 的按钮风格
 */
public class ModernButton extends Button {
    
    private static final int NORMAL_COLOR = 0x20FFFFFF;
    private static final int HOVER_COLOR = 0x30FFFFFF;
    private static final int DISABLED_COLOR = 0x10FFFFFF;
    private static final int BORDER_COLOR = 0x40FFFFFF;
    
    public ModernButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    /** 设置高度（Button 继承的 height 为 protected，在此暴露） */
    public void setHeight(int height) {
        this.height = height;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 确定背景颜色
        int backgroundColor;
        if (!this.active) {
            backgroundColor = DISABLED_COLOR;
        } else if (this.isHovered()) {
            backgroundColor = HOVER_COLOR;
        } else {
            backgroundColor = NORMAL_COLOR;
        }
        
        // 绘制背景
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, backgroundColor);
        
        // 绘制边框
        drawBorder(graphics, this.getX(), this.getY(), this.width, this.height, BORDER_COLOR);
        
        // 绘制文本（小号字体）
        int textColor = this.active ? 0xFFFFFF : 0x808080;
        int textH = UI.lineHeight(DesignTokens.TEXT_SCALE);
        UI.drawScaledCentered(
            graphics,
            Minecraft.getInstance().font,
            this.getMessage(),
            this.getX() + this.width / 2,
            this.getY() + (this.height - textH) / 2,
            DesignTokens.TEXT_SCALE,
            textColor
        );
    }
    
    /**
     * 绘制边框
     */
    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        // 上边框
        graphics.fill(x, y, x + width, y + 1, color);
        // 下边框
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        // 左边框
        graphics.fill(x, y, x + 1, y + height, color);
        // 右边框
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
