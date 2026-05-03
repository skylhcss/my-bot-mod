package name.modid.client;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端初始化类
 * 注册快捷键和客户端事件
 */
public class MyBotModClient implements ClientModInitializer {
	
	// 配置菜单快捷键
	private static KeyMapping configMenuKey;
	
	@Override
	public void onInitializeClient() {
		// 加载配置
		ModConfig.getInstance();
		
		// 注册快捷键
		configMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.my-bot-mod.config_menu",
			GLFW.GLFW_KEY_B,
			"key.categories.my-bot-mod"
		));
		
		// 注册客户端 tick 事件
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// 检查快捷键是否被按下
			while (configMenuKey.consumeClick()) {
				// 打开现代化配置界面
				if (client.screen == null) {
					client.setScreen(new ModernConfigScreen(null));
				}
			}
		});
	}
}
