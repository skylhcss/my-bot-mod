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
 *
 * 拖动原理：
 *   mouseClicked 设置 dragging = true 后，由外部调用方（ConfigPage）
 *   持续调用 forceDrag() 更新值，不再依赖 AbstractWidget.mouseDragged 的
 *   isMouseOver 检查，从而避免 Screen 事件路由导致拖动中断。
 */
public class ModernSlider extends AbstractWidget {

    private final double minValue;
    private final double maxValue;
    private final Function<Double, Component> messageProvider;
    private final Consumer<Double> onValueChange;

    private double value;
    private boolean dragging = false;
    private double lastAppliedValue = Double.NaN;

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
        this.lastAppliedValue = this.minValue + (this.maxValue - this.minValue) * this.value;
        this.updateMessage();
    }

    private void updateMessage() {
        this.setMessage(messageProvider.apply(getCurrentValue()));
    }

    private void applyValue() {
        double cur = getCurrentValue();
        if (cur == lastAppliedValue) return;
        lastAppliedValue = cur;
        onValueChange.accept(cur);
    }

    public double getCurrentValue() {
        return this.minValue + (this.maxValue - this.minValue) * this.value;
    }

    public void setCurrentValue(double val) {
        this.value = Mth.clamp((val - this.minValue) / (this.maxValue - this.minValue), 0.0, 1.0);
        this.lastAppliedValue = val;
        this.updateMessage();
    }

    // ========== 拖动 API ==========

    public boolean isDragging() { return dragging; }

    public void startDrag(double mouseX) {
        dragging = true;
        setValueFromMouse(mouseX);
    }

    /** 强制更新拖动值（由 ConfigPage.mouseDragged 直接调用） */
    public void forceDrag(double mouseX) {
        if (!dragging) return;
        setValueFromMouse(mouseX);
    }

    public void stopDrag() {
        dragging = false;
    }

    private void setValueFromMouse(double mouseX) {
        double effective = mouseX - this.getX() - DesignTokens.SLIDER_HANDLE_WIDTH / 2.0;
        double range = (double) (this.width - DesignTokens.SLIDER_HANDLE_WIDTH);
        double newValue = range > 0 ? effective / range : 0;
        this.value = Mth.clamp(newValue, 0.0, 1.0);
        this.updateMessage();
        this.applyValue();
    }

    // ========== MC 事件 ==========

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            startDrag(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            stopDrag();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            setValueFromMouse(mouseX);
            return true;
        }
        return false;
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

    // ========== 渲染 ==========

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int trackY = this.getY() + (this.height - DesignTokens.SLIDER_TRACK_HEIGHT) / 2;

        graphics.fill(this.getX(), trackY, this.getX() + this.width, trackY + DesignTokens.SLIDER_TRACK_HEIGHT, DesignTokens.SLIDER_TRACK_BG);
        drawBorder(graphics, this.getX(), trackY, this.width, DesignTokens.SLIDER_TRACK_HEIGHT, DesignTokens.SLIDER_TRACK_BORDER);

        int handleX = this.getX() + (int) ((this.width - DesignTokens.SLIDER_HANDLE_WIDTH) * this.value);
        int filledWidth = handleX - this.getX();
        if (filledWidth > 0) {
            graphics.fill(this.getX(), trackY, this.getX() + filledWidth, trackY + DesignTokens.SLIDER_TRACK_HEIGHT, DesignTokens.TAB_ACTIVE_UNDERLINE);
        }

        int handleColor = (this.isHovered() || this.dragging) ? DesignTokens.SLIDER_HANDLE_HOVER : DesignTokens.SLIDER_HANDLE_BG;
        graphics.fill(handleX, this.getY(), handleX + DesignTokens.SLIDER_HANDLE_WIDTH, this.getY() + this.height, handleColor);
        drawBorder(graphics, handleX, this.getY(), DesignTokens.SLIDER_HANDLE_WIDTH, this.height, DesignTokens.SLIDER_HANDLE_BORDER);

        int textH = UI.lineHeight(DesignTokens.TEXT_SCALE);
        int textY = this.getY() + (this.height - textH) / 2;
        UI.drawScaledCentered(graphics, Minecraft.getInstance().font, this.getMessage(),
            this.getX() + this.width / 2, textY, DesignTokens.TEXT_SCALE, 0xFFFFFFFF);
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, this.getMessage());
    }
}
