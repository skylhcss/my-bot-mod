package name.modid.client.screen.widget;

/**
 * 设计令牌 - 集中定义所有 UI 视觉常量
 * 确保整个配置界面风格一致
 *
 * 布局原则：全屏面板，工作台式主页 + 可关闭标签页，内部紧凑分组，
 * 文本统一使用小号字体，所有配置行高固定为 ROW_HEIGHT，避免混排重叠。
 */
public final class DesignTokens {

    private DesignTokens() {} // 纯静态常量类

    // ========== 主题强调色（统一派生自 ACCENT） ==========
    /** 主强调色（青蓝，现代深色主题） */
    public static final int ACCENT = 0xFF56C7F5;
    public static final int ACCENT_SOFT = 0xFF8FD3FF;

    // ========== 文本缩放（统一小号字体，更紧凑） ==========
    /** 小号字体缩放比例，用于所有界面文字。0.7 ≈ 5.6px 高，单页可容纳更多项 */
    public static final float TEXT_SCALE = 0.7F;

    // ========== 全屏面板布局 ==========
    public static final int PANEL_H_MARGIN = 8;
    public static final int PANEL_TOP_MARGIN = 8;
    public static final int PANEL_BOTTOM_MARGIN = 8;

    // ========== 顶部标题栏 ==========
    public static final int HEADER_HEIGHT = 16;
    public static final int HEADER_BOTTOM_GAP = 5;
    public static final int HEADER_TEXT_COLOR = 0xFFFFFFFF;
    public static final int HEADER_DIVIDER_COLOR = 0x22FFFFFF;

    // ========== Tab 栏（旧标签栏样式，保留供兼容） ==========
    public static final int TAB_BAR_HEIGHT = 16;
    public static final int TAB_BAR_GAP = 2;
    public static final int TAB_BAR_BOTTOM_GAP = 5;

    public static final int TAB_NORMAL_BG = 0x00000000;
    public static final int TAB_HOVER_BG = 0x18FFFFFF;
    public static final int TAB_ACTIVE_BG = 0x28FFFFFF;
    public static final int TAB_ACTIVE_UNDERLINE = ACCENT;
    public static final int TAB_TEXT_NORMAL = 0xFF9AA0AA;
    public static final int TAB_TEXT_ACTIVE = ACCENT;

    // ========== 面板颜色 ==========
    public static final int PANEL_BG = 0xE6121620;
    public static final int PANEL_BORDER = 0x40FFFFFF;

    // ========== 卡片 ==========
    public static final int CARD_RADIUS = 4;
    public static final int CARD_H_PADDING = 8;
    public static final int CARD_V_PADDING = 6;
    public static final int CARD_TITLE_HEIGHT = 8;
    public static final int CARD_TITLE_GAP = 4;
    public static final int CARD_GAP = 5;
    public static final int CARD_BG = 0x3C1B2130;
    public static final int CARD_BORDER = 0x28FFFFFF;
    public static final int CARD_TITLE_COLOR = ACCENT_SOFT;

    // ========== 配置项行 ==========
    /** 统一行高：所有控件（slider/checkbox/button）都使用此高度，避免混排重叠 */
    public static final int ROW_HEIGHT = 12;
    public static final int ROW_GAP = 2;
    public static final int ITEM_TEXT_COLOR = 0xFFE2E6EC;

    // ========== 滚动区域 ==========
    public static final int SCROLL_AREA_PADDING = 6;
    public static final int SCROLLBAR_WIDTH = 5;
    /** 滚动条与内容之间的间距 */
    public static final int SCROLLBAR_GAP = 4;
    public static final int SCROLLBAR_MIN_THUMB = 18;
    public static final int SCROLLBAR_TRACK = 0x14FFFFFF;
    public static final int SCROLLBAR_THUMB = 0x44FFFFFF;
    public static final int SCROLLBAR_THUMB_HOVER = 0x77FFFFFF;

    // ========== Checkbox ==========
    public static final int CHECKBOX_BOX_SIZE = 9;
    public static final int CHECKBOX_BOX_BG = 0x22FFFFFF;
    public static final int CHECKBOX_BOX_HOVER = 0x33FFFFFF;
    public static final int CHECKBOX_BOX_BORDER = 0x44FFFFFF;
    public static final int CHECKBOX_CHECK_COLOR = ACCENT;

    // ========== Slider ==========
    public static final int SLIDER_TRACK_HEIGHT = 3;
    public static final int SLIDER_HANDLE_WIDTH = 6;
    public static final int SLIDER_TRACK_BG = 0x25FFFFFF;
    public static final int SLIDER_TRACK_BORDER = 0x35FFFFFF;
    public static final int SLIDER_HANDLE_BG = 0x66FFFFFF;
    public static final int SLIDER_HANDLE_HOVER = 0x88FFFFFF;
    public static final int SLIDER_HANDLE_BORDER = ACCENT;

    // ========== Reset 按钮 ==========
    public static final int RESET_SIZE = 11;
    public static final int RESET_BG = 0x00000000;
    public static final int RESET_HOVER_BG = 0x30FFFFFF;
    public static final int RESET_ICON_COLOR = 0xFFAAB0B8;
    public static final int RESET_HOVER_ICON = 0xFFFFFFFF;
    public static final int RESET_BORDER = 0x30FFFFFF;

    // ========== 底部按钮 ==========
    public static final int FOOTER_HEIGHT = 20;
    public static final int FOOTER_TOP_GAP = 5;
    public static final int DONE_BUTTON_WIDTH = 100;
    public static final int DONE_BUTTON_HEIGHT = 15;
    public static final int DONE_BUTTON_BG = 0x3056C7F5;
    public static final int DONE_BUTTON_HOVER = 0x5056C7F5;
    public static final int DONE_BUTTON_BORDER = 0x8056C7F5;

    // ========== 工作台式主页（分类卡片网格） ==========
    public static final int HOME_COLUMNS = 3;
    public static final int HOME_CARD_HEIGHT = 38;
    public static final int HOME_CARD_GAP = 6;
    public static final int HOME_CARD_BG = 0x3C1B2130;
    public static final int HOME_CARD_HOVER_BG = 0x4056C7F5; // 悬浮青蓝底
    public static final int HOME_CARD_BORDER = 0x30FFFFFF;
    public static final int HOME_CARD_HOVER_BORDER = ACCENT;
    public static final int HOME_CARD_TITLE_COLOR = 0xFFFFFFFF;
    public static final int HOME_CARD_DESC_COLOR = 0xFF9AA0AA;

    // ========== 标签页头部（可关闭 + 搜索） ==========
    public static final int PAGE_HEADER_HEIGHT = 16;
    public static final int CLOSE_ICON_COLOR = 0xFFC0C4CC;
    public static final int CLOSE_ICON_HOVER = 0xFFFF6B6B;

    // ========== 搜索框 ==========
    public static final int SEARCH_HEIGHT = 14;
    public static final int SEARCH_BG = 0x40000000;
    public static final int SEARCH_BORDER = 0x40FFFFFF;
    public static final int SEARCH_BORDER_FOCUS = ACCENT;
    public static final int SEARCH_TEXT_COLOR = 0xFFFFFFFF;
    public static final int SEARCH_HINT_COLOR = 0xFF808890;

    // ========== 颜色工具 ==========
    /**
     * 在 0xAARRGGBB 颜色上叠加透明度倍数（保留 RGB，缩放 alpha）
     */
    public static int withAlpha(int argb, float alphaMul) {
        int a = (int) (((argb >> 24) & 0xFF) * alphaMul);
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
