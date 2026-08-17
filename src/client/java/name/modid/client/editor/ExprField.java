package name.modid.client.editor;

import name.modid.client.editor.BehaviorModels.ExprModel;
import name.modid.client.screen.widget.ModernButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 表达式编辑组件：把一个 ExprModel 的编辑控件铺进宿主表单。
 *
 * 支持 7 种表达式类型切换：数字/文本/布尔/变量/传感器/二元运算/一元运算；
 * 传感器参数与运算子表达式递归嵌套（缩进展示）。控件值实时写回模型，
 * 结构性变更（切换类型/传感器/运算符）触发宿主重建。
 */
public final class ExprField {

    /** 单行控件高度与行距 */
    public static final int ROW = 13;
    public static final int GAP = 2;

    private ExprField() {
    }

    /** 宿主表单：注册控件/标签，触发重建 */
    public interface Host {
        void addWidget(AbstractWidget widget);

        void addLabel(String text, int x, int y, int color);

        Font font();

        /** 结构变更时重建整个表单 */
        void rebuild();
    }

    private static final ExprModel.Kind[] KINDS = {
        ExprModel.Kind.NUM, ExprModel.Kind.STR, ExprModel.Kind.BOOL,
        ExprModel.Kind.VAR, ExprModel.Kind.SENSOR, ExprModel.Kind.BIN, ExprModel.Kind.UN
    };

    private static String kindLabel(ExprModel.Kind kind) {
        return switch (kind) {
            case NUM -> "数字";
            case STR -> "文本";
            case BOOL -> "布尔";
            case VAR -> "变量";
            case SENSOR -> "传感器";
            case BIN -> "二元运算";
            case UN -> "一元运算";
            default -> "?";
        };
    }

    /**
     * 在 (x, y) 处铺设 expr 的编辑控件
     *
     * @param label 参数标签（显示在首行左侧）
     * @return 占用的高度（像素）
     */
    public static int build(ExprModel expr, String label, int x, int y, int width, Host host) {
        int used = 0;
        // 首行：标签 + 类型切换按钮
        host.addLabel(label, x, y + 2, 0xFFE2E6EC);
        int btnX = x + 78;
        int kindIdx = indexOf(KINDS, expr.kind);
        ModernButton kindBtn = new ModernButton(btnX, y, Math.min(52, width - 78), ROW - 1,
            Component.literal(kindLabel(expr.kind)), b -> {
            expr.kind = KINDS[(indexOf(KINDS, expr.kind) + 1) % KINDS.length];
            host.rebuild();
        });
        host.addWidget(kindBtn);
        int cx = btnX + kindBtn.getWidth() + 3;
        int remain = x + width - cx;

        switch (expr.kind) {
            case NUM -> {
                EditBox box = new EditBox(host.font(), cx, y - 1, Math.max(40, remain), ROW + 1, Component.literal("num"));
                box.setValue(trimNum(expr.num));
                box.setResponder(text -> expr.num = parseNum(text));
                host.addWidget(box);
                used += ROW;
            }
            case STR -> {
                EditBox box = new EditBox(host.font(), cx, y - 1, Math.max(40, remain), ROW + 1, Component.literal("str"));
                box.setValue(expr.str);
                box.setResponder(text -> expr.str = text);
                host.addWidget(box);
                used += ROW;
            }
            case BOOL -> {
                ModernButton toggle = new ModernButton(cx, y, Math.min(46, remain), ROW - 1,
                    Component.literal(expr.bool ? "true" : "false"),
                    b -> {
                        expr.bool = !expr.bool;
                        host.rebuild();
                    });
                host.addWidget(toggle);
                used += ROW;
            }
            case VAR -> {
                EditBox box = new EditBox(host.font(), cx, y - 1, Math.max(40, remain), ROW + 1, Component.literal("var"));
                box.setValue(expr.name);
                box.setResponder(text -> expr.name = text);
                host.addWidget(box);
                used += ROW;
            }
            case SENSOR -> used += buildSensor(expr, cx, y, x + width, host);
            case BIN -> used += buildBin(expr, cx, y, x, width, host);
            case UN -> used += buildUn(expr, cx, y, x, width, host);
            default -> used += ROW;
        }
        return Math.max(used, ROW);
    }

    /** 传感器：选择按钮 + 递归参数 */
    private static int buildSensor(ExprModel expr, int cx, int y, int rightX, Host host) {
        List<BlockDef.SensorDef> sensors = BlockDef.SENSORS;
        int idx = sensorIndex(expr.name);
        String shown = idx >= 0 ? sensors.get(idx).zh() + "(" + expr.name + ")" : "(选择传感器)";
        ModernButton pick = new ModernButton(cx, y, Math.min(120, rightX - cx), ROW - 1,
            Component.literal(shown), b -> {
            int cur = sensorIndex(expr.name);
            BlockDef.SensorDef next = sensors.get((cur + 1) % sensors.size());
            expr.name = next.name();
            syncSensorArgs(expr, next);
            host.rebuild();
        });
        host.addWidget(pick);
        int used = ROW;
        if (idx >= 0) {
            BlockDef.SensorDef def = sensors.get(idx);
            syncSensorArgs(expr, def);
            int subY = y + ROW + GAP;
            for (int i = 0; i < expr.items.size(); i++) {
                String pLabel = i < def.paramZh().length ? def.paramZh()[i] : ("参数" + (i + 1));
                subY += build(expr.items.get(i), pLabel, cx + 8, subY, rightX - cx - 8, host) + GAP;
            }
            used = subY - y;
        }
        return used;
    }

    /** 二元运算：运算符选择 + 左/右子表达式 */
    private static int buildBin(ExprModel expr, int cx, int y, int baseX, int width, Host host) {
        if (expr.opSym.isEmpty()) {
            expr.opSym = "+";
        }
        ModernButton opBtn = new ModernButton(cx, y, Math.min(56, baseX + width - cx), ROW - 1,
            Component.literal(expr.opSym), b -> {
            int i = BlockDef.BIN_OPS.indexOf(expr.opSym);
            expr.opSym = BlockDef.BIN_OPS.get((i + 1) % BlockDef.BIN_OPS.size());
            host.rebuild();
        });
        host.addWidget(opBtn);
        if (expr.left == null) {
            expr.left = ExprModel.num(0);
        }
        if (expr.right == null) {
            expr.right = ExprModel.num(0);
        }
        int subY = y + ROW + GAP;
        subY += build(expr.left, "左", baseX + 8, subY, width - 8, host) + GAP;
        subY += build(expr.right, "右", baseX + 8, subY, width - 8, host) + GAP;
        return subY - y;
    }

    /** 一元运算：运算符选择 + 子表达式 */
    private static int buildUn(ExprModel expr, int cx, int y, int baseX, int width, Host host) {
        if (expr.opSym.isEmpty()) {
            expr.opSym = "not";
        }
        ModernButton opBtn = new ModernButton(cx, y, Math.min(56, baseX + width - cx), ROW - 1,
            Component.literal(expr.opSym), b -> {
            int i = BlockDef.UN_OPS.indexOf(expr.opSym);
            expr.opSym = BlockDef.UN_OPS.get((i + 1) % BlockDef.UN_OPS.size());
            host.rebuild();
        });
        host.addWidget(opBtn);
        if (expr.operand == null) {
            expr.operand = ExprModel.num(0);
        }
        int subY = y + ROW + GAP;
        subY += build(expr.operand, "值", baseX + 8, subY, width - 8, host) + GAP;
        return subY - y;
    }

    /** 传感器实参数量与定义对齐（保留已有值） */
    private static void syncSensorArgs(ExprModel expr, BlockDef.SensorDef def) {
        int need = def.paramZh().length;
        while (expr.items.size() < need) {
            expr.items.add(ExprModel.num(0));
        }
        while (expr.items.size() > need) {
            expr.items.remove(expr.items.size() - 1);
        }
    }

    private static int sensorIndex(String name) {
        for (int i = 0; i < BlockDef.SENSORS.size(); i++) {
            if (BlockDef.SENSORS.get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOf(ExprModel.Kind[] arr, ExprModel.Kind kind) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == kind) {
                return i;
            }
        }
        return 0;
    }

    private static String trimNum(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    private static double parseNum(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
