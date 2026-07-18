package name.modid.client.screen.pages;

import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernSlider;
import name.modid.client.screen.widget.SectionCard;
import name.modid.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置页面基类
 * 使用 SectionCard 系统组织配置项，使用 pose translate 实现滚动
 *
 * 关键修复：
 *   1. 滚动条支持鼠标拖拽（track + thumb 命中均可）
 *   2. 内容区右侧为滚动条预留宽度，避免控件与滚动条重叠
 *   3. 悬浮态使用加过 scrollOffset 的鼠标坐标，使渲染中的高亮与命中判定一致
 */
public abstract class ConfigPage {

    protected final ModConfig config;
    protected Minecraft minecraft;
    protected Font font;

    // Section 卡片列表
    protected final List<SectionCard> sections = new ArrayList<>();

    // 布局参数（由主屏幕设置）
    protected int scrollAreaX;
    protected int scrollAreaY;
    protected int scrollAreaWidth;
    protected int scrollAreaHeight;
    protected int contentWidth; // 内容区宽度（面板内减去边距和滚动条）

    // 滚动状态
    public double scrollOffset = 0;
    protected double maxScrollOffset = 0;
    protected int virtualContentHeight = 0;

    // 滚动条拖动状态
    private boolean scrollbarDragging = false;
    /** 拖动开始时鼠标在 thumb 中的相对 Y（用于平滑跟随，而非把 thumb 顶端吸附到鼠标） */
    private double scrollbarDragGrabOffset = 0;

    public ConfigPage(ModConfig config) {
        this.config = config;
    }

    /**
     * 初始化页面（由主屏幕调用）
     */
    public void init(int areaX, int areaY, int areaWidth, int areaHeight, Minecraft minecraft, Font font) {
        this.scrollAreaX = areaX;
        this.scrollAreaY = areaY;
        this.scrollAreaWidth = areaWidth;
        this.scrollAreaHeight = areaHeight;
        // 内容宽度 = 区域宽 - 左右内边距 - 滚动条宽度 - 滚动条与内容间距
        this.contentWidth = areaWidth
            - DesignTokens.SCROLL_AREA_PADDING * 2
            - DesignTokens.SCROLLBAR_WIDTH
            - DesignTokens.SCROLLBAR_GAP;
        this.minecraft = minecraft;
        this.font = font;

        this.sections.clear();
        this.scrollOffset = 0;
        this.scrollbarDragging = false;

        // 子类构建 Section 卡片
        buildPage();

        // 布局所有卡片并计算虚拟高度
        layoutSections();
    }

    /**
     * 子类实现：构建 Section 卡片和配置项
     */
    protected abstract void buildPage();

    /**
     * 获取页面标题
     */
    public abstract Component getTitle();

    /**
     * 添加一个 Section 卡片
     */
    protected SectionCard addSection(String title) {
        SectionCard card = new SectionCard(title);
        sections.add(card);
        return card;
    }

    /**
     * 布局所有 Section 卡片，计算虚拟内容高度
     */
    private void layoutSections() {
        int cardX = scrollAreaX + DesignTokens.SCROLL_AREA_PADDING;
        int currentY = scrollAreaY; // 顶部不再额外留 CONTENT_TOP，由屏幕分配

        for (int i = 0; i < sections.size(); i++) {
            SectionCard card = sections.get(i);
            int cardHeight = card.layout(cardX, currentY, contentWidth);
            currentY += cardHeight;
            if (i < sections.size() - 1) {
                currentY += DesignTokens.CARD_GAP;
            }
        }

        this.virtualContentHeight = currentY - scrollAreaY;
        this.maxScrollOffset = Math.max(0, virtualContentHeight - scrollAreaHeight);
        // 若内容缩小导致当前偏移越界，回拉
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScrollOffset));
    }

    /**
     * 渲染页面
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 裁剪区域（屏幕绝对坐标，不受 pose 影响）
        graphics.enableScissor(scrollAreaX, scrollAreaY, scrollAreaX + scrollAreaWidth, scrollAreaY + scrollAreaHeight);

        // 使用 pose translate 偏移渲染
        graphics.pose().pushPose();
        graphics.pose().translate(0, -scrollOffset, 0);

        // 调整鼠标 Y 坐标以匹配虚拟坐标（重要：悬浮高亮依赖此值）
        double adjustedMouseY = mouseY + scrollOffset;

        for (SectionCard card : sections) {
            card.render(graphics, mouseX, (int) adjustedMouseY, partialTick);
        }

        graphics.pose().popPose();
        graphics.disableScissor();

        // 绘制滚动条（不受 translate 影响）
        if (maxScrollOffset > 0) {
            renderScrollbar(graphics, mouseX, mouseY);
        }
    }

    // ========== 滚动条几何 ==========

    private int scrollbarX() {
        return scrollAreaX + scrollAreaWidth - DesignTokens.SCROLLBAR_WIDTH;
    }

    private int scrollbarThumbHeight() {
        double ratio = (double) scrollAreaHeight / virtualContentHeight;
        return Math.max(DesignTokens.SCROLLBAR_MIN_THUMB, (int) (scrollAreaHeight * ratio));
    }

    private int scrollbarThumbY() {
        int thumbH = scrollbarThumbHeight();
        return scrollAreaY + (int) ((scrollAreaHeight - thumbH) * (maxScrollOffset > 0 ? scrollOffset / maxScrollOffset : 0));
    }

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int sbX = scrollbarX();
        int sbY = scrollAreaY;
        int sbH = scrollAreaHeight;

        graphics.fill(sbX, sbY, sbX + DesignTokens.SCROLLBAR_WIDTH, sbY + sbH, DesignTokens.SCROLLBAR_TRACK);

        int thumbH = scrollbarThumbHeight();
        int thumbY = scrollbarThumbY();

        boolean hoveringThumb = !scrollbarDragging
            && mouseX >= sbX && mouseX < sbX + DesignTokens.SCROLLBAR_WIDTH
            && mouseY >= thumbY && mouseY < thumbY + thumbH;
        int thumbColor = (scrollbarDragging || hoveringThumb) ? DesignTokens.SCROLLBAR_THUMB_HOVER : DesignTokens.SCROLLBAR_THUMB;
        graphics.fill(sbX, thumbY, sbX + DesignTokens.SCROLLBAR_WIDTH, thumbY + thumbH, thumbColor);
    }

    // ========== 鼠标事件 ==========

    /**
     * 处理鼠标滚轮
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInContentArea(mouseX, mouseY)) {
            if (maxScrollOffset > 0) {
                scrollOffset = clampScroll(scrollOffset - scrollY * 20);
                return true;
            }
        }
        return false;
    }

    /**
     * 处理鼠标点击（虚拟坐标）
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // 1) 滚动条命中优先（点击 track/thumb 都可）
        if (maxScrollOffset > 0 && isInScrollbar(mouseX, mouseY)) {
            int thumbY = scrollbarThumbY();
            int thumbH = scrollbarThumbHeight();
            if (mouseY >= thumbY && mouseY < thumbY + thumbH) {
                // 点中 thumb：记录抓取偏移，平滑拖动
                scrollbarDragGrabOffset = mouseY - thumbY;
            } else {
                // 点中 track 空白处：跳转使 thumb 中心对齐鼠标，再以中心为抓取点
                scrollbarDragGrabOffset = thumbH / 2.0;
                setScrollFromThumbMouse(mouseY);
            }
            scrollbarDragging = true;
            return true;
        }

        // 2) 内容区控件
        if (mouseX < scrollAreaX || mouseX > scrollbarX()) return false;

        double adjustedMouseY = mouseY + scrollOffset;

        // 2a) 标题栏点击 -> 折叠/展开
        for (SectionCard card : sections) {
            if (card.isTitleClicked(mouseX, adjustedMouseY)) {
                card.toggleCollapsed();
                layoutSections();
                return true;
            }
        }

        for (SectionCard card : sections) {
            for (AbstractWidget widget : card.getAllWidgets()) {
                if (widget.isMouseOver(mouseX, adjustedMouseY) && widget.mouseClicked(mouseX, adjustedMouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 处理鼠标拖动
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && scrollbarDragging) {
            setScrollFromThumbMouse(mouseY);
            return true;
        }

        double adjustedMouseY = mouseY + scrollOffset;

        // 优先处理正在拖动的滑块：直接调用 forceDrag，跳过 isMouseOver 检查
        for (SectionCard card : sections) {
            for (AbstractWidget widget : card.getAllWidgets()) {
                if (widget instanceof ModernSlider slider && slider.isDragging()) {
                    slider.forceDrag(mouseX);
                    return true;
                }
            }
        }

        // 其他控件正常派发事件
        for (SectionCard card : sections) {
            for (AbstractWidget widget : card.getAllWidgets()) {
                if (widget.mouseDragged(mouseX, adjustedMouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 处理鼠标释放
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }

        double adjustedMouseY = mouseY + scrollOffset;

        for (SectionCard card : sections) {
            for (AbstractWidget widget : card.getAllWidgets()) {
                if (widget.mouseReleased(mouseX, adjustedMouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ========== 内部工具 ==========

    private boolean isInContentArea(double mouseX, double mouseY) {
        return mouseX >= scrollAreaX && mouseX < scrollbarX()
            && mouseY >= scrollAreaY && mouseY < scrollAreaY + scrollAreaHeight;
    }

    private boolean isInScrollbar(double mouseX, double mouseY) {
        return mouseX >= scrollbarX() && mouseX < scrollbarX() + DesignTokens.SCROLLBAR_WIDTH
            && mouseY >= scrollAreaY && mouseY < scrollAreaY + scrollAreaHeight;
    }

    private double clampScroll(double v) {
        return Math.max(0, Math.min(maxScrollOffset, v));
    }

    /**
     * 根据鼠标 Y 设定滚动偏移，使 thumb 的"抓取点"对齐鼠标。
     * scrollbarDragGrabOffset 为抓取点相对 thumb 顶部的偏移。
     */
    private void setScrollFromThumbMouse(double mouseY) {
        int thumbH = scrollbarThumbHeight();
        // 目标 thumb 顶部 = mouseY - grabOffset
        double targetThumbTop = mouseY - scrollbarDragGrabOffset;
        // thumb 顶部可移动范围 [0, sbH - thumbH]
        double maxThumbTravel = scrollAreaHeight - thumbH;
        if (maxThumbTravel <= 0) {
            scrollOffset = 0;
            return;
        }
        double ratio = (targetThumbTop - scrollAreaY) / maxThumbTravel;
        scrollOffset = clampScroll(ratio * maxScrollOffset);
    }
}
