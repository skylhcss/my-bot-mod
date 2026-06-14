package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 选项卡按钮组件
 * 用于顶部的选项卡切换，激活时底部有绿色高亮线
 */
public class TabButton extends Button {
    
    private boolean isActive;
    
    public TabButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.isActive = false;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    public boolean isActive() {
        return this.isActive;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 背景
        int bgColor;
        if (this.isActive) bgColor = DesignTokens.TAB_ACTIVE_BG;
        else if (this.isHovered()) bgColor = DesignTokens.TAB_HOVER_BG;
        else bgColor = DesignTokens.TAB_NORMAL_BG;
        
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
        
        // 激活状态下划线
        if (this.isActive) {
            graphics.fill(this.getX(), this.getY() + this.height - 2,
                         this.getX() + this.width, this.getY() + this.height,
                         DesignTokens.TAB_ACTIVE_UNDERLINE);
        }
        
        // 文本
        int textColor = this.isActive ? DesignTokens.TAB_TEXT_ACTIVE : DesignTokens.TAB_TEXT_NORMAL;
        graphics.drawCenteredString(
            Minecraft.getInstance().font,
            this.getMessage(),
            this.getX() + this.width / 2,
            this.getY() + (this.height - 8) / 2,
            textColor
        );
    }
}
