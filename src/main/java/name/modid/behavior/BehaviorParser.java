package name.modid.behavior;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import name.modid.behavior.BehaviorProgram.Expr;
import name.modid.behavior.BehaviorProgram.Stmt;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 行为文件解析器：JSON（format=1）→ {@link BehaviorProgram}
 *
 * 结构宽容：表达式参数既可为完整表达式对象（{"e":...}），也可为裸字面量；
 * 未知 op / 未知表达式类型 / 未知运算符会带行为名与位置抛出清晰错误。
 */
public final class BehaviorParser {

    /** 支持的语句操作名（含控制流、事件帽子、自定义函数与列表操作） */
    private static final Set<String> KNOWN_OPS = Set.of(
        "say", "wait", "move", "stopMove", "jump", "sneak", "sprint",
        "look", "lookAt", "turn",
        "attack", "use", "stopAttack", "stopUse",
        "slot", "swapHands", "drop", "equipItem", "dropOf", "lookAtEntity",
        "goto", "gotoStop", "mount", "dismount",
        "followEntity", "stopFollow",
        "openContainer", "closeContainer", "takeFromContainer", "putToContainer",
        "readContainer", "dumpContainer", "dumpInventory", "output",
        "outputRaw", "outputTemplate", "outputTable",
        "set", "change",
        "listAdd", "listInsert", "listRemove", "listSet", "listClear",
        "repeat", "while", "forever", "if", "waitUntil",
        "def", "call", "broadcast",
        "onStart", "onChat", "onBroadcast", "onHealthBelow", "onEntityNear",
        "stopThread", "stopSelf", "stopAll"
    );

    /** 支持的二元运算符 */
    private static final Set<String> BIN_OPS = Set.of(
        "+", "-", "*", "/", "%", "==", "!=", "<", ">", "<=", ">=", "and", "or", "concat",
        "min", "max", "pow"
    );

    /** 支持的一元运算符 */
    private static final Set<String> UN_OPS = Set.of(
        "not", "neg", "abs", "floor", "ceil", "round", "sqrt"
    );

    /** 支持的传感器名（含纯函数：字符串/列表/三角） */
    private static final Set<String> SENSORS = Set.of(
        "health", "food", "posX", "posY", "posZ", "dimension", "heldItem",
        "invCount", "nearbyEntities", "containerCount", "isPathfinding",
        "timeOfDay", "random",
        "containerSlots", "containerItem", "containerSlotCount",
        "blockAt", "isRaining", "isDay", "onGround", "inWater", "onFire", "sneaking",
        "holding", "hasItem", "fishHooked", "usingItem", "blockBelow", "facing",
        "xpLevel", "armor", "air", "maxHealth", "botName",
        "nearestPlayerName", "nearestPlayerDistance", "distanceTo",
        "nearestEntityCoord", "nearestEntityDistance", "nearestEntityExists",
        "strLen", "strContains", "strSub", "strUpper", "strLower", "strTrim",
        "strCharAt", "strIndexOf",
        "sin", "cos", "tan",
        "listGet", "listLen", "listContains", "listIndexOf", "listJoin",
        "listSplit", "listRandom"
    );

    private BehaviorParser() {
    }

    /**
     * 解析行为文件
     *
     * @param fileName 文件名（不含路径，用于错误消息与缺省行为名）
     */
    public static BehaviorProgram parse(String fileName, Reader reader) throws BehaviorParseException {
        JsonObject root;
        try {
            JsonElement el = JsonParser.parseReader(reader);
            if (!el.isJsonObject()) {
                throw new BehaviorParseException(fileName + ": 根节点必须是 JSON 对象");
            }
            root = el.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new BehaviorParseException(fileName + ": JSON 语法错误 - " + e.getMessage());
        }

        int format = root.has("format") ? root.get("format").getAsInt() : 1;
        if (format != 1) {
            throw new BehaviorParseException(fileName + ": 不支持的格式版本 " + format + "（当前支持 1）");
        }

        String name = root.has("name") && root.get("name").isJsonPrimitive()
            ? root.get("name").getAsString() : stripExt(fileName);
        String desc = root.has("description") && root.get("description").isJsonPrimitive()
            ? root.get("description").getAsString() : "";
        boolean loop = root.has("loop") && root.get("loop").isJsonPrimitive()
            && root.get("loop").getAsBoolean();

        if (!root.has("program") || !root.get("program").isJsonArray()) {
            throw new BehaviorParseException(fileName + ": 缺少 program 语句数组");
        }
        List<Stmt> body = parseStmtList(fileName, root.getAsJsonArray("program"), "program");
        return new BehaviorProgram(name, desc, loop, body, fileName);
    }

    private static String stripExt(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    // ==================== 语句 ====================

    private static List<Stmt> parseStmtList(String file, JsonArray arr, String path) throws BehaviorParseException {
        List<Stmt> list = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            JsonElement el = arr.get(i);
            if (!el.isJsonObject()) {
                throw new BehaviorParseException(file + ": " + path + "[" + i + "] 不是语句对象");
            }
            list.add(parseStmt(file, el.getAsJsonObject(), path + "[" + i + "]"));
        }
        return list;
    }

    private static Stmt parseStmt(String file, JsonObject obj, String path) throws BehaviorParseException {
        if (!obj.has("op") || !obj.get("op").isJsonPrimitive()) {
            throw new BehaviorParseException(file + ": " + path + " 缺少 op 字段");
        }
        String op = obj.get("op").getAsString();
        if (!KNOWN_OPS.contains(op)) {
            throw new BehaviorParseException(file + ": " + path + " 未知操作 \"" + op + "\"");
        }

        Map<String, Expr> args = new LinkedHashMap<>();
        Map<String, List<Stmt>> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (key.equals("op")) {
                continue;
            }
            if (value.isJsonArray()) {
                // 嵌套语句块：body/then/else
                blocks.put(key, parseStmtList(file, value.getAsJsonArray(), path + "." + key));
            } else {
                args.put(key, parseExpr(file, value, path + "." + key));
            }
        }
        return new Stmt(op, args, blocks);
    }

    // ==================== 表达式 ====================

    static Expr parseExpr(String file, JsonElement el, String path) throws BehaviorParseException {
        // 裸字面量宽容处理
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isNumber()) {
                return new BehaviorProgram.Num(p.getAsDouble());
            }
            if (p.isBoolean()) {
                return new BehaviorProgram.Bool(p.getAsBoolean());
            }
            return new BehaviorProgram.Str(p.getAsString());
        }
        if (!el.isJsonObject()) {
            throw new BehaviorParseException(file + ": " + path + " 不是合法表达式");
        }
        JsonObject obj = el.getAsJsonObject();
        if (!obj.has("e") || !obj.get("e").isJsonPrimitive()) {
            throw new BehaviorParseException(file + ": " + path + " 表达式缺少 e 类型字段");
        }
        String kind = obj.get("e").getAsString();
        switch (kind) {
            case "num":
                return new BehaviorProgram.Num(require(obj, "v", file, path).getAsDouble());
            case "str":
                return new BehaviorProgram.Str(require(obj, "v", file, path).getAsString());
            case "bool":
                return new BehaviorProgram.Bool(require(obj, "v", file, path).getAsBoolean());
            case "list": {
                List<Expr> items = new ArrayList<>();
                if (obj.has("items") && obj.get("items").isJsonArray()) {
                    JsonArray arr = obj.getAsJsonArray("items");
                    for (int i = 0; i < arr.size(); i++) {
                        items.add(parseExpr(file, arr.get(i), path + ".items[" + i + "]"));
                    }
                }
                return new BehaviorProgram.ListLit(items);
            }
            case "var":
                return new BehaviorProgram.Var(require(obj, "n", file, path).getAsString());
            case "bin": {
                String o = require(obj, "o", file, path).getAsString();
                if (!BIN_OPS.contains(o)) {
                    throw new BehaviorParseException(file + ": " + path + " 未知二元运算符 \"" + o + "\"");
                }
                Expr l = parseExpr(file, require(obj, "l", file, path), path + ".l");
                Expr r = parseExpr(file, require(obj, "r", file, path), path + ".r");
                return new BehaviorProgram.Bin(o, l, r);
            }
            case "un": {
                String o = require(obj, "o", file, path).getAsString();
                if (!UN_OPS.contains(o)) {
                    throw new BehaviorParseException(file + ": " + path + " 未知一元运算符 \"" + o + "\"");
                }
                return new BehaviorProgram.Un(o, parseExpr(file, require(obj, "v", file, path), path + ".v"));
            }
            case "sensor": {
                String n = require(obj, "n", file, path).getAsString();
                if (!SENSORS.contains(n)) {
                    throw new BehaviorParseException(file + ": " + path + " 未知传感器 \"" + n + "\"");
                }
                List<Expr> sensorArgs = new ArrayList<>();
                if (obj.has("args") && obj.get("args").isJsonArray()) {
                    JsonArray arr = obj.getAsJsonArray("args");
                    for (int i = 0; i < arr.size(); i++) {
                        sensorArgs.add(parseExpr(file, arr.get(i), path + ".args[" + i + "]"));
                    }
                }
                return new BehaviorProgram.Sensor(n, sensorArgs);
            }
            default:
                throw new BehaviorParseException(file + ": " + path + " 未知表达式类型 \"" + kind + "\"");
        }
    }

    private static JsonElement require(JsonObject obj, String key, String file, String path)
            throws BehaviorParseException {
        JsonElement el = obj.get(key);
        if (el == null) {
            throw new BehaviorParseException(file + ": " + path + " 缺少 " + key + " 字段");
        }
        return el;
    }
}
