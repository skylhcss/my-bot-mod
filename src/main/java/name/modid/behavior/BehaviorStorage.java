package name.modid.behavior;

import name.modid.MyBotMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 行为文件的保存与读取（游戏内编辑器导出用）
 *
 * - 目标目录为空时保存到默认行为目录（config/my-bot-mod/behaviors/）并自动重载；
 * - 允许绝对路径导出到任意文件夹（相对路径按游戏目录解析），拒绝 ".." 路径段；
 * - 文件名白名单清洗并强制 .json 扩展名；保存前经解析器校验，拒绝非法行为文件。
 */
public final class BehaviorStorage {

    /** 单个行为文件的最大大小（防滥用） */
    public static final int MAX_JSON_LENGTH = 512 * 1024;

    private BehaviorStorage() {
    }

    /**
     * 保存行为文件
     *
     * @param fileName 目标文件名（自动清洗并补 .json）
     * @param json     行为 JSON 原文（须可被解析器接受）
     * @param dir      目标目录（空=默认行为目录；可绝对路径）
     * @return 错误消息；null = 成功
     */
    public static String save(String fileName, String json, String dir) {
        if (json == null || json.length() > MAX_JSON_LENGTH) {
            return "内容过长或为空";
        }
        String cleanName = sanitizeName(fileName);
        // 保存前先校验可解析，避免写出坏文件（StringReader 无需关闭）
        try {
            BehaviorParser.parse(cleanName, new java.io.StringReader(json));
        } catch (BehaviorParseException e) {
            return "行为校验失败: " + e.getMessage();
        }
        Path targetDir;
        boolean isDefault;
        if (dir == null || dir.isBlank()) {
            targetDir = BehaviorManager.behaviorDir();
            isDefault = true;
        } else {
            String error = validateDir(dir);
            if (error != null) {
                return error;
            }
            Path p = Path.of(dir.trim());
            targetDir = p.isAbsolute() ? p : FabricLoader.getInstance().getGameDir().resolve(p);
            isDefault = false;
        }
        try {
            Files.createDirectories(targetDir);
            Files.writeString(targetDir.resolve(cleanName), json, StandardCharsets.UTF_8);
        } catch (IOException | java.nio.file.InvalidPathException e) {
            MyBotMod.LOGGER.warn("[行为] 保存行为文件失败 {}: {}", cleanName, e.getMessage());
            return "写入失败: " + e.getMessage();
        }
        MyBotMod.LOGGER.info("[行为] 已保存行为文件 {}", targetDir.resolve(cleanName));
        if (isDefault) {
            // 默认目录：立即重载使新行为可用
            BehaviorManager.reload();
        }
        return null;
    }

    /**
     * 读取默认行为目录中的行为文件原文（编辑器"打开已有行为"用）
     *
     * @return 文件内容；不存在返回 null
     */
    public static String readSource(String fileName) {
        String cleanName = sanitizeName(fileName);
        Path file = BehaviorManager.behaviorDir().resolve(cleanName);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            return content.length() > MAX_JSON_LENGTH ? content.substring(0, MAX_JSON_LENGTH) : content;
        } catch (IOException e) {
            MyBotMod.LOGGER.warn("[行为] 读取行为文件失败 {}: {}", cleanName, e.getMessage());
            return null;
        }
    }

    /** 文件名清洗：仅保留字母/数字/下划线/连字符/中文，强制 .json 扩展名 */
    static String sanitizeName(String raw) {
        String base = raw == null ? "" : raw.replaceAll("[^\\w\\-\\u4e00-\\u9fa5.]", "_");
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        if (base.isEmpty() || base.chars().allMatch(ch -> ch == '_' || ch == '.')) {
            base = "behavior";
        }
        if (base.length() > 64) {
            base = base.substring(0, 64);
        }
        return base + ".json";
    }

    /** 目录校验：拒绝含 ".." 的路径段 @return 错误消息；null = 通过 */
    private static String validateDir(String dir) {
        String trimmed = dir.trim();
        if (trimmed.length() > 512) {
            return "目录路径过长";
        }
        for (String segment : trimmed.replace('\\', '/').split("/")) {
            if (segment.equals("..")) {
                return "目录不允许包含 \"..\"";
            }
        }
        return null;
    }
}
