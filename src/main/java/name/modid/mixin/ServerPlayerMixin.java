package name.modid.mixin;

import name.modid.bot.BotPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 ServerPlayer 类
 * 用于在每个 tick 中应用假人的移动输入
 * 参考 Carpet Mod 的 ServerPlayer_actionPackMixin 实现
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    
    /**
     * 在 tick() 方法的开始处注入
     * 这是 Carpet Mod 调用 EntityPlayerActionPack.onUpdate() 的地方
     * 必须在 HEAD 位置，这样移动输入会在 super.tick() 处理移动之前被设置
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        
        // 只对假人应用移动输入
        if (player instanceof BotPlayer bot) {
            bot.getActionController().applyMovement();
        }
    }
}
