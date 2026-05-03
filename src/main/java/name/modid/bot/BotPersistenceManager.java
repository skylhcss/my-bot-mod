package name.modid.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 假人驻留管理器
 * 负责保存和加载假人数据，实现假人驻留功能
 */
public class BotPersistenceManager {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * 假人数据类
     */
    public static class BotData {
        public String name;
        public UUID uuid;
        public UUID creatorUUID;
        public String creatorName;
        public String dimension;
        public double x, y, z;
        public float yaw, pitch;
        public String gameMode;
        
        // 假人状态（仅在 preserveBotState 为 true 时保存）
        public BotState state;
        
        public static class BotState {
            // 动作状态
            public boolean attacking;
            public boolean using;
            public boolean sneaking;
            public boolean jumping;
            public boolean sprinting;
            
            // 移动状态
            public float forward;
            public float strafing;
            
            // 间隔动作
            public int attackInterval;
            public int useInterval;
            
            // 健康和饥饿
            public float health;
            public int foodLevel;
            public float saturation;
        }
    }
    
    /**
     * 获取假人数据文件夹
     */
    private static File getBotsDataFolder(MinecraftServer server) {
        File worldFolder = server.getLevel(ServerLevel.OVERWORLD).getServer().getServerDirectory();
        File dataFolder = new File(worldFolder, "data");
        File botsFolder = new File(dataFolder, "bots");
        botsFolder.mkdirs();
        return botsFolder;
    }
    
    /**
     * 获取假人数据文件
     */
    private static File getBotDataFile(MinecraftServer server, String botName) {
        return new File(getBotsDataFolder(server), botName.toLowerCase() + ".json");
    }
    
    /**
     * 保存假人数据
     */
    public static void saveBot(BotPlayer bot) {
        var config = name.modid.config.ModConfig.getInstance();
        
        // 如果未启用驻留功能，不保存
        if (!config.botPersistence) {
            return;
        }
        
        try {
            BotData data = new BotData();
            data.name = bot.getName().getString();
            data.uuid = bot.getUUID();
            data.creatorUUID = bot.getCreatorUUID();
            data.creatorName = bot.getCreatorName();
            data.dimension = bot.level().dimension().location().toString();
            data.x = bot.getX();
            data.y = bot.getY();
            data.z = bot.getZ();
            data.yaw = bot.getYRot();
            data.pitch = bot.getXRot();
            data.gameMode = bot.gameMode.getGameModeForPlayer().getName();
            
            // 如果启用了保留状态，保存假人状态
            if (config.preserveBotState) {
                data.state = new BotData.BotState();
                var controller = bot.getActionController();
                
                // 使用反射获取私有字段（因为这些字段是私有的）
                try {
                    var field = controller.getClass().getDeclaredField("attacking");
                    field.setAccessible(true);
                    data.state.attacking = (boolean) field.get(controller);
                    
                    field = controller.getClass().getDeclaredField("using");
                    field.setAccessible(true);
                    data.state.using = (boolean) field.get(controller);
                    
                    field = controller.getClass().getDeclaredField("sneaking");
                    field.setAccessible(true);
                    data.state.sneaking = (boolean) field.get(controller);
                    
                    field = controller.getClass().getDeclaredField("jumping");
                    field.setAccessible(true);
                    data.state.jumping = (boolean) field.get(controller);
                    
                    field = controller.getClass().getDeclaredField("sprinting");
                    field.setAccessible(true);
                    data.state.sprinting = (boolean) field.get(controller);
                    
                    field = controller.getClass().getDeclaredField("forward");
                    field.setAccessible(true);
                    data.state.forward = (float) field.get(controller);
                    
                    field = controller.getClass().getDeclaredField("strafing");
                    field.setAccessible(true);
                    data.state.strafing = (float) field.get(controller);
                    
                    field = controller.getClass().getDeclaredField("attackInterval");
                    field.setAccessible(true);
                    data.state.attackInterval = (int) field.get(controller);
                    
                    field = controller.getClass().getDeclaredField("useInterval");
                    field.setAccessible(true);
                    data.state.useInterval = (int) field.get(controller);
                } catch (Exception e) {
                    System.err.println("无法保存假人状态: " + e.getMessage());
                }
                
                // 保存健康和饥饿
                data.state.health = bot.getHealth();
                data.state.foodLevel = bot.getFoodData().getFoodLevel();
                data.state.saturation = bot.getFoodData().getSaturationLevel();
            }
            
            // 写入文件
            File file = getBotDataFile(bot.getServer(), data.name);
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            System.err.println("无法保存假人数据: " + e.getMessage());
        }
    }
    
    /**
     * 加载假人数据
     */
    public static BotData loadBot(MinecraftServer server, String botName) {
        File file = getBotDataFile(server, botName);
        if (!file.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, BotData.class);
        } catch (IOException e) {
            System.err.println("无法加载假人数据: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 删除假人数据
     */
    public static void deleteBot(MinecraftServer server, String botName) {
        File file = getBotDataFile(server, botName);
        if (file.exists()) {
            file.delete();
        }
    }
    
    /**
     * 获取所有保存的假人数据
     */
    public static List<BotData> getAllBots(MinecraftServer server) {
        List<BotData> bots = new ArrayList<>();
        File folder = getBotsDataFolder(server);
        
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    BotData data = GSON.fromJson(reader, BotData.class);
                    if (data != null) {
                        bots.add(data);
                    }
                } catch (IOException e) {
                    System.err.println("无法加载假人数据: " + file.getName());
                }
            }
        }
        
        return bots;
    }
    
    /**
     * 在服务器启动时加载所有假人
     */
    public static void loadAllBots(MinecraftServer server) {
        var config = name.modid.config.ModConfig.getInstance();
        
        // 如果未启用驻留功能，不加载
        if (!config.botPersistence) {
            return;
        }
        
        List<BotData> bots = getAllBots(server);
        for (BotData data : bots) {
            try {
                // 查找创建者
                ServerPlayer creator = server.getPlayerList().getPlayer(data.creatorUUID);
                if (creator == null) {
                    // 如果创建者不在线，跳过
                    System.out.println("跳过假人 " + data.name + "：创建者不在线");
                    continue;
                }
                
                // 获取世界
                ServerLevel level = server.getLevel(
                    net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new net.minecraft.resources.ResourceLocation(data.dimension)
                    )
                );
                
                if (level == null) {
                    System.err.println("无法加载假人 " + data.name + "：世界不存在");
                    continue;
                }
                
                // 创建假人
                Vec3 position = new Vec3(data.x, data.y, data.z);
                GameType gameMode = GameType.byName(data.gameMode);
                BotPlayer bot = BotManager.createBot(server, creator, data.name, position, gameMode);
                
                if (bot != null) {
                    // 设置旋转
                    bot.setYRot(data.yaw);
                    bot.setXRot(data.pitch);
                    bot.setYHeadRot(data.yaw);
                    
                    // 如果启用了保留状态，恢复假人状态
                    if (config.preserveBotState && data.state != null) {
                        var controller = bot.getActionController();
                        
                        // 恢复动作状态
                        if (data.state.attacking) {
                            if (data.state.attackInterval > 0) {
                                controller.startAttackInterval(data.state.attackInterval);
                            } else {
                                controller.startAttackContinuous();
                            }
                        }
                        
                        if (data.state.using) {
                            if (data.state.useInterval > 0) {
                                controller.startUseInterval(data.state.useInterval);
                            } else {
                                controller.startUseContinuous();
                            }
                        }
                        
                        controller.setSneak(data.state.sneaking);
                        controller.setJump(data.state.jumping);
                        controller.setSprint(data.state.sprinting);
                        
                        // 恢复移动状态
                        if (data.state.forward > 0) {
                            controller.moveForward();
                        } else if (data.state.forward < 0) {
                            controller.moveBackward();
                        }
                        
                        if (data.state.strafing > 0) {
                            controller.moveLeft();
                        } else if (data.state.strafing < 0) {
                            controller.moveRight();
                        }
                        
                        // 恢复健康和饥饿
                        bot.setHealth(data.state.health);
                        bot.getFoodData().setFoodLevel(data.state.foodLevel);
                        bot.getFoodData().setSaturation(data.state.saturation);
                    }
                    
                    System.out.println("成功加载假人: " + data.name);
                }
            } catch (Exception e) {
                System.err.println("无法加载假人 " + data.name + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 在服务器关闭时保存所有假人
     */
    public static void saveAllBots() {
        var config = name.modid.config.ModConfig.getInstance();
        
        // 如果未启用驻留功能，不保存
        if (!config.botPersistence) {
            return;
        }
        
        for (BotPlayer bot : BotManager.getAllBots()) {
            saveBot(bot);
        }
    }
}
