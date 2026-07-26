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

    /** 设置高度（AbstractWidget 的 height 为 protected，需在子类暴露） */
    public void setHeight(int height) {
        this.height = height;
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
        UI.border(graphics, boxX, boxY, boxSize, boxSize, DesignTokens.CHECKBOX_BOX_BORDER);
        
        // 勾选标记
        if (this.selected) {
            graphics.fill(boxX + 3, boxY + 3, boxX + boxSize - 3, boxY + boxSize - 3, DesignTokens.CHECKBOX_CHECK_COLOR);
        }
        
        // 文本（小号字体）
        int textH = UI.lineHeight(DesignTokens.TEXT_SCALE);
        UI.drawScaled(graphics,
            Minecraft.getInstance().font,
            this.getMessage(),
            boxX + boxSize + 5,
            boxY + (boxSize - textH) / 2,
            DesignTokens.TEXT_SCALE,
            DesignTokens.ITEM_TEXT_COLOR);
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
    }
}
