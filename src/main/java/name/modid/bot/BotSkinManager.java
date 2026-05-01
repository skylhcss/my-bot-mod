package name.modid.bot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import name.modid.MyBotMod;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

/**
 * 假人皮肤管理器
 * 负责获取和应用玩家皮肤
 * 支持三种皮肤来源：
 * 1. Mojang API（正版玩家皮肤）
 * 2. 本地 PNG 文件（temporary 文件夹）
 * 3. Base64 编码的 .txt 文件（temporary 文件夹）
 */
public class BotSkinManager {
    
    // 默认皮肤列表（Base64 编码的皮肤数据）
    private static final List<Property> DEFAULT_SKINS = new ArrayList<>();
    
    // PNG 皮肤文件列表
    private static final List<File> PNG_SKIN_FILES = new ArrayList<>();
    
    // 皮肤缓存
    private static final Map<String, Property> skinCache = new HashMap<>();
    
    // temporary 文件夹路径
    private static final String TEMP_FOLDER = "temporary";
    
    // 标记是否已初始化
    private static boolean initialized = false;
    
    /**
     * 获取 PNG 皮肤文件列表（供客户端使用）
     */
    public static List<File> getPngSkinFiles() {
        return new ArrayList<>(PNG_SKIN_FILES);
    }

    /**
     * 为假人设置皮肤
     * @param profile 游戏档案
     * @param botName 假人名字
     */
    public static void applySkin(GameProfile profile, String botName) {
        try {
            // 首先尝试从缓存获取
            if (skinCache.containsKey(botName.toLowerCase())) {
                Property skin = skinCache.get(botName.toLowerCase());
                // 清除现有的纹理属性
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", skin);
                MyBotMod.LOGGER.info("从缓存为假人 {} 应用皮肤", botName);
                return;
            }
            
            // 尝试从 Mojang API 获取正版玩家皮肤
            Property skin = fetchSkinFromMojang(botName);
            if (skin != null) {
                // 清除现有的纹理属性
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", skin);
                skinCache.put(botName.toLowerCase(), skin);
                MyBotMod.LOGGER.info("从 Mojang API 为假人 {} 获取皮肤", botName);
                return;
            }
            
            // 如果获取失败，优先使用 PNG 文件
            if (!PNG_SKIN_FILES.isEmpty()) {
                File pngFile = PNG_SKIN_FILES.get(new Random().nextInt(PNG_SKIN_FILES.size()));
                // PNG 文件需要在客户端加载，这里我们生成一个特殊的标记
                // 实际的纹理加载会在客户端的 mixin 中处理
                String pngMarker = "PNG:" + pngFile.getName();
                Property pngSkin = new Property("textures", pngMarker, "");
                // 清除现有的纹理属性
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", pngSkin);
                MyBotMod.LOGGER.info("为假人 {} 标记使用 PNG 皮肤: {}", botName, pngFile.getName());
                return;
            }
            
            // 最后使用 Base64 编码的 .txt 文件
            if (!DEFAULT_SKINS.isEmpty()) {
                Property defaultSkin = DEFAULT_SKINS.get(new Random().nextInt(DEFAULT_SKINS.size()));
                // 清除现有的纹理属性
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", defaultSkin);
                MyBotMod.LOGGER.info("为假人 {} 应用 temporary 文件夹中的随机 Base64 皮肤", botName);
            } else {
                // 即使没有皮肤文件，也要设置一个空的纹理属性，确保系统使用默认皮肤
                // 这样可以保证 profile.getProperties().containsKey("textures") 返回 true
                Property emptyTexture = new Property("textures", "", "");
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", emptyTexture);
                MyBotMod.LOGGER.warn("没有可用的皮肤文件，假人 {} 将使用 Minecraft 默认皮肤", botName);
            }
            
        } catch (Exception e) {
            MyBotMod.LOGGER.error("为假人 {} 设置皮肤时出错: {}", botName, e.getMessage());
        }
    }
    
    
    /**
     * 检查是否是 PNG 皮肤标记
     * @param textureValue 纹理值
     * @return 是否是 PNG 标记
     */
    public static boolean isPngSkinMarker(String textureValue) {
        return textureValue != null && textureValue.startsWith("PNG:");
    }
    
    /**
     * 从 PNG 标记中提取文件名
     * @param textureValue 纹理值
     * @return PNG 文件名
     */
    public static String extractPngFileName(String textureValue) {
        if (isPngSkinMarker(textureValue)) {
            return textureValue.substring(4); // 移除 "PNG:" 前缀
        }
        return null;
    }

    /**
     * 从 Mojang API 获取玩家皮肤
     * @param playerName 玩家名字
     * @return 皮肤属性，如果失败则返回 null
     */
    private static Property fetchSkinFromMojang(String playerName) {
        try {
            // 第一步：获取玩家 UUID
            String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            HttpURLConnection connection = (HttpURLConnection) new URL(uuidUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JsonObject uuidJson = JsonParser.parseString(response.toString()).getAsJsonObject();
            String uuid = uuidJson.get("id").getAsString();
            
            // 第二步：获取玩家皮肤数据
            String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false";
            connection = (HttpURLConnection) new URL(profileUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }
            
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JsonObject profileJson = JsonParser.parseString(response.toString()).getAsJsonObject();
            if (profileJson.has("properties")) {
                JsonObject properties = profileJson.getAsJsonArray("properties").get(0).getAsJsonObject();
                String value = properties.get("value").getAsString();
                String signature = properties.has("signature") ? properties.get("signature").getAsString() : "";
                
                return new Property("textures", value, signature);
            }
            
        } catch (Exception e) {
            MyBotMod.LOGGER.debug("从 Mojang API 获取玩家 {} 的皮肤失败: {}", playerName, e.getMessage());
        }
        
        return null;
    }

    /**
     * 加载默认皮肤（从 temporary 文件夹）
     */
    private static void loadDefaultSkins() {
        try {
            File tempFolder = new File(TEMP_FOLDER);
            if (!tempFolder.exists() || !tempFolder.isDirectory()) {
                MyBotMod.LOGGER.warn("temporary 文件夹不存在，将在模组初始化时创建");
                return;
            }
            
            // 加载 PNG 文件
            File[] pngFiles = tempFolder.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".png")
            );
            
            if (pngFiles != null && pngFiles.length > 0) {
                for (File file : pngFiles) {
                    PNG_SKIN_FILES.add(file);
                    MyBotMod.LOGGER.info("找到 PNG 皮肤文件: {}", file.getName());
                }
            }
            
            // 加载 .txt 文件（Base64 编码的皮肤数据）
            File[] txtFiles = tempFolder.listFiles((dir, name) -> 
                name.endsWith(".txt") && !name.equalsIgnoreCase("README.txt")
            );
            
            if (txtFiles != null && txtFiles.length > 0) {
                for (File file : txtFiles) {
                    try {
                        String skinData = Files.readString(file.toPath()).trim();
                        if (!skinData.isEmpty()) {
                            DEFAULT_SKINS.add(new Property("textures", skinData, ""));
                            MyBotMod.LOGGER.info("加载 Base64 皮肤文件: {}", file.getName());
                        }
                    } catch (Exception e) {
                        MyBotMod.LOGGER.error("加载皮肤文件 {} 失败: {}", file.getName(), e.getMessage());
                    }
                }
            }
            
            int totalSkins = PNG_SKIN_FILES.size() + DEFAULT_SKINS.size();
            if (totalSkins == 0) {
                MyBotMod.LOGGER.warn("temporary 文件夹中没有找到任何皮肤文件");
                MyBotMod.LOGGER.warn("支持的格式：PNG 文件（64x64 或 64x32）和 Base64 编码的 .txt 文件");
            } else {
                MyBotMod.LOGGER.info("成功加载 {} 个 PNG 皮肤和 {} 个 Base64 皮肤", 
                    PNG_SKIN_FILES.size(), DEFAULT_SKINS.size());
            }
            
        } catch (Exception e) {
            MyBotMod.LOGGER.error("加载默认皮肤时出错: {}", e.getMessage());
        }
    }



    /**
     * 清除皮肤缓存
     */
    public static void clearCache() {
        skinCache.clear();
        MyBotMod.LOGGER.info("已清除皮肤缓存");
    }

    /**
     * 重新加载默认皮肤
     */
    public static void reloadDefaultSkins() {
        DEFAULT_SKINS.clear();
        PNG_SKIN_FILES.clear();
        loadDefaultSkins();
    }

    /**
     * 创建 temporary 文件夹和示例说明文件
     */
    public static void initializeTemporaryFolder() {
        // 防止重复初始化
        if (initialized) {
            return;
        }
        initialized = true;
        
        try {
            File tempFolder = new File(TEMP_FOLDER);
            if (!tempFolder.exists()) {
                tempFolder.mkdirs();
                MyBotMod.LOGGER.info("创建 temporary 文件夹");
            }
            
            // 加载皮肤文件
            loadDefaultSkins();
            
        } catch (Exception e) {
            MyBotMod.LOGGER.error("初始化 temporary 文件夹时出错: {}", e.getMessage());
        }
    }
}
