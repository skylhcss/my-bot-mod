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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端初始化类
 * 注册快捷键和客户端事件，支持组合键（Ctrl/Shift/Alt + 主键）
 */
public class MyBotModClient implements ClientModInitializer {

    /** 上一帧主键是否按下，用于边沿检测（只在按下瞬间触发，避免长按连续打开） */
    private static boolean wasMainKeyPressed = false;

    @Override
    public void onInitializeClient() {
        ModConfig.getInstance();

        // 注册假人背包界面
        MenuScreens.register(ModMenus.BOT_INVENTORY, BotInventoryScreen::new);

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

        // 断开连接时释放 PNG 皮肤动态纹理，避免跨存档累积
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(BotSkinTextureLoader::clearCache));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.screen != null) {
                // 界面打开时重置边沿状态，避免关闭后立即重新触发
                wasMainKeyPressed = true;
                return;
            }

            if (checkKeybind()) {
                client.setScreen(new ModernConfigScreen(null));
            }
        });
    }

    /**
     * 检测快捷键是否触发
     * 统一处理单键和组合键，基于 GLFW 按键状态直接检测
     * 格式: "key.keyboard.b"             → 单键 B（无修饰键）
     *       "key.keyboard.ctrl+shift+b" → Ctrl+Shift+B
     *
     * 关键修复：
     *   1. 单键模式下严格要求无任何修饰键按下（Ctrl/Shift/Alt 都不能按）
     *   2. 始终读取 configMenuKey 配置，而非硬编码的 B 键
     *   3. 使用边沿检测，避免长按连续触发
     */
    private static boolean checkKeybind() {
        String keybind = ModConfig.getInstance().configMenuKey;
        if (keybind == null || !keybind.startsWith("key.keyboard.")) return false;

        String keyPart = keybind.substring("key.keyboard.".length());
        String[] parts = keyPart.split("\\+");

        boolean needCtrl = false, needShift = false, needAlt = false;
        int mainKey = -1;

        for (String part : parts) {
            switch (part.toLowerCase()) {
                case "ctrl" -> needCtrl = true;
                case "shift" -> needShift = true;
                case "alt" -> needAlt = true;
                default -> mainKey = nameToGlfw(part);
            }
        }

        if (mainKey < 0) return false;

        long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();

        // 检测主键当前是否按下
        boolean mainKeyPressed = GLFW.glfwGetKey(window, mainKey) == GLFW.GLFW_PRESS;

        // 边沿检测：只在按下瞬间触发（从 false 变为 true）
        if (!mainKeyPressed) {
            wasMainKeyPressed = false;
            return false;
        }
        if (wasMainKeyPressed) {
            return false; // 已在上一帧触发，不重复
        }
        wasMainKeyPressed = true;

        // 检测修饰键状态
        boolean ctrlDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                         || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean altDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                       || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

        // 精确匹配：配置的修饰键需求必须与实际按键状态完全一致
        // 单键（无修饰键）时，Ctrl/Shift/Alt 都不能按下
        return needCtrl == ctrlDown && needShift == shiftDown && needAlt == altDown;
    }

    private static int nameToGlfw(String name) {
        return switch (name.toLowerCase()) {
            case "a" -> GLFW.GLFW_KEY_A; case "b" -> GLFW.GLFW_KEY_B;
            case "c" -> GLFW.GLFW_KEY_C; case "d" -> GLFW.GLFW_KEY_D;
            case "e" -> GLFW.GLFW_KEY_E; case "f" -> GLFW.GLFW_KEY_F;
            case "g" -> GLFW.GLFW_KEY_G; case "h" -> GLFW.GLFW_KEY_H;
            case "i" -> GLFW.GLFW_KEY_I; case "j" -> GLFW.GLFW_KEY_J;
            case "k" -> GLFW.GLFW_KEY_K; case "l" -> GLFW.GLFW_KEY_L;
            case "m" -> GLFW.GLFW_KEY_M; case "n" -> GLFW.GLFW_KEY_N;
            case "o" -> GLFW.GLFW_KEY_O; case "p" -> GLFW.GLFW_KEY_P;
            case "q" -> GLFW.GLFW_KEY_Q; case "r" -> GLFW.GLFW_KEY_R;
            case "s" -> GLFW.GLFW_KEY_S; case "t" -> GLFW.GLFW_KEY_T;
            case "u" -> GLFW.GLFW_KEY_U; case "v" -> GLFW.GLFW_KEY_V;
            case "w" -> GLFW.GLFW_KEY_W; case "x" -> GLFW.GLFW_KEY_X;
            case "y" -> GLFW.GLFW_KEY_Y; case "z" -> GLFW.GLFW_KEY_Z;
            case "0" -> GLFW.GLFW_KEY_0; case "1" -> GLFW.GLFW_KEY_1;
            case "2" -> GLFW.GLFW_KEY_2; case "3" -> GLFW.GLFW_KEY_3;
            case "4" -> GLFW.GLFW_KEY_4; case "5" -> GLFW.GLFW_KEY_5;
            case "6" -> GLFW.GLFW_KEY_6; case "7" -> GLFW.GLFW_KEY_7;
            case "8" -> GLFW.GLFW_KEY_8; case "9" -> GLFW.GLFW_KEY_9;
            case "f1" -> GLFW.GLFW_KEY_F1; case "f2" -> GLFW.GLFW_KEY_F2;
            case "f3" -> GLFW.GLFW_KEY_F3; case "f4" -> GLFW.GLFW_KEY_F4;
            case "f5" -> GLFW.GLFW_KEY_F5; case "f6" -> GLFW.GLFW_KEY_F6;
            case "f7" -> GLFW.GLFW_KEY_F7; case "f8" -> GLFW.GLFW_KEY_F8;
            case "f9" -> GLFW.GLFW_KEY_F9; case "f10" -> GLFW.GLFW_KEY_F10;
            case "f11" -> GLFW.GLFW_KEY_F11; case "f12" -> GLFW.GLFW_KEY_F12;
            case "space" -> GLFW.GLFW_KEY_SPACE;
            case "enter" -> GLFW.GLFW_KEY_ENTER;
            case "tab" -> GLFW.GLFW_KEY_TAB;
            default -> -1;
        };
    }
}
