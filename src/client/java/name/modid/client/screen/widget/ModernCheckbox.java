package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;

/**
 * 现代化复选框组件
 * 模仿 Sodium 的复选框风格
 */
public class ModernCheckbox extends Checkbox {
    
    private static final int BOX_SIZE = 16;
    private static final int BOX_NORMAL_COLOR = 0x20FFFFFF;
    private static final int BOX_HOVER_COLOR = 0x30FFFFFF;
    private static final int BOX_BORDER_COLOR = 0x40FFFFFF;
    private static final int CHECK_COLOR = 0xFF55FF55;
    private static final int TEXT_COLOR = 0xFFFFFF;
    
    public ModernCheckbox(int x, int y, int width, int height, Component message, boolean selected) {
        super(x, y, width, height, message, selected, false);
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int boxX = this.getX();
        int boxY = this.getY() + (this.height - BOX_SIZE) / 2;
        
        // 确定背景颜色
        int backgroundColor = this.isHovered() ? BOX_HOVER_COLOR : BOX_NORMAL_COLOR;
        
        // 绘制复选框背景
        graphics.fill(boxX, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, backgroundColor);
        
        // 绘制边框
        drawBorder(graphics, boxX, boxY, BOX_SIZE, BOX_SIZE, BOX_BORDER_COLOR);
        
        // 如果选中，绘制勾选标记
        if (this.selected()) {
            // 绘制勾选标记（简单的填充）
            graphics.fill(boxX + 3, boxY + 3, boxX + BOX_SIZE - 3, boxY + BOX_SIZE - 3, CHECK_COLOR);
        }
        
        // 绘制文本
        graphics.drawString(
            Minecraft.getInstance().font,
            this.getMessage(),
            boxX + BOX_SIZE + 6,
            boxY + (BOX_SIZE - 8) / 2,
            TEXT_COLOR
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
