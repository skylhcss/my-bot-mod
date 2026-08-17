package name.modid.behavior;

import com.google.gson.JsonObject;
import name.modid.MyBotMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 假人外置输出：把行为产生的内容写入 <gameDir>/my-bot-mod-exports/ 文件夹
 *
 * 格式：
 * - txt：追加纯文本行 "[时间] [假人] 内容"
 * - json：追加 JSON Lines（每行一个对象）
 * - csv：首次写入表头，之后追加数据行
 * - 自定义：原始行（writeRaw）、模板占位符（writeTemplate）、自定义表格（writeTableRow）
 *
 * 写入模式支持追加/覆盖；文件名经白名单清洗（拒绝路径穿越）；IO 在单线程队列异步执行，不阻塞服务器主线程。
 */
public final class BotOutput {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "my-bot-mod-output");
        t.setDaemon(true);
        return t;
    });

    private BotOutput() {
    }

    /** 测试用：覆盖导出目录（单元测试无 FabricLoader 环境） */
    static Path overrideDir;

    /** 导出目录：<gameDir>/my-bot-mod-exports */
    public static Path exportDir() {
        if (overrideDir != null) {
            return overrideDir;
        }
        return FabricLoader.getInstance().getGameDir().resolve("my-bot-mod-exports");
    }

    /** 测试用：阻塞等待异步队列中已提交的写入全部落盘 */
    static void flush() throws Exception {
        IO.submit(() -> {
        }).get(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    /** 输出一段文本内容（追加模式） */
    public static void writeText(String file, String format, String botName, String content) {
        writeText(file, format, botName, content, false);
    }

    /** 输出一段文本内容 @param overwrite true=覆盖原文件，false=追加 */
    public static void writeText(String file, String format, String botName, String content, boolean overwrite) {
        String fmt = normalizeFormat(format);
        String time = LocalDateTime.now().format(TIME);
        String line;
        switch (fmt) {
            case "json" -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("time", time);
                obj.addProperty("bot", botName);
                obj.addProperty("content", content);
                line = obj.toString();
            }
            case "csv" -> line = csvRow(time, botName, "text", content, "");
            default -> line = "[" + time + "] [" + botName + "] " + content;
        }
        append(file, fmt, line, "time,bot,type,content,extra", overwrite);
    }

    /** 输出物品清单（背包/容器盘点，追加模式） */
    public static void writeItems(String file, String format, String botName, String source,
                                  Map<String, Integer> items) {
        writeItems(file, format, botName, source, items, false);
    }

    /** 输出物品清单 @param overwrite true=覆盖原文件，false=追加 */
    public static void writeItems(String file, String format, String botName, String source,
                                  Map<String, Integer> items, boolean overwrite) {
        String fmt = normalizeFormat(format);
        String time = LocalDateTime.now().format(TIME);
        switch (fmt) {
            case "json" -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("time", time);
                obj.addProperty("bot", botName);
                obj.addProperty("source", source);
                JsonObject itemsObj = new JsonObject();
                items.forEach(itemsObj::addProperty);
                obj.add("items", itemsObj);
                append(file, fmt, obj.toString(), null, overwrite);
            }
            case "csv" -> {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Integer> e : items.entrySet()) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(csvRow(time, botName, source, e.getKey(), String.valueOf(e.getValue())));
                }
                if (sb.length() == 0) {
                    sb.append(csvRow(time, botName, source, "(empty)", "0"));
                }
                append(file, fmt, sb.toString(), "time,bot,source,item,count", overwrite);
            }
            default -> {
                StringBuilder sb = new StringBuilder("[" + time + "] [" + botName + "] " + source + ":");
                if (items.isEmpty()) {
                    sb.append(" (空)");
                } else {
                    items.forEach((id, count) -> sb.append(' ').append(id).append('*').append(count));
                }
                append(file, fmt, sb.toString(), null, overwrite);
            }
        }
    }

    /** 原始文本行输出：直接写入内容本身，不自动添加时间/假人前缀 */
    public static void writeRaw(String file, String content, boolean overwrite) {
        append(file, "txt", content == null ? "" : content, null, overwrite);
    }

    /**
     * 自定义模板输出：模板完全决定行内容，format 仅决定文件扩展名。
     * 占位符：{time} 时间、{bot} 假人名、{var:变量名} 行为变量值；未知占位符原样保留。
     */
    public static void writeTemplate(String file, String format, String botName, String template,
                                     Map<String, String> vars, boolean overwrite) {
        String fmt = normalizeFormat(format);
        String time = LocalDateTime.now().format(TIME);
        append(file, fmt, renderTemplate(template, time, botName, vars), null, overwrite);
    }

    /** 模板占位符替换（包级可见供测试） */
    static String renderTemplate(String template, String time, String botName, Map<String, String> vars) {
        if (template == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            char ch = template.charAt(i);
            if (ch == '{') {
                int end = template.indexOf('}', i + 1);
                if (end > i) {
                    String value = resolvePlaceholder(template.substring(i + 1, end), time, botName, vars);
                    if (value != null) {
                        sb.append(value);
                        i = end + 1;
                        continue;
                    }
                }
            }
            sb.append(ch);
            i++;
        }
        return sb.toString();
    }

    /** @return 占位符值；null = 未知占位符（原样保留） */
    private static String resolvePlaceholder(String key, String time, String botName, Map<String, String> vars) {
        if (key.equals("time")) {
            return time;
        }
        if (key.equals("bot")) {
            return botName;
        }
        if (key.startsWith("var:")) {
            if (vars == null) {
                return "";
            }
            String v = vars.get(key.substring(4));
            return v == null ? "" : v;
        }
        return null;
    }

    /** 自定义表格输出：新文件先写自定义表头，之后追加数据行（CSV 转义） */
    public static void writeTableRow(String file, java.util.List<String> header, java.util.List<String> row,
                                     boolean overwrite) {
        String headerLine = header == null || header.isEmpty()
            ? null : csvRow(header.toArray(new String[0]));
        String dataLine = csvRow(row == null ? new String[0] : row.toArray(new String[0]));
        append(file, "csv", dataLine, headerLine, overwrite);
    }

    // ==================== 内部 ====================

    private static String normalizeFormat(String format) {
        if (format == null) {
            return "txt";
        }
        return switch (format.toLowerCase()) {
            case "json", "jsonl" -> "json";
            case "csv" -> "csv";
            default -> "txt";
        };
    }

    /** 文件名清洗：仅保留字母/数字/下划线/连字符/中文，拒绝路径穿越；扩展名按格式追加 */
    static String sanitizeFileName(String name, String fmt) {
        String base = name == null ? "" : name.replaceAll("[^\\w\\-\\u4e00-\\u9fa5]", "_");
        if (base.isEmpty() || base.chars().allMatch(ch -> ch == '_')) {
            base = "output";
        }
        if (base.length() > 64) {
            base = base.substring(0, 64);
        }
        String ext = fmt.equals("json") ? ".jsonl" : "." + fmt;
        return base + ext;
    }

    private static String csvRow(String... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String cell = cells[i] == null ? "" : cells[i];
            if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                cell = '"' + cell.replace("\"", "\"\"") + '"';
            }
            sb.append(cell);
        }
        return sb.toString();
    }

    /** 异步写入一行（csv 在新文件时先写表头） @param overwrite true=覆盖，false=追加 */
    private static void append(String rawName, String fmt, String line, String csvHeader, boolean overwrite) {
        String fileName = sanitizeFileName(rawName, fmt);
        IO.execute(() -> {
            try {
                Path dir = exportDir();
                Files.createDirectories(dir);
                Path target = dir.resolve(fileName);
                String prefix = "";
                if (fmt.equals("csv") && csvHeader != null && (overwrite || !Files.exists(target))) {
                    prefix = csvHeader + System.lineSeparator();
                }
                var options = overwrite
                    ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING}
                    : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND};
                Files.writeString(target, prefix + line + System.lineSeparator(), StandardCharsets.UTF_8, options);
            } catch (IOException e) {
                MyBotMod.LOGGER.warn("[行为] 导出写入失败 {}: {}", fileName, e.getMessage());
            }
        });
    }
}
