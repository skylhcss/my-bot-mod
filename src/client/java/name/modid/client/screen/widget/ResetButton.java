package name.modid.client.screen.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 重置按钮组件
 * 用于重置单个配置项到默认值
 */
public class ResetButton extends AbstractWidget {
    
    private static final int BUTTON_SIZE = 16;
    private static final int BACKGROUND_COLOR = 0x80404040;
    private static final int HOVER_COLOR = 0xA0606060;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    
    private final Runnable onPress;
    
    public ResetButton(int x, int y, Runnable onPress) {
        super(x, y, BUTTON_SIZE, BUTTON_SIZE, Component.literal("↺"));
        this.onPress = onPress;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制背景
        int bgColor = this.isHovered() ? HOVER_COLOR : BACKGROUND_COLOR;
        graphics.fill(this.getX(), this.getY(), 
                     this.getX() + this.width, this.getY() + this.height, 
                     bgColor);
        
        // 绘制边框
        drawBorder(graphics, this.getX(), this.getY(), this.width, this.height, 0x40FFFFFF);
        
        // 绘制文本（居中）
        graphics.drawCenteredString(
            net.minecraft.client.Minecraft.getInstance().font,
            "↺",
            this.getX() + this.width / 2,
            this.getY() + (this.height - 8) / 2,
            TEXT_COLOR
        );
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.onPress != null) {
            this.onPress.run();
        }
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
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, 
                  Component.literal("重置为默认值"));
    }
}
