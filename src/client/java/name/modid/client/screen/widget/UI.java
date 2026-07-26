package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * UI 绘制工具
 * 通过 pose 缩放实现"小号字体"，并把屏幕坐标换算为缩放空间下的整数坐标，
 * 避免子像素采样造成的模糊。
 * 提供居中/左对齐两种小号文字绘制方法。
 *
 * 注意：使用 pushPose/scale/popPose，不直接操作矩阵字段，
 * 以兼容 MC 1.20.1 自带的 joml 版本（其 Matrix4f 字段非 public）。
 */
public final class UI {

    private UI() {}

    /**
     * 绘制小号居中文字
     *
     * @param centerX 文字中心 X（屏幕绝对坐标）
     * @param y       文字顶部 Y（屏幕绝对坐标）
     */
    public static void drawScaledCentered(GuiGraphics g, Font font, Component text, int centerX, int y, float scale, int color) {
        // 把屏幕坐标转换为缩放空间坐标：先除以 scale 得到缩放空间下的整数坐标，
        // 再整体 scale，使得最终落点恰为原始屏幕坐标，避免浮点漂移与模糊
        int scaledCenterX = Math.round(centerX / scale);
        int scaledY = Math.round(y / scale);
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0F);
        g.drawCenteredString(font, text, scaledCenterX, scaledY, color);
        g.pose().popPose();
    }

    /**
     * 绘制小号居中文字（字符串重载）
     */
    public static void drawScaledCentered(GuiGraphics g, Font font, String text, int centerX, int y, float scale, int color) {
        drawScaledCentered(g, font, Component.literal(text), centerX, y, scale, color);
    }

    /**
     * 绘制小号左对齐文字
     *
     * @param x 文字左边界 X（屏幕绝对坐标）
     * @param y 文字顶部 Y（屏幕绝对坐标）
     */
    public static void drawScaled(GuiGraphics g, Font font, Component text, int x, int y, float scale, int color) {
        int scaledX = Math.round(x / scale);
        int scaledY = Math.round(y / scale);
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, scaledX, scaledY, color, false);
        g.pose().popPose();
    }

    /**
     * 绘制小号左对齐文字（字符串重载）
     */
    public static void drawScaled(GuiGraphics g, Font font, String text, int x, int y, float scale, int color) {
        drawScaled(g, font, Component.literal(text), x, y, scale, color);
    }

    /**
     * 测量文字在指定缩放下的像素宽度
     */
    public static float textWidth(Font font, Component text, float scale) {
        return font.width(text) * scale;
    }

    /**
     * 小号字体的默认行高
     */
    public static int lineHeight(float scale) {
        return Math.round(9 * scale);
    }

    /**
     * 便捷获取默认字体
     */
    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /** 绘制 1px 矩形边框（收敛各处重复的 drawBorder 实现） */
    public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
