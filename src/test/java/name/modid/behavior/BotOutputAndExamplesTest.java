package name.modid.behavior;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 外置输出端到端测试（txt/json/csv 实际写盘并校验内容）
 * 与随包示例行为文件的解析验证（保证 editor/examples 可被运行时接受）。
 */
class BotOutputAndExamplesTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        BotOutput.overrideDir = tempDir;
    }

    @AfterEach
    void tearDown() {
        BotOutput.overrideDir = null;
    }

    // ==================== 输出格式 ====================

    @Test
    @DisplayName("txt 输出：追加带时间与假人名的文本行")
    void writeTextTxt() throws Exception {
        BotOutput.writeText("log", "txt", "Steve", "hello world");
        BotOutput.writeText("log", "txt", "Steve", "second line");
        BotOutput.flush();
        List<String> lines = Files.readAllLines(tempDir.resolve("log.txt"), StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("[Steve] hello world"));
        assertTrue(lines.get(1).contains("[Steve] second line"));
    }

    @Test
    @DisplayName("json 输出：JSON Lines 每行一个对象")
    void writeTextJson() throws Exception {
        BotOutput.writeText("data", "json", "Alex", "content \"quoted\"");
        BotOutput.flush();
        List<String> lines = Files.readAllLines(tempDir.resolve("data.jsonl"), StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        var obj = com.google.gson.JsonParser.parseString(lines.get(0)).getAsJsonObject();
        assertEquals("Alex", obj.get("bot").getAsString());
        assertEquals("content \"quoted\"", obj.get("content").getAsString());
    }

    @Test
    @DisplayName("csv 输出：首行表头 + 特殊字符转义")
    void writeItemsCsv() throws Exception {
        Map<String, Integer> items = new LinkedHashMap<>();
        items.put("minecraft:cobblestone", 64);
        items.put("minecraft:oak_log", 12);
        BotOutput.writeItems("inv", "csv", "Steve", "inventory", items);
        BotOutput.flush();
        List<String> lines = Files.readAllLines(tempDir.resolve("inv.csv"), StandardCharsets.UTF_8);
        assertEquals("time,bot,source,item,count", lines.get(0));
        assertEquals(3, lines.size());
        assertTrue(lines.get(1).endsWith("Steve,inventory,minecraft:cobblestone,64"));
        assertTrue(lines.get(2).endsWith("Steve,inventory,minecraft:oak_log,12"));

        // 第二次写入不重复表头
        BotOutput.writeItems("inv", "csv", "Steve", "inventory", items);
        BotOutput.flush();
        assertEquals(5, Files.readAllLines(tempDir.resolve("inv.csv"), StandardCharsets.UTF_8).size());
    }

    @Test
    @DisplayName("json 物品清单：物品映射完整")
    void writeItemsJson() throws Exception {
        Map<String, Integer> items = new LinkedHashMap<>();
        items.put("minecraft:diamond", 3);
        BotOutput.writeItems("chest", "json", "Steve", "container", items);
        BotOutput.flush();
        var obj = com.google.gson.JsonParser.parseString(
            Files.readAllLines(tempDir.resolve("chest.jsonl"), StandardCharsets.UTF_8).get(0)).getAsJsonObject();
        assertEquals("container", obj.get("source").getAsString());
        assertEquals(3, obj.getAsJsonObject("items").get("minecraft:diamond").getAsInt());
    }

    // ==================== 自定义输出（原始行/模板/表格/覆盖模式） ====================

    @Test
    @DisplayName("原始行输出：无时间/假人前缀，多次写入追加")
    void writeRawAppends() throws Exception {
        BotOutput.writeRaw("raw", "line one", false);
        BotOutput.writeRaw("raw", "line two", false);
        BotOutput.flush();
        List<String> lines = Files.readAllLines(tempDir.resolve("raw.txt"), StandardCharsets.UTF_8);
        assertEquals(List.of("line one", "line two"), lines);
    }

    @Test
    @DisplayName("模板占位符替换：{time}/{bot}/{var:名}，未知占位符原样保留")
    void renderTemplatePlaceholders() {
        Map<String, String> vars = Map.of("hp", "20");
        String out = BotOutput.renderTemplate("[{time}] {bot} hp={var:hp} {unknown}", "T", "Steve", vars);
        assertEquals("[T] Steve hp=20 {unknown}", out);
        // 变量不存在时为空串
        assertEquals("x=", BotOutput.renderTemplate("x={var:none}", "T", "B", Map.of()));
    }

    @Test
    @DisplayName("模板输出落盘：模板完全决定行内容")
    void writeTemplateToFile() throws Exception {
        BotOutput.writeTemplate("tpl", "txt", "Alex", "{bot}: {var:msg}", Map.of("msg", "hi"), false);
        BotOutput.flush();
        List<String> lines = Files.readAllLines(tempDir.resolve("tpl.txt"), StandardCharsets.UTF_8);
        assertEquals(List.of("Alex: hi"), lines);
    }

    @Test
    @DisplayName("自定义表格：新文件写自定义表头，之后追加数据行不重复表头")
    void writeTableRowHeaderOnce() throws Exception {
        BotOutput.writeTableRow("tbl", List.of("名称", "数量"), List.of("圆石", "64"), false);
        BotOutput.writeTableRow("tbl", List.of("名称", "数量"), List.of("含,逗号", "1"), false);
        BotOutput.flush();
        List<String> lines = Files.readAllLines(tempDir.resolve("tbl.csv"), StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertEquals("名称,数量", lines.get(0));
        assertEquals("圆石,64", lines.get(1));
        assertEquals("\"含,逗号\",1", lines.get(2));
    }

    @Test
    @DisplayName("覆盖模式：overwrite 截断原内容，csv 覆盖时重写表头")
    void overwriteMode() throws Exception {
        BotOutput.writeText("ow", "txt", "Steve", "old line", false);
        BotOutput.writeText("ow", "txt", "Steve", "new line", true);
        BotOutput.flush();
        List<String> lines = Files.readAllLines(tempDir.resolve("ow.txt"), StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("new line"));

        BotOutput.writeTableRow("owc", List.of("a", "b"), List.of("1", "2"), false);
        BotOutput.writeTableRow("owc", List.of("a", "b"), List.of("3", "4"), true);
        BotOutput.flush();
        List<String> csv = Files.readAllLines(tempDir.resolve("owc.csv"), StandardCharsets.UTF_8);
        assertEquals(List.of("a,b", "3,4"), csv);
    }

    // ==================== 行为文件导出（游戏内编辑器保存路径） ====================

    private static final String VALID_BEHAVIOR =
        "{\"format\":1,\"name\":\"t\",\"program\":[{\"op\":\"wait\",\"ticks\":1}]}";

    @Test
    @DisplayName("导出到任意绝对目录：文件名清洗后写入")
    void storageSaveToAbsoluteDir() throws Exception {
        String err = BehaviorStorage.save("my bot/test.json", VALID_BEHAVIOR, tempDir.toString());
        assertEquals(null, err);
        // "my bot/test.json" 清洗为 my_bot_test.json（空格与路径分隔符均被替换）
        assertTrue(Files.exists(tempDir.resolve("my_bot_test.json")));
    }

    @Test
    @DisplayName("导出目录含 .. 路径段被拒绝")
    void storageRejectTraversalDir() {
        String err = BehaviorStorage.save("x", VALID_BEHAVIOR, tempDir.toString() + "/../evil");
        assertTrue(err != null && err.contains(".."));
    }

    @Test
    @DisplayName("非法行为 JSON 在保存前被拦截，不落盘")
    void storageRejectInvalidBehavior() {
        String err = BehaviorStorage.save("bad", "{\"format\":1}", tempDir.toString());
        assertTrue(err != null);
        assertFalse(Files.exists(tempDir.resolve("bad.json")));
    }

    // ==================== 随包示例行为文件 ====================

    /** 从工作目录向上查找仓库根（Stonecutter 子项目的测试工作目录在 versions/<mc>/ 下） */
    private static Path repoRoot() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++, dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve("editor").resolve("examples"))) {
                return dir;
            }
        }
        throw new IllegalStateException("找不到仓库根（editor/examples）");
    }

    @Test
    @DisplayName("随包示例行为文件全部可被解析器接受")
    void exampleBehaviorsParse() throws Exception {
        Path examples = repoRoot().resolve("editor").resolve("examples");
        List<Path> files;
        try (var stream = Files.list(examples)) {
            files = stream.filter(p -> p.getFileName().toString().endsWith(".json")).toList();
        }
        assertTrue(files.size() >= 2, "应至少有 2 个示例行为");
        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                BehaviorProgram program = BehaviorParser.parse(file.getFileName().toString(), reader);
                assertTrue(program.statementCount() > 0, file + " 语句数应大于 0");
            }
        }
    }

    @Test
    @DisplayName("巡逻示例：两个帽子块并联（onStart 巡逻 + onChat 问候）")
    void patrolExampleRuns() throws Exception {
        Path file = repoRoot().resolve("editor").resolve("examples").resolve("patrol-greeter.json");
        BehaviorProgram program;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            program = BehaviorParser.parse("patrol-greeter.json", reader);
        }
        assertEquals(2, program.body.size());
        assertEquals("onStart", program.body.get(0).op());
        assertEquals("onChat", program.body.get(1).op());
        // 纯逻辑模式：含 onChat 触发器应驻留，聊天命中后回应变量可用
        BehaviorRuntime r = new BehaviorRuntime(null, program);
        for (int i = 0; i < 5; i++) {
            r.tick();
        }
        assertFalse(r.isFinished(), "含聊天触发器应驻留监听");
        assertTrue(r.onChatMessage("Steve", "你好呀"), "包含匹配应命中");
    }
}
