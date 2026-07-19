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
	
	// Carpet Mod 是否已加载
	private static boolean carpetModLoaded = false;

	@Override
	public void onInitialize() {
		// 模组初始化
		LOGGER.info("假人模组正在加载...");
		
		// 检测 Carpet Mod
		checkCarpetMod();

		// 初始化 run/skins 文件夹
		BotSkinManager.initializeSkinFolder();

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
			LOGGER.info("玩家 {} 加入，触发假人加载检查", handler.getPlayer().getName().getString());
			BotPersistenceManager.onPlayerJoin(server, handler.getPlayer());
			// 向加入的玩家下发当前假人列表
			BotNetworking.sendBotList(handler.getPlayer());
		});
		
		// 注册服务器 tick 事件，用于定期刷新区块加载票据
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("服务器启动完成，开始监控假人区块加载");
			// 重置驻留假人加载标记
			BotPersistenceManager.resetLoadedFlag(server);
		});
		
		// 注册服务器 tick 事件（每 100 tick 刷新一次区块票据）
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
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
	
	/**
	 * 检测 Carpet Mod 是否已加载
	 */
	private void checkCarpetMod() {
		try {
			// 尝试加载 Carpet Mod 的类
			Class.forName("carpet.CarpetServer");
			carpetModLoaded = true;
			LOGGER.warn("检测到 Carpet Mod 已加载！");
			
			var config = name.modid.config.ModConfig.getInstance();
			if (config.carpetModCompatibility) {
				LOGGER.warn("Carpet Mod 兼容模式已启用，假人功能将被禁用以避免冲突");
				LOGGER.warn("如果你想同时使用两个模组的假人功能，请在配置中禁用兼容模式");
				config.enableBotFeature = false;
				config.save();
			} else {
				LOGGER.warn("Carpet Mod 兼容模式已禁用，两个模组的假人功能可能会冲突");
				LOGGER.warn("建议：只使用其中一个模组的假人功能");
			}
		} catch (ClassNotFoundException e) {
			// Carpet Mod 未加载
			carpetModLoaded = false;
			LOGGER.info("未检测到 Carpet Mod");
		}
	}
	
	/**
	 * 检查 Carpet Mod 是否已加载
	 */
	public static boolean isCarpetModLoaded() {
		return carpetModLoaded;
	}
}