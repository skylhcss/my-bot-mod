package name.modid.mixin;

import name.modid.bot.BotPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 到 ServerPlayer 类
 * 用于在每个 tick 中应用假人的移动输入和更新动作状态
 * 参考 Carpet Mod 的 ServerPlayer_actionPackMixin 实现
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    
    /**
     * 在 tick() 方法的开始处注入
     * 这是 Carpet Mod 调用 EntityPlayerActionPack.onUpdate() 的地方
     * 必须在 HEAD 位置，这样移动输入会在 super.tick() 处理移动之前被设置
     * 
     * 关键顺序：
     * 1. 先调用 tick() 更新动作状态（潜行、疾跑、跳跃等）
     * 2. 再调用 applyMovement() 应用移动输入
     * 3. 然后 ServerPlayer.tick() 继续执行，处理物理和移动
     * 
     * 这个顺序确保：
     * - 疾跑状态在移动处理前生效（影响速度）
     * - 跳跃状态在物理处理前设置（触发跳跃）
     * - 移动输入在每个 tick 都被更新（即使值为 0）
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        
        // 只对假人处理
        if (player instanceof BotPlayer bot) {
            // 异常兑底：此处位于 BotPlayer.tick 的保护之外，
            // 若不拦截，动作/寻路异常会沿 ServerLevel.tickNonPlayerEntity 上抛崩掉整个服务器 tick
            try {
                // 先更新动作状态（潜行、疾跑、跳跃等）
                bot.getActionController().tick();
                // 然后应用移动输入
                bot.getActionController().applyMovement();
            } catch (Exception e) {
                name.modid.MyBotMod.LOGGER.error("假人 {} 动作更新异常，已停止其全部动作: {}",
                    bot.getName().getString(), e.getMessage());
                try {
                    bot.getActionController().stopAll();
                } catch (Exception ignored) {
                    // 停止动作自身再异常时不再处理，等待下一 tick 重试
                }
            }
        }
    }
}
