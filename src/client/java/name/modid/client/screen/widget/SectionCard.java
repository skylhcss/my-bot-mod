package name.modid.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Section 卡片组件
 * 带圆角背景、边框和标题的容器，用于组织配置项。
 * 支持搜索过滤：命中标题则整卡显示；否则仅显示标签命中的项；无命中则整卡隐藏。
 */
public class SectionCard {

    private final String title;
    private int x, y;
    private int width;
    private int height;
    private boolean collapsed = false;
    private final List<AbstractWidget> items = new ArrayList<>();
    private final List<ResetButton> resets = new ArrayList<>();

    // 搜索过滤状态
    private boolean[] itemVisible = new boolean[0];
    private boolean sectionVisible = true;

    public SectionCard(String title) {
        this.title = title;
    }

    public boolean isCollapsed() { return collapsed; }
    public void setCollapsed(boolean collapsed) { this.collapsed = collapsed; }
    public void toggleCollapsed() { this.collapsed = !this.collapsed; }
    public boolean isVisibleSection() { return sectionVisible; }

    /** 标题栏（可点击折叠）高度 */
    public int headerHeight() {
        return DesignTokens.CARD_V_PADDING + DesignTokens.CARD_TITLE_HEIGHT + DesignTokens.CARD_TITLE_GAP;
    }

    /** 判断坐标是否命中标题栏（虚拟坐标） */
    public boolean isTitleClicked(double mouseX, double mouseY) {
        return sectionVisible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + headerHeight();
    }

    /**
     * 添加一个配置项和可选的重置按钮
     */
    public void addItem(AbstractWidget widget, ResetButton reset) {
        items.add(widget);
        resets.add(reset);
    }

    public void addItem(AbstractWidget widget) {
        items.add(widget);
        resets.add(null);
    }

    /**
     * 应用搜索过滤（queryLower 已转小写）。
     * @return 本卡过滤后是否可见
     */
    public boolean applyFilter(String queryLower) {
        int n = items.size();
        if (itemVisible.length != n) {
            itemVisible = new boolean[n];
        }
        if (queryLower == null || queryLower.isEmpty()) {
            Arrays.fill(itemVisible, true);
            sectionVisible = true;
            return true;
        }
        boolean titleMatch = SearchLang.textContains(title, queryLower);
        boolean any = false;
        for (int i = 0; i < n; i++) {
            boolean m = titleMatch || SearchLang.widgetMatches(items.get(i), queryLower);
            itemVisible[i] = m;
            any = any || m;
        }
        sectionVisible = any;
        return any;
    }

    private boolean visible(int i) {
        return i >= itemVisible.length || itemVisible[i];
    }

    /**
     * 计算并布局所有子项，返回卡片总高度（隐藏时返回 0）。
     * 所有行统一高度 ROW_HEIGHT，避免不同控件类型混排出现重叠。
     */
    public int layout(int cardX, int cardY, int cardWidth) {
        this.x = cardX;
        this.y = cardY;
        this.width = cardWidth;

        if (!sectionVisible) {
            this.height = 0;
            return 0;
        }

        // 折叠时仅保留标题行高度
        if (collapsed) {
            this.height = DesignTokens.CARD_V_PADDING + DesignTokens.CARD_TITLE_HEIGHT + DesignTokens.CARD_V_PADDING;
            return this.height;
        }

        int currentY = cardY + DesignTokens.CARD_V_PADDING + DesignTokens.CARD_TITLE_HEIGHT + DesignTokens.CARD_TITLE_GAP;
        // 留出右侧一个 reset 按钮的宽度（reset 紧贴右内边距），控件文字居中时不会被 reset 遮挡
        int itemWidth = cardWidth - DesignTokens.CARD_H_PADDING * 2 - DesignTokens.RESET_SIZE - 2;

        boolean placedAny = false;
        for (int i = 0; i < items.size(); i++) {
            if (!visible(i)) {
                continue;
            }
            AbstractWidget item = items.get(i);
            // 统一行高，避免不同控件（slider/checkbox/button）自定义高度造成重叠
            item.setX(cardX + DesignTokens.CARD_H_PADDING);
            item.setY(currentY);
            item.setWidth(itemWidth);
            if (item instanceof ModernCheckbox cb) cb.setHeight(DesignTokens.ROW_HEIGHT);
            else if (item instanceof ModernSlider sl) sl.setHeight(DesignTokens.ROW_HEIGHT);
            else if (item instanceof ModernButton btn) btn.setHeight(DesignTokens.ROW_HEIGHT);
            else if (item instanceof ResetButton rb) rb.setHeight(DesignTokens.ROW_HEIGHT);

            ResetButton reset = resets.get(i);
            if (reset != null) {
                reset.setX(cardX + cardWidth - DesignTokens.CARD_H_PADDING - DesignTokens.RESET_SIZE);
                reset.setY(currentY + (DesignTokens.ROW_HEIGHT - DesignTokens.RESET_SIZE) / 2);
            }

            currentY += DesignTokens.ROW_HEIGHT + DesignTokens.ROW_GAP;
            placedAny = true;
        }

        // 移除末尾多余间距
        if (placedAny) {
            currentY -= DesignTokens.ROW_GAP;
        }
        currentY += DesignTokens.CARD_V_PADDING;

        this.height = currentY - cardY;
        return this.height;
    }

    /**
     * 渲染卡片及其子项
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!sectionVisible) {
            return;
        }

        // 绘制圆角背景
        drawRoundedRect(graphics, x, y, width, height, DesignTokens.CARD_BG);

        // 绘制边框
        drawRoundedBorder(graphics, x, y, width, height, DesignTokens.CARD_BORDER);

        // 绘制标题（小号字体），前缀折叠箭头
        String arrow = collapsed ? "▶ " : "▼ ";
        UI.drawScaled(graphics,
            Minecraft.getInstance().font,
            Component.literal(arrow + title),
            x + DesignTokens.CARD_H_PADDING,
            y + DesignTokens.CARD_V_PADDING,
            DesignTokens.TEXT_SCALE,
            DesignTokens.CARD_TITLE_COLOR);

        // 折叠时不渲染子项
        if (collapsed) {
            return;
        }

        // 渲染可见子项
        for (int i = 0; i < items.size(); i++) {
            if (!visible(i)) {
                continue;
            }
            items.get(i).render(graphics, mouseX, mouseY, partialTick);
            ResetButton reset = resets.get(i);
            if (reset != null) {
                reset.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * 获取所有可见子 widget（含 ResetButton）
     */
    public List<AbstractWidget> getAllWidgets() {
        List<AbstractWidget> all = new ArrayList<>();
        // 隐藏或折叠时不暴露子控件，避免隐藏控件被点击
        if (!sectionVisible || collapsed) {
            return all;
        }
        for (int i = 0; i < items.size(); i++) {
            if (!visible(i)) {
                continue;
            }
            all.add(items.get(i));
            ResetButton reset = resets.get(i);
            if (reset != null) {
                all.add(reset);
            }
        }
        return all;
    }

    public int getHeight() { return height; }
    public int getY() { return y; }

    // ========== 圆角矩形绘制 ==========

    private void drawRoundedRect(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        int r = DesignTokens.CARD_RADIUS;
        // 顶部缩进
        graphics.fill(x + r, y, x + w - r, y + 1, color);
        for (int i = 1; i < r; i++) {
            graphics.fill(x + r - i, y + i, x + w - r + i, y + i + 1, color);
        }
        // 中间满宽
        graphics.fill(x, y + r, x + w, y + h - r, color);
        // 底部缩进
        for (int i = 1; i < r; i++) {
            graphics.fill(x + r - i, y + h - r + i - 1, x + w - r + i, y + h - r + i, color);
        }
        graphics.fill(x + r, y + h - 1, x + w - r, y + h, color);
    }

    private void drawRoundedBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        int r = DesignTokens.CARD_RADIUS;
        // 顶部边框
        graphics.fill(x + r, y, x + w - r, y + 1, color);
        // 底部边框
        graphics.fill(x + r, y + h - 1, x + w - r, y + h, color);
        // 左侧边框
        graphics.fill(x, y + r, x + 1, y + h - r, color);
        // 右侧边框
        graphics.fill(x + w - 1, y + r, x + w, y + h - r, color);
        // 圆角过渡
        for (int i = 1; i < r; i++) {
            graphics.fill(x + r - i, y + i, x + r - i + 1, y + i + 1, color);
            graphics.fill(x + w - r + i - 1, y + i, x + w - r + i, y + i + 1, color);
            graphics.fill(x + r - i, y + h - i - 1, x + r - i + 1, y + h - i, color);
            graphics.fill(x + w - r + i - 1, y + h - i - 1, x + w - r + i, y + h - i, color);
        }
    }
}
