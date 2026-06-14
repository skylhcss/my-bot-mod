package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 现代化复选框组件
 * 继承 AbstractWidget，完全自主管理选中状态，零反射
 */
public class ModernCheckbox extends AbstractWidget {
    
    private boolean selected;
    private Runnable onChanged;
    
    public ModernCheckbox(int x, int y, int width, int height, Component message, boolean selected) {
        super(x, y, width, height, message);
        this.selected = selected;
    }
    
    public boolean selected() {
        return this.selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        this.selected = !this.selected;
        if (this.onChanged != null) {
            this.onChanged.run();
        }
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int boxX = this.getX();
        int boxY = this.getY() + (this.height - DesignTokens.CHECKBOX_BOX_SIZE) / 2;
        int boxSize = DesignTokens.CHECKBOX_BOX_SIZE;
        
        // 背景色
        int bgColor = this.isHovered() ? DesignTokens.CHECKBOX_BOX_HOVER : DesignTokens.CHECKBOX_BOX_BG;
        graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, bgColor);
        
        // 边框
        drawBorder(graphics, boxX, boxY, boxSize, boxSize, DesignTokens.CHECKBOX_BOX_BORDER);
        
        // 勾选标记
        if (this.selected) {
            graphics.fill(boxX + 3, boxY + 3, boxX + boxSize - 3, boxY + boxSize - 3, DesignTokens.CHECKBOX_CHECK_COLOR);
        }
        
        // 文本
        graphics.drawString(
            Minecraft.getInstance().font,
            this.getMessage(),
            boxX + boxSize + 6,
            boxY + (boxSize - 8) / 2,
            DesignTokens.ITEM_TEXT_COLOR
        );
    }
    
    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
    }
}
