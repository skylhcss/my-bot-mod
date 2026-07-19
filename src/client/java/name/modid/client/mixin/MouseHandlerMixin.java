package name.modid.client.mixin;

import name.modid.client.baton.BatonClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端 Mixin：拦截鼠标滚轮。
 * 手持指挥棒且无界面打开时：
 * - Ctrl + 滚轮 切换模式
 * - Alt + 滚轮 切换选中的假人
 * 并取消默认滚轮行为（切换快捷栏），避免冲突。
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void myBotMod$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;
        if (vertical == 0) return;
        if (!BatonClientState.isHoldingBaton(mc.player)) return;

        int dir = vertical > 0 ? 1 : -1;
        if (Screen.hasControlDown()) {
            BatonClientState.cycleMode(dir);
            ci.cancel();
        } else if (Screen.hasAltDown()) {
            BatonClientState.cycleBot(dir);
            ci.cancel();
        }
    }
}
