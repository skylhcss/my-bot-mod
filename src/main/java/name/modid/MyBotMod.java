package name.modid;

import name.modid.bot.BotManager;
import name.modid.bot.BotPersistenceManager;
import name.modid.bot.BotPlayer;
import name.modid.bot.BotSkinManager;
import name.modid.command.BotCommand;
import name.modid.menu.ModMenus;
import name.modid.net.BotNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 假人模组主类
 * 提供类似 Carpet Mod 的假人功能
 */
public class MyBotMod implements ModInitializer {
	public static final String MOD_ID = "my-bot-mod";

	// 日志记录器，用于输出信息到控制台和日志文件
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 模组初始化
		LOGGER.info("假人模组正在加载...");

		// 初始化 run/skins 文件夹
		BotSkinManager.initializeSkinFolder();

		// 初始化行为系统（扫描 config/my-bot-mod/behaviors/）
		name.modid.behavior.BehaviorManager.init();

		// 释放行为编辑器到 config/my-bot-mod/editor/（编辑器随 JAR 打包，发布无需单独附带）
		name.modid.behavior.BehaviorEditorInstaller.install();

		// 玩家聊天 → 行为脚本 onChat 事件（假人自身的 say 为系统消息，不经过此事件，不会自触发）
		net.fabricmc.fabric.api.message.v1.ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			if (!(sender instanceof name.modid.bot.BotPlayer)) {
				name.modid.behavior.BehaviorManager.onPlayerChat(
					sender.getName().getString(), message.signedContent());
			}
		});

		// 注册物品（指挥棒）
		name.modid.item.ModItems.register();

		// 注册自定义容器菜单（假人背包）
		ModMenus.register();

		// 注册网络接收器（更新假人个人配置、请求假人列表）
		BotNetworking.registerServerReceivers();

		// 注册右键假人打开设置面板（服务端拦截实体交互，下发打开面板数据包）
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!level.isClientSide()
					&& hand == InteractionHand.MAIN_HAND
					&& entity instanceof BotPlayer bot
					&& player instanceof ServerPlayer serverPlayer) {
				var config = name.modid.config.ModConfig.getInstance();
				if (config.allowNonOpControlBot || serverPlayer.hasPermissions(2)) {
					BotNetworking.sendOpenPanel(serverPlayer, bot);
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});

		// 注册命令
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BotCommand.register(dispatcher);
			name.modid.command.BotModCommand.register(dispatcher);
		});
		
		// 注册玩家加入事件，用于加载假人
		// 参考 GCA：在第一个玩家加入时触发加载，而不是在服务器启动时
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			net.minecraft.server.level.ServerPlayer joined = handler.getPlayer();
			// 假人自身也走正规上线流程会触发本事件——过滤掉，避免向假连接发包/重复加载
			if (joined instanceof name.modid.bot.BotPlayer) {
				return;
			}
			LOGGER.info("玩家 {} 加入，触发假人加载检查", joined.getName().getString());
			BotPersistenceManager.onPlayerJoin(server, joined);
			// 向加入的玩家下发当前假人列表
			BotNetworking.sendBotList(joined);
		});
		
		// 注册服务器 tick 事件，用于定期刷新区块加载票据
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("服务器启动完成，开始监控假人区块加载");
			// 重置驻留假人加载标记
			BotPersistenceManager.resetLoadedFlag(server);
		});
		
		// 注册服务器 tick 事件（每 100 tick 刷新一次区块票据；每 tick 驱动行为脚本）
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			// 行为脚本解释器（内部无运行行为时为空操作）
			name.modid.behavior.BehaviorManager.tick(server);
			// 每 100 tick（5秒）刷新一次
			if (server.getTickCount() % 100 == 0) {
				BotPersistenceManager.refreshAllChunkTickets(server);
			}
		});
		
		// 服务器关闭前保存假人并清理区块票据
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("服务器正在关闭，正在保存驻留假人...");
			BotPersistenceManager.saveAllBots(server);
			BotPersistenceManager.clearAllChunkTickets(server);
			BotPersistenceManager.clearAllLoadedFlags();
			// 清理假人内存记录，确保下次启动时可以正确加载
			BotManager.clearAllBots();
		});

		LOGGER.info("假人模组加载完成！");
	}
}