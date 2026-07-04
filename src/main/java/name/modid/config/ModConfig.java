package name.modid.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 模组配置类
 * 管理所有配置项的读取、保存和访问
 */
public class ModConfig {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
        FabricLoader.getInstance().getConfigDir().toFile(), 
        "my-bot-mod.json"
    );
    
    private static ModConfig INSTANCE = null;
    
    // ========== 总开关 ==========
    /**
     * 是否启用假人功能
     */
    public boolean enableBotFeature = true;
    
    // ========== 快捷键配置 ==========
    /**
     * 打开配置菜单的快捷键（默认：B）
     */
    public String configMenuKey = "key.keyboard.b";
    
    // ========== 假人功能配置 ==========
    /**
     * 攻击/破坏距离（格）
     * 生存模式默认 3.0，创造模式默认 5.0
     */
    public double attackReachDistance = 3.0;
    
    /**
     * 创造模式攻击/破坏距离（格）
     */
    public double creativeAttackReachDistance = 5.0;
    
    /**
     * 是否启用杀戮光环（攻击范围内所有实体）
     * 启用后会攻击周围所有实体，禁用后只攻击视线前方的实体
     */
    public boolean enableKillAura = false;
    
    /**
     * 杀戮光环范围（格）
     */
    public double killAuraRange = 3.0;
    
    /**
     * 骑乘实体白名单（实体类型ID）
     * 例如：["minecraft:pig", "minecraft:horse", "minecraft:boat"]
     */
    public List<String> mountWhitelist = new ArrayList<>();
    
    /**
     * 是否允许假人骑乘其他假人
     */
    public boolean allowMountOtherBots = false;
    
    /**
     * 假人最大数量限制（0 = 无限制）
     */
    public int maxBotCount = 0;
    
    /**
     * 是否允许非 OP 玩家创建假人
     */
    public boolean allowNonOpCreateBot = false;
    
    /**
     * 假人死亡后是否自动重生
     */
    public boolean autoRespawnOnDeath = false;
    
    /**
     * 假人是否受到伤害
     */
    public boolean botTakeDamage = true;
    
    /**
     * 假人是否会饥饿
     */
    public boolean botHunger = true;
    
    /**
     * 假人驻留（退出世界重进后假人依然存在）
     */
    public boolean botPersistence = false;
    
    /**
     * 保留假人状态（不但驻留，而且保留退出前的状态和动作）
     */
    public boolean preserveBotState = false;
    
    /**
     * Carpet Mod 兼容模式
     * 启用后，如果检测到 Carpet Mod，将自动禁用本模组的假人功能以避免冲突
     */
    public boolean carpetModCompatibility = true;
    
    // ========== 动作设置 ==========
    /**
     * 假人自动跳跃
     * 启用后，假人在移动时遇到1格高障碍物会自动跳跃（与真实玩家行为一致）
     */
    public boolean allowBotAutoJump = true;
    
    // ========== 关于信息（transient 防止 Gson 序列化/反序列化 final 字段） ==========
    public transient final String modName = "我的机器人";
    public transient final String modVersion = "1.2.1b";
    public transient final String author = "Skyline_hcss";
    public transient final String email = "Skyline.hcss@gmail.com";
    public transient final String githubRepo = "https://github.com/skylhcss/my-bot-mod";
    public transient final String description = "一个类似 Carpet Mod 的假人（机器人玩家）模组，用于 Minecraft 1.20.1 Fabric";
    public transient final String license = "MIT License";
    
    /**
     * 获取配置实例
     */
    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }
    
    /**
     * 重新加载配置文件
     * 从磁盘重新读取配置，替换当前单例
     */
    public static ModConfig reload() {
        INSTANCE = load();
        return INSTANCE;
    }
    
    /**
     * 从文件加载配置
     */
    private static ModConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config != null) {
                    // 只在白名单为 null 时初始化默认值（JSON 解析失败）
                    // 空白名单 [] 是用户有意清空，不应被覆盖
                    if (config.mountWhitelist == null) {
                        config.mountWhitelist = new ArrayList<>();
                        config.initDefaultMountWhitelist();
                    }
                    return config;
                }
            } catch (IOException e) {
                System.err.println("无法加载配置文件: " + e.getMessage());
            }
        }
        
        // 创建默认配置
        ModConfig config = new ModConfig();
        config.initDefaultMountWhitelist();
        config.save();
        return config;
    }
    
    /**
     * 初始化默认的骑乘白名单
     */
    private void initDefaultMountWhitelist() {
        mountWhitelist.add("minecraft:pig");
        mountWhitelist.add("minecraft:horse");
        mountWhitelist.add("minecraft:donkey");
        mountWhitelist.add("minecraft:mule");
        mountWhitelist.add("minecraft:llama");
        mountWhitelist.add("minecraft:boat");
        mountWhitelist.add("minecraft:chest_boat");
        mountWhitelist.add("minecraft:minecart");
        mountWhitelist.add("minecraft:strider");
    }
    
    /**
     * 保存配置到文件
     */
    public void save() {
        try {
            // 确保配置目录存在
            CONFIG_FILE.getParentFile().mkdirs();
            
            // 原子写入：先写临时文件，再重命名，避免写入中途崩溃导致配置损坏
            File tempFile = new File(CONFIG_FILE.getParentFile(), CONFIG_FILE.getName() + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                GSON.toJson(this, writer);
            }
            // 重命名（在同一文件系统上是原子操作）
            if (CONFIG_FILE.exists()) {
                CONFIG_FILE.delete();
            }
            if (!tempFile.renameTo(CONFIG_FILE)) {
                // 重命名失败时回退为直接写入
                try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                    GSON.toJson(this, writer);
                }
                tempFile.delete();
            }
        } catch (IOException e) {
            name.modid.MyBotMod.LOGGER.error("无法保存配置文件: {}", e.getMessage());
        }
    }
    
    /**
     * 重置为默认配置
     */
    public void reset() {
        enableBotFeature = true;
        configMenuKey = "key.keyboard.b";
        attackReachDistance = 3.0;
        creativeAttackReachDistance = 5.0;
        enableKillAura = false;
        killAuraRange = 3.0;
        allowMountOtherBots = false;
        maxBotCount = 0;
        allowNonOpCreateBot = false;
        autoRespawnOnDeath = false;
        botTakeDamage = true;
        botHunger = true;
        botPersistence = false;
        preserveBotState = false;
        carpetModCompatibility = true;
        allowBotAutoJump = true;
        
        mountWhitelist.clear();
        initDefaultMountWhitelist();
        
        save();
    }
}
