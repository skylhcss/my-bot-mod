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

    /** S2C 列表数量上限：防异常/恶意服务端超大计数导致客户端无界分配 */
    private static final int MAX_LIST = 8192;

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

        // 注册 S2C 接收器（1.20.5+ 改用 CustomPacketPayload）
        //? if >=1.20.5 {
        /*BotNetworking.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.OPEN_BOT_PANEL_TYPE,
            (payload, ctx) -> handleOpenBotPanel(ctx.client(), payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BOT_LIST_TYPE,
            (payload, ctx) -> handleBotList(ctx.client(), payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BOT_LIST_UPDATE_TYPE,
            (payload, ctx) -> handleBotListUpdate(ctx.client(), payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BOT_SKIN_TYPE,
            (payload, ctx) -> handleBotSkin(ctx.client(), payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BEHAVIOR_LIST_TYPE,
            (payload, ctx) -> handleBehaviorList(ctx.client(), payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BEHAVIOR_SOURCE_TYPE,
            (payload, ctx) -> handleBehaviorSource(ctx.client(), payload.data()));
        *///?} else {
        // 注册 S2C：右键假人时打开设置面板
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.OPEN_BOT_PANEL,
            (client, handler, buf, responseSender) -> handleOpenBotPanel(client, buf));

        // 注册 S2C：假人列表同步
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BOT_LIST,
            (client, handler, buf, responseSender) -> handleBotList(client, buf));

        // 注册 S2C：假人列表增量更新（新增/移除）
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BOT_LIST_UPDATE,
            (client, handler, buf, responseSender) -> handleBotListUpdate(client, buf));

        // 注册 S2C：假人 PNG 皮肤映射（UUID -> 文件名）
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BOT_SKIN,
            (client, handler, buf, responseSender) -> handleBotSkin(client, buf));

        // 注册 S2C：行为列表（可用行为 + 指定假人状态 + 解析错误）
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BEHAVIOR_LIST,
            (client, handler, buf, responseSender) -> handleBehaviorList(client, buf));

        // 注册 S2C：行为文件原文（游戏内编辑器打开已有行为）
        ClientPlayNetworking.registerGlobalReceiver(BotNetworking.BEHAVIOR_SOURCE,
            (client, handler, buf, responseSender) -> handleBehaviorSource(client, buf));
        //?}

        // 断开连接时释放 PNG 皮肤动态纹理，避免跨存档累积；并重置指挥棒/假人列表/行为缓存
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> {
                BotSkinTextureLoader.clearCache();
                name.modid.client.baton.BatonClientState.reset();
                BotClientData.set(java.util.List.of());
                BehaviorClientData.set(new BehaviorClientData.State(
                    java.util.List.of(), "", java.util.List.of(), false, "", 0, 0, java.util.Map.of()));
            }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new ModernConfigScreen(null));
                }
            }
        });
    }

    // ==================== S2C 处理器（两种网络 API 共用） ====================

    private static void handleOpenBotPanel(net.minecraft.client.Minecraft client, net.minecraft.network.FriendlyByteBuf buf) {
        BotPanelData data = BotPanelData.read(buf);
        client.execute(() -> {
            if (client.level != null) {
                client.setScreen(new BotPanelScreen(data));
            }
        });
    }

    private static void handleBotList(net.minecraft.client.Minecraft client, net.minecraft.network.FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), MAX_LIST);
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
    }

    private static void handleBotListUpdate(net.minecraft.client.Minecraft client, net.minecraft.network.FriendlyByteBuf buf) {
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
    }

    private static void handleBotSkin(net.minecraft.client.Minecraft client, net.minecraft.network.FriendlyByteBuf buf) {
        java.util.UUID uuid = buf.readUUID();
        String png = buf.readUtf();
        client.execute(() -> BotSkinTextureLoader.setPngName(uuid, png));
    }

    private static void handleBehaviorList(net.minecraft.client.Minecraft client, net.minecraft.network.FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), MAX_LIST);
        java.util.List<BehaviorClientData.BehaviorEntry> behaviors = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            behaviors.add(new BehaviorClientData.BehaviorEntry(
                buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readBoolean()));
        }
        String botName = buf.readUtf();
        int assignedCount = Math.min(buf.readVarInt(), MAX_LIST);
        java.util.List<String> assigned = new java.util.ArrayList<>();
        for (int i = 0; i < assignedCount; i++) {
            assigned.add(buf.readUtf());
        }
        boolean running = buf.readBoolean();
        String current = buf.readUtf();
        int queueIndex = buf.readVarInt();
        int queueTotal = buf.readVarInt();
        int errorCount = Math.min(buf.readVarInt(), MAX_LIST);
        java.util.Map<String, String> errors = new java.util.LinkedHashMap<>();
        for (int i = 0; i < errorCount; i++) {
            errors.put(buf.readUtf(), buf.readUtf());
        }
        BehaviorClientData.State state = new BehaviorClientData.State(
            java.util.List.copyOf(behaviors), botName, java.util.List.copyOf(assigned), running,
            current, queueIndex, queueTotal,
            java.util.Collections.unmodifiableMap(errors));
        client.execute(() -> {
            BehaviorClientData.set(state);
            if (client.screen instanceof name.modid.client.screen.BotBehaviorScreen bs) {
                bs.refresh();
            }
        });
    }

    private static void handleBehaviorSource(net.minecraft.client.Minecraft client, net.minecraft.network.FriendlyByteBuf buf) {
        String fileName = buf.readUtf();
        String content = buf.readUtf(name.modid.behavior.BehaviorStorage.MAX_JSON_LENGTH);
        client.execute(() -> {
            if (client.screen instanceof name.modid.client.editor.BehaviorEditorScreen es) {
                es.loadSource(fileName, content);
            }
        });
    }
}
