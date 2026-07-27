package name.modid.behavior;

import name.modid.MyBotMod;
import name.modid.bot.BotPlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 行为管理器（服务端单例）
 *
 * - 扫描 config/my-bot-mod/behaviors/ 下的 .json 行为文件并解析缓存（含错误列表）
 * - 维护每假人的行为播放列表（多个行为按顺序执行）与运行状态
 * - 由服务器 END_SERVER_TICK 驱动所有运行中的 {@link BehaviorRuntime}
 */
public final class BehaviorManager {

    /** 已解析的行为：文件名 → 程序（LinkedHashMap 保持文件名序） */
    private static final Map<String, BehaviorProgram> BEHAVIORS = new LinkedHashMap<>();
    /** 解析失败的行为：文件名 → 错误消息 */
    private static final Map<String, String> ERRORS = new LinkedHashMap<>();
    /** 每假人已分配的行为播放列表（按执行顺序） */
    private static final Map<UUID, List<String>> ASSIGNED = new ConcurrentHashMap<>();
    /** 每假人的运行状态 */
    private static final Map<UUID, Active> RUNNING = new ConcurrentHashMap<>();

    /** 运行状态：当前队列位置 + 解释器 */
    private static final class Active {
        final BotPlayer bot;
        int queueIndex;
        BehaviorRuntime runtime;

        Active(BotPlayer bot, int queueIndex, BehaviorRuntime runtime) {
            this.bot = bot;
            this.queueIndex = queueIndex;
            this.runtime = runtime;
        }
    }

    private BehaviorManager() {
    }

    /** 行为文件夹：<gameDir>/config/my-bot-mod/behaviors */
    public static Path behaviorDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("my-bot-mod").resolve("behaviors");
    }

    /** 初始化：建目录 + 首次扫描（模组加载时调用一次） */
    public static void init() {
        try {
            Files.createDirectories(behaviorDir());
        } catch (IOException e) {
            MyBotMod.LOGGER.warn("[行为] 无法创建行为文件夹: {}", e.getMessage());
        }
        reload();
    }

    /** 重新扫描行为文件夹（同步，文件数量小） */
    public static synchronized void reload() {
        BEHAVIORS.clear();
        ERRORS.clear();
        Path dir = behaviorDir();
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                .sorted()
                .forEach(BehaviorManager::loadFile);
        } catch (IOException e) {
            MyBotMod.LOGGER.warn("[行为] 扫描行为文件夹失败: {}", e.getMessage());
        }
        MyBotMod.LOGGER.info("[行为] 已加载 {} 个行为，{} 个解析失败", BEHAVIORS.size(), ERRORS.size());
    }

    private static void loadFile(Path path) {
        String fileName = path.getFileName().toString();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            BEHAVIORS.put(fileName, BehaviorParser.parse(fileName, reader));
        } catch (BehaviorParseException e) {
            ERRORS.put(fileName, e.getMessage());
            MyBotMod.LOGGER.warn("[行为] {}", e.getMessage());
        } catch (IOException e) {
            ERRORS.put(fileName, fileName + ": 读取失败 - " + e.getMessage());
            MyBotMod.LOGGER.warn("[行为] 读取 {} 失败: {}", fileName, e.getMessage());
        }
    }

    // ==================== 查询 ====================

    public static List<String> getBehaviorNames() {
        synchronized (BehaviorManager.class) {
            return new ArrayList<>(BEHAVIORS.keySet());
        }
    }

    public static BehaviorProgram getProgram(String fileName) {
        synchronized (BehaviorManager.class) {
            return BEHAVIORS.get(fileName);
        }
    }

    public static Map<String, String> getErrors() {
        synchronized (BehaviorManager.class) {
            return new LinkedHashMap<>(ERRORS);
        }
    }

    public static List<String> getAssigned(BotPlayer bot) {
        return new ArrayList<>(ASSIGNED.getOrDefault(bot.getUUID(), List.of()));
    }

    public static boolean isRunning(BotPlayer bot) {
        return RUNNING.containsKey(bot.getUUID());
    }

    /** 当前正在执行的行为显示名（未运行返回 null） */
    public static String currentBehaviorName(BotPlayer bot) {
        Active active = RUNNING.get(bot.getUUID());
        return active != null && active.runtime != null ? active.runtime.getProgram().name : null;
    }

    // ==================== 播放列表管理 ====================

    /** @return 是否成功（行为存在且未重复分配） */
    public static boolean assign(BotPlayer bot, String fileName) {
        if (getProgram(fileName) == null) {
            return false;
        }
        List<String> list = ASSIGNED.computeIfAbsent(bot.getUUID(), k -> new ArrayList<>());
        synchronized (list) {
            if (list.contains(fileName)) {
                return false;
            }
            list.add(fileName);
        }
        return true;
    }

    public static boolean unassign(BotPlayer bot, String fileName) {
        List<String> list = ASSIGNED.get(bot.getUUID());
        if (list == null) {
            return false;
        }
        synchronized (list) {
            return list.remove(fileName);
        }
    }

    /** 播放列表内上移一位 @return 是否移动成功 */
    public static boolean moveUp(BotPlayer bot, String fileName) {
        List<String> list = ASSIGNED.get(bot.getUUID());
        if (list == null) {
            return false;
        }
        synchronized (list) {
            int i = list.indexOf(fileName);
            if (i <= 0) {
                return false;
            }
            list.set(i, list.get(i - 1));
            list.set(i - 1, fileName);
            return true;
        }
    }

    /** 恢复播放列表（驻留恢复用，静默忽略已不存在的行为文件） */
    public static void restoreAssigned(BotPlayer bot, List<String> behaviors) {
        List<String> valid = new ArrayList<>();
        for (String name : behaviors) {
            if (getProgram(name) != null) {
                valid.add(name);
            }
        }
        if (!valid.isEmpty()) {
            ASSIGNED.put(bot.getUUID(), valid);
        }
    }

    // ==================== 启停 ====================

    /** 从播放列表头开始执行 @return 是否成功启动 */
    public static boolean start(BotPlayer bot) {
        List<String> list = ASSIGNED.getOrDefault(bot.getUUID(), List.of());
        if (list.isEmpty()) {
            return false;
        }
        Active active = new Active(bot, 0, null);
        if (!advance(active)) {
            return false;
        }
        RUNNING.put(bot.getUUID(), active);
        return true;
    }

    /** 停止行为执行（保留播放列表分配） */
    public static void stop(BotPlayer bot) {
        Active removed = RUNNING.remove(bot.getUUID());
        if (removed != null) {
            if (removed.runtime != null) {
                removed.runtime.closeIfOpen();
            }
            bot.getActionController().stopAll();
        }
    }

    /** 假人删除/移除时清理全部状态 */
    public static void onBotRemoved(UUID botUuid) {
        RUNNING.remove(botUuid);
        ASSIGNED.remove(botUuid);
    }

    /** 队列推进到下一个可解析的行为 @return false = 队列耗尽 */
    private static boolean advance(Active active) {
        List<String> list = ASSIGNED.getOrDefault(active.bot.getUUID(), List.of());
        while (active.queueIndex < list.size()) {
            BehaviorProgram program = getProgram(list.get(active.queueIndex));
            active.queueIndex++;
            if (program != null) {
                active.runtime = new BehaviorRuntime(active.bot, program);
                return true;
            }
        }
        return false;
    }

    /** 玩家聊天时分发给所有运行中的行为（onChat 事件触发器） */
    public static void onPlayerChat(String sender, String message) {
        if (RUNNING.isEmpty()) {
            return;
        }
        for (Active active : RUNNING.values()) {
            if (active.runtime != null && !active.runtime.isFinished()) {
                active.runtime.onChatMessage(sender, message);
            }
        }
    }

    // ==================== tick 驱动 ====================

    /** 每服务器 tick 调用：驱动所有运行中的行为 */
    public static void tick(MinecraftServer server) {
        if (RUNNING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Active>> it = RUNNING.entrySet().iterator();
        while (it.hasNext()) {
            Active active = it.next().getValue();
            BotPlayer bot = active.bot;
            if (bot.isRemoved() || bot.getServer() != server) {
                it.remove();
                continue;
            }
            active.runtime.tick();
            if (active.runtime.isFinished()) {
                // 当前行为结束：关掉其可能打开的容器，推进播放列表，耗尽则停止
                active.runtime.closeIfOpen();
                if (!advance(active)) {
                    it.remove();
                }
            }
        }
    }
}
