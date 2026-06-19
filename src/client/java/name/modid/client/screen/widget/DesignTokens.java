package name.modid.client.screen.widget;

/**
 * 设计令牌 - 集中定义所有 UI 视觉常量
 * 确保整个配置界面风格一致
 *
 * 布局原则：全屏面板，内部紧凑分组，文本统一使用 0.75 倍缩放小号字体，
 * 所有行高固定为 ROW_HEIGHT，避免不同控件类型混排时出现重叠。
 */
public final class DesignTokens {

    private DesignTokens() {} // 纯静态常量类

    // ========== 文本缩放（统一小号字体） ==========
    /** 小号字体缩放比例，用于所有界面文字。0.75 ≈ 6px 高 */
    public static final float TEXT_SCALE = 0.75F;

    // ========== 全屏面板布局 ==========
    public static final int PANEL_H_MARGIN = 8;
    public static final int PANEL_TOP_MARGIN = 8;
    public static final int PANEL_BOTTOM_MARGIN = 8;

    // ========== 顶部标题栏 ==========
    public static final int HEADER_HEIGHT = 18;
    public static final int HEADER_BOTTOM_GAP = 6;
    public static final int HEADER_TEXT_COLOR = 0xFFFFFFFF;
    public static final int HEADER_DIVIDER_COLOR = 0x20FFFFFF;

    // ========== Tab 栏 ==========
    public static final int TAB_BAR_HEIGHT = 18;
    public static final int TAB_BAR_GAP = 2;
    public static final int TAB_BAR_BOTTOM_GAP = 6;

    public static final int TAB_NORMAL_BG = 0x00000000;
    public static final int TAB_HOVER_BG = 0x15FFFFFF;
    public static final int TAB_ACTIVE_BG = 0x25FFFFFF;
    public static final int TAB_ACTIVE_UNDERLINE = 0xFF55FF55;
    public static final int TAB_TEXT_NORMAL = 0xFFA0A0A0;
    public static final int TAB_TEXT_ACTIVE = 0xFF55FF55;

    // ========== 面板颜色 ==========
    public static final int PANEL_BG = 0xC0101018;
    public static final int PANEL_BORDER = 0x30FFFFFF;

    // ========== 卡片 ==========
    public static final int CARD_RADIUS = 4;
    public static final int CARD_H_PADDING = 10;
    public static final int CARD_V_PADDING = 8;
    public static final int CARD_TITLE_HEIGHT = 9;
    public static final int CARD_TITLE_GAP = 6;
    public static final int CARD_GAP = 6;
    public static final int CARD_BG = 0x30181820;
    public static final int CARD_BORDER = 0x20FFFFFF;
    public static final int CARD_TITLE_COLOR = 0xFFB0E0B0;

    // ========== 配置项行 ==========
    /** 统一行高：所有控件（slider/checkbox/button）都使用此高度，避免混排重叠 */
    public static final int ROW_HEIGHT = 14;
    public static final int ROW_GAP = 2;
    public static final int ITEM_TEXT_COLOR = 0xFFE0E0E0;

    // ========== 滚动区域 ==========
    public static final int SCROLL_AREA_PADDING = 6;
    public static final int SCROLLBAR_WIDTH = 6;
    /** 滚动条与内容之间的间距 */
    public static final int SCROLLBAR_GAP = 4;
    public static final int SCROLLBAR_MIN_THUMB = 20;
    public static final int SCROLLBAR_TRACK = 0x10FFFFFF;
    public static final int SCROLLBAR_THUMB = 0x40FFFFFF;
    public static final int SCROLLBAR_THUMB_HOVER = 0x70FFFFFF;

    // ========== Checkbox ==========
    public static final int CHECKBOX_BOX_SIZE = 10;
    public static final int CHECKBOX_BOX_BG = 0x20FFFFFF;
    public static final int CHECKBOX_BOX_HOVER = 0x30FFFFFF;
    public static final int CHECKBOX_BOX_BORDER = 0x40FFFFFF;
    public static final int CHECKBOX_CHECK_COLOR = 0xFF55FF55;

    // ========== Slider ==========
    public static final int SLIDER_TRACK_HEIGHT = 3;
    public static final int SLIDER_HANDLE_WIDTH = 6;
    public static final int SLIDER_TRACK_BG = 0x25FFFFFF;
    public static final int SLIDER_TRACK_BORDER = 0x35FFFFFF;
    public static final int SLIDER_HANDLE_BG = 0x60FFFFFF;
    public static final int SLIDER_HANDLE_HOVER = 0x80FFFFFF;
    public static final int SLIDER_HANDLE_BORDER = 0x40FFFFFF;

    // ========== Reset 按钮 ==========
    public static final int RESET_SIZE = 12;
    public static final int RESET_BG = 0x00000000;
    public static final int RESET_HOVER_BG = 0x30FFFFFF;
    public static final int RESET_ICON_COLOR = 0xFFAAAAAA;
    public static final int RESET_HOVER_ICON = 0xFFFFFFFF;
    public static final int RESET_BORDER = 0x30FFFFFF;

    // ========== 底部按钮 ==========
    public static final int FOOTER_HEIGHT = 22;
    public static final int FOOTER_TOP_GAP = 6;
    public static final int DONE_BUTTON_WIDTH = 100;
    public static final int DONE_BUTTON_HEIGHT = 16;
    public static final int DONE_BUTTON_BG = 0x3055FF55;
    public static final int DONE_BUTTON_HOVER = 0x5055FF55;
    public static final int DONE_BUTTON_BORDER = 0x6055FF55;

    // ========== 颜色工具 ==========
    /**
     * 在 0xAARRGGBB 颜色上叠加透明度倍数（保留 RGB，缩放 alpha）
     */
    public static int withAlpha(int argb, float alphaMul) {
        int a = (int) (((argb >> 24) & 0xFF) * alphaMul);
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
