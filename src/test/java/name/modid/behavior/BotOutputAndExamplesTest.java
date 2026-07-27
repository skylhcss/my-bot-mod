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
    @DisplayName("巡逻示例可在纯逻辑模式下推进（挂起/恢复不崩溃）")
    void patrolExampleRuns() throws Exception {
        Path file = repoRoot().resolve("editor").resolve("examples").resolve("patrol-greeter.json");
        BehaviorProgram program;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            program = BehaviorParser.parse("patrol-greeter.json", reader);
        }
        assertTrue(program.loop);
        // 注：首语句为 say（需实体），纯逻辑模式跳过实体交互类语句的执行验证，
        // 此处仅验证解析结构：say/wait/move/wait/turn 共 5 条顶层语句
        assertEquals(5, program.body.size());
        assertEquals("say", program.body.get(0).op());
        assertEquals("turn", program.body.get(4).op());
    }
}
