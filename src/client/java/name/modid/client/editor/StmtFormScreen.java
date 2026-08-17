package name.modid.client.editor;

import name.modid.client.editor.BehaviorModels.ExprModel;
import name.modid.client.editor.BehaviorModels.StmtModel;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.UI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 单个积木的参数表单（按 BlockDef 动态生成）。
 *
 * 编辑在语句副本上进行，点击"确定"后通过回调写回；嵌套体（body/then/else）
 * 不在此编辑，由主界面的"进入"导航处理。
 */
public class StmtFormScreen extends Screen {

    private static final int ROW = ExprField.ROW;
    private static final int GAP = ExprField.GAP;

    private final Screen parent;
    private final BlockDef.Def def;
    private final StmtModel stmt;
    private final Consumer<StmtModel> onConfirm;

    private int panelX, panelY, panelWidth, panelHeight;
    private int contentTop, contentHeight;
    private int scroll;
    private int contentTotal;
    private final List<int[]> labels = new ArrayList<>(); // [x, y, color] + 文本平行数组
    private final List<String> labelTexts = new ArrayList<>();

    public StmtFormScreen(Screen parent, BlockDef.Def def, StmtModel stmtCopy, Consumer<StmtModel> onConfirm) {
        super(Component.literal(def.zh()));
        this.parent = parent;
        this.def = def;
        this.stmt = stmtCopy;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(320, this.width - 30);
        panelHeight = Math.min(220, this.height - 30);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        contentTop = panelY + 22;
        contentHeight = panelHeight - 22 - 22;
        rebuildForm();
    }

    /** 重建全部表单控件（结构变更/滚动时调用） */
    private void rebuildForm() {
        super.clearWidgets();
        labels.clear();
        labelTexts.clear();

        int pad = 8;
        int x = panelX + pad;
        int width = panelWidth - pad * 2;
        int y = contentTop + GAP - scroll;

        ExprField.Host host = new ExprField.Host() {
            @Override
            public void addWidget(AbstractWidget widget) {
                StmtFormScreen.this.addRenderableWidget(widget);
            }

            @Override
            public void addLabel(String text, int lx, int ly, int color) {
                labels.add(new int[]{lx, ly, color});
                labelTexts.add(text);
            }

            @Override
            public Font font() {
                return StmtFormScreen.this.font;
            }

            @Override
            public void rebuild() {
                StmtFormScreen.this.rebuildForm();
            }
        };

        for (BlockDef.Param param : def.params()) {
            y += buildParam(param, x, y, width, host) + GAP;
        }
        contentTotal = Math.max(0, y + scroll - contentTop);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentTotal - contentHeight)));

        // 底部固定按钮
        int btnY = panelY + panelHeight - 18;
        this.addRenderableWidget(new ModernButton(panelX + pad, btnY, 60, 14,
            Component.literal("确定"), b -> {
                onConfirm.accept(stmt);
                this.minecraft.setScreen(parent);
            }));
        this.addRenderableWidget(new ModernButton(panelX + pad + 64, btnY, 60, 14,
            Component.literal("取消"), b -> this.minecraft.setScreen(parent)));
    }

    /** 构建单个参数行 @return 占用高度 */
    private int buildParam(BlockDef.Param param, int x, int y, int width, ExprField.Host host) {
        switch (param.type()) {
            case TEXT, NUM, LISTTEXT -> {
                host.addLabel(param.zh(), x, y + 2, 0xFFE2E6EC);
                EditBox box = new EditBox(font, x + 78, y - 1, width - 78, ROW + 1,
                    Component.literal(param.name()));
                box.setValue(argText(param));
                if (param.type() == BlockDef.PType.NUM) {
                    box.setResponder(text -> stmt.args.put(param.name(),
                        ExprModel.num(parseNum(text))));
                } else if (param.type() == BlockDef.PType.LISTTEXT) {
                    box.setResponder(text -> stmt.args.put(param.name(), toListExpr(text)));
                } else {
                    box.setResponder(text -> stmt.args.put(param.name(), ExprModel.str(text)));
                }
                host.addWidget(box);
                return ROW;
            }
            case BOOL -> {
                host.addLabel(param.zh(), x, y + 2, 0xFFE2E6EC);
                boolean value = argBool(param);
                ModernButton toggle = new ModernButton(x + 78, y, Math.min(60, width - 78), ROW - 1,
                    Component.literal(value ? "开" : "关"), b -> {
                    boolean next = !argBool(param);
                    stmt.args.put(param.name(), ExprModel.bool(next));
                    rebuildForm();
                });
                host.addWidget(toggle);
                return ROW;
            }
            case DROPDOWN -> {
                host.addLabel(param.zh(), x, y + 2, 0xFFE2E6EC);
                String value = argText(param);
                if (value.isEmpty() && param.options().length > 0) {
                    value = param.options()[0];
                    stmt.args.put(param.name(), ExprModel.str(value));
                }
                final String shown = value;
                ModernButton cycle = new ModernButton(x + 78, y, Math.min(90, width - 78), ROW - 1,
                    Component.literal(shown), b -> {
                    String cur = argText(param);
                    String[] opts = param.options();
                    int i = 0;
                    for (int k = 0; k < opts.length; k++) {
                        if (opts[k].equals(cur)) {
                            i = k;
                            break;
                        }
                    }
                    stmt.args.put(param.name(), ExprModel.str(opts[(i + 1) % opts.length]));
                    rebuildForm();
                });
                host.addWidget(cycle);
                return ROW;
            }
            case EXPR -> {
                ExprModel expr = stmt.args.get(param.name());
                if (expr == null) {
                    expr = ExprModel.num(0);
                    stmt.args.put(param.name(), expr);
                }
                return ExprField.build(expr, param.zh(), x, y, width, host);
            }
            default -> {
                return ROW;
            }
        }
    }

    // ==================== 参数值读写 ====================

    private String argText(BlockDef.Param param) {
        ExprModel expr = stmt.args.get(param.name());
        if (expr == null) {
            return "";
        }
        return switch (expr.kind) {
            case STR -> expr.str;
            case NUM -> trimNum(expr.num);
            case LIST -> {
                StringBuilder sb = new StringBuilder();
                for (ExprModel item : expr.items) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(display(item));
                }
                yield sb.toString();
            }
            default -> "";
        };
    }

    /** 表达式的简易文本展示（LIST 参数回填用） */
    private static String display(ExprModel expr) {
        return switch (expr.kind) {
            case STR -> expr.str;
            case NUM -> trimNum(expr.num);
            case BOOL -> String.valueOf(expr.bool);
            case VAR -> expr.name;
            case SENSOR -> expr.name + "(..." + ")";
            default -> "…";
        };
    }

    private boolean argBool(BlockDef.Param param) {
        ExprModel expr = stmt.args.get(param.name());
        // 缺省语义：sneak/sprint 的 on 缺省为开；drop 的 stack 缺省为关（有值按值）
        if (expr == null) {
            return param.name().equals("on");
        }
        return expr.bool;
    }

    private static ExprModel toListExpr(String csv) {
        ExprModel list = new ExprModel();
        list.kind = ExprModel.Kind.LIST;
        for (String piece : csv.split(",", -1)) {
            list.items.add(ExprModel.str(piece.trim()));
        }
        return list;
    }

    private static double parseNum(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String trimNum(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    // ==================== 渲染与事件 ====================

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
            panelX + panelWidth / 2, panelY + 6, sc * 1.15F, DesignTokens.HEADER_TEXT_COLOR);

        // 参数标签（小号字体）
        for (int i = 0; i < labels.size(); i++) {
            int[] l = labels.get(i);
            UI.drawScaled(graphics, this.font, Component.literal(labelTexts.get(i)),
                l[0], l[1], sc, l[2]);
        }
        // 滚动提示
        if (contentTotal > contentHeight) {
            UI.drawScaled(graphics, this.font, Component.literal("↕ 滚轮滚动"),
                panelX + panelWidth - 46, panelY + 6, sc * 0.85F, 0xFF808890);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    //? if >=1.20.2 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        scrollBy(delta);
        return true;
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollBy(delta);
        return true;
    }
    //?}

    private void scrollBy(double delta) {
        int max = Math.max(0, contentTotal - contentHeight);
        scroll = Math.max(0, Math.min(max, scroll - (int) (delta * 12)));
        rebuildForm();
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
