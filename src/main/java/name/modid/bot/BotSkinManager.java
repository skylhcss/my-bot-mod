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
 * 2. 本地 PNG 文件（run/skins 文件夹）
 * 3. Base64 编码的 .txt 文件（run/skins 文件夹）
 */
public class BotSkinManager {
    
    // 默认皮肤列表（Base64 编码的皮肤数据）
    private static final List<Property> DEFAULT_SKINS = new ArrayList<>();
    
    // PNG 皮肤文件列表
    private static final List<File> PNG_SKIN_FILES = new ArrayList<>();
    
    // 皮肤缓存
    private static final Map<String, Property> skinCache = new HashMap<>();
    
    // 皮肤文件夹路径（在游戏目录下）
    private static File skinFolder = null;
    
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
                MyBotMod.LOGGER.info("为假人 {} 应用 run/skins 文件夹中的随机 Base64 皮肤", botName);
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
        HttpURLConnection connection = null;
        try {
            // 第一步：获取玩家 UUID
            String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            connection = (HttpURLConnection) new URL(uuidUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }
            
            String uuidJsonStr;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                uuidJsonStr = response.toString();
            } finally {
                connection.disconnect();
            }
            
            JsonObject uuidJson = JsonParser.parseString(uuidJsonStr).getAsJsonObject();
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
            
            String profileJsonStr;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                profileJsonStr = response.toString();
            } finally {
                connection.disconnect();
            }
            
            JsonObject profileJson = JsonParser.parseString(profileJsonStr).getAsJsonObject();
            if (profileJson.has("properties")) {
                JsonObject properties = profileJson.getAsJsonArray("properties").get(0).getAsJsonObject();
                String value = properties.get("value").getAsString();
                String signature = properties.has("signature") ? properties.get("signature").getAsString() : "";
                
                return new Property("textures", value, signature);
            }
            
        } catch (Exception e) {
            MyBotMod.LOGGER.debug("从 Mojang API 获取玩家 {} 的皮肤失败: {}", playerName, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        
        return null;
    }

    /**
     * 加载默认皮肤（从 skins 文件夹）
     */
    private static void loadDefaultSkins() {
        try {
            if (skinFolder == null || !skinFolder.exists() || !skinFolder.isDirectory()) {
                MyBotMod.LOGGER.warn("皮肤文件夹不存在: {}", skinFolder != null ? skinFolder.getAbsolutePath() : "null");
                return;
            }
            
            MyBotMod.LOGGER.info("正在从 {} 加载皮肤文件", skinFolder.getAbsolutePath());
            
            // 加载 PNG 文件
            File[] pngFiles = skinFolder.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".png")
            );
            
            if (pngFiles != null && pngFiles.length > 0) {
                for (File file : pngFiles) {
                    PNG_SKIN_FILES.add(file);
                }
            }
            
            // 加载 .txt 文件（Base64 编码的皮肤数据）
            File[] txtFiles = skinFolder.listFiles((dir, name) -> 
                name.endsWith(".txt") && !name.equalsIgnoreCase("README.txt")
            );
            
            if (txtFiles != null && txtFiles.length > 0) {
                for (File file : txtFiles) {
                    try {
                        String skinData = Files.readString(file.toPath()).trim();
                        if (!skinData.isEmpty()) {
                            DEFAULT_SKINS.add(new Property("textures", skinData, ""));
                        }
                    } catch (Exception e) {
                        MyBotMod.LOGGER.error("加载皮肤文件 {} 失败: {}", file.getName(), e.getMessage());
                    }
                }
            }
            
            // 统计并输出日志
            int totalSkins = PNG_SKIN_FILES.size() + DEFAULT_SKINS.size();
            if (totalSkins == 0) {
                MyBotMod.LOGGER.warn("皮肤文件夹中没有找到任何皮肤文件");
                MyBotMod.LOGGER.warn("支持的格式：PNG 文件（64x64 或 64x32）和 Base64 编码的 .txt 文件");
            } else {
                MyBotMod.LOGGER.info("成功加载 {} 个皮肤文件（PNG: {}, Base64: {}）", 
                    totalSkins, PNG_SKIN_FILES.size(), DEFAULT_SKINS.size());
            }
            
        } catch (Exception e) {
            MyBotMod.LOGGER.error("加载默认皮肤时出错: {}", e.getMessage());
            e.printStackTrace();
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
     * 初始化皮肤文件夹
     */
    public static void initializeSkinFolder() {
        // 防止重复初始化
        if (initialized) {
            return;
        }
        initialized = true;
        
        try {
            // 获取游戏运行目录（.minecraft 或开发环境的 run 目录）
            File gameDir = new File(".");
            MyBotMod.LOGGER.info("游戏目录: {}", gameDir.getAbsolutePath());
            
            // 尝试多个可能的皮肤文件夹位置
            File[] possibleLocations = {
                new File(gameDir, "skins"),           // ./skins
                new File(gameDir, "run/skins"),       // ./run/skins
                new File("run/skins"),                // run/skins（相对路径）
                new File("skins")                     // skins（相对路径）
            };
            
            // 查找存在的皮肤文件夹
            for (File location : possibleLocations) {
                MyBotMod.LOGGER.info("检查皮肤文件夹: {}", location.getAbsolutePath());
                if (location.exists() && location.isDirectory()) {
                    skinFolder = location;
                    MyBotMod.LOGGER.info("找到皮肤文件夹: {}", skinFolder.getAbsolutePath());
                    break;
                }
            }
            
            // 如果没有找到，创建默认位置
            if (skinFolder == null) {
                skinFolder = new File(gameDir, "skins");
                MyBotMod.LOGGER.info("创建皮肤文件夹: {}", skinFolder.getAbsolutePath());
                skinFolder.mkdirs();
            }
            
            // 加载皮肤文件
            loadDefaultSkins();
            
        } catch (Exception e) {
            MyBotMod.LOGGER.error("初始化皮肤文件夹时出错: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
