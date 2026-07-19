package name.modid.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import name.modid.MyBotMod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 假人管理器
 * 负责管理所有假人的创建、删除和查询
 */
public class BotManager {
    
    private static final Map<String, BotPlayer> bots = new ConcurrentHashMap<>();
    private static final Map<UUID, BotPlayer> botsByUUID = new ConcurrentHashMap<>();

    /**
     * 创建并召唤一个假人
     * @param server 服务器实例
     * @param creator 创建者
     * @param botName 假人名字
     * @param position 召唤位置（null则使用创建者位置）
     * @param gameMode 游戏模式（null则使用创建者游戏模式）
     * @return 创建的假人，如果失败则返回null
     */
    public static BotPlayer createBot(MinecraftServer server, ServerPlayer creator, String botName, Vec3 position, GameType gameMode) {
        try {
            var config = name.modid.config.ModConfig.getInstance();
            
            // 检查假人功能是否启用
            if (!config.enableBotFeature) {
                return null;
            }
            
            // 检查假人数量限制
            if (config.maxBotCount > 0 && bots.size() >= config.maxBotCount) {
                return null;
            }
            
            // 验证假人名字格式
            if (!isValidBotName(botName)) {
                return null;
            }
            
            // 检查假人名字是否已存在
            if (bots.containsKey(botName.toLowerCase())) {
                return null;
            }

            // 检查是否与真实玩家重名
            if (server.getPlayerList().getPlayerByName(botName) != null) {
                return null;
            }

            // 创建游戏档案
            GameProfile profile = new GameProfile(UUID.randomUUID(), botName);
            
            // 应用皮肤
            BotSkinManager.applySkin(profile, botName);
            
            // 获取世界
            ServerLevel level = creator.serverLevel();
            
            // 创建假的网络连接
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            
            // 创建假人
            BotPlayer bot = new BotPlayer(server, level, profile, connection, creator);
            
            // 设置位置和旋转
            Vec3 spawnPos = position != null ? position : creator.position();
            float yaw = creator.getYRot();
            float pitch = creator.getXRot();
            bot.setPositionAndRotation(spawnPos, yaw, pitch);
            
            // 设置游戏模式
            GameType mode = gameMode != null ? gameMode : creator.gameMode.getGameModeForPlayer();
            bot.setGameMode(mode);
            
            // 规范注册：使用 PlayerList.placeNewPlayer 加入玩家列表（含 playersByUUID）、
            // 生成实体并下发玩家信息包，保证 getPlayer(UUID)、计分板等原版逻辑正常
            // 参考 Carpet Mod 的 EntityPlayerMPFake 做法
            server.getPlayerList().placeNewPlayer(connection, bot);

            // placeNewPlayer 可能按存档/出生点重置位置，纠正到目标位置与朝向
            bot.teleportTo(level, spawnPos.x, spawnPos.y, spawnPos.z, yaw, pitch);
            bot.setYHeadRot(yaw);
            bot.setHealth(bot.getMaxHealth());

            // 发送头部旋转包，确保客户端正确渲染假人的头部朝向
            server.getPlayerList().broadcastAll(
                new net.minecraft.network.protocol.game.ClientboundRotateHeadPacket(bot, (byte) (yaw * 256 / 360)),
                level.dimension()
            );
            
            // 注册假人
            bots.put(botName.toLowerCase(), bot);
            botsByUUID.put(bot.getUUID(), bot);
            
            // 异步获取 Mojang 正版皮肤（避免阻塞服务器线程），成功后刷新客户端显示
            BotSkinManager.fetchMojangSkinAsync(server, bot, botName);
            
            // 向客户端广播更新后的假人列表
            name.modid.net.BotNetworking.broadcastBotList(server);
            
            // 如果启用了驻留功能，添加区块加载票据
            if (config.botPersistence) {
                BotPersistenceManager manager = BotPersistenceManager.get(server);
                if (manager != null) {
                    manager.addChunkTicket(server, botName, level, bot.blockPosition());
                }
            }
            
            return bot;
        } catch (Exception e) {
            MyBotMod.LOGGER.error("创建假人 {} 时发生错误: {}", botName, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 验证假人名字是否有效
     * Minecraft 玩家名规则：
     * - 长度 3-16 个字符
     * - 只包含字母、数字和下划线
     * @param name 假人名字
     * @return 是否有效
     */
    public static boolean isValidBotName(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) {
            return false;
        }
        // 只允许字母、数字和下划线
        return name.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * 移除假人
     * @param botName 假人名字
     * @return 是否成功移除
     */
    public static boolean removeBot(String botName) {
        BotPlayer bot = bots.remove(botName.toLowerCase());
        if (bot != null) {
            botsByUUID.remove(bot.getUUID());
            MinecraftServer server = bot.getServer();

            // 规范移除：触发连接断开，走原版 PlayerList.remove 完整清理
            // （保存数据、退出队伍、从 playersByUUID 移除、从世界移除实体、广播移除信息包）
            bot.connection.onDisconnect(net.minecraft.network.chat.Component.literal("假人已移除"));

            // 删除驻留数据
            BotPersistenceManager.deleteBot(server, botName);

            // 向客户端广播更新后的假人列表
            name.modid.net.BotNetworking.broadcastBotList(server);

            return true;
        }
        return false;
    }
    
    /**
     * 根据 UUID 移除假人（用于死亡事件）
     * @param uuid 假人 UUID
     * @return 是否成功移除
     */
    public static boolean removeBotByUUID(UUID uuid) {
        BotPlayer bot = botsByUUID.get(uuid);
        if (bot != null) {
            return removeBot(bot.getName().getString());
        }
        return false;
    }

    /**
     * 根据名字获取假人
     */
    public static BotPlayer getBot(String botName) {
        return bots.get(botName.toLowerCase());
    }

    /**
     * 根据UUID获取假人
     */
    public static BotPlayer getBot(UUID uuid) {
        return botsByUUID.get(uuid);
    }

    /**
     * 获取所有假人
     */
    public static Collection<BotPlayer> getAllBots() {
        return bots.values();
    }

    /**
     * 获取指定玩家创建的所有假人
     */
    public static List<BotPlayer> getBotsByCreator(ServerPlayer creator) {
        List<BotPlayer> result = new ArrayList<>();
        for (BotPlayer bot : bots.values()) {
            if (bot.getCreatorUUID().equals(creator.getUUID())) {
                result.add(bot);
            }
        }
        return result;
    }

    /**
     * 检查假人是否存在
     * 不仅检查内存中的记录，还要验证假人是否真的存在于世界中
     */
    public static boolean hasBot(String botName) {
        BotPlayer bot = bots.get(botName.toLowerCase());
        if (bot == null) {
            return false;
        }
        
        // 检查假人是否已被移除
        if (bot.isRemoved()) {
            // 如果假人已被移除，清理内存中的记录
            bots.remove(botName.toLowerCase());
            botsByUUID.remove(bot.getUUID());
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理所有假人的内存记录
     * 在服务器关闭时调用，确保下次启动时可以正确加载驻留假人
     */
    public static void clearAllBots() {
        bots.clear();
        botsByUUID.clear();
        MyBotMod.LOGGER.info("清理了所有假人的内存记录");
    }

    /**
     * 移除所有假人
     */
    public static void removeAllBots() {
        for (BotPlayer bot : new ArrayList<>(bots.values())) {
            removeBot(bot.getName().getString());
        }
    }

    /**
     * 移除指定玩家创建的所有假人
     */
    public static void removeBotsByCreator(ServerPlayer creator) {
        List<BotPlayer> creatorBots = getBotsByCreator(creator);
        for (BotPlayer bot : creatorBots) {
            removeBot(bot.getName().getString());
        }
    }
}
