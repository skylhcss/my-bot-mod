package name.modid;

import name.modid.bot.BotPersistenceManager;
import name.modid.bot.BotSkinManager;
import name.modid.command.BotCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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

		// 注册命令
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BotCommand.register(dispatcher);
			name.modid.command.BotModCommand.register(dispatcher);
		});
		
		// 注册服务器生命周期事件
		// 服务器启动完成后加载假人
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("服务器启动完成，正在加载驻留假人...");
			BotPersistenceManager.loadAllBots(server);
		});
		
		// 服务器关闭前保存假人
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("服务器正在关闭，正在保存驻留假人...");
			BotPersistenceManager.saveAllBots();
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