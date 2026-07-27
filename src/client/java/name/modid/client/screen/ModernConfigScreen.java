package name.modid.client.screen;

import name.modid.client.screen.pages.*;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.UI;
import name.modid.config.ModConfig;
import name.modid.net.BotNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 现代化配置界面主屏幕（全屏）。
 *
 * 交互模型（浏览器式标签页 + 工作台式主页）：
 *   - 顶部是标签栏：最左为「主页」按钮，其后为已打开的分类标签，每个标签可单独关闭（✕）。
 *   - 主页仿 Minecraft 工作台：九宫格槽位中摆放各分类的物品图标，点击进入该分类标签页。
 *   - 点击某分类：若未打开则新建标签并切换过去；已打开则直接切换。
 *   - 关闭整个界面会记住已打开的标签与当前标签（static），下次打开自动恢复。
 */
public class ModernConfigScreen extends Screen {

    private final Screen parent;
    private final ModConfig config;

    private int panelX, panelY, panelWidth, panelHeight;
    private int tabBarY, contentY, contentHeight, footerY;

    // 分类页面
    private final List<ConfigPage> pages = new ArrayList<>();
    private static final String[] CATEGORY_KEYS = {"general", "combat", "survival", "pathfinding", "behaviors", "advanced", "bots"};
    private static final net.minecraft.world.item.Item[] CATEGORY_ITEMS = {
        Items.LEVER, Items.IRON_SWORD, Items.COOKED_BEEF, Items.COMPASS,
        Items.REPEATING_COMMAND_BLOCK, Items.COMPARATOR, Items.PLAYER_HEAD
    };

    // 浏览器式标签状态（static 以便重开时恢复）
    private static final List<Integer> openTabs = new ArrayList<>();
    /** 当前查看的分类索引；-1 表示主页 */
    private static int activeCategory = -1;

    private ConfigPage currentPage;

    // 标签栏命中矩形
    private int[] homeBtnRect;                       // [x,y,w,h]
    private final List<int[]> tabRects = new ArrayList<>(); // [x,y,w,h,categoryIndex]

    // 搜索框（标签页模式，位于标签栏右端；中英文均可匹配）
    private static final int SEARCH_W = 90;
    private EditBox searchBox;
    private int searchX, searchY;

    // 主页工作台几何
    private int homeSlot;
    private final List<int[]> homeRects = new ArrayList<>(); // 与 pages 顺序对齐 [x,y,w,h]
    private int homeWbX, homeWbY;

    public ModernConfigScreen(Screen parent) {
        super(Component.translatable("gui.my-bot-mod.config.title"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        pages.clear();
        homeRects.clear();
        tabRects.clear();
        searchBox = null;

        panelX = DesignTokens.PANEL_H_MARGIN;
        panelY = DesignTokens.PANEL_TOP_MARGIN;
        panelWidth = this.width - DesignTokens.PANEL_H_MARGIN * 2;
        panelHeight = this.height - DesignTokens.PANEL_TOP_MARGIN - DesignTokens.PANEL_BOTTOM_MARGIN;

        tabBarY = panelY + DesignTokens.SCROLL_AREA_PADDING;
        footerY = panelY + panelHeight - DesignTokens.FOOTER_HEIGHT;
        contentY = tabBarY + DesignTokens.TAB_BAR_HEIGHT + DesignTokens.TAB_BAR_BOTTOM_GAP;
        contentHeight = footerY - DesignTokens.FOOTER_TOP_GAP - contentY;

        pages.add(new GeneralPage(this, config));
        pages.add(new CombatPage(this, config));
        pages.add(new SurvivalPage(this, config));
        pages.add(new PathfindingPage(this, config));
        pages.add(new BehaviorPage(this, config));
        pages.add(new AdvancedPage(this, config));
        pages.add(new BotsPage(this, config));

        // 清理越界的已保存标签（分类数量变化时）
        openTabs.removeIf(i -> i < 0 || i >= pages.size());
        if (activeCategory >= pages.size() || (activeCategory >= 0 && !openTabs.contains(activeCategory))) {
            activeCategory = -1;
        }

        // 底部完成按钮
        int doneW = DesignTokens.DONE_BUTTON_WIDTH;
        int doneH = DesignTokens.DONE_BUTTON_HEIGHT;
        int doneX = panelX + (panelWidth - doneW) / 2;
        int doneY = footerY + (DesignTokens.FOOTER_HEIGHT - doneH) / 2;
        this.addRenderableWidget(new ModernButton(doneX, doneY, doneW, doneH,
            CommonComponents.GUI_DONE, b -> saveAndClose()));

        layoutTabBar();

        if (activeCategory < 0) {
            layoutHome();
            currentPage = null;
        } else {
            currentPage = pages.get(activeCategory);
            int pad = DesignTokens.SCROLL_AREA_PADDING;
            currentPage.init(panelX + pad, contentY, panelWidth - pad * 2, contentHeight, this.minecraft, this.font);
            // 标签栏右端的优雅搜索框（自绘底色/描边，EditBox 仅渲染文本）
            searchX = panelX + panelWidth - pad - SEARCH_W;
            searchY = tabBarY;
            searchBox = new EditBox(this.font, searchX + 5, searchY + 4, SEARCH_W - 10, 10,
                Component.translatable("gui.my-bot-mod.search.hint"));
            searchBox.setBordered(false);
            searchBox.setMaxLength(64);
            searchBox.setHint(Component.translatable("gui.my-bot-mod.search.hint"));
            searchBox.setResponder(s -> {
                if (currentPage != null) {
                    currentPage.setFilter(s);
                }
            });
            this.addRenderableWidget(searchBox);
            requestBotListIfNeeded();
        }
    }

    // ========== 标签栏 ==========

    private void layoutTabBar() {
        tabRects.clear();
        int pad = DesignTokens.SCROLL_AREA_PADDING;
        int h = DesignTokens.TAB_BAR_HEIGHT;
        int x = panelX + pad;
        int homeW = 30;
        homeBtnRect = new int[]{x, tabBarY, homeW, h};
        x += homeW + 3;

        int n = openTabs.size();
        if (n > 0) {
            // 标签页模式下为右端搜索框预留空间
            int reserved = activeCategory >= 0 ? SEARCH_W + 6 : 0;
            int avail = panelX + panelWidth - pad - x - reserved;
            int tabW = Math.max(24, Math.min(84, (avail - 3 * (n - 1)) / n));
            for (int i = 0; i < n; i++) {
                tabRects.add(new int[]{x, tabBarY, tabW, h, openTabs.get(i)});
                x += tabW + 3;
            }
        }
    }

    private void renderTabBar(GuiGraphics g, int mouseX, int mouseY) {
        float sc = DesignTokens.TEXT_SCALE;
        // 主页按钮
        boolean homeActive = activeCategory < 0;
        boolean homeHover = inRect(homeBtnRect, mouseX, mouseY);
        g.fill(homeBtnRect[0], homeBtnRect[1], homeBtnRect[0] + homeBtnRect[2], homeBtnRect[1] + homeBtnRect[3],
            homeActive ? DesignTokens.TAB_ACTIVE_BG : (homeHover ? DesignTokens.TAB_HOVER_BG : DesignTokens.TAB_NORMAL_BG));
        UI.drawScaledCentered(g, this.font, Component.translatable("gui.my-bot-mod.tab.home"),
            homeBtnRect[0] + homeBtnRect[2] / 2, homeBtnRect[1] + (homeBtnRect[3] - UI.lineHeight(sc)) / 2, sc,
            homeActive ? DesignTokens.TAB_TEXT_ACTIVE : DesignTokens.TAB_TEXT_NORMAL);
        if (homeActive) {
            g.fill(homeBtnRect[0], homeBtnRect[1] + homeBtnRect[3] - 1, homeBtnRect[0] + homeBtnRect[2], homeBtnRect[1] + homeBtnRect[3],
                DesignTokens.TAB_ACTIVE_UNDERLINE);
        }

        // 各标签
        for (int[] r : tabRects) {
            int cat = r[4];
            boolean active = cat == activeCategory;
            boolean hover = inRect(r, mouseX, mouseY);
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3],
                active ? DesignTokens.TAB_ACTIVE_BG : (hover ? DesignTokens.TAB_HOVER_BG : DesignTokens.TAB_NORMAL_BG));
            // 标题（左对齐，右侧留出关闭按钮空间）
            UI.drawScaled(g, this.font, pages.get(cat).getTitle(), r[0] + 4, r[1] + (r[3] - UI.lineHeight(sc)) / 2, sc,
                active ? DesignTokens.TAB_TEXT_ACTIVE : DesignTokens.TAB_TEXT_NORMAL);
            // 关闭 ✕
            int cxX = r[0] + r[2] - 9;
            boolean closeHover = mouseX >= cxX - 1 && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
            UI.drawScaled(g, this.font, Component.literal("✕"), cxX, r[1] + (r[3] - UI.lineHeight(sc)) / 2, sc,
                closeHover ? DesignTokens.CLOSE_ICON_HOVER : DesignTokens.CLOSE_ICON_COLOR);
            if (active) {
                g.fill(r[0], r[1] + r[3] - 1, r[0] + r[2], r[1] + r[3], DesignTokens.TAB_ACTIVE_UNDERLINE);
            }
        }
    }

    // ========== 主页（工作台式九宫格） ==========

    private void layoutHome() {
        homeRects.clear();
        homeSlot = 24;
        int gridW = homeSlot * 3;
        int panelPad = 8;
        int ctW = gridW + panelPad * 2;
        int ctH = gridW + panelPad * 2 + 26; // 底部放工作台图标
        int cx = panelX + panelWidth / 2;
        int ctX = cx - ctW / 2;
        int ctY = contentY + 8;

        int gx = ctX + panelPad;
        int gy = ctY + panelPad;
        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            int sx = gx + col * homeSlot;
            int sy = gy + row * homeSlot;
            if (i < pages.size()) {
                homeRects.add(new int[]{sx, sy, homeSlot, homeSlot});
            }
        }
        homeWbX = cx - 8;
        homeWbY = gy + gridW + 6;
        // 记录工作台面板矩形供渲染
        homePanel = new int[]{ctX, ctY, ctW, ctH};
    }

    private int[] homePanel;

    private void renderHome(GuiGraphics g, int mouseX, int mouseY) {
        if (homePanel == null) return;
        float sc = DesignTokens.TEXT_SCALE;
        // 仿 MC GUI 的灰色工作台面板
        g.fill(homePanel[0], homePanel[1], homePanel[0] + homePanel[2], homePanel[1] + homePanel[3], 0xFFC6C6C6);
        UI.border(g, homePanel[0], homePanel[1], homePanel[2], homePanel[3], 0xFF555555);

        int hovered = -1;
        for (int i = 0; i < homeRects.size(); i++) {
            int[] r = homeRects.get(i);
            drawSlot(g, r[0], r[1], homeSlot);
            boolean hover = inRect(r, mouseX, mouseY);
            if (hover) {
                g.fill(r[0] + 1, r[1] + 1, r[0] + homeSlot - 1, r[1] + homeSlot - 1, 0x80FFFFFF);
                hovered = i;
            }
            if (i < CATEGORY_ITEMS.length) {
                g.renderItem(new ItemStack(CATEGORY_ITEMS[i]), r[0] + (homeSlot - 16) / 2, r[1] + (homeSlot - 16) / 2);
            }
        }

        // 底部工作台图标（"工作台"）
        g.renderItem(new ItemStack(Items.CRAFTING_TABLE), homeWbX, homeWbY);

        // 悬浮分类的标题/描述（在灰面板下方的深色区域）
        int infoY = homePanel[1] + homePanel[3] + 8;
        int cx = panelX + panelWidth / 2;
        if (hovered >= 0 && hovered < CATEGORY_KEYS.length) {
            UI.drawScaledCentered(g, this.font, pages.get(hovered).getTitle(), cx, infoY, sc * 1.2F, DesignTokens.HEADER_TEXT_COLOR);
            UI.drawScaledCentered(g, this.font, Component.translatable("gui.my-bot-mod.home.desc." + CATEGORY_KEYS[hovered]),
                cx, infoY + 12, sc, DesignTokens.HOME_CARD_DESC_COLOR);
        } else {
            UI.drawScaledCentered(g, this.font, Component.translatable("gui.my-bot-mod.home.subtitle"), cx, infoY, sc, DesignTokens.HOME_CARD_DESC_COLOR);
        }
    }

    /** 仿 MC 物品栏凹槽 */
    private void drawSlot(GuiGraphics g, int x, int y, int s) {
        g.fill(x, y, x + s, y + s, 0xFF8B8B8B);
        g.fill(x, y, x + s, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + s, 0xFF373737);
        g.fill(x, y + s - 1, x + s, y + s, 0xFFFFFFFF);
        g.fill(x + s - 1, y, x + s, y + s, 0xFFFFFFFF);
    }

    // ========== 标签操作 ==========

    private void openTab(int cat) {
        if (cat < 0 || cat >= pages.size()) return;
        if (!openTabs.contains(cat)) {
            openTabs.add(cat);
        }
        activeCategory = cat;
        this.rebuildWidgets();
    }

    private void switchTab(int cat) {
        activeCategory = cat;
        this.rebuildWidgets();
    }

    private void showHome() {
        activeCategory = -1;
        this.rebuildWidgets();
    }

    private void closeTab(int cat) {
        openTabs.remove(Integer.valueOf(cat));
        if (activeCategory == cat) {
            activeCategory = openTabs.isEmpty() ? -1 : openTabs.get(openTabs.size() - 1);
        }
        this.rebuildWidgets();
    }

    private void saveAndClose() {
        config.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void requestBotListIfNeeded() {
        if (currentPage instanceof BotsPage && this.minecraft != null && this.minecraft.getConnection() != null) {
            ClientPlayNetworking.send(BotNetworking.REQUEST_BOT_LIST, BotNetworking.c2s());
        }
    }

    /** 假人列表 S2C 到达后刷新"假人"标签页 */
    public void refreshCurrentPage() {
        if (currentPage instanceof BotsPage) {
            int pad = DesignTokens.SCROLL_AREA_PADDING;
            currentPage.init(panelX + pad, contentY, panelWidth - pad * 2, contentHeight, this.minecraft, this.font);
        }
    }

    // ========== 渲染 ==========

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if >=1.20.2 {
        /*this.renderBackground(graphics, mouseX, mouseY, partialTick);
        *///?} else {
        this.renderBackground(graphics);
        //?}
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, DesignTokens.PANEL_BG);
        UI.border(graphics, panelX, panelY, panelWidth, panelHeight, DesignTokens.PANEL_BORDER);

        renderTabBar(graphics, mouseX, mouseY);

        // 搜索框装饰（优雅样式：深底 + 聚焦时青蓝描边）
        if (activeCategory >= 0 && searchBox != null) {
            boolean focused = searchBox.isFocused();
            graphics.fill(searchX, searchY, searchX + SEARCH_W, searchY + DesignTokens.TAB_BAR_HEIGHT, DesignTokens.SEARCH_BG);
            UI.border(graphics, searchX, searchY, SEARCH_W, DesignTokens.TAB_BAR_HEIGHT,
                focused ? DesignTokens.SEARCH_BORDER_FOCUS : DesignTokens.SEARCH_BORDER);
        }

        // 页脚分隔线
        int pad = DesignTokens.SCROLL_AREA_PADDING;
        int footerDividerY = footerY - DesignTokens.FOOTER_TOP_GAP / 2;
        graphics.fill(panelX + pad, footerDividerY, panelX + panelWidth - pad, footerDividerY + 1, DesignTokens.HEADER_DIVIDER_COLOR);

        if (activeCategory < 0) {
            renderHome(graphics, mouseX, mouseY);
        } else if (currentPage != null) {
            currentPage.render(graphics, mouseX, mouseY, partialTick);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // ========== 鼠标 ==========

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // 标签栏：主页按钮
        if (inRect(homeBtnRect, mouseX, mouseY)) {
            showHome();
            return true;
        }
        // 标签栏：各标签（先判关闭 ✕，再判切换）
        for (int[] r : tabRects) {
            if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
                if (mouseX >= r[0] + r[2] - 10) {
                    closeTab(r[4]);
                } else {
                    switchTab(r[4]);
                }
                return true;
            }
        }

        if (activeCategory < 0) {
            // 主页：命中分类槽位
            for (int i = 0; i < homeRects.size(); i++) {
                if (inRect(homeRects.get(i), mouseX, mouseY)) {
                    openTab(i);
                    return true;
                }
            }
            return false;
        }
        return currentPage != null && currentPage.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeCategory >= 0 && currentPage != null && currentPage.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (activeCategory >= 0 && currentPage != null && currentPage.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    //? if >=1.20.2 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        if (activeCategory >= 0 && currentPage != null && currentPage.mouseScrolled(mouseX, mouseY, 0, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (activeCategory >= 0 && currentPage != null && currentPage.mouseScrolled(mouseX, mouseY, 0, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    //?}

    @Override
    public void onClose() {
        config.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private static boolean inRect(int[] r, double mx, double my) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }
}
