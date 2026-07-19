package name.modid.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 假人玩家类
 * 继承自 ServerPlayer，代表一个由命令控制的机器人玩家
 * 
 * 实现参考 Carpet Mod 的 EntityPlayerMPFake
 */
public class BotPlayer extends ServerPlayer {
    
    /**
     * 假人的动作控制器
     */
    private final BotActionController actionController;
    
    /**
     * 创建假人的玩家UUID
     */
    private final java.util.UUID creatorUUID;
    
    /**
     * 创建假人的玩家名字
     */
    private final String creatorName;

    /**
     * 假人个人配置（覆盖全局配置）
     */
    private final BotSettings settings = new BotSettings();

    /**
     * 构造函数
     * @param server 服务器实例
     * @param level 世界
     * @param profile 游戏档案（包含假人名字和UUID）
     * @param connection 网络连接
     * @param creator 创建假人的玩家
     */
    public BotPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, Connection connection, ServerPlayer creator) {
        super(server, level, profile);
        this.connection = new FakeServerGamePacketListenerImpl(server, connection, this);
        this.creatorUUID = creator.getUUID();
        this.creatorName = creator.getName().getString();
        this.actionController = new BotActionController(this);
        
        // 设置假人的物理属性，使其能够跳跃、被击退和碰撞
        // 参考 Carpet Mod 的 EntityPlayerMPFake
        // 设置步高为 0.6（允许自动上台阶）
        this.setMaxUpStep(0.6F);
        
        // 确保假人不是旁观者模式（旁观者无法被碰撞）
        // 这在 BotManager.createBot() 中设置游戏模式时会被覆盖
    }

    /**
     * 获取动作控制器
     */
    public BotActionController getActionController() {
        return actionController;
    }

    /**
     * 获取创建者UUID
     */
    public java.util.UUID getCreatorUUID() {
        return creatorUUID;
    }
    
    /**
     * 获取创建者名字
     */
    public String getCreatorName() {
        return creatorName;
    }

    /**
     * 获取假人个人配置（覆盖全局配置）
     */
    public BotSettings getSettings() {
        return settings;
    }

    /**
     * 每tick更新假人状态
     * 参考 Carpet Mod 的 EntityPlayerMPFake.tick()
     */
    @Override
    public void tick() {
        // 每 10 tick 重置位置
        // 这是 Carpet Mod 的做法，确保假人的位置同步
        if (this.level().getServer().getTickCount() % 10 == 0) {
            this.connection.resetPosition();
        }
        
        // 每 200 tick（10秒）保存一次假人数据（如果启用了驻留功能）
        // 按实体 id 错峰，避免所有假人在同一 tick 集中保存造成 I/O 峰值
        if ((this.level().getServer().getTickCount() + Math.floorMod(this.getId(), 200)) % 200 == 0) {
            var config = name.modid.config.ModConfig.getInstance();
            if (config.botPersistence) {
                BotPersistenceManager.saveBot(this);
                // 同时更新区块加载票据（如果假人移动到了新区块）
                BotPersistenceManager.updateChunkTicket(this);
            }
        }
        
        try {
            // 调用父类 tick（包含物理处理）
            // 注意：移动输入的应用在 tick() 开始时通过 ServerPlayerMixin 完成
            super.tick();
            
            // 在 super.tick() 之后调用 doTick()
            // 这是 Carpet Mod 的做法，确保所有玩家逻辑都被处理
            this.doTick();
            
            // 处理饥饿系统（假人个人配置优先于全局配置）
            var config = name.modid.config.ModConfig.getInstance();
            if (!BotSettings.resolve(settings.hunger, config.botHunger)) {
                // 如果禁用饥饿，保持满饱食度
                this.getFoodData().setFoodLevel(20);
                this.getFoodData().setSaturation(20.0F);
            }
        } catch (NullPointerException e) {
            // 记录 NPE 以便调试，而非静默吞掉
            name.modid.MyBotMod.LOGGER.warn("假人 {} tick 时发生 NPE: {}", this.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * 假人死亡时的处理
     * 根据配置决定是否自动重生或移除假人
     */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        super.die(damageSource);
        
        var config = name.modid.config.ModConfig.getInstance();
        
        if (BotSettings.resolve(settings.autoRespawn, config.autoRespawnOnDeath)) {
            // 自动重生：延迟 10 tick 后复活
            // 注意：必须在 deathTime 达到 20（LivingEntity 自动移除阀值）之前复活
            this.level().getServer().tell(new net.minecraft.server.TickTask(
                this.level().getServer().getTickCount() + 10,
                () -> {
                    // 安全检查：确保假人尚未被移除且仍在管理器中
                    if (this.isRemoved() || BotManager.getBot(this.getName().getString()) == null) {
                        return;
                    }
                    // 复活：重置死亡标志、死亡计时、血量、饱食度、效果与着火
                    // 仅重置血量不够：若不清除 dead 标志，isDeadOrDying() 仍为 true 导致被移除
                    this.dead = false;
                    this.deathTime = 0;
                    this.setHealth(this.getMaxHealth());
                    this.getFoodData().setFoodLevel(20);
                    this.getFoodData().setSaturation(5.0F);
                    this.removeAllEffects();
                    this.clearFire();
                    
                    // 传送回重生点（含维度）；若未设置则回退到创建者位置，否则留在原地
                    var respawnPos = this.getRespawnPosition();
                    if (respawnPos != null) {
                        net.minecraft.server.level.ServerLevel respawnLevel =
                            this.level().getServer().getLevel(this.getRespawnDimension());
                        double rx = respawnPos.getX() + 0.5, ry = respawnPos.getY(), rz = respawnPos.getZ() + 0.5;
                        if (respawnLevel != null && respawnLevel != this.serverLevel()) {
                            // 重生点在其他维度：跨维度传送
                            this.teleportTo(respawnLevel, rx, ry, rz, this.getYRot(), this.getXRot());
                        } else {
                            this.teleportTo(rx, ry, rz);
                        }
                    } else {
                        ServerPlayer creator = this.level().getServer().getPlayerList().getPlayer(this.creatorUUID);
                        if (creator != null) {
                            if (creator.level() != this.level()) {
                                this.teleportTo(creator.serverLevel(), creator.getX(), creator.getY(), creator.getZ(), creator.getYRot(), creator.getXRot());
                            } else {
                                this.teleportTo(creator.getX(), creator.getY(), creator.getZ());
                            }
                        }
                    }
                }
            ));
        } else {
            // 从管理器中移除假人
            BotManager.removeBot(this.getName().getString());
        }
    }
    
    /**
     * 假人是否受到伤害
     * 根据配置决定是否可以受到伤害
     */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        var config = name.modid.config.ModConfig.getInstance();
        
        // 如果配置为不受伤害，则忽略所有伤害（假人个人配置优先于全局配置）
        if (!BotSettings.resolve(settings.takeDamage, config.botTakeDamage)) {
            return false;
        }
        
        return super.hurt(source, amount);
    }

    /**
     * 判断是否为假人
     */
    public static boolean isBot(Player player) {
        return player instanceof BotPlayer;
    }

    /**
     * 设置假人的位置和旋转
     */
    public void setPositionAndRotation(Vec3 pos, float yaw, float pitch) {
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setYHeadRot(yaw);
    }
}
