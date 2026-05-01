package name.modid;

import name.modid.bot.BotSkinManager;
import name.modid.command.BotCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

		// 初始化 temporary 文件夹
		BotSkinManager.initializeTemporaryFolder();

		// 注册命令
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BotCommand.register(dispatcher);
		});

		LOGGER.info("假人模组加载完成！");
	}
}