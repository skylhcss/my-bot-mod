package name.modid.client.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏内行为编辑器的纯数据模型与 JSON 序列化。
 *
 * 格式与服务端 BehaviorParser（format=1）完全一致，与 HTML 编辑器产物双向兼容：
 * - 语句：{"op": 操作名, 参数名: 表达式, body/then/else: 语句数组}
 * - 表达式：裸字面量 或 {"e": num|str|bool|var|bin|un|sensor|list, ...}
 *
 * 注意：列表字面量必须写 {"e":"list","items":[...]} 对象形式——
 * 裸 JSON 数组会被解析器当作嵌套语句块。
 */
public final class BehaviorModels {

    private BehaviorModels() {
    }

    // ==================== 模型 ====================

    /** 行为程序：名称/描述/循环 + 顶层语句列表 */
    public static final class ProgramModel {
        public String name = "";
        public String description = "";
        public boolean loop;
        public final List<StmtModel> program = new ArrayList<>();
    }

    /** 语句：op + 表达式参数 + 嵌套语句块（body/then/else） */
    public static final class StmtModel {
        public String op = "";
        public final Map<String, ExprModel> args = new LinkedHashMap<>();
        public final Map<String, List<StmtModel>> blocks = new LinkedHashMap<>();

        public StmtModel() {
        }

        public StmtModel(String op) {
            this.op = op;
        }
    }

    /** 表达式节点 */
    public static final class ExprModel {
        public enum Kind { NUM, STR, BOOL, VAR, BIN, UN, SENSOR, LIST }

        public Kind kind = Kind.NUM;
        public double num;
        public String str = "";
        public boolean bool;
        /** VAR/SENSOR 的名字 */
        public String name = "";
        /** BIN/UN 的运算符 */
        public String opSym = "";
        public ExprModel left;
        public ExprModel right;
        public ExprModel operand;
        /** LIST 字面量元素 / SENSOR 实参 */
        public final List<ExprModel> items = new ArrayList<>();

        public ExprModel() {
        }

        public static ExprModel num(double v) {
            ExprModel e = new ExprModel();
            e.kind = Kind.NUM;
            e.num = v;
            return e;
        }

        public static ExprModel str(String v) {
            ExprModel e = new ExprModel();
            e.kind = Kind.STR;
            e.str = v == null ? "" : v;
            return e;
        }

        public static ExprModel bool(boolean v) {
            ExprModel e = new ExprModel();
            e.kind = Kind.BOOL;
            e.bool = v;
            return e;
        }
    }

    // ==================== 序列化（模型 → JSON 原文） ====================

    public static String toJson(ProgramModel program) {
        JsonObject root = new JsonObject();
        root.addProperty("format", 1);
        root.addProperty("name", program.name);
        root.addProperty("description", program.description);
        root.addProperty("loop", program.loop);
        root.add("program", stmtsToJson(program.program));
        return root.toString();
    }

    private static JsonArray stmtsToJson(List<StmtModel> stmts) {
        JsonArray arr = new JsonArray();
        for (StmtModel s : stmts) {
            arr.add(stmtToJson(s));
        }
        return arr;
    }

    private static JsonObject stmtToJson(StmtModel s) {
        JsonObject obj = new JsonObject();
        obj.addProperty("op", s.op);
        for (Map.Entry<String, ExprModel> e : s.args.entrySet()) {
            if (e.getValue() != null) {
                obj.add(e.getKey(), exprToJson(e.getValue()));
            }
        }
        for (Map.Entry<String, List<StmtModel>> b : s.blocks.entrySet()) {
            obj.add(b.getKey(), stmtsToJson(b.getValue()));
        }
        return obj;
    }

    private static JsonElement exprToJson(ExprModel e) {
        switch (e.kind) {
            case NUM: {
                JsonObject o = new JsonObject();
                o.addProperty("e", "num");
                o.addProperty("v", e.num);
                return o;
            }
            case STR: {
                JsonObject o = new JsonObject();
                o.addProperty("e", "str");
                o.addProperty("v", e.str);
                return o;
            }
            case BOOL: {
                JsonObject o = new JsonObject();
                o.addProperty("e", "bool");
                o.addProperty("v", e.bool);
                return o;
            }
            case VAR: {
                JsonObject o = new JsonObject();
                o.addProperty("e", "var");
                o.addProperty("n", e.name);
                return o;
            }
            case BIN: {
                JsonObject o = new JsonObject();
                o.addProperty("e", "bin");
                o.addProperty("o", e.opSym);
                o.add("l", exprToJson(e.left == null ? ExprModel.num(0) : e.left));
                o.add("r", exprToJson(e.right == null ? ExprModel.num(0) : e.right));
                return o;
            }
            case UN: {
                JsonObject o = new JsonObject();
                o.addProperty("e", "un");
                o.addProperty("o", e.opSym);
                o.add("v", exprToJson(e.operand == null ? ExprModel.num(0) : e.operand));
                return o;
            }
            case SENSOR: {
                JsonObject o = new JsonObject();
                o.addProperty("e", "sensor");
                o.addProperty("n", e.name);
                JsonArray args = new JsonArray();
                for (ExprModel item : e.items) {
                    args.add(exprToJson(item));
                }
                o.add("args", args);
                return o;
            }
            case LIST: {
                JsonObject o = new JsonObject();
                o.addProperty("e", "list");
                JsonArray items = new JsonArray();
                for (ExprModel item : e.items) {
                    items.add(exprToJson(item));
                }
                o.add("items", items);
                return o;
            }
            default:
                return new JsonPrimitive(0);
        }
    }

    // ==================== 反序列化（JSON 原文 → 模型） ====================

    /** @throws IllegalArgumentException 内容不是合法的行为 JSON */
    public static ProgramModel fromJson(String json) {
        JsonObject root;
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) {
                throw new IllegalArgumentException("根节点必须是 JSON 对象");
            }
            root = el.getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 语法错误: " + e.getMessage());
        }
        ProgramModel program = new ProgramModel();
        program.name = strField(root, "name");
        program.description = strField(root, "description");
        program.loop = root.has("loop") && root.get("loop").isJsonPrimitive()
            && root.get("loop").getAsBoolean();
        if (!root.has("program") || !root.get("program").isJsonArray()) {
            throw new IllegalArgumentException("缺少 program 语句数组");
        }
        for (JsonElement el : root.getAsJsonArray("program")) {
            program.program.add(stmtFromJson(el));
        }
        return program;
    }

    private static StmtModel stmtFromJson(JsonElement el) {
        if (!el.isJsonObject()) {
            throw new IllegalArgumentException("语句必须是 JSON 对象");
        }
        JsonObject obj = el.getAsJsonObject();
        StmtModel stmt = new StmtModel(strField(obj, "op"));
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (key.equals("op")) {
                continue;
            }
            if (value.isJsonArray()) {
                List<StmtModel> block = new ArrayList<>();
                for (JsonElement item : value.getAsJsonArray()) {
                    block.add(stmtFromJson(item));
                }
                stmt.blocks.put(key, block);
            } else {
                stmt.args.put(key, exprFromJson(value));
            }
        }
        return stmt;
    }

    private static ExprModel exprFromJson(JsonElement el) {
        // 裸字面量（宽容处理，与解析器一致）
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isNumber()) {
                return ExprModel.num(p.getAsDouble());
            }
            if (p.isBoolean()) {
                return ExprModel.bool(p.getAsBoolean());
            }
            return ExprModel.str(p.getAsString());
        }
        if (!el.isJsonObject()) {
            throw new IllegalArgumentException("表达式不合法");
        }
        JsonObject obj = el.getAsJsonObject();
        String kind = strField(obj, "e");
        ExprModel expr = new ExprModel();
        switch (kind) {
            case "num" -> {
                expr.kind = ExprModel.Kind.NUM;
                expr.num = obj.has("v") ? obj.get("v").getAsDouble() : 0;
            }
            case "str" -> {
                expr.kind = ExprModel.Kind.STR;
                expr.str = strField(obj, "v");
            }
            case "bool" -> {
                expr.kind = ExprModel.Kind.BOOL;
                expr.bool = obj.has("v") && obj.get("v").getAsBoolean();
            }
            case "var" -> {
                expr.kind = ExprModel.Kind.VAR;
                expr.name = strField(obj, "n");
            }
            case "bin" -> {
                expr.kind = ExprModel.Kind.BIN;
                expr.opSym = strField(obj, "o");
                expr.left = obj.has("l") ? exprFromJson(obj.get("l")) : ExprModel.num(0);
                expr.right = obj.has("r") ? exprFromJson(obj.get("r")) : ExprModel.num(0);
            }
            case "un" -> {
                expr.kind = ExprModel.Kind.UN;
                expr.opSym = strField(obj, "o");
                expr.operand = obj.has("v") ? exprFromJson(obj.get("v")) : ExprModel.num(0);
            }
            case "sensor" -> {
                expr.kind = ExprModel.Kind.SENSOR;
                expr.name = strField(obj, "n");
                if (obj.has("args") && obj.get("args").isJsonArray()) {
                    for (JsonElement item : obj.getAsJsonArray("args")) {
                        expr.items.add(exprFromJson(item));
                    }
                }
            }
            case "list" -> {
                expr.kind = ExprModel.Kind.LIST;
                if (obj.has("items") && obj.get("items").isJsonArray()) {
                    for (JsonElement item : obj.getAsJsonArray("items")) {
                        expr.items.add(exprFromJson(item));
                    }
                }
            }
            default -> throw new IllegalArgumentException("未知表达式类型 \"" + kind + "\"");
        }
        return expr;
    }

    private static String strField(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : "";
    }

    // ==================== 深拷贝（表单编辑副本，确认后才写回） ====================

    public static StmtModel copy(StmtModel src) {
        StmtModel out = new StmtModel(src.op);
        for (Map.Entry<String, ExprModel> e : src.args.entrySet()) {
            out.args.put(e.getKey(), copy(e.getValue()));
        }
        for (Map.Entry<String, List<StmtModel>> b : src.blocks.entrySet()) {
            List<StmtModel> block = new ArrayList<>();
            for (StmtModel s : b.getValue()) {
                block.add(copy(s));
            }
            out.blocks.put(b.getKey(), block);
        }
        return out;
    }

    public static ExprModel copy(ExprModel src) {
        if (src == null) {
            return null;
        }
        ExprModel out = new ExprModel();
        out.kind = src.kind;
        out.num = src.num;
        out.str = src.str;
        out.bool = src.bool;
        out.name = src.name;
        out.opSym = src.opSym;
        out.left = copy(src.left);
        out.right = copy(src.right);
        out.operand = copy(src.operand);
        for (ExprModel item : src.items) {
            out.items.add(copy(item));
        }
        return out;
    }
}
