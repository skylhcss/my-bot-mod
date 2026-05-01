package name.modid.client.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import name.modid.MyBotMod;
import name.modid.bot.BotSkinManager;
import name.modid.client.BotSkinTextureLoader;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * 客户端 Mixin：拦截玩家信息的皮肤纹理获取，支持 PNG 文件
 * 这个 Mixin 会在客户端渲染玩家时被调用
 */
@Mixin(PlayerInfo.class)
public class PlayerInfoMixin {
    
    @Shadow
    @Final
    private GameProfile profile;
    
    /**
     * 拦截 getSkinLocation 方法，处理 PNG 皮肤标记
     * 这个方法返回玩家皮肤的纹理位置
     */
    @Inject(method = "getSkinLocation", 
            at = @At("HEAD"), 
            cancellable = true)
    private void onGetSkinLocation(CallbackInfoReturnable<ResourceLocation> cir) {
        try {
            // 检查是否有纹理属性
            if (profile.getProperties().containsKey("textures")) {
                Property textureProperty = profile.getProperties().get("textures").iterator().next();
                String textureValue = textureProperty.getValue();
                
                // 检查是否是 PNG 皮肤标记
                if (BotSkinManager.isPngSkinMarker(textureValue)) {
                    String pngFileName = BotSkinManager.extractPngFileName(textureValue);
                    UUID playerUUID = profile.getId();
                    
                    // 使用客户端加载器加载 PNG 皮肤纹理
                    ResourceLocation skinLocation = BotSkinTextureLoader.loadPngSkinTexture(playerUUID, pngFileName);
                    
                    if (skinLocation != null) {
                        MyBotMod.LOGGER.debug("为假人 {} 应用 PNG 皮肤: {}", profile.getName(), pngFileName);
                        cir.setReturnValue(skinLocation);
                    }
                }
            }
        } catch (Exception e) {
            // 如果出错，让原方法继续执行
            MyBotMod.LOGGER.error("加载假人 PNG 皮肤时出错: {}", e.getMessage(), e);
        }
    }
}
