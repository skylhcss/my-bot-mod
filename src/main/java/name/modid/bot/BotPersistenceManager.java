package name.modid.bot;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import name.modid.MyBotMod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 假人驻留管理器
 * 参考 GugleCarpetAddition (GCA) 的实现
 * 
 * GCA 的核心设计理念：
 * 1. 使用 SavedData 系统持久化假人数据到世界文件中
 * 2. 在第一个玩家加入时延迟加载假人（而非服务器启动时）
 * 3. 使用区块加载票据（Chunk Ticket）保持假人所在区块加载
 * 4. 支持保存和恢复假人的完整状态（位置、动作、物品栏、药水效果等）
 * 5. 支持跨维度的假人驻留（主世界、下界、末地）
 * 
 * 实现细节：
 * - 假人数据存储在主世界的 SavedData 中（data/my_bot_mod_bots.dat）
 * - 使用 JSON 格式序列化假人数据，便于调试和修改
 * - 区块加载票据每 100 tick（5秒）自动刷新，确保假人区块始终加载
 * - 假人每 200 tick（10秒）自动保存一次状态
 * - 服务器关闭时保存所有假人并清理区块加载票据
 * 
 * 与 GCA 的差异：
 * - GCA 使用 Carpet Mod 的假人系统，我们使用自己的 BotPlayer
 * - GCA 支持 /bot 命令管理假人组，我们暂时只支持单个假人管理
 * - GCA 支持更多高级功能（自动补货、自动钓鱼等），我们专注于核心功能
 */
public class BotPersistenceManager extends SavedData {
    
    private static final String DATA_NAME = "my_bot_mod_bots";
    
    // 区块加载票据类型 - 用于保持假人所在区块加载
    private static final TicketType<ChunkPos> BOT_CHUNK_TICKET = TicketType.create(
        "my_bot_mod:bot_chunk", 
        Comparator.comparingLong(ChunkPos::toLong),
        300 // 票据有效期（tick）
    );
    
    // 存储所有假人数据（每个假人一个 CompoundTag，直接以 NBT 持久化，线程安全）
    private final Map<String, CompoundTag> botsData = new ConcurrentHashMap<>();
    
    // 存储假人的区块加载票据（假人名 -> 区块位置）
    /** 票据位置：维度 + 区块坐标（用于跨维度精确移除/刷新票据） */
    private record TicketLoc(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, ChunkPos pos) {}

    private final Map<String, TicketLoc> botChunkTickets = new ConcurrentHashMap<>();
    
    /**
     * 假人数据类
     * 存储假人的所有持久化信息
     */
    public static class BotData {
        // 基本信息
        public String name;
        public UUID uuid;
        public UUID creatorUUID;
        public String creatorName;
        
        // 位置信息
        public String dimension;
        public double x, y, z;
        public float yaw, pitch;
        
        // 游戏模式
        public String gameMode;
        
        // 假人状态（仅在 preserveBotState 为 true 时保存）
        public BotState state;
        
        // 物品栏数据（NBT 格式的 JSON 字符串）
        public String inventoryData;
        
        // 末影箱数据
        public String enderChestData;
        
        // 当前手持（选中）快捷栏槽位（0-8）
        public int selectedSlot;
        
        // 假人个人配置（覆盖全局配置）
        public BotSettings settings;
        
        /**
         * 假人状态类
         * 存储假人的动作和生存状态
         */
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
            public float exhaustion;
            
            // 经验
            public int experienceLevel;
            public float experienceProgress;
            
            // 效果（药水效果）
            public List<String> effects = new ArrayList<>();
        }
    }
    
    /**
     * 构造函数
     */
    public BotPersistenceManager() {
        super();
    }
    
    /**
     * 从 NBT 加载数据
     */
    public static BotPersistenceManager load(CompoundTag tag) {
        BotPersistenceManager manager = new BotPersistenceManager();
        
        if (tag.contains("Bots")) {
            ListTag botsList = tag.getList("Bots", 10); // 10 = CompoundTag
            for (int i = 0; i < botsList.size(); i++) {
                CompoundTag botTag = botsList.getCompound(i);
                String name = botTag.getString("Name");
                if (!name.isEmpty()) {
                    manager.botsData.put(name.toLowerCase(), botTag);
                }
            }
        }
        
        return manager;
    }
    
    /**
     * 保存数据到 NBT
     */
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag botsList = new ListTag();
        
        for (Map.Entry<String, CompoundTag> entry : botsData.entrySet()) {
            botsList.add(entry.getValue().copy());
        }
        
        tag.put("Bots", botsList);
        return tag;
    }
    
    /**
     * 获取或创建 BotPersistenceManager 实例
     */
    public static BotPersistenceManager get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        if (overworld == null) {
            return null;
        }
        
        //? if >=1.20.2 {
        /*// 1.20.2+ 改用 SavedData.Factory（DataFixTypes 传 null：模组数据无需升级链）
        return overworld.getDataStorage().computeIfAbsent(
            new net.minecraft.world.level.saveddata.SavedData.Factory<>(
                BotPersistenceManager::new,
                BotPersistenceManager::load,
                null),
            DATA_NAME
        );
        *///?} else {
        return overworld.getDataStorage().computeIfAbsent(
            BotPersistenceManager::load,
            BotPersistenceManager::new,
            DATA_NAME
        );
        //?}
    }
    
    /**
     * 保存假人数据
     * 参考 GCA 的实现，保存完整的假人状态
     */
    public static void saveBot(BotPlayer bot) {
        var config = name.modid.config.ModConfig.getInstance();
        
        // 如果未启用驻留功能，不保存
        if (!config.botPersistence) {
            return;
        }
        
        MinecraftServer server = bot.getServer();
        if (server == null) {
            return;
        }
        
        BotPersistenceManager manager = get(server);
        if (manager == null) {
            return;
        }
        
        try {
            String botName = bot.getName().getString();
            CompoundTag data = new CompoundTag();

            // 基本信息
            data.putString("Name", botName);
            data.putUUID("UUID", bot.getUUID());
            data.putUUID("CreatorUUID", bot.getCreatorUUID());
            data.putString("CreatorName", bot.getCreatorName());

            // 位置信息
            data.putString("Dimension", bot.level().dimension().location().toString());
            data.putDouble("X", bot.getX());
            data.putDouble("Y", bot.getY());
            data.putDouble("Z", bot.getZ());
            data.putFloat("Yaw", bot.getYRot());
            data.putFloat("Pitch", bot.getXRot());

            // 游戏模式 + 当前手持槽位
            data.putString("GameMode", bot.gameMode.getGameModeForPlayer().getName());
            data.putInt("SelectedSlot", bot.getInventory().selected);

            // 个人配置（三态，按 KEYS 存整型 id）
            CompoundTag settingsTag = new CompoundTag();
            for (String k : BotSettings.KEYS) {
                settingsTag.putInt(k, bot.getSettings().get(k).id());
            }
            data.put("Settings", settingsTag);

            // 行为播放列表（文件名列表，随驻留保存）
            ListTag behaviorsTag = new ListTag();
            for (String behaviorName : name.modid.behavior.BehaviorManager.getAssigned(bot)) {
                behaviorsTag.add(net.minecraft.nbt.StringTag.valueOf(behaviorName));
            }
            if (!behaviorsTag.isEmpty()) {
                data.put("Behaviors", behaviorsTag);
            }

            // 物品栏（直接以 NBT 存储，无 SNBT 字符串往返）
            ListTag inventoryList = new ListTag();
            bot.getInventory().save(inventoryList);
            data.put("Inventory", inventoryList);

            // 末影箱（直接以 NBT 存储）
            ListTag enderList = new ListTag();
            var enderChest = bot.getEnderChestInventory();
            for (int i = 0; i < enderChest.getContainerSize(); i++) {
                var stack = enderChest.getItem(i);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putByte("Slot", (byte) i);
                    stack.save(itemTag);
                    enderList.add(itemTag);
                }
            }
            data.put("EnderItems", enderList);

            // 状态（仅在 preserveBotState 时保存）
            if (config.preserveBotState) {
                var controller = bot.getActionController();
                // 寻路中的移动/跳跃/疾跑是寻路器的瞬态输入，不应持久化，
                // 否则重进世界后（路径并未保存）假人会无目标地一直向前走
                boolean pathing = controller.isPathfinding();
                CompoundTag st = new CompoundTag();
                st.putBoolean("Attacking", controller.isAttacking());
                st.putBoolean("Using", controller.isUsing());
                st.putBoolean("Sneaking", controller.isSneaking());
                st.putBoolean("Jumping", !pathing && controller.isJumping());
                st.putBoolean("Sprinting", !pathing && controller.isSprinting());
                st.putFloat("Forward", pathing ? 0.0F : controller.getForward());
                st.putFloat("Strafing", pathing ? 0.0F : controller.getStrafing());
                st.putInt("AttackInterval", controller.getAttackInterval());
                st.putInt("UseInterval", controller.getUseInterval());
                st.putFloat("Health", bot.getHealth());
                st.putInt("FoodLevel", bot.getFoodData().getFoodLevel());
                st.putFloat("Saturation", bot.getFoodData().getSaturationLevel());
                st.putFloat("Exhaustion", bot.getFoodData().getExhaustionLevel());
                st.putInt("XpLevel", bot.experienceLevel);
                st.putFloat("XpProgress", bot.experienceProgress);
                ListTag effects = new ListTag();
                bot.getActiveEffects().forEach(effect -> {
                    CompoundTag et = new CompoundTag();
                    effect.save(et);
                    effects.add(et);
                });
                st.put("Effects", effects);
                data.put("State", st);
            }

            manager.botsData.put(botName.toLowerCase(), data);
            manager.setDirty();
            MyBotMod.LOGGER.debug("保存假人数据: {} 在 {}", botName, data.getString("Dimension"));
        } catch (Exception e) {
            MyBotMod.LOGGER.error("无法保存假人数据: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 删除假人数据
     */
    public static void deleteBot(MinecraftServer server, String botName) {
        BotPersistenceManager manager = get(server);
        if (manager != null) {
            // 必须先移除区块票据，因为 removeChunkTicket 需要 botsData 中的维度信息
            manager.removeChunkTicket(server, botName);
            manager.botsData.remove(botName.toLowerCase());
            
            manager.setDirty();
            MyBotMod.LOGGER.info("删除假人数据: {}", botName);
        }
    }
    
    /**
     * 为假人添加区块加载票据
     * 确保假人所在区块保持加载状态
     */
    public void addChunkTicket(MinecraftServer server, String botName, ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        
        // 添加区块加载票据
        level.getChunkSource().addRegionTicket(BOT_CHUNK_TICKET, chunkPos, 2, chunkPos);
        
        // 记录票据（含维度，便于跨维度精确移除）
        botChunkTickets.put(botName.toLowerCase(), new TicketLoc(level.dimension(), chunkPos));
        
        MyBotMod.LOGGER.info("为假人 {} 添加区块加载票据: {} @ {}", botName, chunkPos, level.dimension().location());
    }
    
    /**
     * 移除假人的区块加载票据
     */
    public void removeChunkTicket(MinecraftServer server, String botName) {
        TicketLoc loc = botChunkTickets.remove(botName.toLowerCase());
        if (loc != null) {
            ServerLevel level = server.getLevel(loc.dimension());
            if (level != null) {
                level.getChunkSource().removeRegionTicket(BOT_CHUNK_TICKET, loc.pos(), 2, loc.pos());
                MyBotMod.LOGGER.info("移除假人 {} 的区块加载票据: {}", botName, loc.pos());
            }
        }
    }
    
    /**
     * 更新假人的区块加载票据
     * 当假人移动到新区块时调用
     */
    public static void updateChunkTicket(BotPlayer bot) {
        refreshTicket(bot);
    }

    /**
     * 刷新假人的区块加载票据：
     * - 无条件重新添加当前区块票据（刷新有效期，防止 300tick 过期导致静止假人区块被卸载）
     * - 维度或区块变化时，先从“旧维度”精确移除旧票据（修复跨维度票据泄漏）
     */
    private static void refreshTicket(BotPlayer bot) {
        MinecraftServer server = bot.getServer();
        if (server == null) return;
        
        BotPersistenceManager manager = get(server);
        if (manager == null) return;
        
        String key = bot.getName().getString().toLowerCase();
        ServerLevel level = bot.serverLevel();
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> newDim = level.dimension();
        ChunkPos newChunk = new ChunkPos(bot.blockPosition());
        
        TicketLoc old = manager.botChunkTickets.get(key);
        if (old != null && (!old.dimension().equals(newDim) || !old.pos().equals(newChunk))) {
            ServerLevel oldLevel = server.getLevel(old.dimension());
            if (oldLevel != null) {
                oldLevel.getChunkSource().removeRegionTicket(BOT_CHUNK_TICKET, old.pos(), 2, old.pos());
            }
        }
        
        level.getChunkSource().addRegionTicket(BOT_CHUNK_TICKET, newChunk, 2, newChunk);
        manager.botChunkTickets.put(key, new TicketLoc(newDim, newChunk));
    }
    
    /**
     * 获取所有保存的假人数据
     */
    public static List<CompoundTag> getAllBots(MinecraftServer server) {
        BotPersistenceManager manager = get(server);
        if (manager == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(manager.botsData.values());
    }
    
    // 标记是否已经加载过驻留假人（每个世界独立）
    private static final Map<String, Boolean> worldLoadedFlags = new ConcurrentHashMap<>();
    
    /**
     * 在玩家加入时加载假人
     * 参考 GCA 的实现：在第一个玩家加入时触发加载，而不是在服务器启动时
     * 这样可以确保世界完全加载完成
     * 
     * GCA 的实现要点：
     * 1. 延迟 40 tick（2秒）后加载，确保玩家完全加入世界
     * 2. 只在第一个玩家加入时加载一次
     * 3. 支持多世界（单人游戏切换存档）
     */
    public static void onPlayerJoin(MinecraftServer server, ServerPlayer player) {
        var config = name.modid.config.ModConfig.getInstance();
        
        // 如果未启用驻留功能，不加载
        if (!config.botPersistence) {
            return;
        }
        
        // 获取世界标识符（用于区分不同的存档）
        String worldId = getWorldId(server);
        
        // 检查是否已经加载过驻留假人（避免重复加载）
        // 注意：这里检查的是是否加载过驻留数据，而不是当前是否有假人
        // 因为玩家可能在游戏中创建了新假人，但驻留的假人还没加载
        if (worldLoadedFlags.getOrDefault(worldId, false)) {
            MyBotMod.LOGGER.info("驻留假人已加载过（世界: {}），跳过", worldId);
            return;
        }
        
        // 标记为已加载
        worldLoadedFlags.put(worldId, true);
        
        MyBotMod.LOGGER.info("检测到玩家 {} 加入，准备加载驻留假人", player.getName().getString());
        
        // 延迟加载，确保玩家完全加入世界
        // 参考 GCA：延迟 40 tick（2秒）
        server.tell(new net.minecraft.server.TickTask(
            server.getTickCount() + 40,
            () -> loadAllBotsForPlayer(server, player)
        ));
    }
    
    /**
     * 获取世界标识符
     * 用于区分不同的存档（单人游戏可能切换存档）
     */
    private static String getWorldId(MinecraftServer server) {
        // 使用世界文件夹名称作为标识符
        try {
            return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .toAbsolutePath().toString();
        } catch (Exception e) {
            // 如果获取失败，使用默认标识符
            return "default";
        }
    }
    
    /**
     * 为特定玩家加载所有假人
     * 这是实际执行加载的方法
     * 
     * GCA 的加载流程：
     * 1. 从 SavedData 读取所有假人数据
     * 2. 遍历每个假人数据，检查是否已存在
     * 3. 获取目标维度的世界实例
     * 4. 创建假人实体并设置位置、旋转、游戏模式
     * 5. 恢复假人的状态（动作、物品栏、药水效果等）
     * 6. 添加区块加载票据，确保假人所在区块保持加载
     * 7. 将假人添加到世界和玩家列表
     */
    private static void loadAllBotsForPlayer(MinecraftServer server, ServerPlayer player) {
        List<CompoundTag> bots = getAllBots(server);
        
        if (bots.isEmpty()) {
            MyBotMod.LOGGER.info("没有需要加载的驻留假人");
            return;
        }
        
        MyBotMod.LOGGER.info("开始加载 {} 个驻留假人...", bots.size());
        MyBotMod.LOGGER.info("当前内存中的假人数量: {}", BotManager.getAllBots().size());
        
        int loadedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        
        for (CompoundTag data : bots) {
            String botName = data.getString("Name");
            try {
                // 已存在则跳过（可能是游戏中创建的）
                if (BotManager.hasBot(botName)) {
                    MyBotMod.LOGGER.info("假人 {} 已存在于世界中，跳过加载", botName);
                    skippedCount++;
                    continue;
                }

                String dimStr = data.getString("Dimension");
                ServerLevel level = server.getLevel(
                    net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new net.minecraft.resources.ResourceLocation(dimStr)
                    )
                );
                if (level == null) {
                    MyBotMod.LOGGER.error("无法加载假人 {}：世界 {} 不存在", botName, dimStr);
                    failedCount++;
                    continue;
                }

                Vec3 position = new Vec3(data.getDouble("X"), data.getDouble("Y"), data.getDouble("Z"));
                GameType gameMode = GameType.byName(data.getString("GameMode"), GameType.SURVIVAL);
                UUID botUuid = data.hasUUID("UUID") ? data.getUUID("UUID") : null;
                UUID creatorUuid = data.hasUUID("CreatorUUID") ? data.getUUID("CreatorUUID") : null;
                String creatorName = data.contains("CreatorName") ? data.getString("CreatorName") : null;

                // 透传保存的维度/UUID/创建者，保持跨维度位置与身份一致
                BotPlayer bot = BotManager.createBot(server, player, botName, position, gameMode,
                    level, botUuid, creatorUuid, creatorName);

                if (bot != null) {
                    float yaw = data.getFloat("Yaw");
                    bot.setYRot(yaw);
                    bot.setXRot(data.getFloat("Pitch"));
                    bot.setYHeadRot(yaw);

                    // 恢复个人配置（三态）
                    if (data.contains("Settings")) {
                        CompoundTag settingsTag = data.getCompound("Settings");
                        for (String k : BotSettings.KEYS) {
                            if (settingsTag.contains(k)) {
                                bot.getSettings().set(k, BotSettings.Override.byId(settingsTag.getInt(k)));
                            }
                        }
                    }

                    // 恢复行为播放列表（已不存在的行为文件静默忽略；不自动启动）
                    if (data.contains("Behaviors")) {
                        ListTag behaviorsTag = data.getList("Behaviors", 8);
                        java.util.List<String> behaviorNames = new java.util.ArrayList<>();
                        for (int bi = 0; bi < behaviorsTag.size(); bi++) {
                            behaviorNames.add(behaviorsTag.getString(bi));
                        }
                        name.modid.behavior.BehaviorManager.restoreAssigned(bot, behaviorNames);
                    }

                    // 恢复物品栏（直接读 NBT）
                    if (data.contains("Inventory")) {
                        try {
                            bot.getInventory().load(data.getList("Inventory", 10));
                            bot.getInventory().selected = Math.max(0, Math.min(8, data.getInt("SelectedSlot")));
                        } catch (Exception e) {
                            MyBotMod.LOGGER.error("无法恢复假人 {} 的物品栏: {}", botName, e.getMessage());
                        }
                    }

                    // 恢复末影箱（直接读 NBT）
                    if (data.contains("EnderItems")) {
                        try {
                            ListTag enderItems = data.getList("EnderItems", 10);
                            var enderChest = bot.getEnderChestInventory();
                            enderChest.clearContent();
                            for (int i = 0; i < enderItems.size(); i++) {
                                CompoundTag itemTag = enderItems.getCompound(i);
                                int slot = itemTag.getByte("Slot") & 255;
                                if (slot < enderChest.getContainerSize()) {
                                    var stack = net.minecraft.world.item.ItemStack.of(itemTag);
                                    if (!stack.isEmpty()) enderChest.setItem(slot, stack);
                                }
                            }
                        } catch (Exception e) {
                            MyBotMod.LOGGER.error("无法恢复假人 {} 的末影箱: {}", botName, e.getMessage());
                        }
                    }

                    // 恢复状态（延迟，确保假人完全加载）
                    var config = name.modid.config.ModConfig.getInstance();
                    if (config.preserveBotState && data.contains("State")) {
                        CompoundTag stateTag = data.getCompound("State");
                        server.tell(new net.minecraft.server.TickTask(
                            server.getTickCount() + 5,
                            () -> restoreBotState(bot, stateTag)
                        ));
                    }

                    loadedCount++;
                    MyBotMod.LOGGER.info("成功加载假人: {} [{}] ({}, {}, {})",
                        botName, dimStr,
                        String.format("%.1f", data.getDouble("X")),
                        String.format("%.1f", data.getDouble("Y")),
                        String.format("%.1f", data.getDouble("Z")));
                } else {
                    MyBotMod.LOGGER.error("无法创建假人: {}（createBot 返回 null）", botName);
                    failedCount++;
                }
            } catch (Exception e) {
                MyBotMod.LOGGER.error("加载假人 {} 时发生错误: {}", botName, e.getMessage(), e);
                failedCount++;
            }
        }
        
        // 输出加载结果摘要
        StringBuilder summary = new StringBuilder("[假人模组] 驻留假人加载完成：");
        if (loadedCount > 0) {
            summary.append("成功 ").append(loadedCount).append(" 个");
        }
        if (skippedCount > 0) {
            if (loadedCount > 0) summary.append("，");
            summary.append("跳过 ").append(skippedCount).append(" 个");
        }
        if (failedCount > 0) {
            if (loadedCount > 0 || skippedCount > 0) summary.append("，");
            summary.append("失败 ").append(failedCount).append(" 个");
        }
        
        if (loadedCount > 0 || skippedCount > 0 || failedCount > 0) {
            MyBotMod.LOGGER.info("驻留假人加载完成：{}", summary);
        }
        
        MyBotMod.LOGGER.info("加载完成后内存中的假人数量: {}", BotManager.getAllBots().size());
    }
    
    /**
     * 恢复假人状态
     * 包括动作、健康、饥饿、经验、药水效果等
     */
    private static void restoreBotState(BotPlayer bot, CompoundTag state) {
        try {
            if (bot.isRemoved()) return; // 延迟恢复期间假人可能已被移除，避免对已移除实体操作
            var controller = bot.getActionController();

            if (state.getBoolean("Attacking")) {
                int ai = state.getInt("AttackInterval");
                if (ai > 0) controller.startAttackInterval(ai); else controller.startAttackContinuous();
            }
            if (state.getBoolean("Using")) {
                int ui = state.getInt("UseInterval");
                if (ui > 0) controller.startUseInterval(ui); else controller.startUseContinuous();
            }
            controller.setSneak(state.getBoolean("Sneaking"));
            controller.setJump(state.getBoolean("Jumping"));
            controller.setSprint(state.getBoolean("Sprinting"));

            float forward = state.getFloat("Forward");
            if (forward > 0) controller.moveForward(); else if (forward < 0) controller.moveBackward();
            float strafing = state.getFloat("Strafing");
            if (strafing > 0) controller.moveLeft(); else if (strafing < 0) controller.moveRight();

            bot.setHealth(Math.min(state.getFloat("Health"), bot.getMaxHealth()));
            bot.getFoodData().setFoodLevel(state.getInt("FoodLevel"));
            bot.getFoodData().setSaturation(state.getFloat("Saturation"));
            bot.getFoodData().setExhaustion(state.getFloat("Exhaustion"));

            bot.experienceLevel = state.getInt("XpLevel");
            bot.experienceProgress = state.getFloat("XpProgress");

            if (state.contains("Effects")) {
                ListTag effects = state.getList("Effects", 10);
                for (int i = 0; i < effects.size(); i++) {
                    try {
                        var effect = net.minecraft.world.effect.MobEffectInstance.load(effects.getCompound(i));
                        if (effect != null) bot.addEffect(effect);
                    } catch (Exception e) {
                        MyBotMod.LOGGER.error("无法恢复药水效果: {}", e.getMessage());
                    }
                }
            }
            MyBotMod.LOGGER.info("成功恢复假人 {} 的状态", bot.getName().getString());
        } catch (Exception e) {
            MyBotMod.LOGGER.error("恢复假人状态时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 在服务器关闭时保存所有假人
     * 
     * GCA 的保存策略：
     * 1. 遍历所有在线假人
     * 2. 保存每个假人的完整状态
     * 3. 清理区块加载票据
     * 4. 重置加载标记
     */
    public static void saveAllBots(MinecraftServer server) {
        var config = name.modid.config.ModConfig.getInstance();
        
        // 如果未启用驻留功能，不保存
        if (!config.botPersistence) {
            return;
        }
        
        int savedCount = 0;
        for (BotPlayer bot : BotManager.getAllBots()) {
            saveBot(bot);
            savedCount++;
        }
        
        if (savedCount > 0) {
            MyBotMod.LOGGER.info("保存了 {} 个假人到世界数据", savedCount);
        }
        
        // 重置加载标记，以便下次服务器启动时可以重新加载
        String worldId = getWorldId(server);
        worldLoadedFlags.remove(worldId);
        MyBotMod.LOGGER.info("重置世界 {} 的加载标记", worldId);
    }
    
    /**
     * 定期刷新所有假人的区块加载票据
     * 应该在服务器 tick 中定期调用（例如每 100 tick）
     */
    public static void refreshAllChunkTickets(MinecraftServer server) {
        var config = name.modid.config.ModConfig.getInstance();
        
        // 如果运行时关闭了驻留功能，主动清理残余票据，而非等待自然过期
        if (!config.botPersistence) {
            BotPersistenceManager manager = get(server);
            if (manager != null && !manager.botChunkTickets.isEmpty()) {
                clearAllChunkTickets(server);
            }
            return;
        }
        
        BotPersistenceManager manager = get(server);
        if (manager == null) {
            return;
        }
        
        // 无条件刷新所有在线假人的区块票据（刷新有效期，防止静止假人票据过期）
        for (BotPlayer bot : BotManager.getAllBots()) {
            try {
                refreshTicket(bot);
            } catch (Exception e) {
                MyBotMod.LOGGER.error("刷新假人 {} 的区块票据时发生错误: {}", bot.getName().getString(), e.getMessage());
            }
        }
    }
    
    /**
     * 清理所有区块加载票据
     * 在服务器关闭时调用
     * 遍历所有维度以确保票据被正确移除，不依赖 botsData
     */
    public static void clearAllChunkTickets(MinecraftServer server) {
        BotPersistenceManager manager = get(server);
        if (manager == null) {
            return;
        }
        
        for (Map.Entry<String, TicketLoc> entry : manager.botChunkTickets.entrySet()) {
            TicketLoc loc = entry.getValue();
            ServerLevel level = server.getLevel(loc.dimension());
            if (level != null) {
                level.getChunkSource().removeRegionTicket(BOT_CHUNK_TICKET, loc.pos(), 2, loc.pos());
            } else {
                // 维度不可用时，遍历所有维度兜底移除
                for (ServerLevel l : server.getAllLevels()) {
                    l.getChunkSource().removeRegionTicket(BOT_CHUNK_TICKET, loc.pos(), 2, loc.pos());
                }
            }
        }
        
        manager.botChunkTickets.clear();
        MyBotMod.LOGGER.info("清理了所有区块加载票据");
    }
    
    /**
     * 重置驻留假人加载标记
     * 在服务器启动时调用，确保可以重新加载驻留假人
     * 
     * GCA 的做法：
     * - 每次服务器启动时清除加载标记
     * - 支持多世界（单人游戏切换存档）
     */
    public static void resetLoadedFlag(MinecraftServer server) {
        String worldId = getWorldId(server);
        worldLoadedFlags.remove(worldId);
        MyBotMod.LOGGER.info("重置世界 {} 的加载标记", worldId);
    }
    
    /**
     * 清除所有世界的加载标记
     * 在服务器关闭时调用，防止单人游戏切换存档时的内存泄漏
     */
    public static void clearAllLoadedFlags() {
        worldLoadedFlags.clear();
        MyBotMod.LOGGER.info("已清除所有世界的加载标记");
    }
}
