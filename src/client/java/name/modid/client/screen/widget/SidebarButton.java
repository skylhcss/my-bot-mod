package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 侧边栏按钮组件
 * 用于左侧的分类导航
 */
public class SidebarButton extends Button {
    
    private static final int NORMAL_COLOR = 0x20FFFFFF;
    private static final int HOVER_COLOR = 0x30FFFFFF;
    private static final int ACTIVE_COLOR = 0x40FFFFFF;
    private static final int ACTIVE_BORDER_COLOR = 0xFF55FF55;
    
    private boolean isActive;
    
    public SidebarButton(int x, int y, int width, int height, Component message, OnPress onPress) {
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
        
        // 如果是激活状态，绘制左侧高亮线
        if (this.isActive) {
            graphics.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.height, ACTIVE_BORDER_COLOR);
        }
        
        // 绘制文本（居中）
        int textColor = this.isActive ? 0x55FF55 : 0xFFFFFF;
        graphics.drawCenteredString(
            Minecraft.getInstance().font,
            this.getMessage(),
            this.getX() + this.width / 2,
            this.getY() + (this.height - 8) / 2,
            textColor
        );
    }
}
