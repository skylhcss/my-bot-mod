package name.modid.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
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
     * @param creatorUUID 创建假人的玩家 UUID（驻留恢复时为原始创建者，可能不在线）
     * @param creatorName 创建假人的玩家名字
     */
    public BotPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, Connection connection, java.util.UUID creatorUUID, String creatorName) {
        //? if >=1.20.2 {
        /*super(server, level, profile, net.minecraft.server.level.ClientInformation.createDefault());
        this.connection = new FakeServerGamePacketListenerImpl(server, connection, this,
            //? if >=1.20.5 {
            net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false));
            //?} else {
            net.minecraft.server.network.CommonListenerCookie.createInitial(profile));
            //?}
        *///?} else {
        super(server, level, profile);
        this.connection = new FakeServerGamePacketListenerImpl(server, connection, this);
        //?}
        this.creatorUUID = creatorUUID;
        this.creatorName = creatorName;
        this.actionController = new BotActionController(this);

        // 设置假人的物理属性，使其能够跳跃、被击退和碰撞
        // 参考 Carpet Mod 的 EntityPlayerMPFake
        // 设置步高为 0.6（允许自动上台阶）；1.20.5+ 改用步进高度属性且玩家默认即 0.6，无需手动设置
        //? if <1.20.5 {
        this.setMaxUpStep(0.6F);
        //?}
        
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
                // 如果禁用饥饿，保持满饱食度（仅在值不对时写入，减少不必要调用）
                if (this.getFoodData().getFoodLevel() != 20) {
                    this.getFoodData().setFoodLevel(20);
                }
                if (this.getFoodData().getSaturationLevel() < 20.0F) {
                    this.getFoodData().setSaturation(20.0F);
                }
            }
        } catch (Exception e) {
            // 记录完整堆栈以定位根因（而非仅消息掉掉问题）；捕获避免单个假人异常中断服务器 tick 循环
            name.modid.MyBotMod.LOGGER.error("假人 {} tick 时发生异常", this.getName().getString(), e);
        }
    }
    
    /**
     * 移动物理：支持创造模式飞行
     * abilities.flying 时（召唤时继承自创造飞行玩家）跳过重力悬停：
     * 水平按输入移动，跳跃/潜行 上升/下降，落地自动结束飞行（与原版创造飞行一致）。
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.getAbilities().flying && !this.isPassenger()) {
            if (this.onGround()) {
                // 落地结束飞行（与原版行为一致）
                this.getAbilities().flying = false;
            } else {
                float speed = Math.max(0.01F, this.getAbilities().getFlyingSpeed());
                double mx = travelVector.x * speed * 4.0;
                double mz = travelVector.z * speed * 4.0;
                double my = 0.0;
                if (this.jumping) {
                    my += speed * 2.0;
                }
                if (this.isShiftKeyDown()) {
                    my -= speed * 2.0;
                }
                this.setDeltaMovement(mx, my, mz);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(Vec3.ZERO);
                this.calculateEntityAnimation(false);
                return;
            }
        }
        super.travel(travelVector);
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
            scheduleDelayed(10, this::respawnAfterDeath);
        } else {
            // 延迟到下一 tick 移除假人，避免在实体 tick 处理期间同步移除导致 ConcurrentModificationException
            String botName = this.getName().getString();
            scheduleDelayed(1, () -> BotManager.removeBot(botName));
        }
    }

    /** 延迟任务调度（1.21.2+ tell 更名为 schedule） */
    private void scheduleDelayed(int ticks, Runnable task) {
        var server = this.level().getServer();
        //? if >=1.21.2 {
        /*server.schedule(new net.minecraft.server.TickTask(server.getTickCount() + ticks, task));
        *///?} else {
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + ticks, task));
        //?}
    }

    /** 死亡后延迟重生逻辑 */
    private void respawnAfterDeath() {
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

        // 客户端重建：死亡时客户端收到实体事件 byte 3 置 dead=true，
        // 没有任何包能清除；不移除并重新添加实体会永久躺尸并自移除
        this.refreshEntityOnClients();

        // 传送回重生点（含维度）；若未设置则回退到创建者位置，否则留在原地
        var respawnPos = this.getRespawnPosition();
        if (respawnPos != null) {
            net.minecraft.server.level.ServerLevel respawnLevel =
                this.level().getServer().getLevel(this.getRespawnDimension());
            double rx = respawnPos.getX() + 0.5, ry = respawnPos.getY(), rz = respawnPos.getZ() + 0.5;
            if (respawnLevel != null && respawnLevel != this.serverLevel()) {
                // 重生点在其他维度：跨维度传送
                BotManager.teleportCrossLevel(this, respawnLevel, rx, ry, rz, this.getYRot(), this.getXRot());
            } else {
                this.teleportTo(rx, ry, rz);
            }
        } else {
            ServerPlayer creator = this.level().getServer().getPlayerList().getPlayer(this.creatorUUID);
            if (creator != null) {
                if (creator.level() != this.level()) {
                    BotManager.teleportCrossLevel(this, creator.serverLevel(), creator.getX(), creator.getY(), creator.getZ(), creator.getYRot(), creator.getXRot());
                } else {
                    this.teleportTo(creator.getX(), creator.getY(), creator.getZ());
                }
            }
        }
    }

    /**
     * 在所有客户端重建本实体（移除 + 重新添加），清除客户端的死亡状态
     */
    private void refreshEntityOnClients() {
        var playerList = this.level().getServer().getPlayerList();
        playerList.broadcastAll(new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(this.getId()));
        //? if >=1.21 {
        /*// 1.21+ ClientboundAddEntityPacket(Entity) 便捷构造移除，改用完整构造
        playerList.broadcastAll(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(
            this.getId(), this.getUUID(), this.getX(), this.getY(), this.getZ(),
            this.getXRot(), this.getYRot(), this.getType(), 0, this.getDeltaMovement(), (double) this.getYHeadRot()));
        *///?} else if >=1.20.2 {
        // 1.20.2–1.20.x 玩家实体也走通用的 AddEntity 包（AddPlayer 包已移除）
        playerList.broadcastAll(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this));
        //?} else {
        playerList.broadcastAll(new net.minecraft.network.protocol.game.ClientboundAddPlayerPacket(this));
        //?}
    }

    //? if >=1.21.2 {
    /*// 1.21.2+ Entity.hurt 变为 final（返回值改为 void），伤害入口改为 hurtServer（额外接收 ServerLevel）
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel world, net.minecraft.world.damagesource.DamageSource source, float amount) {
        var config = name.modid.config.ModConfig.getInstance();

        // 如果配置为不受伤害，则忽略所有伤害（假人个人配置优先于全局配置）
        if (!BotSettings.resolve(settings.takeDamage, config.botTakeDamage)) {
            return false;
        }

        // 免疫火焰/岩浆伤害（含着火、岩浆、火球等；假人个人配置优先于全局配置）
        if (BotSettings.resolve(settings.fireImmune, config.botFireImmune)
                && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }

        return super.hurtServer(world, source, amount);
    }
    *///?} else {
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

        // 免疫火焰/岩浆伤害（含着火、岩浆、火球等；假人个人配置优先于全局配置）
        if (BotSettings.resolve(settings.fireImmune, config.botFireImmune)
                && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }

        return super.hurt(source, amount);
    }
    //?}

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
