package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 现代化滑块组件
 * 模仿 Sodium 的滑块风格
 */
public class ModernSlider extends AbstractSliderButton {
    
    private static final int TRACK_COLOR = 0x20FFFFFF;
    private static final int TRACK_BORDER_COLOR = 0x40FFFFFF;
    private static final int HANDLE_COLOR = 0x60FFFFFF;
    private static final int HANDLE_HOVER_COLOR = 0x80FFFFFF;
    private static final int HANDLE_WIDTH = 8;
    
    private final double minValue;
    private final double maxValue;
    private final Function<Double, Component> messageProvider;
    private final Consumer<Double> onValueChange;
    
    public ModernSlider(int x, int y, int width, int height, 
                       double minValue, double maxValue, double currentValue,
                       Function<Double, Component> messageProvider,
                       Consumer<Double> onValueChange) {
        super(x, y, width, height, Component.empty(), 
              (currentValue - minValue) / (maxValue - minValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.messageProvider = messageProvider;
        this.onValueChange = onValueChange;
        this.updateMessage();
    }
    
    @Override
    protected void updateMessage() {
        double currentValue = this.minValue + (this.maxValue - this.minValue) * this.value;
        this.setMessage(messageProvider.apply(currentValue));
    }
    
    @Override
    protected void applyValue() {
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
        int handleColor = this.isHovered() ? HANDLE_HOVER_COLOR : HANDLE_COLOR;
        
        // 绘制滑块
        graphics.fill(handleX, handleY, handleX + HANDLE_WIDTH, handleY + this.height, handleColor);
        
        // 绘制滑块边框
        drawBorder(graphics, handleX, handleY, HANDLE_WIDTH, this.height, TRACK_BORDER_COLOR);
    }
    
    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        this.setValueFromMouse(mouseX);
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setValueFromMouse(mouseX);
    }
    
    private void setValueFromMouse(double mouseX) {
        this.value = Mth.clamp((mouseX - this.getX()) / (double)(this.width - HANDLE_WIDTH), 0.0, 1.0);
        this.updateMessage();
        this.applyValue();
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean flag = keyCode == 263; // 左箭头
        if (flag || keyCode == 262) { // 右箭头
            float step = flag ? -0.05F : 0.05F;
            this.value = Mth.clamp(this.value + step, 0.0, 1.0);
            this.updateMessage();
            this.applyValue();
            return true;
        }
        return false;
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
}
