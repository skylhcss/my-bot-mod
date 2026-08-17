package name.modid.client.editor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏内行为编辑器的积木元数据注册表（数据驱动）。
 *
 * 与服务端 BehaviorParser 的 KNOWN_OPS/SENSORS 一一对应：
 * - DEFS：全部语句 op 的参数表/嵌套体/分类，供调色板与参数表单生成
 * - SENSORS：全部传感器的参数标签，供表达式编辑器的传感器选择
 */
public final class BlockDef {

    private BlockDef() {
    }

    // ==================== 元数据类型 ====================

    /** 参数控件类型 */
    public enum PType { TEXT, NUM, BOOL, DROPDOWN, EXPR, LISTTEXT }

    /** 语句参数定义 */
    public record Param(String name, String zh, String en, PType type, String[] options) {
    }

    /** 语句积木定义 */
    public record Def(String op, String zh, String en, String category, Param[] params, String[] bodies) {
    }

    /** 传感器定义（表达式用） */
    public record SensorDef(String name, String zh, String en, String[] paramZh) {
    }

    // ==================== 分类 ====================

    public static final String CAT_EVENT = "event";
    public static final String CAT_ACTION = "action";
    public static final String CAT_MOVE = "move";
    public static final String CAT_CONTAINER = "container";
    public static final String CAT_CONTROL = "control";
    public static final String CAT_VARS = "vars";
    public static final String CAT_OUTPUT = "output";

    /** 分类显示名（中/英） */
    public static final String[][] CATEGORIES = {
        {CAT_EVENT, "事件", "Events"},
        {CAT_ACTION, "动作", "Actions"},
        {CAT_MOVE, "移动", "Motion"},
        {CAT_CONTAINER, "容器", "Containers"},
        {CAT_CONTROL, "控制", "Control"},
        {CAT_VARS, "变量与函数", "Vars & Functions"},
        {CAT_OUTPUT, "输出", "Output"},
    };

    // ==================== 参数构造助手 ====================

    private static Param text(String name, String zh, String en) {
        return new Param(name, zh, en, PType.TEXT, null);
    }

    private static Param num(String name, String zh, String en) {
        return new Param(name, zh, en, PType.NUM, null);
    }

    private static Param bool(String name, String zh, String en) {
        return new Param(name, zh, en, PType.BOOL, null);
    }

    private static Param drop(String name, String zh, String en, String... options) {
        return new Param(name, zh, en, PType.DROPDOWN, options);
    }

    private static Param expr(String name, String zh, String en) {
        return new Param(name, zh, en, PType.EXPR, null);
    }

    private static Param listText(String name, String zh, String en) {
        return new Param(name, zh, en, PType.LISTTEXT, null);
    }

    private static final String[] FMT = {"txt", "json", "csv"};
    private static final String[] MODE = {"append", "overwrite"};
    private static final String[] ATTACK_MODE = {"once", "continuous", "interval"};
    private static final String[] MOVE_DIR = {"forward", "backward", "left", "right"};
    private static final String[] LOOK_DIR = {"up", "down", "left", "right", "north", "south", "east", "west"};

    private static Def d(String op, String zh, String en, String cat, Param[] params, String... bodies) {
        return new Def(op, zh, en, cat, params, bodies);
    }

    // ==================== 语句注册表 ====================

    public static final List<Def> DEFS = buildDefs();

    public static final Map<String, Def> BY_OP = new LinkedHashMap<>();

    static {
        for (Def def : DEFS) {
            BY_OP.put(def.op(), def);
        }
    }

    private static List<Def> buildDefs() {
        List<Def> list = new ArrayList<>();
        // ---- 事件（帽子块，带嵌套体） ----
        list.add(d("onStart", "当行为启动时", "when behavior starts", CAT_EVENT, new Param[]{}, "body"));
        list.add(d("onChat", "当玩家发送消息", "when player says", CAT_EVENT,
            new Param[]{text("text", "匹配文本(空=任意)", "pattern (empty=any)")}, "body"));
        list.add(d("onBroadcast", "当收到广播", "when I receive broadcast", CAT_EVENT,
            new Param[]{text("name", "广播名", "name")}, "body"));
        list.add(d("onHealthBelow", "当血量低于", "when health below", CAT_EVENT,
            new Param[]{num("value", "血量值", "value")}, "body"));
        list.add(d("onEntityNear", "当附近出现实体", "when entity near", CAT_EVENT,
            new Param[]{num("range", "范围", "range"), text("type", "实体类型(空=任意)", "type (empty=any)")},
            "body"));
        // ---- 动作 ----
        list.add(d("say", "说话", "say", CAT_ACTION, new Param[]{expr("text", "内容", "text")}));
        list.add(d("attack", "攻击", "attack", CAT_ACTION,
            new Param[]{drop("mode", "模式", "mode", ATTACK_MODE), num("interval", "间隔tick(interval用)", "interval ticks")}));
        list.add(d("use", "使用/右键", "use", CAT_ACTION,
            new Param[]{drop("mode", "模式", "mode", ATTACK_MODE), num("interval", "间隔tick(interval用)", "interval ticks")}));
        list.add(d("stopAttack", "停止攻击", "stop attack", CAT_ACTION, new Param[]{}));
        list.add(d("stopUse", "停止使用", "stop use", CAT_ACTION, new Param[]{}));
        list.add(d("slot", "切换手持槽位", "set slot", CAT_ACTION, new Param[]{num("n", "槽位(0-8)", "slot (0-8)")}));
        list.add(d("swapHands", "交换主副手", "swap hands", CAT_ACTION, new Param[]{}));
        list.add(d("drop", "丢弃手持", "drop held", CAT_ACTION, new Param[]{bool("stack", "丢整组", "whole stack")}));
        list.add(d("equipItem", "把物品拿到手上", "equip item", CAT_ACTION,
            new Param[]{text("item", "物品ID", "item id")}));
        list.add(d("dropOf", "丢弃指定物品", "drop of item", CAT_ACTION,
            new Param[]{text("item", "物品ID", "item id"), num("count", "数量", "count")}));
        list.add(d("lookAtEntity", "看向最近实体", "look at nearest entity", CAT_ACTION,
            new Param[]{text("type", "实体类型(空=任意)", "type"), num("range", "范围", "range")}));
        list.add(d("followEntity", "持续看向跟随实体", "follow entity", CAT_ACTION,
            new Param[]{text("type", "实体类型(空=任意)", "type"), num("range", "范围", "range")}));
        list.add(d("stopFollow", "停止跟随", "stop follow", CAT_ACTION, new Param[]{}));
        list.add(d("mount", "骑乘附近实体", "mount", CAT_ACTION, new Param[]{}));
        list.add(d("dismount", "下马", "dismount", CAT_ACTION, new Param[]{}));
        // ---- 移动 ----
        list.add(d("move", "定向移动", "move", CAT_MOVE,
            new Param[]{drop("dir", "方向", "direction", MOVE_DIR), num("ticks", "持续tick(空=一直)", "ticks (empty=forever)")}));
        list.add(d("stopMove", "停止移动", "stop move", CAT_MOVE, new Param[]{}));
        list.add(d("jump", "跳跃", "jump", CAT_MOVE, new Param[]{}));
        list.add(d("sneak", "潜行", "sneak", CAT_MOVE, new Param[]{bool("on", "开启", "on")}));
        list.add(d("sprint", "疾跑", "sprint", CAT_MOVE, new Param[]{bool("on", "开启", "on")}));
        list.add(d("look", "看向方向", "look", CAT_MOVE, new Param[]{drop("dir", "方向", "direction", LOOK_DIR)}));
        list.add(d("lookAt", "看向坐标", "look at", CAT_MOVE,
            new Param[]{num("x", "X", "X"), num("y", "Y", "Y"), num("z", "Z", "Z")}));
        list.add(d("turn", "旋转视角", "turn", CAT_MOVE,
            new Param[]{num("yaw", "偏航角", "yaw"), num("pitch", "俯仰角", "pitch")}));
        list.add(d("goto", "寻路到坐标", "goto", CAT_MOVE,
            new Param[]{num("x", "X", "X"), num("y", "Y", "Y"), num("z", "Z", "Z")}));
        list.add(d("gotoStop", "取消寻路", "stop pathfinding", CAT_MOVE, new Param[]{}));
        // ---- 容器 ----
        list.add(d("openContainer", "打开容器", "open container", CAT_CONTAINER,
            new Param[]{num("x", "X", "X"), num("y", "Y", "Y"), num("z", "Z", "Z")}));
        list.add(d("closeContainer", "关闭容器", "close container", CAT_CONTAINER, new Param[]{}));
        list.add(d("takeFromContainer", "从容器取出", "take from container", CAT_CONTAINER,
            new Param[]{num("x", "X", "X"), num("y", "Y", "Y"), num("z", "Z", "Z"),
                text("item", "物品ID(空=任意)", "item (empty=any)"), num("count", "数量", "count"),
                text("var", "实存数量变量", "moved-count var")}));
        list.add(d("putToContainer", "放入容器", "put to container", CAT_CONTAINER,
            new Param[]{num("x", "X", "X"), num("y", "Y", "Y"), num("z", "Z", "Z"),
                text("item", "物品ID(空=任意)", "item (empty=any)"), num("count", "数量", "count"),
                text("var", "实放数量变量", "moved-count var")}));
        list.add(d("readContainer", "读取容器清单到变量", "read container", CAT_CONTAINER,
            new Param[]{num("x", "X", "X"), num("y", "Y", "Y"), num("z", "Z", "Z"),
                text("var", "变量名", "var")}));
        list.add(d("dumpContainer", "导出容器清单", "dump container", CAT_CONTAINER,
            new Param[]{num("x", "X", "X"), num("y", "Y", "Y"), num("z", "Z", "Z"),
                text("file", "文件名", "file"), drop("format", "格式", "format", FMT),
                drop("mode", "写入模式", "write mode", MODE)}));
        // ---- 控制 ----
        list.add(d("wait", "等待", "wait", CAT_CONTROL, new Param[]{num("ticks", "tick数", "ticks")}));
        list.add(d("waitUntil", "等待直到条件", "wait until", CAT_CONTROL,
            new Param[]{expr("cond", "条件", "condition")}));
        list.add(d("repeat", "重复N次", "repeat", CAT_CONTROL,
            new Param[]{num("times", "次数", "times")}, "body"));
        list.add(d("while", "当条件成立重复", "repeat while", CAT_CONTROL,
            new Param[]{expr("cond", "条件", "condition")}, "body"));
        list.add(d("forever", "永远重复", "forever", CAT_CONTROL, new Param[]{}, "body"));
        list.add(d("if", "如果/否则", "if/else", CAT_CONTROL,
            new Param[]{expr("cond", "条件", "condition")}, "then", "else"));
        list.add(d("broadcast", "发送广播", "broadcast", CAT_CONTROL,
            new Param[]{text("name", "广播名", "name")}));
        list.add(d("stopThread", "停止本脚本", "stop this script", CAT_CONTROL, new Param[]{}));
        list.add(d("stopSelf", "结束行为", "stop behavior", CAT_CONTROL, new Param[]{}));
        list.add(d("stopAll", "停止一切", "stop all", CAT_CONTROL, new Param[]{}));
        // ---- 变量/列表/函数 ----
        list.add(d("set", "赋值变量", "set var", CAT_VARS,
            new Param[]{text("var", "变量名", "var"), expr("value", "值", "value")}));
        list.add(d("change", "变量增减", "change var", CAT_VARS,
            new Param[]{text("var", "变量名", "var"), expr("value", "增量", "delta")}));
        list.add(d("listAdd", "列表加入", "list add", CAT_VARS,
            new Param[]{text("var", "列表变量", "list var"), expr("value", "值", "value")}));
        list.add(d("listInsert", "列表插入", "list insert", CAT_VARS,
            new Param[]{text("var", "列表变量", "list var"), num("index", "位置(1起)", "index"),
                expr("value", "值", "value")}));
        list.add(d("listRemove", "列表删除", "list remove", CAT_VARS,
            new Param[]{text("var", "列表变量", "list var"), num("index", "位置(1起)", "index")}));
        list.add(d("listSet", "列表替换", "list set", CAT_VARS,
            new Param[]{text("var", "列表变量", "list var"), num("index", "位置(1起)", "index"),
                expr("value", "值", "value")}));
        list.add(d("listClear", "列表清空", "list clear", CAT_VARS,
            new Param[]{text("var", "列表变量", "list var")}));
        list.add(d("def", "定义函数", "define function", CAT_VARS,
            new Param[]{text("name", "函数名", "name"), text("params", "参数名(逗号分隔)", "params (comma-sep)")},
            "body"));
        list.add(d("call", "调用函数", "call function", CAT_VARS,
            new Param[]{text("name", "函数名", "name"), expr("arg0", "参数1(可选)", "arg 1"),
                expr("arg1", "参数2(可选)", "arg 2"), expr("arg2", "参数3(可选)", "arg 3"),
                expr("arg3", "参数4(可选)", "arg 4")}));
        // ---- 输出 ----
        list.add(d("output", "输出文本到文件", "output text", CAT_OUTPUT,
            new Param[]{text("file", "文件名", "file"), drop("format", "格式", "format", FMT),
                expr("content", "内容", "content"), drop("mode", "写入模式", "write mode", MODE)}));
        list.add(d("outputRaw", "原始行输出(无前缀)", "raw line output", CAT_OUTPUT,
            new Param[]{text("file", "文件名", "file"), expr("content", "内容", "content"),
                drop("mode", "写入模式", "write mode", MODE)}));
        list.add(d("outputTemplate", "自定义模板输出", "template output", CAT_OUTPUT,
            new Param[]{text("file", "文件名", "file"), drop("format", "格式(仅扩展名)", "format (ext only)", FMT),
                text("template", "模板({time} {bot} {var:名})", "template"),
                drop("mode", "写入模式", "write mode", MODE)}));
        list.add(d("outputTable", "自定义表格输出(CSV)", "custom table output", CAT_OUTPUT,
            new Param[]{text("file", "文件名", "file"), listText("header", "表头列(逗号分隔)", "header (comma-sep)"),
                listText("row", "本行数据(逗号分隔)", "row (comma-sep)"),
                drop("mode", "写入模式", "write mode", MODE)}));
        list.add(d("dumpInventory", "导出背包清单", "dump inventory", CAT_OUTPUT,
            new Param[]{text("file", "文件名", "file"), drop("format", "格式", "format", FMT),
                drop("mode", "写入模式", "write mode", MODE)}));
        return List.copyOf(list);
    }

    // ==================== 传感器注册表 ====================

    private static SensorDef s(String name, String zh, String en, String... paramZh) {
        return new SensorDef(name, zh, en, paramZh);
    }

    public static final List<SensorDef> SENSORS = List.of(
        s("health", "血量", "health"),
        s("maxHealth", "血量上限", "max health"),
        s("food", "饥饿值", "food"),
        s("xpLevel", "经验等级", "xp level"),
        s("armor", "护甲值", "armor"),
        s("air", "氧气值", "air"),
        s("posX", "X坐标", "pos X"),
        s("posY", "Y坐标", "pos Y"),
        s("posZ", "Z坐标", "pos Z"),
        s("dimension", "维度", "dimension"),
        s("botName", "自己的名字", "bot name"),
        s("heldItem", "手持物品", "held item"),
        s("onGround", "在地面上?", "on ground"),
        s("inWater", "在水中?", "in water"),
        s("onFire", "着火?", "on fire"),
        s("sneaking", "潜行中?", "sneaking"),
        s("isRaining", "下雨?", "raining"),
        s("isDay", "白天?", "daytime"),
        s("isPathfinding", "寻路中?", "pathfinding"),
        s("timeOfDay", "当日时间", "time of day"),
        s("fishHooked", "鱼上钩?", "fish hooked"),
        s("usingItem", "正在使用物品?", "using item"),
        s("blockBelow", "脚下方块", "block below"),
        s("facing", "朝向方位", "facing"),
        s("nearestPlayerName", "最近玩家名", "nearest player name"),
        s("nearestPlayerDistance", "最近玩家距离", "nearest player dist"),
        s("blockAt", "指定坐标方块", "block at", "X", "Y", "Z"),
        s("distanceTo", "到坐标距离", "distance to", "X", "Y", "Z"),
        s("invCount", "背包物品计数", "inventory count", "物品ID"),
        s("nearbyEntities", "周围生物数", "nearby entities", "范围", "类型"),
        s("holding", "手持是否为某物", "holding item", "物品ID"),
        s("hasItem", "背包是否有某物", "has item", "物品ID"),
        s("nearestEntityCoord", "最近实体坐标", "nearest entity coord", "范围", "类型", "轴(x/y/z)"),
        s("nearestEntityDistance", "最近实体距离", "nearest entity dist", "范围", "类型"),
        s("nearestEntityExists", "最近实体存在?", "nearest entity exists", "范围", "类型"),
        s("containerSlots", "容器格子数", "container slots", "X", "Y", "Z"),
        s("containerItem", "容器指定格物品", "container slot item", "X", "Y", "Z", "格"),
        s("containerSlotCount", "容器指定格数量", "container slot count", "X", "Y", "Z", "格"),
        s("containerCount", "容器物品总数", "container item count", "X", "Y", "Z", "物品ID"),
        s("random", "随机数", "random", "最小", "最大"),
        s("sin", "正弦(角度)", "sin", "角度"),
        s("cos", "余弦(角度)", "cos", "角度"),
        s("tan", "正切(角度)", "tan", "角度"),
        s("strLen", "文本长度", "text length", "文本"),
        s("strContains", "文本包含?", "text contains", "文本", "子串"),
        s("strSub", "子串", "substring", "文本", "起", "止"),
        s("strUpper", "转大写", "uppercase", "文本"),
        s("strLower", "转小写", "lowercase", "文本"),
        s("strTrim", "去空格", "trim", "文本"),
        s("strCharAt", "第i个字符", "char at", "文本", "i"),
        s("strIndexOf", "查找位置", "index of", "文本", "子串"),
        s("listGet", "列表第i项", "list get", "列表", "i"),
        s("listLen", "列表长度", "list length", "列表"),
        s("listContains", "列表包含?", "list contains", "列表", "值"),
        s("listIndexOf", "列表查找位置", "list index of", "列表", "值"),
        s("listJoin", "列表拼接为文本", "list join", "列表", "分隔符"),
        s("listSplit", "文本拆分为列表", "text split", "文本", "分隔符"),
        s("listRandom", "列表随机项", "list random", "列表")
    );

    public static final Map<String, SensorDef> SENSOR_BY_NAME = new LinkedHashMap<>();

    static {
        for (SensorDef def : SENSORS) {
            SENSOR_BY_NAME.put(def.name(), def);
        }
    }

    /** 二元运算符（表达式编辑器"运算"用） */
    public static final List<String> BIN_OPS = List.of(
        "+", "-", "*", "/", "%", "==", "!=", "<", ">", "<=", ">=", "and", "or", "concat", "min", "max", "pow");

    /** 一元运算符 */
    public static final List<String> UN_OPS = List.of(
        "not", "neg", "abs", "floor", "ceil", "round", "sqrt");

    /** 分类下的积木列表 */
    public static List<Def> byCategory(String category) {
        List<Def> out = new ArrayList<>();
        for (Def def : DEFS) {
            if (def.category().equals(category)) {
                out.add(def);
            }
        }
        return out;
    }
}
