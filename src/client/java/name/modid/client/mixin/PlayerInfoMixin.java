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
 * 1.20.1 拦截 getSkinLocation；1.20.2+ 皮肤系统重构为 PlayerSkin record，改为拦截 getSkin
 */
@Mixin(PlayerInfo.class)
public class PlayerInfoMixin {
    
    @Shadow
    @Final
    private GameProfile profile;
    
    /** 从档案 textures 属性解析 PNG 皮肤标记并加载本地纹理；无标记或加载失败返回 null */
    private ResourceLocation myBotMod$resolvePngSkin() {
        try {
            // 检查是否有纹理属性
            if (profile.getProperties().containsKey("textures")) {
                Property textureProperty = profile.getProperties().get("textures").iterator().next();
                // 1.20.2+ 的 authlib 5.x 中 Property 改为 record（value()）
                //? if >=1.20.2 {
                /*String textureValue = textureProperty.value();
                *///?} else {
                String textureValue = textureProperty.getValue();
                //?}
                
                // 检查是否是 PNG 皮肤标记（"PNG:filename.png"）
                if (BotSkinManager.isPngSkinMarker(textureValue)) {
                    String pngFileName = BotSkinManager.extractPngFileName(textureValue);
                    UUID playerUUID = profile.getId();
                    if (playerUUID == null) {
                        return null;
                    }
                    
                    // 使用客户端加载器加载 PNG 皮肤纹理
                    return BotSkinTextureLoader.loadPngSkinTexture(playerUUID, pngFileName);
                }
            }
        } catch (Exception e) {
            // 如果出错，让原方法继续执行
            MyBotMod.LOGGER.error("加载假人 PNG 皮肤时出错: {}", e.getMessage());
        }
        return null;
    }
    
    //? if >=1.20.2 {
    /*// 1.20.2+：拦截 getSkin，用 PNG 纹理替换 PlayerSkin 的主体纹理（披风/鞘翅/模型保持原值）
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void onGetSkin(CallbackInfoReturnable<net.minecraft.client.resources.PlayerSkin> cir) {
        ResourceLocation skinLocation = myBotMod$resolvePngSkin();
        if (skinLocation != null) {
            net.minecraft.client.resources.PlayerSkin original = cir.getReturnValue();
            cir.setReturnValue(new net.minecraft.client.resources.PlayerSkin(
                skinLocation,
                null,
                original != null ? original.capeTexture() : null,
                original != null ? original.elytraTexture() : null,
                original != null ? original.model() : net.minecraft.client.resources.PlayerSkin.Model.WIDE,
                false));
        }
    }
    *///?} else {
    /**
     * 拦截 getSkinLocation 方法，处理 PNG 皮肤标记
     * 这个方法返回玩家皮肤的纹理位置
     */
    @Inject(method = "getSkinLocation", 
            at = @At("HEAD"), 
            cancellable = true)
    private void onGetSkinLocation(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation skinLocation = myBotMod$resolvePngSkin();
        if (skinLocation != null) {
            cir.setReturnValue(skinLocation);
        }
    }
    //?}
}
