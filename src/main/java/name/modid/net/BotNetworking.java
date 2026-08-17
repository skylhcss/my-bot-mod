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

    //? if >=1.20.5 {
    /*// ===== 1.20.5+ ：Fabric 改用 CustomPacketPayload，不再支持 ResourceLocation 通道 =====

    // 通用负载：携带已序列化的原始缓冲区（复用下方既有的 buf 读写逻辑）
    // 记录自动生成的 type() 访问器（返回 Type<RawPayload>）即以协变满足接口要求
    public record RawPayload(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> type, FriendlyByteBuf data)
            implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    }

    private static ResourceLocation rl(String path) {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath("my-bot-mod", path);
        //?} else {
        return new ResourceLocation("my-bot-mod", path);
        //?}
    }

    private static net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> type(String path) {
        return new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(rl(path));
    }

    private static net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RawPayload> codec(
            net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> type) {
        return net.minecraft.network.codec.StreamCodec.of(
            (buf, payload) -> buf.writeBytes(payload.data()),
            buf -> {
                // 拷贝到堆内存缓冲区：网络 buf 归 Netty 池管理，处理器异步读取不能持有其切片
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new RawPayload(type, new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(bytes)));
            }
        );
    }

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> OPEN_BOT_PANEL_TYPE = type("open_bot_panel");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> BOT_LIST_TYPE = type("bot_list");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> UPDATE_SETTING_TYPE = type("update_bot_setting");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> REQUEST_BOT_LIST_TYPE = type("request_bot_list");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> BATON_ACTION_TYPE = type("baton_action");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> BOT_LIST_UPDATE_TYPE = type("bot_list_update");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> BOT_SKIN_TYPE = type("bot_skin");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> REQUEST_BEHAVIOR_LIST_TYPE = type("request_behavior_list");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> BEHAVIOR_COMMAND_TYPE = type("behavior_command");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> BEHAVIOR_LIST_TYPE = type("behavior_list");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> BEHAVIOR_SAVE_TYPE = type("behavior_save");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> BEHAVIOR_SOURCE_REQUEST_TYPE = type("behavior_source_request");
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> BEHAVIOR_SOURCE_TYPE = type("behavior_source");

    // 负载类型必须在注册接收器前注册；S2C 与 C2S 分别注册到对应方向
    // 幂等：单人游戏中服务端与客户端初始化都会调用，重复注册会抛异常
    private static boolean payloadTypesRegistered;
    public static void registerPayloadTypes() {
        if (payloadTypesRegistered) return;
        payloadTypesRegistered = true;
        var s2c = net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C();
        s2c.register(OPEN_BOT_PANEL_TYPE, codec(OPEN_BOT_PANEL_TYPE));
        s2c.register(BOT_LIST_TYPE, codec(BOT_LIST_TYPE));
        s2c.register(BOT_LIST_UPDATE_TYPE, codec(BOT_LIST_UPDATE_TYPE));
        s2c.register(BOT_SKIN_TYPE, codec(BOT_SKIN_TYPE));
        s2c.register(BEHAVIOR_LIST_TYPE, codec(BEHAVIOR_LIST_TYPE));
        s2c.register(BEHAVIOR_SOURCE_TYPE, codec(BEHAVIOR_SOURCE_TYPE));
        var c2s = net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S();
        c2s.register(UPDATE_SETTING_TYPE, codec(UPDATE_SETTING_TYPE));
        c2s.register(REQUEST_BOT_LIST_TYPE, codec(REQUEST_BOT_LIST_TYPE));
        c2s.register(BATON_ACTION_TYPE, codec(BATON_ACTION_TYPE));
        c2s.register(REQUEST_BEHAVIOR_LIST_TYPE, codec(REQUEST_BEHAVIOR_LIST_TYPE));
        c2s.register(BEHAVIOR_COMMAND_TYPE, codec(BEHAVIOR_COMMAND_TYPE));
        c2s.register(BEHAVIOR_SAVE_TYPE, codec(BEHAVIOR_SAVE_TYPE));
        c2s.register(BEHAVIOR_SOURCE_REQUEST_TYPE, codec(BEHAVIOR_SOURCE_REQUEST_TYPE));
    }
    *///?} else {

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

    /** C2S：请求行为列表（附指定假人的播放列表状态） */
    public static final ResourceLocation REQUEST_BEHAVIOR_LIST =
        new ResourceLocation("my-bot-mod", "request_behavior_list");

    /** C2S：行为指令（0=分配 1=移除 2=启动 3=停止 4=重扫 5=上移 6=快速执行单个 7=下移 8=清空列表） */
    public static final ResourceLocation BEHAVIOR_COMMAND =
        new ResourceLocation("my-bot-mod", "behavior_command");

    /** S2C：行为列表（可用行为 + 指定假人状态 + 解析错误 + 运行进度） */
    public static final ResourceLocation BEHAVIOR_LIST =
        new ResourceLocation("my-bot-mod", "behavior_list");

    /** C2S：保存行为文件（游戏内编辑器导出：JSON原文 + 文件名 + 目标目录） */
    public static final ResourceLocation BEHAVIOR_SAVE =
        new ResourceLocation("my-bot-mod", "behavior_save");

    /** C2S：请求行为文件原文（编辑器打开已有行为） */
    public static final ResourceLocation BEHAVIOR_SOURCE_REQUEST =
        new ResourceLocation("my-bot-mod", "behavior_source_request");

    /** S2C：行为文件原文（文件名 + 内容，不存在时内容为空） */
    public static final ResourceLocation BEHAVIOR_SOURCE =
        new ResourceLocation("my-bot-mod", "behavior_source");
    //?}

    /** 网络协议版本（C2S 包携带并由服务端校验，防旧客户端/异常输入） */
    public static final int PROTOCOL_VERSION = 1;
    private static final int MAX_NAME_LEN = 16;
    private static final int MAX_KEY_LEN = 32;
    private static final int MAX_BEHAVIOR_LEN = 128;
    private static final int MAX_BEHAVIOR_JSON_LEN = name.modid.behavior.BehaviorStorage.MAX_JSON_LENGTH;
    private static final int MAX_DIR_LEN = 512;

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
        //? if >=1.20.5 {
        /*registerPayloadTypes();
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_SETTING_TYPE,
            (payload, ctx) -> handleUpdateSetting(ctx.server(), ctx.player(), payload.data()));
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_BOT_LIST_TYPE,
            (payload, ctx) -> handleRequestBotList(ctx.server(), ctx.player(), payload.data()));
        ServerPlayNetworking.registerGlobalReceiver(BATON_ACTION_TYPE,
            (payload, ctx) -> handleBatonActionPacket(ctx.server(), ctx.player(), payload.data()));
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_BEHAVIOR_LIST_TYPE,
            (payload, ctx) -> handleRequestBehaviorList(ctx.server(), ctx.player(), payload.data()));
        ServerPlayNetworking.registerGlobalReceiver(BEHAVIOR_COMMAND_TYPE,
            (payload, ctx) -> handleBehaviorCommandPacket(ctx.server(), ctx.player(), payload.data()));
        ServerPlayNetworking.registerGlobalReceiver(BEHAVIOR_SAVE_TYPE,
            (payload, ctx) -> handleBehaviorSave(ctx.server(), ctx.player(), payload.data()));
        ServerPlayNetworking.registerGlobalReceiver(BEHAVIOR_SOURCE_REQUEST_TYPE,
            (payload, ctx) -> handleBehaviorSourceRequest(ctx.server(), ctx.player(), payload.data()));
        *///?} else {
        // 更新假人个人配置
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_SETTING,
            (server, player, handler, buf, responseSender) -> handleUpdateSetting(server, player, buf));

        // 请求假人列表
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_BOT_LIST,
            (server, player, handler, buf, responseSender) -> handleRequestBotList(server, player, buf));

        // 指挥棒下令（寻路/传送）
        ServerPlayNetworking.registerGlobalReceiver(BATON_ACTION,
            (server, player, handler, buf, responseSender) -> handleBatonActionPacket(server, player, buf));

        // 请求行为列表
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_BEHAVIOR_LIST,
            (server, player, handler, buf, responseSender) -> handleRequestBehaviorList(server, player, buf));

        // 行为指令（分配/移除/启动/停止/重扫/上移/快速执行/下移）
        ServerPlayNetworking.registerGlobalReceiver(BEHAVIOR_COMMAND,
            (server, player, handler, buf, responseSender) -> handleBehaviorCommandPacket(server, player, buf));

        // 保存行为文件（游戏内编辑器导出）
        ServerPlayNetworking.registerGlobalReceiver(BEHAVIOR_SAVE,
            (server, player, handler, buf, responseSender) -> handleBehaviorSave(server, player, buf));

        // 请求行为文件原文（编辑器打开已有行为）
        ServerPlayNetworking.registerGlobalReceiver(BEHAVIOR_SOURCE_REQUEST,
            (server, player, handler, buf, responseSender) -> handleBehaviorSourceRequest(server, player, buf));
        //?}
    }

    private static void handleUpdateSetting(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf) {
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
    }

    private static void handleRequestBotList(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf) {
        if (badVersion(buf)) return;
        server.execute(() -> sendBotList(player));
    }

    private static void handleBatonActionPacket(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf) {
        if (badVersion(buf)) return;
        int actionType = buf.readVarInt();
        String botName = buf.readUtf(MAX_NAME_LEN);
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        server.execute(() -> handleBatonAction(player, actionType, botName, x, y, z));
    }

    private static void handleRequestBehaviorList(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf) {
        if (badVersion(buf)) return;
        String botName = buf.readUtf(MAX_NAME_LEN);
        server.execute(() -> sendBehaviorList(player, botName));
    }

    private static void handleBehaviorCommandPacket(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf) {
        if (badVersion(buf)) return;
        int action = buf.readVarInt();
        String botName = buf.readUtf(MAX_NAME_LEN);
        String behaviorName = buf.readUtf(MAX_BEHAVIOR_LEN);
        server.execute(() -> handleBehaviorCommand(player, action, botName, behaviorName));
    }

    private static void handleBehaviorSave(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf) {
        if (badVersion(buf)) return;
        String fileName = buf.readUtf(MAX_BEHAVIOR_LEN);
        String dir = buf.readUtf(MAX_DIR_LEN);
        String json = buf.readUtf(MAX_BEHAVIOR_JSON_LEN);
        server.execute(() -> {
            if (!player.hasPermissions(2)
                    && !name.modid.config.ModConfig.getInstance().allowNonOpControlBot) {
                return;
            }
            String error = name.modid.behavior.BehaviorStorage.save(fileName, json, dir);
            if (error == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "msg.my-bot-mod.behavior.save.ok", fileName));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "msg.my-bot-mod.behavior.save.fail", error));
            }
        });
    }

    private static void handleBehaviorSourceRequest(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf) {
        if (badVersion(buf)) return;
        String fileName = buf.readUtf(MAX_BEHAVIOR_LEN);
        server.execute(() -> {
            if (!player.hasPermissions(2)
                    && !name.modid.config.ModConfig.getInstance().allowNonOpControlBot) {
                return;
            }
            String content = name.modid.behavior.BehaviorStorage.readSource(fileName);
            FriendlyByteBuf out = PacketByteBufs.create();
            out.writeUtf(fileName, MAX_BEHAVIOR_LEN);
            out.writeUtf(content == null ? "" : content, MAX_BEHAVIOR_JSON_LEN);
            sendBehaviorSourcePayload(player, out);
        });
    }

    // ==================== 版本分支发送辅助 ====================
    // 各通道负载的 buf 由上方逻辑构建，此处仅负责按版本选择传输方式

    //? if >=1.20.5 {
    /*private static void sendOpenPanelPayload(ServerPlayer player, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, new RawPayload(OPEN_BOT_PANEL_TYPE, buf));
    }
    private static void sendBotListPayload(ServerPlayer player, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, new RawPayload(BOT_LIST_TYPE, buf));
    }
    private static void sendBotListUpdatePayload(ServerPlayer player, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, new RawPayload(BOT_LIST_UPDATE_TYPE, buf));
    }
    private static void sendBehaviorListPayload(ServerPlayer player, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, new RawPayload(BEHAVIOR_LIST_TYPE, buf));
    }
    private static void sendBehaviorSourcePayload(ServerPlayer player, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, new RawPayload(BEHAVIOR_SOURCE_TYPE, buf));
    }
    *///?} else {
    private static void sendOpenPanelPayload(ServerPlayer player, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, OPEN_BOT_PANEL, buf);
    }
    private static void sendBotListPayload(ServerPlayer player, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, BOT_LIST, buf);
    }
    private static void sendBotListUpdatePayload(ServerPlayer player, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, BOT_LIST_UPDATE, buf);
    }
    private static void sendBehaviorListPayload(ServerPlayer player, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, BEHAVIOR_LIST, buf);
    }
    private static void sendBehaviorSourcePayload(ServerPlayer player, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, BEHAVIOR_SOURCE, buf);
    }
    //?}

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
        int minBuild;
        int maxBuild;
        //? if >=1.21.2 {
        /*minBuild = plevel.dimensionType().minY();
        maxBuild = minBuild + plevel.dimensionType().height();
        *///?} else {
        minBuild = plevel.getMinBuildHeight();
        maxBuild = plevel.getMaxBuildHeight();
        //?}
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !plevel.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(x, y, z))
                || y < minBuild - 64 || y > maxBuild + 64) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.my-bot-mod.baton.invalid_pos"));
            return;
        }
        BotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return;
        }

        // 指挥棒下令优先：暂停该假人正在运行的行为脚本，避免抢控制权
        name.modid.behavior.BehaviorManager.stop(bot);

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
            // 传送模式：任意游戏模式均可使用（OP/指挥棒权限校验已在上方完成）
            bot.getActionController().cancelPath();
            BotManager.teleportCrossLevel(bot, player.serverLevel(), x, y, z, player.getYRot(), 0.0F);
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
        sendOpenPanelPayload(player, buf);
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
        sendBotListPayload(player, buf);
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
            sendBotListUpdatePayload(player, buf);
        }
    }

    // ==================== 行为系统 ====================

    /**
     * 处理行为指令：0=分配 1=移除 2=启动 3=停止 4=重扫 5=上移 6=快速执行单个 7=下移；完成后回发最新列表
     */
    private static void handleBehaviorCommand(ServerPlayer player, int action, String botName, String behaviorName) {
        var config = name.modid.config.ModConfig.getInstance();
        if (!player.hasPermissions(2) && !config.allowNonOpControlBot) {
            return;
        }
        if (action == 4) {
            name.modid.behavior.BehaviorManager.reload();
            sendBehaviorList(player, botName);
            return;
        }
        BotPlayer bot = BotManager.getBot(botName);
        if (bot != null) {
            switch (action) {
                case 0 -> {
                    if (name.modid.behavior.BehaviorManager.assign(bot, behaviorName)) {
                        name.modid.bot.BotPersistenceManager.saveBot(bot);
                    }
                }
                case 1 -> {
                    if (name.modid.behavior.BehaviorManager.unassign(bot, behaviorName)) {
                        name.modid.bot.BotPersistenceManager.saveBot(bot);
                    }
                }
                case 2 -> name.modid.behavior.BehaviorManager.start(bot);
                case 3 -> name.modid.behavior.BehaviorManager.stop(bot);
                case 5 -> {
                    if (name.modid.behavior.BehaviorManager.moveUp(bot, behaviorName)) {
                        name.modid.bot.BotPersistenceManager.saveBot(bot);
                    }
                }
                case 6 -> name.modid.behavior.BehaviorManager.startSingle(bot, behaviorName);
                case 7 -> {
                    if (name.modid.behavior.BehaviorManager.moveDown(bot, behaviorName)) {
                        name.modid.bot.BotPersistenceManager.saveBot(bot);
                    }
                }
                case 8 -> {
                    if (name.modid.behavior.BehaviorManager.clearAssigned(bot)) {
                        name.modid.bot.BotPersistenceManager.saveBot(bot);
                    }
                }
                default -> {
                }
            }
        }
        sendBehaviorList(player, botName);
    }

    /**
     * 下发行为列表：可用行为（文件名/显示名/描述/块数/循环）+ 指定假人的播放列表与运行态 + 解析错误
     */
    public static void sendBehaviorList(ServerPlayer player, String botName) {
        var names = name.modid.behavior.BehaviorManager.getBehaviorNames();
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(names.size());
        for (String n : names) {
            var program = name.modid.behavior.BehaviorManager.getProgram(n);
            buf.writeUtf(n);
            buf.writeUtf(program == null ? "" : truncate(program.name, 100), MAX_BEHAVIOR_LEN);
            buf.writeUtf(program == null ? "" : truncate(program.description, 240), 256);
            buf.writeVarInt(program == null ? 0 : program.statementCount());
            buf.writeBoolean(program != null && program.loop);
        }
        // 指定假人的状态
        BotPlayer bot = botName.isEmpty() ? null : BotManager.getBot(botName);
        buf.writeUtf(botName);
        var assigned = bot == null ? java.util.List.<String>of()
            : name.modid.behavior.BehaviorManager.getAssigned(bot);
        buf.writeVarInt(assigned.size());
        for (String n : assigned) {
            buf.writeUtf(n);
        }
        buf.writeBoolean(bot != null && name.modid.behavior.BehaviorManager.isRunning(bot));
        // 运行进度：当前执行行为显示名 + 队列位置（未运行时为空/0）
        String current = bot == null ? null : name.modid.behavior.BehaviorManager.currentBehaviorName(bot);
        buf.writeUtf(current == null ? "" : truncate(current, 100), MAX_BEHAVIOR_LEN);
        int[] progress = bot == null ? null : name.modid.behavior.BehaviorManager.progress(bot);
        buf.writeVarInt(progress == null ? 0 : progress[0]);
        buf.writeVarInt(progress == null ? 0 : progress[1]);
        // 解析错误
        var errors = name.modid.behavior.BehaviorManager.getErrors();
        buf.writeVarInt(errors.size());
        for (var e : errors.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeUtf(truncate(e.getValue(), 480), 512);
        }
        sendBehaviorListPayload(player, buf);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
