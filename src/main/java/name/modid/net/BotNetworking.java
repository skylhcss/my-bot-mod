package name.modid.net;

import name.modid.bot.BotManager;
import name.modid.bot.BotPersistenceManager;
import name.modid.bot.BotPlayer;
import name.modid.bot.BotSettings;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 假人模组网络通道
 * - S2C open_bot_panel：右键假人时下发假人快照，客户端打开设置面板
 * - S2C bot_list：下发假人列表（供全局配置界面的"假人"标签页）
 * - C2S update_bot_setting：客户端更新假人个人配置
 * - C2S request_bot_list：客户端请求刷新假人列表
 */
public class BotNetworking {

    /** S2C：打开假人设置面板 */
    public static final ResourceLocation OPEN_BOT_PANEL =
        new ResourceLocation("my-bot-mod", "open_bot_panel");

    /** S2C：假人列表 */
    public static final ResourceLocation BOT_LIST =
        new ResourceLocation("my-bot-mod", "bot_list");

    /** C2S：更新假人个人配置 */
    public static final ResourceLocation UPDATE_SETTING =
        new ResourceLocation("my-bot-mod", "update_bot_setting");

    /** C2S：请求刷新假人列表 */
    public static final ResourceLocation REQUEST_BOT_LIST =
        new ResourceLocation("my-bot-mod", "request_bot_list");

    /**
     * 注册服务端接收器（在主初始化中调用一次）
     */
    public static void registerServerReceivers() {
        // 更新假人个人配置
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_SETTING,
            (server, player, handler, buf, responseSender) -> {
                String botName = buf.readUtf();
                String key = buf.readUtf();
                int stateId = buf.readVarInt();
                server.execute(() -> {
                    if (!player.hasPermissions(2) && !name.modid.config.ModConfig.getInstance().allowNonOpCreateBot) {
                        return;
                    }
                    BotPlayer bot = BotManager.getBot(botName);
                    if (bot != null) {
                        bot.getSettings().set(key, BotSettings.Override.byId(stateId));
                        BotPersistenceManager.saveBot(bot);
                    }
                });
            });

        // 请求假人列表
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_BOT_LIST,
            (server, player, handler, buf, responseSender) -> server.execute(() -> sendBotList(player)));
    }

    /**
     * 向指定玩家下发"打开假人面板"数据包
     */
    public static void sendOpenPanel(ServerPlayer player, BotPlayer bot) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        BotPanelData.fromBot(bot).write(buf);
        ServerPlayNetworking.send(player, OPEN_BOT_PANEL, buf);
    }

    /**
     * 向指定玩家下发假人列表（名字 + 维度）
     */
    public static void sendBotList(ServerPlayer player) {
        var bots = BotManager.getAllBots();
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(bots.size());
        for (BotPlayer bot : bots) {
            buf.writeUtf(bot.getName().getString());
            buf.writeUtf(bot.level().dimension().location().toString());
        }
        ServerPlayNetworking.send(player, BOT_LIST, buf);
    }

    /**
     * 向所有真实玩家广播假人列表（假人创建/删除时调用）
     */
    public static void broadcastBotList(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player instanceof BotPlayer) {
                continue; // 不发给假人自己
            }
            sendBotList(player);
        }
    }
}
