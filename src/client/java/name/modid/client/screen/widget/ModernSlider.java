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
 * 文字在控件内部居中渲染，不与相邻配置项重叠
 */
public class ModernSlider extends AbstractWidget {
    
    private final double minValue;
    private final double maxValue;
    private final Function<Double, Component> messageProvider;
    private final Consumer<Double> onValueChange;
    
    private double value; // 0.0 ~ 1.0 归一化值
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
        
        this.value = Mth.clamp((currentValue - minValue) / (maxValue - minValue), 0.0, 1.0);
        this.updateMessage();
    }
    
    private void updateMessage() {
        double currentValue = this.minValue + (this.maxValue - this.minValue) * this.value;
        this.setMessage(messageProvider.apply(currentValue));
    }
    
    private void applyValue() {
        double currentValue = this.minValue + (this.maxValue - this.minValue) * this.value;
        onValueChange.accept(currentValue);
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int trackY = this.getY() + (this.height - DesignTokens.SLIDER_TRACK_HEIGHT) / 2;
        
        // 轨道背景
        graphics.fill(this.getX(), trackY, this.getX() + this.width, trackY + DesignTokens.SLIDER_TRACK_HEIGHT, DesignTokens.SLIDER_TRACK_BG);
        
        // 轨道边框
        drawBorder(graphics, this.getX(), trackY, this.width, DesignTokens.SLIDER_TRACK_HEIGHT, DesignTokens.SLIDER_TRACK_BORDER);
        
        // 滑块把手
        int handleX = this.getX() + (int) ((this.width - DesignTokens.SLIDER_HANDLE_WIDTH) * this.value);
        int handleColor = (this.isHovered() || this.isDragging) ? DesignTokens.SLIDER_HANDLE_HOVER : DesignTokens.SLIDER_HANDLE_BG;
        graphics.fill(handleX, this.getY(), handleX + DesignTokens.SLIDER_HANDLE_WIDTH, this.getY() + this.height, handleColor);
        drawBorder(graphics, handleX, this.getY(), DesignTokens.SLIDER_HANDLE_WIDTH, this.height, DesignTokens.SLIDER_HANDLE_BORDER);
        
        // 文字在控件内部居中渲染（MC 原版风格）
        int textY = this.getY() + (this.height - 8) / 2;
        graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, textY, 0xFFFFFFFF);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.clicked(mouseX, mouseY)) {
            this.isDragging = true;
            this.setValueFromMouse(mouseX);
            return true;
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
        if (this.isDragging && button == 0) {
            this.setValueFromMouse(mouseX);
            return true;
        }
        return false;
    }
    
    private void setValueFromMouse(double mouseX) {
        double newValue = (mouseX - this.getX()) / (double) (this.width - DesignTokens.SLIDER_HANDLE_WIDTH);
        this.value = Mth.clamp(newValue, 0.0, 1.0);
        this.updateMessage();
        this.applyValue();
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused()) return false;
        boolean isLeft = keyCode == 263;
        boolean isRight = keyCode == 262;
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
        return this.active && this.visible
            && mouseX >= this.getX() && mouseX < this.getX() + this.width
            && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }
    
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible
            && mouseX >= this.getX() && mouseX < this.getX() + this.width
            && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }
    
    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
    
    public double getCurrentValue() {
        return this.minValue + (this.maxValue - this.minValue) * this.value;
    }
    
    public void setCurrentValue(double val) {
        this.value = Mth.clamp((val - this.minValue) / (this.maxValue - this.minValue), 0.0, 1.0);
        this.updateMessage();
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
    }
}
