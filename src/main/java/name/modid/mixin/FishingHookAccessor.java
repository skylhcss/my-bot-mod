package name.modid.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * FishingHook 访问器：读取私有字段 nibble（鱼咬钩倒计时）
 * nibble > 0 表示鱼正在咬钩、此刻收竿可钓起（行为系统"鱼上钩?"传感器使用）
 */
@Mixin(FishingHook.class)
public interface FishingHookAccessor {

    @Accessor("nibble")
    int myBotMod$getNibble();
}
