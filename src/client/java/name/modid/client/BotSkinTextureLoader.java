package name.modid.client;

import com.mojang.blaze3d.platform.NativeImage;
import name.modid.MyBotMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 客户端 PNG 皮肤纹理加载器
 * 负责在客户端加载和缓存 PNG 皮肤文件
 */
public class BotSkinTextureLoader {
    
    // PNG 皮肤纹理缓存（后台线程读盘后由主线程 put，使用并发容器）
    private static final Map<UUID, ResourceLocation> pngSkinTextures = new ConcurrentHashMap<>();

    // 加载失败的 PNG 皮肤（负缓存，避免渲染热路径每帧重复 IO 与刷屏日志）
    private static final Set<UUID> failedSkins = ConcurrentHashMap.newKeySet();

    // 正在后台加载的皮肤（避免重复提交任务）
    private static final Set<UUID> loadingSkins = ConcurrentHashMap.newKeySet();

    // 假人 PNG 皮肤映射（UUID -> 文件名），由服务端 S2C 下发（替代污染 GameProfile 的旧标记方案）
    private static final Map<UUID, String> pngNameByBot = new ConcurrentHashMap<>();

    // 皮肤文件读取线程池（后台读盘/解码，避免阻塞渲染线程）
    private static final ExecutorService SKIN_IO_EXECUTOR =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "bot-skin-io");
            t.setDaemon(true);
            return t;
        });

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
        
        File gameDir = FabricLoader.getInstance().getGameDir().toFile();
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
    
    /** 设置某假人的 PNG 皮肤文件名（收到 S2C BOT_SKIN 时调用，主线程） */
    public static void setPngName(UUID botUUID, String pngName) {
        if (pngName == null || pngName.isEmpty()) {
            pngNameByBot.remove(botUUID);
        } else {
            pngNameByBot.put(botUUID, pngName);
        }
        failedSkins.remove(botUUID); // 允许用新名字重试
    }

    /** 获取某假人的 PNG 皮肤文件名（PlayerInfoMixin 查询），无则返回 null */
    public static String getPngName(UUID botUUID) {
        return pngNameByBot.get(botUUID);
    }

    /**
     * 在客户端加载 PNG 皮肤纹理
     * @param botUUID 假人 UUID
     * @param pngFileName PNG 文件名
     * @return 纹理资源位置
     */
    public static ResourceLocation loadPngSkinTexture(UUID botUUID, String pngFileName) {
        // 命中缓存直接返回（渲染热路径）
        ResourceLocation cached = pngSkinTextures.get(botUUID);
        if (cached != null) return cached;
        // 负缓存 / 正在加载：本帧返回 null（先用默认皮肤，加载完成后自动切换）
        if (failedSkins.contains(botUUID) || !loadingSkins.add(botUUID)) {
            return null;
        }
        // 提交后台任务：读盘 + 解码在 IO 线程，纹理注册回主线程
        SKIN_IO_EXECUTOR.submit(() -> loadSkinAsync(botUUID, pngFileName));
        return null;
    }

    /** 后台读取并解码 PNG，成功后回主线程注册纹理 */
    private static void loadSkinAsync(UUID botUUID, String pngFileName) {
        try {
            File pngFile = new File(resolveSkinFolder(), pngFileName);
            if (!pngFile.exists()) {
                MyBotMod.LOGGER.error("PNG 皮肤文件不存在: {}", pngFileName);
                failedSkins.add(botUUID);
                loadingSkins.remove(botUUID);
                return;
            }
            final NativeImage image;
            try (FileInputStream fis = new FileInputStream(pngFile)) {
                image = NativeImage.read(fis);
            }
            // 验证皮肤尺寸
            if (image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
                MyBotMod.LOGGER.error("PNG 皮肤文件尺寸不正确: {} ({}x{}), 应该是 64x64 或 64x32",
                    pngFileName, image.getWidth(), image.getHeight());
                image.close();
                failedSkins.add(botUUID);
                loadingSkins.remove(botUUID);
                return;
            }
            // 纹理创建/注册必须在渲染主线程
            Minecraft.getInstance().execute(() -> {
                try {
                    if (failedSkins.contains(botUUID) || pngSkinTextures.containsKey(botUUID)) {
                        image.close();
                        return;
                    }
                    DynamicTexture texture = new DynamicTexture(image);
                    ResourceLocation location = new ResourceLocation("my-bot-mod", "skins/bot_" + botUUID.toString());
                    Minecraft.getInstance().getTextureManager().register(location, texture);
                    pngSkinTextures.put(botUUID, location);
                    MyBotMod.LOGGER.info("成功加载 PNG 皮肤: {} -> {}", pngFileName, location);
                } catch (Exception e) {
                    image.close();
                    failedSkins.add(botUUID);
                    MyBotMod.LOGGER.error("注册 PNG 皮肤纹理时出错: {}", e.getMessage());
                } finally {
                    loadingSkins.remove(botUUID);
                }
            });
        } catch (Exception e) {
            MyBotMod.LOGGER.error("加载 PNG 皮肤时出错: {}", e.getMessage());
            failedSkins.add(botUUID);
            loadingSkins.remove(botUUID);
        }
    }
    
    /**
     * 清除纹理缓存（释放 GPU 纹理资源）
     */
    public static void clearCache() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation location : pngSkinTextures.values()) {
            textureManager.release(location);
        }
        pngSkinTextures.clear();
        failedSkins.clear();
        loadingSkins.clear();
        pngNameByBot.clear();
        resolvedSkinFolder = null; // 跨存档/切服后重新解析皮肤文件夹
        MyBotMod.LOGGER.info("已清除 PNG 皮肤纹理缓存（已释放 GPU 资源）");
    }
    
    /**
     * 移除特定假人的纹理缓存（释放 GPU 纹理资源）
     * @param botUUID 假人 UUID
     */
    public static void removeCache(UUID botUUID) {
        failedSkins.remove(botUUID);
        loadingSkins.remove(botUUID);
        pngNameByBot.remove(botUUID);
        ResourceLocation location = pngSkinTextures.remove(botUUID);
        if (location != null) {
            Minecraft.getInstance().getTextureManager().release(location);
        }
    }
}
