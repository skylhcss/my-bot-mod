package name.modid.behavior;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行为系统测试：解析器（格式/报错）与解释器（控制流/变量/挂起/预算）。
 * 解释器测试用 bot=null 的纯逻辑模式，仅覆盖不接触实体的语句。
 */
class BehaviorSystemTest {

    private BehaviorProgram parse(String json) throws BehaviorParseException {
        return BehaviorParser.parse("test.json", new StringReader(json));
    }

    /** 运行至完成或 maxTicks 次 tick，返回解释器以便断言变量 */
    private BehaviorRuntime run(String json, int maxTicks) throws BehaviorParseException {
        BehaviorRuntime runtime = new BehaviorRuntime(null, parse(json));
        for (int i = 0; i < maxTicks && !runtime.isFinished(); i++) {
            runtime.tick();
        }
        return runtime;
    }

    // ==================== 解析器 ====================

    @Test
    @DisplayName("解析最小合法程序")
    void parseMinimal() throws Exception {
        BehaviorProgram p = parse("{\"format\":1,\"name\":\"t\",\"program\":[{\"op\":\"wait\",\"ticks\":5}]}");
        assertEquals("t", p.name);
        assertEquals(1, p.body.size());
        assertEquals("wait", p.body.get(0).op());
    }

    @Test
    @DisplayName("缺少 program 报错")
    void parseMissingProgram() {
        assertThrows(BehaviorParseException.class, () -> parse("{\"format\":1,\"name\":\"t\"}"));
    }

    @Test
    @DisplayName("未知 op 报错并包含位置")
    void parseUnknownOp() {
        BehaviorParseException e = assertThrows(BehaviorParseException.class,
            () -> parse("{\"program\":[{\"op\":\"fly\"}]}"));
        assertTrue(e.getMessage().contains("fly"));
        assertTrue(e.getMessage().contains("program[0]"));
    }

    @Test
    @DisplayName("未知运算符报错")
    void parseUnknownOperator() {
        assertThrows(BehaviorParseException.class, () -> parse(
            "{\"program\":[{\"op\":\"set\",\"var\":\"x\",\"value\":{\"e\":\"bin\",\"o\":\"**\",\"l\":1,\"r\":2}}]}"));
    }

    @Test
    @DisplayName("裸字面量自动包装为表达式")
    void parseBareLiterals() throws Exception {
        BehaviorProgram p = parse("{\"program\":[{\"op\":\"say\",\"text\":\"hi\"}]}");
        assertNotNull(p.body.get(0).arg("text"));
    }

    @Test
    @DisplayName("行为名缺省取文件名")
    void parseDefaultName() throws Exception {
        assertEquals("test", parse("{\"program\":[]}").name);
    }

    // ==================== 解释器：变量与运算 ====================

    @Test
    @DisplayName("set/change 与四则运算")
    void varsAndArithmetic() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"x\",\"value\":10},"
            + "{\"op\":\"change\",\"var\":\"x\",\"value\":5},"
            + "{\"op\":\"set\",\"var\":\"y\",\"value\":{\"e\":\"bin\",\"o\":\"*\",\"l\":{\"e\":\"var\",\"n\":\"x\"},\"r\":2}}"
            + "]}", 10);
        assertTrue(r.isFinished());
        assertEquals(15, r.getVar("x").asNumber());
        assertEquals(30, r.getVar("y").asNumber());
    }

    @Test
    @DisplayName("Scratch 式宽松相等与字符串拼接")
    void looseEqualsAndConcat() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"eq\",\"value\":{\"e\":\"bin\",\"o\":\"==\",\"l\":\"5\",\"r\":5}},"
            + "{\"op\":\"set\",\"var\":\"s\",\"value\":{\"e\":\"bin\",\"o\":\"concat\",\"l\":\"a\",\"r\":1}}"
            + "]}", 10);
        assertTrue(r.getVar("eq").asBool());
        assertEquals("a1", r.getVar("s").asString());
    }

    @Test
    @DisplayName("除零安全返回 0")
    void divisionByZero() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"d\",\"value\":{\"e\":\"bin\",\"o\":\"/\",\"l\":10,\"r\":0}}"
            + "]}", 10);
        assertEquals(0, r.getVar("d").asNumber());
    }

    // ==================== 解释器：控制流 ====================

    @Test
    @DisplayName("repeat 循环执行 N 次")
    void repeatLoop() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"n\",\"value\":0},"
            + "{\"op\":\"repeat\",\"times\":7,\"body\":[{\"op\":\"change\",\"var\":\"n\",\"value\":1}]}"
            + "]}", 10);
        assertTrue(r.isFinished());
        assertEquals(7, r.getVar("n").asNumber());
    }

    @Test
    @DisplayName("while 条件循环")
    void whileLoop() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"i\",\"value\":0},"
            + "{\"op\":\"while\",\"cond\":{\"e\":\"bin\",\"o\":\"<\",\"l\":{\"e\":\"var\",\"n\":\"i\"},\"r\":5},"
            + "\"body\":[{\"op\":\"change\",\"var\":\"i\",\"value\":1}]}"
            + "]}", 10);
        assertTrue(r.isFinished());
        assertEquals(5, r.getVar("i").asNumber());
    }

    @Test
    @DisplayName("if/else 分支")
    void ifElse() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"x\",\"value\":3},"
            + "{\"op\":\"if\",\"cond\":{\"e\":\"bin\",\"o\":\">\",\"l\":{\"e\":\"var\",\"n\":\"x\"},\"r\":10},"
            + "\"then\":[{\"op\":\"set\",\"var\":\"r\",\"value\":\"big\"}],"
            + "\"else\":[{\"op\":\"set\",\"var\":\"r\",\"value\":\"small\"}]}"
            + "]}", 10);
        assertEquals("small", r.getVar("r").asString());
    }

    @Test
    @DisplayName("嵌套 repeat 正确计数")
    void nestedRepeat() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"n\",\"value\":0},"
            + "{\"op\":\"repeat\",\"times\":3,\"body\":["
            + "  {\"op\":\"repeat\",\"times\":4,\"body\":[{\"op\":\"change\",\"var\":\"n\",\"value\":1}]}"
            + "]}"
            + "]}", 10);
        assertEquals(12, r.getVar("n").asNumber());
    }

    // ==================== 解释器：挂起与预算 ====================

    @Test
    @DisplayName("wait 挂起后按 tick 恢复")
    void waitSuspends() throws Exception {
        BehaviorRuntime r = new BehaviorRuntime(null, parse("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"a\",\"value\":1},"
            + "{\"op\":\"wait\",\"ticks\":3},"
            + "{\"op\":\"set\",\"var\":\"b\",\"value\":2}"
            + "]}"));
        r.tick(); // 执行 set a 并在 wait 挂起
        assertEquals(1, r.getVar("a").asNumber());
        assertEquals(0, r.getVar("b").asNumber());
        assertFalse(r.isFinished());
        r.tick();
        r.tick();
        r.tick(); // 等待结束
        r.tick(); // 执行 set b 并完成
        assertEquals(2, r.getVar("b").asNumber());
        assertTrue(r.isFinished());
    }

    @Test
    @DisplayName("无 wait 的 forever 循环受预算限制不冻结")
    void foreverBudget() throws Exception {
        BehaviorRuntime r = new BehaviorRuntime(null, parse("{\"program\":["
            + "{\"op\":\"forever\",\"body\":[{\"op\":\"change\",\"var\":\"n\",\"value\":1}]}"
            + "]}"));
        long start = System.currentTimeMillis();
        r.tick(); // 单 tick 必须在预算内返回
        assertTrue(System.currentTimeMillis() - start < 2000, "单 tick 应远快于 2 秒");
        assertFalse(r.isFinished());
        double afterOne = r.getVar("n").asNumber();
        assertTrue(afterOne > 0, "循环体应被执行");
        r.tick();
        assertTrue(r.getVar("n").asNumber() > afterOne, "下一 tick 应继续执行");
    }

    @Test
    @DisplayName("顶层 loop 每轮让出一 tick")
    void topLevelLoopYields() throws Exception {
        BehaviorRuntime r = new BehaviorRuntime(null, parse("{\"loop\":true,\"program\":["
            + "{\"op\":\"change\",\"var\":\"n\",\"value\":1}"
            + "]}"));
        r.tick();
        double first = r.getVar("n").asNumber();
        r.tick();
        assertTrue(r.getVar("n").asNumber() > first);
        assertFalse(r.isFinished());
    }

    @Test
    @DisplayName("stopSelf 立即结束行为")
    void stopSelf() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"a\",\"value\":1},"
            + "{\"op\":\"stopSelf\"},"
            + "{\"op\":\"set\",\"var\":\"b\",\"value\":2}"
            + "]}", 10);
        assertTrue(r.isFinished());
        assertEquals(1, r.getVar("a").asNumber());
        assertEquals(0, r.getVar("b").asNumber());
    }

    // ==================== 数学函数与新运算 ====================

    @Test
    @DisplayName("一元数学函数 abs/floor/ceil/round/sqrt")
    void mathFunctions() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"a\",\"value\":{\"e\":\"un\",\"o\":\"abs\",\"v\":-7.5}},"
            + "{\"op\":\"set\",\"var\":\"f\",\"value\":{\"e\":\"un\",\"o\":\"floor\",\"v\":3.9}},"
            + "{\"op\":\"set\",\"var\":\"c\",\"value\":{\"e\":\"un\",\"o\":\"ceil\",\"v\":3.1}},"
            + "{\"op\":\"set\",\"var\":\"r\",\"value\":{\"e\":\"un\",\"o\":\"round\",\"v\":2.5}},"
            + "{\"op\":\"set\",\"var\":\"s\",\"value\":{\"e\":\"un\",\"o\":\"sqrt\",\"v\":16}}"
            + "]}", 10);
        assertEquals(7.5, r.getVar("a").asNumber());
        assertEquals(3, r.getVar("f").asNumber());
        assertEquals(4, r.getVar("c").asNumber());
        assertEquals(3, r.getVar("r").asNumber());
        assertEquals(4, r.getVar("s").asNumber());
    }

    @Test
    @DisplayName("二元运算 min/max/pow")
    void minMaxPow() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"set\",\"var\":\"lo\",\"value\":{\"e\":\"bin\",\"o\":\"min\",\"l\":3,\"r\":8}},"
            + "{\"op\":\"set\",\"var\":\"hi\",\"value\":{\"e\":\"bin\",\"o\":\"max\",\"l\":3,\"r\":8}},"
            + "{\"op\":\"set\",\"var\":\"p\",\"value\":{\"e\":\"bin\",\"o\":\"pow\",\"l\":2,\"r\":10}}"
            + "]}", 10);
        assertEquals(3, r.getVar("lo").asNumber());
        assertEquals(8, r.getVar("hi").asNumber());
        assertEquals(1024, r.getVar("p").asNumber());
    }

    // ==================== 自定义函数 ====================

    @Test
    @DisplayName("def/call 带参数函数调用")
    void functionCall() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"def\",\"name\":\"addTwice\",\"params\":\"x\",\"body\":["
            + "  {\"op\":\"change\",\"var\":\"total\",\"value\":{\"e\":\"var\",\"n\":\"x\"}},"
            + "  {\"op\":\"change\",\"var\":\"total\",\"value\":{\"e\":\"var\",\"n\":\"x\"}}"
            + "]},"
            + "{\"op\":\"set\",\"var\":\"total\",\"value\":0},"
            + "{\"op\":\"call\",\"name\":\"addTwice\",\"arg0\":5},"
            + "{\"op\":\"call\",\"name\":\"addTwice\",\"arg0\":7}"
            + "]}", 10);
        assertTrue(r.isFinished());
        assertEquals(24, r.getVar("total").asNumber());
    }

    @Test
    @DisplayName("未定义函数调用不崩溃并记录错误")
    void callUndefined() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"call\",\"name\":\"nope\"},"
            + "{\"op\":\"set\",\"var\":\"after\",\"value\":1}"
            + "]}", 10);
        assertTrue(r.isFinished());
        assertEquals(1, r.getVar("after").asNumber());
        assertNotNull(r.getLastError());
    }

    @Test
    @DisplayName("递归函数受深度限制不爆栈")
    void recursionLimited() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"def\",\"name\":\"loop\",\"body\":["
            + "  {\"op\":\"change\",\"var\":\"depth\",\"value\":1},"
            + "  {\"op\":\"call\",\"name\":\"loop\"}"
            + "]},"
            + "{\"op\":\"call\",\"name\":\"loop\"}"
            + "]}", 50);
        assertTrue(r.isFinished());
        assertTrue(r.getVar("depth").asNumber() > 0);
        assertNotNull(r.getLastError());
    }

    // ==================== 事件（帽子块） ====================

    @Test
    @DisplayName("onStart 事件体并入主流程执行")
    void onStartRuns() throws Exception {
        BehaviorRuntime r = run("{\"program\":["
            + "{\"op\":\"onStart\",\"body\":[{\"op\":\"set\",\"var\":\"ran\",\"value\":1}]}"
            + "]}", 10);
        assertTrue(r.isFinished());
        assertEquals(1, r.getVar("ran").asNumber());
    }

    @Test
    @DisplayName("onChat 匹配触发并注入 chat_sender/chat_message 变量")
    void onChatTriggers() throws Exception {
        BehaviorRuntime r = new BehaviorRuntime(null, parse("{\"program\":["
            + "{\"op\":\"onChat\",\"text\":\"你好\",\"body\":[{\"op\":\"set\",\"var\":\"hits\",\"value\":1}]}"
            + "]}"));
        r.tick();
        assertFalse(r.isFinished(), "含聊天触发器应驻留监听而非结束");
        assertFalse(r.onChatMessage("Steve", "无关消息"), "不匹配不触发");
        assertTrue(r.onChatMessage("Steve", "嗨 你好 假人"), "包含匹配应触发");
        r.tick();
        assertEquals(1, r.getVar("hits").asNumber());
        assertEquals("Steve", r.getVar("chat_sender").asString());
        assertEquals("嗨 你好 假人", r.getVar("chat_message").asString());
        assertFalse(r.isFinished(), "触发体跑完后继续驻留监听");
    }

    @Test
    @DisplayName("onChat 空模式匹配任意消息")
    void onChatEmptyPattern() throws Exception {
        BehaviorRuntime r = new BehaviorRuntime(null, parse("{\"program\":["
            + "{\"op\":\"onChat\",\"text\":\"\",\"body\":[{\"op\":\"change\",\"var\":\"n\",\"value\":1}]}"
            + "]}"));
        r.tick();
        assertTrue(r.onChatMessage("Alex", "anything at all"));
        r.tick();
        assertEquals(1, r.getVar("n").asNumber());
    }

    // ==================== 文件名清洗 ====================

    @Test
    @DisplayName("导出文件名拒绝路径穿越")
    void sanitizeFileName() {
        assertEquals("____evil.txt", BotOutput.sanitizeFileName("../\\evil", "txt"));
        assertEquals("output.txt", BotOutput.sanitizeFileName("../..", "txt"));
        assertEquals("盘点.csv", BotOutput.sanitizeFileName("盘点", "csv"));
        assertEquals("data.jsonl", BotOutput.sanitizeFileName("data", "json"));
    }
}
