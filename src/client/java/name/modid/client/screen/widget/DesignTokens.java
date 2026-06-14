package name.modid.client.screen.widget;

/**
 * 设计令牌 - 集中定义所有 UI 视觉常量
 * 确保整个配置界面风格一致
 */
public final class DesignTokens {
    
    private DesignTokens() {} // 纯静态常量类
    
    // ========== 面板布局 ==========
    public static final int PANEL_MAX_WIDTH = 380;
    public static final int PANEL_H_PADDING = 20;
    public static final int PANEL_TOP_MARGIN = 32;
    public static final int PANEL_BOTTOM_MARGIN = 40;
    
    // ========== Tab 栏 ==========
    public static final int TAB_BAR_HEIGHT = 28;
    public static final int TAB_BAR_GAP = 2;
    
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
    public static final int CARD_H_PADDING = 12;
    public static final int CARD_V_PADDING = 10;
    public static final int CARD_TITLE_HEIGHT = 14;
    public static final int CARD_TITLE_GAP = 8;
    public static final int CARD_GAP = 10;
    public static final int CARD_BG = 0x30181820;
    public static final int CARD_BORDER = 0x20FFFFFF;
    public static final int CARD_TITLE_COLOR = 0xFFFFFFFF;
    
    // ========== 配置项行 ==========
    public static final int ROW_HEIGHT = 22;
    public static final int ROW_GAP = 4;
    public static final int ITEM_TEXT_COLOR = 0xFFE0E0E0;
    
    // ========== 滚动区域 ==========
    public static final int SCROLL_AREA_PADDING = 4;
    public static final int CONTENT_TOP = 8;
    public static final int SCROLLBAR_WIDTH = 4;
    public static final int SCROLLBAR_MIN_THUMB = 16;
    public static final int SCROLLBAR_TRACK = 0x10FFFFFF;
    public static final int SCROLLBAR_THUMB = 0x40FFFFFF;
    
    // ========== Checkbox ==========
    public static final int CHECKBOX_BOX_SIZE = 14;
    public static final int CHECKBOX_BOX_BG = 0x20FFFFFF;
    public static final int CHECKBOX_BOX_HOVER = 0x30FFFFFF;
    public static final int CHECKBOX_BOX_BORDER = 0x40FFFFFF;
    public static final int CHECKBOX_CHECK_COLOR = 0xFF55FF55;
    
    // ========== Slider ==========
    public static final int SLIDER_TRACK_HEIGHT = 4;
    public static final int SLIDER_HANDLE_WIDTH = 8;
    public static final int SLIDER_TRACK_BG = 0x25FFFFFF;
    public static final int SLIDER_TRACK_BORDER = 0x35FFFFFF;
    public static final int SLIDER_HANDLE_BG = 0x60FFFFFF;
    public static final int SLIDER_HANDLE_HOVER = 0x80FFFFFF;
    public static final int SLIDER_HANDLE_BORDER = 0x40FFFFFF;
    
    // ========== Reset 按钮 ==========
    public static final int RESET_SIZE = 16;
    public static final int RESET_BG = 0x00000000;
    public static final int RESET_HOVER_BG = 0x30FFFFFF;
    public static final int RESET_ICON_COLOR = 0xFFAAAAAA;
    public static final int RESET_HOVER_ICON = 0xFFFFFFFF;
    public static final int RESET_BORDER = 0x30FFFFFF;
    
    // ========== 底部按钮 ==========
    public static final int DONE_BUTTON_WIDTH = 120;
    public static final int DONE_BUTTON_HEIGHT = 20;
    public static final int DONE_BUTTON_BG = 0x3055FF55;
    public static final int DONE_BUTTON_HOVER = 0x5055FF55;
    public static final int DONE_BUTTON_BORDER = 0x6055FF55;
}
