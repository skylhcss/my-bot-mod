package name.modid.client;

import name.modid.client.menu.BotInventoryScreen;
import name.modid.client.screen.BotPanelScreen;
import name.modid.client.screen.ModernConfigScreen;
import name.modid.config.ModConfig;
import name.modid.menu.ModMenus;
import name.modid.net.BotNetworking;
import name.modid.net.BotPanelData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端初始化类
 * 注册按键与客户端事件
 */
public class MyBotModClient implements ClientModInitializer {

    /** 打开配置菜单的按键（可在原版“控制”菜单中重绑定，默认 B） */
    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        ModConfig.getInstance();

        // 注册打开配置菜单的按键（进入原版“控制”菜单，可重绑定/检测冲突）
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.my-bot-mod.config_menu", GLFW.GLFW_KEY_B, "key.categories.my-bot-mod"));

        // 注册假人背包界面
        MenuScreens.register(ModMenus.BOT_INVENTORY, BotInventoryScreen::new);

        // 注册指挥棒输入回调（选人 / 下令）与手持 HUD
        name.modid.client.baton.BatonInputHandler.register();
        name.modid.client.baton.BatonHudOverlay.register();

        // 注册 S2C：右键假人时打开设置面板
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.OPEN_BOT_PANEL,
            (client, handler, buf, responseSender) -> {
                BotPanelData data = BotPanelData.read(buf);
                client.execute(() -> client.setScreen(new BotPanelScreen(data)));
            });

        // 注册 S2C：假人列表同步
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BOT_LIST,
            (client, handler, buf, responseSender) -> {
                int count = buf.readVarInt();
                java.util.List<BotClientData.Entry> list = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String name = buf.readUtf();
                    String dim = buf.readUtf();
                    list.add(new BotClientData.Entry(name, dim));
                }
                client.execute(() -> {
                    BotClientData.set(list);
                    if (client.screen instanceof name.modid.client.screen.ModernConfigScreen ms) {
                        ms.refreshCurrentPage();
                    }
                });
            });

        // 注册 S2C：假人列表增量更新（新增/移除）
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BOT_LIST_UPDATE,
            (client, handler, buf, responseSender) -> {
                int op = buf.readVarInt();
                if (op == 0) {
                    String name = buf.readUtf();
                    String dim = buf.readUtf();
                    client.execute(() -> {
                        BotClientData.addOrUpdate(new BotClientData.Entry(name, dim));
                        if (client.screen instanceof name.modid.client.screen.ModernConfigScreen ms) ms.refreshCurrentPage();
                    });
                } else {
                    String name = buf.readUtf();
                    client.execute(() -> {
                        BotClientData.remove(name);
                        if (client.screen instanceof name.modid.client.screen.ModernConfigScreen ms) ms.refreshCurrentPage();
                    });
                }
            });

        // 注册 S2C：假人 PNG 皮肤映射（UUID -> 文件名）
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BOT_SKIN,
            (client, handler, buf, responseSender) -> {
                java.util.UUID uuid = buf.readUUID();
                String png = buf.readUtf();
                client.execute(() -> BotSkinTextureLoader.setPngName(uuid, png));
            });

        // 注册 S2C：行为列表（可用行为 + 指定假人状态 + 解析错误）
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BEHAVIOR_LIST,
            (client, handler, buf, responseSender) -> {
                int count = buf.readVarInt();
                java.util.List<BehaviorClientData.BehaviorEntry> behaviors = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) {
                    behaviors.add(new BehaviorClientData.BehaviorEntry(
                        buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readBoolean()));
                }
                String botName = buf.readUtf();
                int assignedCount = buf.readVarInt();
                java.util.List<String> assigned = new java.util.ArrayList<>();
                for (int i = 0; i < assignedCount; i++) {
                    assigned.add(buf.readUtf());
                }
                boolean running = buf.readBoolean();
                int errorCount = buf.readVarInt();
                java.util.Map<String, String> errors = new java.util.LinkedHashMap<>();
                for (int i = 0; i < errorCount; i++) {
                    errors.put(buf.readUtf(), buf.readUtf());
                }
                BehaviorClientData.State state = new BehaviorClientData.State(
                    java.util.List.copyOf(behaviors), botName, java.util.List.copyOf(assigned), running,
                    java.util.Collections.unmodifiableMap(errors));
                client.execute(() -> {
                    BehaviorClientData.set(state);
                    if (client.screen instanceof name.modid.client.screen.BotBehaviorScreen bs) {
                        bs.refresh();
                    }
                });
            });

        // 断开连接时释放 PNG 皮肤动态纹理，避免跨存档累积；并重置指挥棒与行为缓存
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> {
                BotSkinTextureLoader.clearCache();
                name.modid.client.baton.BatonClientState.reset();
                BehaviorClientData.set(new BehaviorClientData.State(
                    java.util.List.of(), "", java.util.List.of(), false, java.util.Map.of()));
            }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new ModernConfigScreen(null));
                }
            }
        });
    }
}
