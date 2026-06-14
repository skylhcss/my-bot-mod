package name.modid.client;

import com.mojang.blaze3d.platform.NativeImage;
import name.modid.MyBotMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端 PNG 皮肤纹理加载器
 * 负责在客户端加载和缓存 PNG 皮肤文件
 */
public class BotSkinTextureLoader {
    
    // PNG 皮肤纹理缓存
    private static final Map<UUID, ResourceLocation> pngSkinTextures = new HashMap<>();
    
    // 解析后的皮肤文件夹路径（延迟初始化）
    private static File resolvedSkinFolder = null;
    
    /**
     * 解析皮肤文件夹路径
     * 与 BotSkinManager 服务端逻辑保持一致，搜索多个可能位置
     */
    private static File resolveSkinFolder() {
        if (resolvedSkinFolder != null) {
            return resolvedSkinFolder;
        }
        
        File gameDir = new File(".");
        File[] possibleLocations = {
            new File(gameDir, "skins"),
            new File(gameDir, "run/skins"),
            new File("run/skins"),
            new File("skins")
        };
        
        for (File location : possibleLocations) {
            if (location.exists() && location.isDirectory()) {
                resolvedSkinFolder = location;
                return resolvedSkinFolder;
            }
        }
        
        // 回退默认值
        resolvedSkinFolder = new File(gameDir, "skins");
        return resolvedSkinFolder;
    }
    
    /**
     * 在客户端加载 PNG 皮肤纹理
     * @param botUUID 假人 UUID
     * @param pngFileName PNG 文件名
     * @return 纹理资源位置
     */
    public static ResourceLocation loadPngSkinTexture(UUID botUUID, String pngFileName) {
        try {
            // 检查缓存
            if (pngSkinTextures.containsKey(botUUID)) {
                return pngSkinTextures.get(botUUID);
            }
            
            // 查找 PNG 文件
            File skinFolder = resolveSkinFolder();
            File pngFile = new File(skinFolder, pngFileName);
            if (!pngFile.exists()) {
                MyBotMod.LOGGER.error("PNG 皮肤文件不存在: {}", pngFileName);
                return null;
            }
            
            // 读取 PNG 文件
            try (FileInputStream fis = new FileInputStream(pngFile)) {
                NativeImage image = NativeImage.read(fis);
                
                // 验证皮肤尺寸
                if ((image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32))) {
                    MyBotMod.LOGGER.error("PNG 皮肤文件尺寸不正确: {} ({}x{}), 应该是 64x64 或 64x32", 
                        pngFileName, image.getWidth(), image.getHeight());
                    image.close();
                    return null;
                }
                
                // 创建动态纹理
                DynamicTexture texture = new DynamicTexture(image);
                
                // 注册纹理
                ResourceLocation location = new ResourceLocation("my-bot-mod", "skins/bot_" + botUUID.toString());
                Minecraft.getInstance().getTextureManager().register(location, texture);
                
                // 缓存纹理位置
                pngSkinTextures.put(botUUID, location);
                
                MyBotMod.LOGGER.info("成功加载 PNG 皮肤: {} -> {}", pngFileName, location);
                return location;
            }
            
        } catch (Exception e) {
            MyBotMod.LOGGER.error("加载 PNG 皮肤时出错: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 清除纹理缓存
     */
    public static void clearCache() {
        pngSkinTextures.clear();
        MyBotMod.LOGGER.info("已清除 PNG 皮肤纹理缓存");
    }
    
    /**
     * 移除特定假人的纹理缓存
     * @param botUUID 假人 UUID
     */
    public static void removeCache(UUID botUUID) {
        pngSkinTextures.remove(botUUID);
    }
}
