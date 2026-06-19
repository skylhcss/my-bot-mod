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
 * 注册快捷键和客户端事件，支持组合键（Ctrl/Shift/Alt + 主键）
 */
public class MyBotModClient implements ClientModInitializer {

    private static KeyMapping configMenuKey;

    @Override
    public void onInitializeClient() {
        ModConfig.getInstance();

        configMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.my-bot-mod.config_menu",
            GLFW.GLFW_KEY_B,
            "key.categories.my-bot-mod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.screen != null) return;

            // 优先检测组合键
            if (checkComboKeybind()) {
                client.setScreen(new ModernConfigScreen(null));
                return;
            }

            // 回退到 MC 原生 KeyMapping（单键）
            while (configMenuKey.consumeClick()) {
                client.setScreen(new ModernConfigScreen(null));
            }
        });
    }

    /**
     * 检测组合快捷键
     * 格式: "key.keyboard.ctrl+shift+b" → Ctrl+Shift+B
     *       "key.keyboard.b"             → 单键（交给 KeyMapping 处理）
     */
    private static boolean checkComboKeybind() {
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

        if (GLFW.glfwGetKey(window, mainKey) != GLFW.GLFW_PRESS) return false;

        boolean ctrlDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                         || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean altDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                       || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

        if (needCtrl != ctrlDown || needShift != shiftDown || needAlt != altDown) return false;

        // 纯单键时让 MC 原生 KeyMapping 处理
        if (!needCtrl && !needShift && !needAlt) return false;

        return true;
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
