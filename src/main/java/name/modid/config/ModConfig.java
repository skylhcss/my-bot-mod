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
    
    private static volatile ModConfig INSTANCE = null;
    
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
     * 是否允许非 OP 玩家创建并控制假人（完整权限：创建/删除/传送/背包/指挥等）
     */
    @com.google.gson.annotations.SerializedName(value = "allowNonOpControlBot", alternate = {"allowNonOpCreateBot"})
    public boolean allowNonOpControlBot = false;
    
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

    // ========== 指挥棒设置 ==========
    /**
     * 传送模式是否允许非创造模式使用
     * 默认 false：仅当手持指挥棒的玩家处于创造模式时才能使用传送模式；
     * 启用后，任意游戏模式的玩家都可使用传送模式。
     */
    public boolean allowBatonTeleportNonCreative = false;
    
    // ========== 外观与防护 ==========
    /**
     * 假人是否发光（创建时生效，便于在远处定位假人）
     */
    public boolean botGlowing = false;
    
    /**
     * 假人是否免疫火焰/岩浆伤害
     */
    public boolean botFireImmune = false;
    
    // ========== OP/权限系统 ==========
    /**
     * 每位玩家可创建的假人数量上限（0 = 无限制）
     */
    public int maxBotsPerPlayer = 0;
    
    /**
     * 指挥棒是否需要 OP 权限（开启后非 OP 即使有 allowNonOpControlBot 也不能用指挥棒）
     */
    public boolean batonRequiresOp = false;
    
    // ========== 寻路系统 ==========
    /**
     * 最大寻路距离（格），超出直接拒绝寻路
     */
    public int maxPathfindingDistance = 256;
    
    /**
     * 寻路允许跑酷跳跃（跨越 2-4 格裂谷）
     */
    public boolean pathfindingAllowParkour = true;
    
    /**
     * 寻路允许游泳路线
     */
    public boolean pathfindingAllowSwim = true;
    
    // ========== 关于信息 ==========
    /**
     * 模组名称（从 fabric.mod.json 动态读取，避免与 gradle 双源漂移）
     */
    public final transient String modName = readModMeta(true, "我的机器人");
    
    /**
     * 模组版本（从 fabric.mod.json 动态读取，单一数据源）
     */
    public final transient String modVersion = readModMeta(false, "1.3.1");
    
    /**
     * 作者信息
     */
    public final transient String author = "Skyline_hcss";
    
    /**
     * 作者邮箱
     */
    public final transient String email = "Skyline.hcss@gmail.com";
    
    /**
     * GitHub 仓库地址
     */
    public final transient String githubRepo = "https://github.com/skylhcss/my-bot-mod";
    
    /**
     * 模组描述
     */
    public final transient String description = "一个类似 Carpet Mod 的假人（机器人玩家）模组，用于 Minecraft 1.20.1 Fabric";
    
    /**
     * 许可证
     */
    public final transient String license = "MIT License";
    
    /**
     * 从 fabric.mod.json 元数据读取模组名称/版本（单一数据源，避免与 gradle.properties 漂移）
     * @param name true 读取名称，false 读取版本
     */
    private static String readModMeta(boolean name, String fallback) {
        try {
            var opt = FabricLoader.getInstance().getModContainer("my-bot-mod");
            if (opt.isPresent()) {
                var meta = opt.get().getMetadata();
                return name ? meta.getName() : meta.getVersion().getFriendlyString();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    /** 校验并夹取数值字段到合理范围，防止手改 JSON 写入非法值 */
    private void validate() {
        attackReachDistance = clamp(attackReachDistance, 0.0, 64.0);
        creativeAttackReachDistance = clamp(creativeAttackReachDistance, 0.0, 128.0);
        killAuraRange = clamp(killAuraRange, 0.0, 64.0);
        if (maxBotCount < 0) maxBotCount = 0;
        if (maxBotsPerPlayer < 0) maxBotsPerPlayer = 0;
        maxPathfindingDistance = (int) clamp(maxPathfindingDistance, 32, 1024);
        if (configMenuKey == null || configMenuKey.isEmpty()) configMenuKey = "key.keyboard.b";
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }

    /**
     * 获取配置实例
     */
    public static ModConfig getInstance() {
        ModConfig result = INSTANCE;
        if (result == null) {
            synchronized (ModConfig.class) {
                result = INSTANCE;
                if (result == null) {
                    result = load();
                    INSTANCE = result;
                }
            }
        }
        return result;
    }
    
    /**
     * 重新加载配置文件
     * 从磁盘重新读取配置，替换当前单例
     */
    public static ModConfig reload() {
        synchronized (ModConfig.class) {
            INSTANCE = load();
            return INSTANCE;
        }
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
                    config.validate();
                    return config;
                }
            } catch (IOException | com.google.gson.JsonParseException e) {
                name.modid.MyBotMod.LOGGER.error("无法加载配置文件，将使用默认配置: {}", e.getMessage());
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
            
            // 写入配置文件
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
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
        autoRespawnOnDeath = false;
        botTakeDamage = true;
        botHunger = true;
        botPersistence = false;
        preserveBotState = false;
        carpetModCompatibility = true;
        allowBotAutoJump = true;
        allowBatonTeleportNonCreative = false;
        botGlowing = false;
        botFireImmune = false;
        maxBotsPerPlayer = 0;
        batonRequiresOp = false;
        maxPathfindingDistance = 256;
        pathfindingAllowParkour = true;
        pathfindingAllowSwim = true;
        allowNonOpControlBot = false;
        
        mountWhitelist.clear();
        initDefaultMountWhitelist();
        
        save();
    }
}
