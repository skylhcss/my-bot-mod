package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 现代化滑块组件
 * 完全重写，避免滑块互相干扰
 */
public class ModernSlider extends AbstractWidget {
    
    private static final int TRACK_COLOR = 0x20FFFFFF;
    private static final int TRACK_BORDER_COLOR = 0x40FFFFFF;
    private static final int HANDLE_COLOR = 0x60FFFFFF;
    private static final int HANDLE_HOVER_COLOR = 0x80FFFFFF;
    private static final int HANDLE_WIDTH = 8;
    
    private final double minValue;
    private final double maxValue;
    private final Function<Double, Component> messageProvider;
    private final Consumer<Double> onValueChange;
    
    // 滑块状态
    private double value; // 0.0 到 1.0 之间的归一化值
    private boolean isDragging = false;
    
    public ModernSlider(int x, int y, int width, int height, 
                       double minValue, double maxValue, double currentValue,
                       Function<Double, Component> messageProvider,
                       Consumer<Double> onValueChange) {
        super(x, y, width, height, Component.empty());
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.messageProvider = messageProvider;
        this.onValueChange = onValueChange;
        
        // 初始化值
        this.value = Mth.clamp((currentValue - minValue) / (maxValue - minValue), 0.0, 1.0);
        this.updateMessage();
    }
    
    /**
     * 更新显示的消息
     */
    private void updateMessage() {
        double currentValue = this.minValue + (this.maxValue - this.minValue) * this.value;
        this.setMessage(messageProvider.apply(currentValue));
    }
    
    /**
     * 应用值的变化
     */
    private void applyValue() {
        double currentValue = this.minValue + (this.maxValue - this.minValue) * this.value;
        onValueChange.accept(currentValue);
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制文本（在滑块上方）
        graphics.drawString(
            Minecraft.getInstance().font,
            this.getMessage(),
            this.getX(),
            this.getY() - 12,
            0xFFFFFF
        );
        
        // 绘制轨道背景
        int trackY = this.getY() + this.height / 2 - 2;
        graphics.fill(this.getX(), trackY, 
                     this.getX() + this.width, trackY + 4, 
                     TRACK_COLOR);
        
        // 绘制轨道边框
        drawBorder(graphics, this.getX(), trackY, this.width, 4, TRACK_BORDER_COLOR);
        
        // 计算滑块位置
        int handleX = this.getX() + (int)((this.width - HANDLE_WIDTH) * this.value);
        int handleY = this.getY();
        
        // 确定滑块颜色
        int handleColor = (this.isHovered() || this.isDragging) ? HANDLE_HOVER_COLOR : HANDLE_COLOR;
        
        // 绘制滑块
        graphics.fill(handleX, handleY, handleX + HANDLE_WIDTH, handleY + this.height, handleColor);
        
        // 绘制滑块边框
        drawBorder(graphics, handleX, handleY, HANDLE_WIDTH, this.height, TRACK_BORDER_COLOR);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isValidClickButton(button)) {
            if (this.clicked(mouseX, mouseY)) {
                this.isDragging = true;
                this.setValueFromMouse(mouseX);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isDragging) {
            this.isDragging = false;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 只有在拖拽状态且是左键时才处理
        if (this.isDragging && button == 0) {
            this.setValueFromMouse(mouseX);
            return true;
        }
        return false;
    }
    
    /**
     * 从鼠标位置设置值
     * 移除了过于严格的边界检查，允许拖动时鼠标超出滑块范围
     */
    private void setValueFromMouse(double mouseX) {
        // 计算新值（允许鼠标超出范围，但值会被clamp限制）
        double newValue = (mouseX - this.getX()) / (double)(this.width - HANDLE_WIDTH);
        this.value = Mth.clamp(newValue, 0.0, 1.0);
        
        this.updateMessage();
        this.applyValue();
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 只有当滑块获得焦点时才响应键盘事件
        if (!this.isFocused()) {
            return false;
        }
        
        boolean isLeft = keyCode == 263; // 左箭头
        boolean isRight = keyCode == 262; // 右箭头
        
        if (isLeft || isRight) {
            float step = isLeft ? -0.05F : 0.05F;
            this.value = Mth.clamp(this.value + step, 0.0, 1.0);
            this.updateMessage();
            this.applyValue();
            return true;
        }
        
        return false;
    }
    
    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        // 严格检查点击是否在组件范围内
        return this.active && this.visible &&
               mouseX >= this.getX() && mouseX < this.getX() + this.width &&
               mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }
    
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // 严格检查鼠标是否在组件范围内
        return this.active && this.visible &&
               mouseX >= this.getX() && mouseX < this.getX() + this.width &&
               mouseY >= this.getY() && mouseY < this.getY() + this.height;
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
    
    /**
     * 获取当前值
     */
    public double getCurrentValue() {
        return this.minValue + (this.maxValue - this.minValue) * this.value;
    }
    
    /**
     * 设置当前值
     */
    public void setCurrentValue(double value) {
        this.value = Mth.clamp((value - this.minValue) / (this.maxValue - this.minValue), 0.0, 1.0);
        this.updateMessage();
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
    }
}
