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

    /** S2C：假人列表增量更新（0=新增/更新，1=移除） */
    public static final ResourceLocation BOT_LIST_UPDATE =
        new ResourceLocation("my-bot-mod", "bot_list_update");

    /** S2C：假人 PNG 皮肤映射（UUID -> 文件名） */
    public static final ResourceLocation BOT_SKIN =
        new ResourceLocation("my-bot-mod", "bot_skin");

    /** 网络协议版本（C2S 包携带并由服务端校验，防旧客户端/异常输入） */
    public static final int PROTOCOL_VERSION = 1;
    private static final int MAX_NAME_LEN = 16;
    private static final int MAX_KEY_LEN = 32;

    /** 创建带协议版本前缀的 C2S 缓冲（客户端发送 C2S 包时使用） */
    public static FriendlyByteBuf c2s() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(PROTOCOL_VERSION);
        return buf;
    }

    /** 读取并校验协议版本；不匹配返回 true（应丢弃该包） */
    private static boolean badVersion(FriendlyByteBuf buf) {
        return buf.readableBytes() < 1 || buf.readVarInt() != PROTOCOL_VERSION;
    }

    /**
     * 注册服务端接收器（在主初始化中调用一次）
     */
    public static void registerServerReceivers() {
        // 更新假人个人配置
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_SETTING,
            (server, player, handler, buf, responseSender) -> {
                if (badVersion(buf)) return;
                String botName = buf.readUtf(MAX_NAME_LEN);
                String key = buf.readUtf(MAX_KEY_LEN);
                int stateId = buf.readVarInt();
                server.execute(() -> {
                    if (!player.hasPermissions(2) && !name.modid.config.ModConfig.getInstance().allowNonOpControlBot) {
                        return;
                    }
                    BotPlayer bot = BotManager.getBot(botName);
                    if (bot != null) {
                        bot.getSettings().set(key, BotSettings.Override.byId(stateId));
                        // 即时应用外观类设置（发光）
                        bot.setGlowingTag(BotSettings.resolve(bot.getSettings().glowing,
                            name.modid.config.ModConfig.getInstance().botGlowing));
                        BotPersistenceManager.saveBot(bot);
                    }
                });
            });

        // 请求假人列表
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_BOT_LIST,
            (server, player, handler, buf, responseSender) -> {
                if (badVersion(buf)) return;
                server.execute(() -> sendBotList(player));
            });

        // 指挥棒下令（寻路/传送）
        ServerPlayNetworking.registerGlobalReceiver(BATON_ACTION,
            (server, player, handler, buf, responseSender) -> {
                if (badVersion(buf)) return;
                int actionType = buf.readVarInt();
                String botName = buf.readUtf(MAX_NAME_LEN);
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
        if (!player.hasPermissions(2) && (!config.allowNonOpControlBot || config.batonRequiresOp)) {
            return;
        }
        // 校验坐标：拒绝 NaN/Infinity、世界边界外、以及远超建筑高度的 y（防恶意/异常客户端触发寻路死循环或在未加载区块操作）
        net.minecraft.server.level.ServerLevel plevel = player.serverLevel();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !plevel.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(x, y, z))
                || y < plevel.getMinBuildHeight() - 64 || y > plevel.getMaxBuildHeight() + 64) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.my-bot-mod.baton.invalid_pos"));
            return;
        }
        BotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return;
        }

        if (actionType == 0) {
            // 寻路模式：禁止跨维度寻路——假人不在指挥者所在维度时直接拒绝
            if (!bot.level().dimension().equals(player.level().dimension())) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.my-bot-mod.baton.cross_dim"));
                return;
            }
            bot.getActionController().cancelPath();
            boolean ok = bot.getActionController().pathTo(net.minecraft.core.BlockPos.containing(x, y, z));
            if (ok) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "msg.my-bot-mod.baton.pathfind_ok", botName, (int) x, (int) y, (int) z), true);
                player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.6F);
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.my-bot-mod.baton.pathfind_fail"));
            }
        } else {
            // 传送模式：默认仅指挥者处于创造模式可用，配置可放开到其他模式
            boolean creative = player.gameMode.getGameModeForPlayer().isCreative();
            if (!creative && !config.allowBatonTeleportNonCreative) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.my-bot-mod.baton.tp_creative_only"));
                return;
            }
            bot.getActionController().cancelPath();
            bot.teleportTo(player.serverLevel(), x, y, z, player.getYRot(), 0.0F);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "msg.my-bot-mod.baton.teleport_ok", botName, (int) x, (int) y, (int) z), true);
            player.playNotifySound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.4F);
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

    /** 增量广播“新增/更新”一个假人（创建时调用，替代全量广播） */
    public static void broadcastBotAdded(MinecraftServer server, BotPlayer bot) {
        if (server == null) return;
        String name = bot.getName().getString();
        String dim = bot.level().dimension().location().toString();
        broadcast(server, b -> { b.writeVarInt(0); b.writeUtf(name); b.writeUtf(dim); });
    }

    /** 增量广播“移除”一个假人（删除时调用） */
    public static void broadcastBotRemoved(MinecraftServer server, String name) {
        if (server == null) return;
        broadcast(server, b -> { b.writeVarInt(1); b.writeUtf(name); });
    }

    private static void broadcast(MinecraftServer server, java.util.function.Consumer<FriendlyByteBuf> writer) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player instanceof BotPlayer) continue;
            FriendlyByteBuf buf = PacketByteBufs.create();
            writer.accept(buf);
            ServerPlayNetworking.send(player, BOT_LIST_UPDATE, buf);
        }
    }
}
