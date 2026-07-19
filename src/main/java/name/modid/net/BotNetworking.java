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

    /** C2S：指挥棒下令（0=寻路，1=传送） */
    public static final ResourceLocation BATON_ACTION =
        new ResourceLocation("my-bot-mod", "baton_action");

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
                    if (!player.hasPermissions(2) && !name.modid.config.ModConfig.getInstance().allowNonOpControlBot) {
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

        // 指挥棒下令（寻路/传送）
        ServerPlayNetworking.registerGlobalReceiver(BATON_ACTION,
            (server, player, handler, buf, responseSender) -> {
                int actionType = buf.readVarInt();
                String botName = buf.readUtf();
                double x = buf.readDouble();
                double y = buf.readDouble();
                double z = buf.readDouble();
                server.execute(() -> handleBatonAction(player, actionType, botName, x, y, z));
            });
    }

    /**
     * 处理指挥棒下令：校验权限后让假人寻路或传送到指定位置
     */
    private static void handleBatonAction(ServerPlayer player, int actionType, String botName,
                                          double x, double y, double z) {
        var config = name.modid.config.ModConfig.getInstance();
        if (!player.hasPermissions(2) && !config.allowNonOpControlBot) {
            return;
        }
        BotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c假人 " + botName + " 不存在"));
            return;
        }

        if (actionType == 0) {
            // 寻路模式：先停止旧寻路；若假人在其他维度，先传送到指挥者所在维度再寻路
            bot.getActionController().cancelPath();
            if (!bot.level().dimension().equals(player.level().dimension())) {
                bot.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(), bot.getYRot(), bot.getXRot());
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e假人在其他维度，已先传送到你所在维度再寻路"));
            }
            boolean ok = bot.getActionController().pathTo(net.minecraft.core.BlockPos.containing(x, y, z));
            if (!ok) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c无法找到通往目标的路径"));
            }
        } else {
            // 传送模式：默认仅指挥者处于创造模式可用，配置可放开到其他模式
            boolean creative = player.gameMode.getGameModeForPlayer().isCreative();
            if (!creative && !config.allowBatonTeleportNonCreative) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c传送模式默认仅创造模式可用（可在配置中开放 allowBatonTeleportNonCreative）"));
                return;
            }
            bot.getActionController().cancelPath();
            bot.teleportTo(player.serverLevel(), x, y, z, player.getYRot(), 0.0F);
        }
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
