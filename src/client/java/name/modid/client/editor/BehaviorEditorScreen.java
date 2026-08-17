package name.modid.client.editor;

import name.modid.client.editor.BehaviorModels.ProgramModel;
import name.modid.client.editor.BehaviorModels.StmtModel;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.UI;
import name.modid.net.BotNetworking;
import name.modid.client.BotClientNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * 游戏内行为编辑器主界面（列表式积木编辑）。
 *
 * 布局：顶部元信息（名称/描述/循环）与保存/导出/打开；左侧分类面板；
 * 中部积木调色板；右侧当前层语句列表（编辑/上下移/删除/进入嵌套体）。
 * 产物与服务端 BehaviorParser 及 HTML 编辑器完全兼容（format=1 JSON）。
 */
public class BehaviorEditorScreen extends Screen {

    /** 导航层：当前正在编辑的语句列表 + 面包屑标题 */
    private record NavLevel(List<StmtModel> list, String title) {
    }

    private final Screen parent;
    private ProgramModel model = new ProgramModel();
    private final Deque<NavLevel> navStack = new ArrayDeque<>();
    /** 正在编辑的文件名（空=新建） */
    private String openFile = "";

    private int categoryIndex;
    private int panelX, panelY, panelWidth, panelHeight;
    private int listX, listY, listWidth, listHeight;
    private int paletteX, paletteY, paletteWidth, paletteHeight;
    private int listScroll;
    private int paletteScroll;
    private int listTotal;
    private int paletteTotal;
    private String status = "";
    private int statusTicks;

    private EditBox nameBox;
    private EditBox descBox;
    private EditBox dirBox;
    private EditBox openBox;
    private ModernButton loopBtn;
    /** 首次构建标记：init() 会因窗口缩放/分类切换多次执行，仅首次重置导航与请求源文件 */
    private boolean firstInit = true;
    /** 打开文件的源请求是否已发出（避免重建时重复请求覆盖已编辑内容） */
    private boolean sourceRequested;

    /** @param openFile 打开已有行为（null=新建） */
    public BehaviorEditorScreen(Screen parent, String openFile) {
        super(Component.literal("行为编辑器"));
        this.parent = parent;
        if (openFile != null) {
            this.openFile = openFile;
        }
    }

    @Override
    protected void init() {
        super.init();
        layoutAll(firstInit);
        firstInit = false;
    }

    /**
     * 布局全部控件。
     * @param firstTime 首次进入：重置导航栈并按需请求打开文件；
     *                  非首次（窗口缩放/切换分类）：保留导航位置与已编辑内容，不重发请求
     */
    private void layoutAll(boolean firstTime) {
        panelWidth = this.width - DesignTokens.PANEL_H_MARGIN * 2;
        panelHeight = this.height - DesignTokens.PANEL_TOP_MARGIN - DesignTokens.PANEL_BOTTOM_MARGIN;
        panelX = DesignTokens.PANEL_H_MARGIN;
        panelY = DesignTokens.PANEL_TOP_MARGIN;

        if (firstTime) {
            navStack.clear();
            navStack.push(new NavLevel(model.program, "程序"));
        }

        // 区域划分：左分类(56) + 调色板(104) + 语句列表(剩余)
        int topBarY = panelY + 18;
        int row2Y = topBarY + 18;
        int bodyY = row2Y + 20;
        int bodyHeight = panelY + panelHeight - bodyY - 4;
        paletteX = panelX + 4 + 60;
        paletteY = bodyY;
        paletteWidth = 104;
        paletteHeight = bodyHeight;
        listX = paletteX + paletteWidth + 4;
        listY = bodyY;
        listWidth = panelX + panelWidth - 4 - listX;
        listHeight = bodyHeight;

        // rebuildList 会全量重建顶栏/分类/调色板/语句列表
        rebuildList();

        if (firstTime && !openFile.isEmpty() && !sourceRequested) {
            sourceRequested = true;
            requestSource(openFile);
        }
    }

    // ==================== 构建：分类/调色板/语句列表 ====================

    private void rebuildCategories(int catX, int bodyY) {
        for (int i = 0; i < BlockDef.CATEGORIES.length; i++) {
            final int idx = i;
            String label = BlockDef.CATEGORIES[i][1];
            ModernButton btn = new ModernButton(catX, bodyY + i * 15, 56, 13,
                Component.literal((i == categoryIndex ? "▶ " : "") + label), b -> {
                categoryIndex = idx;
                paletteScroll = 0;
                layoutAll(false);
            });
            this.addRenderableWidget(btn);
        }
    }

    private void rebuildPalette() {
        List<BlockDef.Def> defs = BlockDef.byCategory(BlockDef.CATEGORIES[categoryIndex][0]);
        paletteTotal = defs.size() * 15 + 4;
        int y = paletteY + 2 - paletteScroll;
        for (BlockDef.Def def : defs) {
            // 仅当按钮完整落在调色板区域内才渲染，避免超出背景框底边
            if (y >= paletteY && y + 13 <= paletteY + paletteHeight) {
                ModernButton btn = new ModernButton(paletteX, y, paletteWidth, 13,
                    Component.literal(def.zh()), b -> addBlock(def));
                this.addRenderableWidget(btn);
            }
            y += 15;
        }
    }

    private void rebuildList() {
        // 移除旧的列表区控件（简化处理：整体重建界面会丢失输入框内容，故仅重建按钮区不可行——
        // 采用全量重建并回填文本框值）
        String nameText = nameBox == null ? model.name : nameBox.getValue();
        String descText = descBox == null ? model.description : descBox.getValue();
        String dirText = dirBox == null ? "" : dirBox.getValue();
        String openText = openBox == null ? openFile : openBox.getValue();
        boolean loopState = model.loop;
        super.clearWidgets();
        model.name = nameText;
        model.description = descText;
        model.loop = loopState;

        int topBarY = panelY + 18;
        // 描述框宽度自适应：窄窗口下收缩，避免顶栏按钮超出面板右边框
        int descW = Math.max(40, Math.min(110, panelWidth - 244));
        int cx = panelX + 6;
        nameBox = new EditBox(this.font, cx, topBarY, 80, 14, Component.literal("name"));
        nameBox.setValue(nameText);
        nameBox.setResponder(v -> model.name = v);
        this.addRenderableWidget(nameBox);
        cx += 84;
        descBox = new EditBox(this.font, cx, topBarY, descW, 14, Component.literal("desc"));
        descBox.setValue(descText);
        descBox.setResponder(v -> model.description = v);
        this.addRenderableWidget(descBox);
        cx += descW + 4;
        loopBtn = new ModernButton(cx, topBarY + 1, 56, 12,
            Component.literal(model.loop ? "循环: 开" : "循环: 关"), b -> {
            model.loop = !model.loop;
            loopBtn.setMessage(Component.literal(model.loop ? "循环: 开" : "循环: 关"));
        });
        this.addRenderableWidget(loopBtn);
        cx += 60;
        this.addRenderableWidget(new ModernButton(cx, topBarY + 1, 40, 12,
            Component.literal("保存"), b -> save()));
        cx += 44;
        this.addRenderableWidget(new ModernButton(cx, topBarY + 1, 40, 12,
            Component.literal("关闭"), b -> onClose()));

        int row2Y = topBarY + 18;
        dirBox = new EditBox(this.font, panelX + 6, row2Y, 150, 14, Component.literal("dir"));
        dirBox.setValue(dirText);
        dirBox.setSuggestion("导出目录(空=behaviors/)");
        this.addRenderableWidget(dirBox);
        openBox = new EditBox(this.font, panelX + 162, row2Y, 90, 14, Component.literal("open"));
        openBox.setValue(openText);
        openBox.setSuggestion("输入行为文件名");
        this.addRenderableWidget(openBox);
        this.addRenderableWidget(new ModernButton(panelX + 256, row2Y + 1, 40, 12,
            Component.literal("打开"), b -> requestSource(openBox.getValue())));

        int bodyY = row2Y + 20;
        rebuildCategories(panelX + 4, bodyY);
        rebuildPalette();

        // 语句列表（含面包屑返回行）
        NavLevel current = navStack.peek();
        List<StmtModel> list = current.list();
        int rowHeight = 15;
        int rows = list.size() + (navStack.size() > 1 ? 1 : 0) + 1;
        listTotal = rows * rowHeight;
        int y = listY + 2 - listScroll;
        if (navStack.size() > 1) {
            if (y >= listY && y + 13 <= listY + listHeight) {
                this.addRenderableWidget(new ModernButton(listX, y, 60, 13,
                    Component.literal("◀ 返回上层"), b -> {
                    navStack.pop();
                    listScroll = 0;
                    rebuildList();
                }));
            }
            y += rowHeight;
        }
        if (y >= listY && y + 13 <= listY + listHeight) {
            this.addRenderableWidget(new ModernButton(listX, y, listWidth, 13,
                Component.literal("+ 从左侧选择积木添加"), b -> {
            }));
        }
        y += rowHeight;
        for (int i = 0; i < list.size(); i++) {
            if (y >= listY && y + 13 <= listY + listHeight) {
                y += buildStmtRow(list, i, y, rowHeight);
            } else {
                y += rowHeight;
            }
        }
    }

    /** 构建单个语句行 @return 占用高度 */
    private int buildStmtRow(List<StmtModel> list, int index, int y, int rowHeight) {
        StmtModel stmt = list.get(index);
        BlockDef.Def def = BlockDef.BY_OP.get(stmt.op);
        String summary = summarize(stmt, def);
        int btnW = 16;
        int bodyCount = def == null ? 0 : def.bodies().length;
        // 右侧按钮实际占宽：文本按钮后 2 间隔 + 4 个常规按钮(宽16+1间隔) + 每个嵌套体按钮(宽22+1间隔)；
        // 按真实宽度预留，避免嵌套语句行超出列表右边框
        int rightWidth = 2 + 4 * (btnW + 1) + bodyCount * (btnW + 7);
        int textW = Math.max(24, listWidth - rightWidth - 2);

        ModernButton textBtn = new ModernButton(listX, y, textW, 13,
            Component.literal(summary), b -> editStmt(list, index));
        this.addRenderableWidget(textBtn);
        int bx = listX + textBtn.getWidth() + 2;
        this.addRenderableWidget(new ModernButton(bx, y, btnW, 13, Component.literal("✎"), b -> editStmt(list, index)));
        bx += btnW + 1;
        this.addRenderableWidget(new ModernButton(bx, y, btnW, 13, Component.literal("▲"), b -> {
            if (index > 0) {
                list.add(index - 1, list.remove(index));
                rebuildList();
            }
        }));
        bx += btnW + 1;
        this.addRenderableWidget(new ModernButton(bx, y, btnW, 13, Component.literal("▼"), b -> {
            if (index < list.size() - 1) {
                list.add(index + 1, list.remove(index));
                rebuildList();
            }
        }));
        bx += btnW + 1;
        this.addRenderableWidget(new ModernButton(bx, y, btnW, 13, Component.literal("✕"), b -> {
            list.remove(index);
            rebuildList();
        }));
        bx += btnW + 1;
        if (def != null) {
            for (String bodyKey : def.bodies()) {
                String btnLabel = bodyKey.equals("body") ? "→" : ("→" + bodyKey);
                this.addRenderableWidget(new ModernButton(bx, y, btnW + 6, 13,
                    Component.literal(btnLabel), b -> enterBody(stmt, def, bodyKey)));
                bx += btnW + 7;
            }
        }
        return rowHeight;
    }

    // ==================== 操作 ====================

    private void addBlock(BlockDef.Def def) {
        StmtModel stmt = new StmtModel(def.op());
        if (def.params().length == 0 && def.bodies().length == 0) {
            navStack.peek().list().add(stmt);
            rebuildList();
            return;
        }
        // 预建嵌套体（便于直接进入编辑）
        for (String bodyKey : def.bodies()) {
            stmt.blocks.put(bodyKey, new ArrayList<>());
        }
        StmtModel copy = BehaviorModels.copy(stmt);
        this.minecraft.setScreen(new StmtFormScreen(this, def, copy, confirmed -> {
            navStack.peek().list().add(confirmed);
            rebuildList();
        }));
    }

    private void editStmt(List<StmtModel> list, int index) {
        StmtModel stmt = list.get(index);
        BlockDef.Def def = BlockDef.BY_OP.get(stmt.op);
        if (def == null) {
            status("未知积木: " + stmt.op);
            return;
        }
        if (def.params().length == 0) {
            return;
        }
        StmtModel copy = BehaviorModels.copy(stmt);
        this.minecraft.setScreen(new StmtFormScreen(this, def, copy, confirmed -> {
            // 保留原嵌套体内容（表单不编辑嵌套体）
            confirmed.blocks.clear();
            confirmed.blocks.putAll(stmt.blocks);
            list.set(index, confirmed);
            rebuildList();
        }));
    }

    private void enterBody(StmtModel stmt, BlockDef.Def def, String bodyKey) {
        List<StmtModel> body = stmt.blocks.computeIfAbsent(bodyKey, k -> new ArrayList<>());
        String label = def.bodies().length == 1 ? def.zh() : (def.zh() + ":" + bodyKey);
        navStack.push(new NavLevel(body, label));
        listScroll = 0;
        rebuildList();
    }

    private void save() {
        model.name = nameBox.getValue();
        model.description = descBox.getValue();
        if (model.program.isEmpty()) {
            status("无法保存：程序为空");
            return;
        }
        String json = BehaviorModels.toJson(model);
        if (json.length() > name.modid.behavior.BehaviorStorage.MAX_JSON_LENGTH) {
            status("无法保存：内容过大");
            return;
        }
        String file = openFile.isEmpty()
            ? (model.name.isEmpty() ? "behavior" : model.name)
            : openFile;
        FriendlyByteBuf buf = BotNetworking.c2s();
        buf.writeUtf(file, 128);
        buf.writeUtf(dirBox.getValue(), 512);
        buf.writeUtf(json, name.modid.behavior.BehaviorStorage.MAX_JSON_LENGTH);
        BotClientNetworking.sendBehaviorSave(buf);
        status("已发送保存请求（结果见聊天栏）");
    }

    private void requestSource(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            status("请输入要打开的行为文件名");
            return;
        }
        FriendlyByteBuf buf = BotNetworking.c2s();
        buf.writeUtf(fileName.trim(), 128);
        BotClientNetworking.sendBehaviorSourceRequest(buf);
        status("正在请求 " + fileName.trim() + " …");
    }

    /** 收到服务端行为原文（MyBotModClient 路由） */
    public void loadSource(String fileName, String content) {
        if (content == null || content.isEmpty()) {
            status("文件不存在: " + fileName);
            return;
        }
        try {
            ProgramModel loaded = BehaviorModels.fromJson(content);
            model = loaded;
            openFile = fileName;
            navStack.clear();
            navStack.push(new NavLevel(model.program, "程序"));
            listScroll = 0;
            rebuildList();
            if (nameBox != null) {
                nameBox.setValue(model.name);
                descBox.setValue(model.description);
                loopBtn.setMessage(Component.literal(model.loop ? "循环: 开" : "循环: 关"));
            }
            status("已加载: " + fileName);
        } catch (IllegalArgumentException e) {
            status("解析失败: " + e.getMessage());
        }
    }

    private void status(String text) {
        status = text;
        statusTicks = 120;
    }

    /** 语句摘要：中文名 + 关键参数 */
    private String summarize(StmtModel stmt, BlockDef.Def def) {
        String base = def == null ? stmt.op : def.zh();
        StringBuilder sb = new StringBuilder(base);
        for (Map.Entry<String, BehaviorModels.ExprModel> e : stmt.args.entrySet()) {
            String v = displayExpr(e.getValue());
            if (!v.isEmpty()) {
                sb.append(' ').append(v);
            }
            if (sb.length() > 30) {
                break;
            }
        }
        for (String bodyKey : stmt.blocks.keySet()) {
            sb.append(" [").append(bodyKey).append(':').append(stmt.blocks.get(bodyKey).size()).append(']');
        }
        return sb.length() > 44 ? sb.substring(0, 44) + "…" : sb.toString();
    }

    private static String displayExpr(BehaviorModels.ExprModel expr) {
        if (expr == null) {
            return "";
        }
        return switch (expr.kind) {
            case NUM -> trimNum(expr.num);
            case STR -> expr.str;
            case BOOL -> String.valueOf(expr.bool);
            case VAR -> expr.name;
            case SENSOR -> expr.name + "(…)";
            case BIN -> "(…) " + expr.opSym + " (…)";
            case UN -> expr.opSym + "(…)";
            case LIST -> "[" + expr.items.size() + "项]";
            default -> "";
        };
    }

    private static String trimNum(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    // ==================== 渲染与事件 ====================

    @Override
    public void tick() {
        super.tick();
        if (statusTicks > 0) {
            statusTicks--;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if >=1.20.2 {
        /*this.renderBackground(graphics, mouseX, mouseY, partialTick);
        *///?} else {
        this.renderBackground(graphics);
        //?}
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, DesignTokens.PANEL_BG);
        UI.border(graphics, panelX, panelY, panelWidth, panelHeight, DesignTokens.PANEL_BORDER);

        float sc = DesignTokens.TEXT_SCALE;
        UI.drawScaledCentered(graphics, this.font, this.title,
            panelX + panelWidth / 2, panelY + 4, sc * 1.15F, DesignTokens.HEADER_TEXT_COLOR);

        // 面包屑（语句列表顶部）
        StringBuilder crumb = new StringBuilder();
        java.util.Iterator<NavLevel> it = navStack.descendingIterator();
        while (it.hasNext()) {
            if (crumb.length() > 0) {
                crumb.append(" ▸ ");
            }
            crumb.append(it.next().title());
        }
        UI.drawScaled(graphics, this.font, Component.literal(crumb.toString()),
            listX + 2, listY - 10, sc, DesignTokens.CARD_TITLE_COLOR);
        UI.drawScaled(graphics, this.font, Component.literal("积木调色板"),
            paletteX, paletteY - 10, sc, DesignTokens.CARD_TITLE_COLOR);

        // 状态行
        if (statusTicks > 0 && !status.isEmpty()) {
            UI.drawScaledCentered(graphics, this.font, Component.literal(status),
                panelX + panelWidth / 2, panelY + panelHeight - 10, sc, 0xFFFFD93D);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    //? if >=1.20.2 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        if (mouseX >= listX && mouseX <= listX + listWidth) {
            listScroll = clampScroll(listScroll - (int) (delta * 12), listTotal, listHeight);
            rebuildList();
            return true;
        }
        if (mouseX >= paletteX && mouseX <= paletteX + paletteWidth) {
            paletteScroll = clampScroll(paletteScroll - (int) (delta * 12), paletteTotal, paletteHeight);
            rebuildList();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listX && mouseX <= listX + listWidth) {
            listScroll = clampScroll(listScroll - (int) (delta * 12), listTotal, listHeight);
            rebuildList();
            return true;
        }
        if (mouseX >= paletteX && mouseX <= paletteX + paletteWidth) {
            paletteScroll = clampScroll(paletteScroll - (int) (delta * 12), paletteTotal, paletteHeight);
            rebuildList();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    //?}

    private static int clampScroll(int value, int total, int viewport) {
        return Math.max(0, Math.min(Math.max(0, total - viewport), value));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
