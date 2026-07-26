package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 重置按钮组件
 * 用于将单个配置项恢复到默认值
 */
public class ResetButton extends AbstractWidget {
    
    private final Runnable onPress;
    
    public ResetButton(int x, int y, Runnable onPress) {
        super(x, y, DesignTokens.RESET_SIZE, DesignTokens.RESET_SIZE, Component.literal("↺"));
        this.onPress = onPress;
        this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.my-bot-mod.reset.tooltip")));
    }

    /** 设置高度（AbstractWidget 的 height 为 protected，需在子类暴露） */
    public void setHeight(int height) {
        this.height = height;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bgColor = this.isHovered() ? DesignTokens.RESET_HOVER_BG : DesignTokens.RESET_BG;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
        
        UI.border(graphics, this.getX(), this.getY(), this.width, this.height, DesignTokens.RESET_BORDER);
        
        int textColor = this.isHovered() ? DesignTokens.RESET_HOVER_ICON : DesignTokens.RESET_ICON_COLOR;
        int textH = UI.lineHeight(DesignTokens.TEXT_SCALE);
        UI.drawScaledCentered(
            graphics,
            Minecraft.getInstance().font,
            "↺",
            this.getX() + this.width / 2,
            this.getY() + (this.height - textH) / 2,
            DesignTokens.TEXT_SCALE,
            textColor
        );
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.onPress != null) {
            this.onPress.run();
        }
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, Component.translatable("gui.my-bot-mod.reset.tooltip"));
    }
}
