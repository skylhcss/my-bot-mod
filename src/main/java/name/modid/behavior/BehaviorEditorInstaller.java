package name.modid.behavior;

import name.modid.MyBotMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * 行为编辑器安装器
 *
 * 行为编辑器（HTML + 离线 Blockly 资源 + 示例）由 build.gradle 打包在 JAR 的
 * assets/my-bot-mod/editor/ 内，模组启动时释放到 config/my-bot-mod/editor/，
 * 修复发布包只含 JAR、用户拿不到编辑器的问题。
 *
 * 目标文件缺失或大小变化时才写入，日常启动近乎零开销，升级模组后自动更新。
 */
public final class BehaviorEditorInstaller {

    /** JAR 内编辑器资源目录 */
    private static final String EDITOR_RESOURCE_DIR = "assets/" + MyBotMod.MOD_ID + "/editor";

    private BehaviorEditorInstaller() {
    }

    /** 释放行为编辑器到 config/my-bot-mod/editor/（失败仅告警，不阻断模组加载） */
    public static void install() {
        try {
            Path target = FabricLoader.getInstance().getConfigDir()
                    .resolve(MyBotMod.MOD_ID).resolve("editor");
            FabricLoader.getInstance().getModContainer(MyBotMod.MOD_ID).ifPresent(container -> {
                int copied = 0;
                for (Path root : container.getRootPaths()) {
                    // 生产环境 root 为 JAR 内已打开的 zip 根目录（可当目录读）；
                    // 开发环境为资源目录；个别情况返回 JAR 文件本身时按 zip 打开兜底
                    if (Files.isDirectory(root)) {
                        copied += copyTree(root.resolve(EDITOR_RESOURCE_DIR), target);
                    } else {
                        copied += copyJarTree(root, target);
                    }
                }
                if (copied > 0) {
                    MyBotMod.LOGGER.info("[行为] 行为编辑器已释放（{} 个文件）: {}",
                            copied, target.resolve("behavior-editor.html"));
                }
            });
        } catch (Exception e) {
            MyBotMod.LOGGER.warn("[行为] 释放行为编辑器失败: {}", e.getMessage());
        }
    }

    /** JAR 文件兜底：以 zip 文件系统读取编辑器目录后复制 */
    private static int copyJarTree(Path jar, Path target) {
        try (FileSystem fs = FileSystems.newFileSystem(jar, (ClassLoader) null)) {
            return copyTree(fs.getPath(EDITOR_RESOURCE_DIR), target);
        } catch (IOException e) {
            MyBotMod.LOGGER.warn("[行为] 读取 JAR 内行为编辑器失败: {}", e.getMessage());
            return 0;
        }
    }

    /** 复制目录树：目标文件缺失或大小不同才覆盖，避免每次启动全量写入 */
    private static int copyTree(Path src, Path target) {
        if (!Files.isDirectory(src)) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(src)) {
            List<Path> list = files.filter(Files::isRegularFile).toList();
            int copied = 0;
            for (Path file : list) {
                Path dest = target.resolve(src.relativize(file).toString());
                if (Files.exists(dest) && Files.size(dest) == Files.size(file)) {
                    continue;
                }
                Files.createDirectories(dest.getParent());
                try (InputStream in = Files.newInputStream(file)) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                    copied++;
                }
            }
            return copied;
        } catch (IOException e) {
            MyBotMod.LOGGER.warn("[行为] 释放行为编辑器失败: {}", e.getMessage());
            return 0;
        }
    }
}
