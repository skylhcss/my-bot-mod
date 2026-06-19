package name.modid.bot;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/**
 * 假人动作控制器
 * 负责控制假人的各种动作，如攻击、使用物品、潜行、跳跃、看向等
 * 参考 Carpet Mod 的 EntityPlayerActionPack
 */
public class BotActionController {
    
    private final BotPlayer bot;
    
    // 动作状态
    private boolean attacking = false;
    private boolean using = false;
    private boolean sneaking = false;
    private boolean jumping = false;
    private boolean sprinting = false;
    
    // 移动状态（参考 Carpet Mod）
    private float forward = 0.0F;   // 前后移动（zza）
    private float strafing = 0.0F;  // 左右移动（xxa）
    
    // 连续动作计数器
    private int attackingTicks = 0;
    private int usingTicks = 0;
    
    // 间隔动作（参考 Carpet Mod）
    private int attackInterval = 0;  // 攻击间隔（tick）
    private int attackIntervalCounter = 0;  // 攻击间隔计数器
    private int useInterval = 0;  // 使用间隔（tick）
    private int useIntervalCounter = 0;  // 使用间隔计数器
    
    public BotActionController(BotPlayer bot) {
        this.bot = bot;
    }

    /**
     * 每tick更新动作状态
     * 参考 Carpet Mod 的 EntityPlayerActionPack.onUpdate()
     */
    public void tick() {
        // 处理攻击动作
        if (attacking && attackingTicks > 0) {
            // 如果是间隔模式，检查间隔计数器
            if (attackInterval > 0) {
                attackIntervalCounter++;
                if (attackIntervalCounter >= attackInterval) {
                    attackIntervalCounter = 0;
                    performAttack();
                }
            } else {
                // 持续模式或单次模式
                performAttack();
            }
            
            attackingTicks--;
            if (attackingTicks <= 0) {
                attacking = false;
                attackInterval = 0;
                attackIntervalCounter = 0;
            }
        }
        
        // 处理使用物品动作
        if (using && usingTicks > 0) {
            // 如果是间隔模式，检查间隔计数器
            if (useInterval > 0) {
                useIntervalCounter++;
                if (useIntervalCounter >= useInterval) {
                    useIntervalCounter = 0;
                    performUse();
                }
            } else {
                // 持续模式或单次模式
                performUse();
            }
            
            usingTicks--;
            if (usingTicks <= 0) {
                using = false;
                useInterval = 0;
                useIntervalCounter = 0;
            }
        }
        
        // 更新潜行状态
        bot.setShiftKeyDown(sneaking);
        
        // 更新疾跑状态
        // 必须在跳跃之前设置疾跑状态，因为疾跑会影响移动速度
        bot.setSprinting(sprinting);
        
        // 更新跳跃状态
        // 使用 setJumping() 而不是直接调用 jumpFromGround()
        // 这样可以让游戏的物理引擎正确处理跳跃
        bot.setJumping(jumping);
    }
    
    /**
     * 执行攻击动作
     * 参考 Carpet Mod：使用射线追踪检测视线前方的目标
     * 1. 优先攻击视线前方的实体
     * 2. 如果没有实体，则挖掘视线前方的方块
     */
    private void performAttack() {
        bot.swing(InteractionHand.MAIN_HAND);
        
        // 从配置获取攻击距离
        var config = name.modid.config.ModConfig.getInstance();
        double reachDistance = bot.gameMode.getGameModeForPlayer().isCreative() 
            ? config.creativeAttackReachDistance 
            : config.attackReachDistance;
        
        // 如果启用了杀戮光环
        if (config.enableKillAura) {
            performKillAura(config.killAuraRange);
            return;
        }
        
        // 执行射线追踪
        var hitResult = bot.pick(reachDistance, 0.0F, false);
        
        // 检查是否击中实体（此处 killAura 已在上方 return，无需分支判断）
        var entityHitResult = getEntityHitResult(bot, reachDistance);
        
        if (entityHitResult != null && entityHitResult.getEntity() instanceof net.minecraft.world.entity.LivingEntity) {
            // 攻击实体
            bot.attack(entityHitResult.getEntity());
        } else if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            // 挖掘方块
            var blockHitResult = (net.minecraft.world.phys.BlockHitResult) hitResult;
            var blockPos = blockHitResult.getBlockPos();
            var blockState = bot.level().getBlockState(blockPos);
            
            // 只有在非空气方块时才尝试破坏
            if (!blockState.isAir()) {
                // 尝试破坏方块
                // 在创造模式下直接破坏，在生存模式下需要持续挖掘
                if (bot.gameMode.getGameModeForPlayer().isCreative()) {
                    // 创造模式：直接破坏
                    bot.gameMode.destroyBlock(blockPos);
                } else {
                    // 生存模式：持续挖掘
                    // 使用 continueDestroyBlock 来模拟持续挖掘
                    bot.gameMode.destroyBlock(blockPos);
                }
            }
        }
    }
    
    /**
     * 执行杀戮光环（攻击范围内所有实体）
     * @param range 攻击范围
     */
    private void performKillAura(double range) {
        // 查找范围内的所有生物实体
        var entities = bot.level().getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class,
            bot.getBoundingBox().inflate(range),
            entity -> entity != bot && !entity.isSpectator()
        );
        
        // 攻击所有实体
        for (var entity : entities) {
            bot.attack(entity);
        }
    }
    
    /**
     * 获取视线前方的实体
     * 使用射线追踪检测假人视线前方的实体
     * @param player 玩家
     * @param reachDistance 攻击距离
     * @return 实体命中结果，如果没有则返回 null
     */
    private net.minecraft.world.phys.EntityHitResult getEntityHitResult(net.minecraft.server.level.ServerPlayer player, double reachDistance) {
        // 获取玩家的视线方向
        var viewVector = player.getViewVector(1.0F);
        var eyePosition = player.getEyePosition(1.0F);
        var reachVector = eyePosition.add(viewVector.x * reachDistance, viewVector.y * reachDistance, viewVector.z * reachDistance);
        
        // 创建边界框用于检测
        var aabb = player.getBoundingBox().expandTowards(viewVector.scale(reachDistance)).inflate(1.0D);
        
        // 查找视线范围内的所有实体
        var entities = player.level().getEntities(player, aabb, entity -> 
            !entity.isSpectator() && entity.isPickable()
        );
        
        net.minecraft.world.phys.EntityHitResult closestHit = null;
        double closestDistance = reachDistance;
        
        // 遍历所有实体，找到最近的被视线击中的实体
        for (var entity : entities) {
            var entityAABB = entity.getBoundingBox().inflate(entity.getPickRadius());
            var clipResult = entityAABB.clip(eyePosition, reachVector);
            
            if (clipResult.isPresent()) {
                double distance = eyePosition.distanceTo(clipResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestHit = new net.minecraft.world.phys.EntityHitResult(entity, clipResult.get());
                }
            }
        }
        
        return closestHit;
    }
    
    /**
     * 执行右键交互（模拟右键点击）
     * 优先级：实体交互 > 方块交互 > 使用物品
     */
    private void performUse() {
        bot.swing(InteractionHand.MAIN_HAND);
        
        var config = name.modid.config.ModConfig.getInstance();
        double reachDistance = bot.gameMode.getGameModeForPlayer().isCreative()
            ? config.creativeAttackReachDistance
            : config.attackReachDistance;
        
        // 射线追踪：检测视线前方目标
        var blockHitResult = bot.pick(reachDistance, 0.0F, false);
        var entityHitResult = getEntityHitResult(bot, reachDistance);
        
        if (entityHitResult != null) {
            // 优先与实体交互
            var entity = entityHitResult.getEntity();
            var hand = InteractionHand.MAIN_HAND;
            var result = entity.interact(bot, hand);
            if (!result.consumesAction()) {
                bot.attack(entity);
            }
        } else if (blockHitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            // 与方块交互（放置方块、打开箱子等）
            var hitResult = (net.minecraft.world.phys.BlockHitResult) blockHitResult;
            var itemInHand = bot.getItemInHand(InteractionHand.MAIN_HAND);
            bot.gameMode.useItemOn(bot, bot.level(), itemInHand, InteractionHand.MAIN_HAND, hitResult);
        } else {
            // 没有目标：使用物品（拉弓、吃食物等）
            bot.gameMode.useItem(bot, bot.level(), bot.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND);
        }
    }
    
    /**
     * 应用移动输入到假人
     * 这个方法在 ServerPlayerMixin 中每个 tick 被调用
     * 参考 Carpet Mod 的实现：
     * - 每个 tick 都更新 zza 和 xxa，即使值为 0
     * - 潜行时速度降低到 0.3 倍
     */
    public void applyMovement() {
        float vel = sneaking ? 0.3F : 1.0F;
        // 关键：始终设置这些值，即使是 0
        // Carpet Mod 的条件：if (forward != 0.0F || player instanceof EntityPlayerMPFake)
        // 对于假人，始终更新这些值
        bot.zza = forward * vel;
        bot.xxa = strafing * vel;
    }

    /**
     * 开始攻击（单次）
     * 参考 Carpet Mod: /player <name> attack once
     */
    public void startAttackOnce() {
        this.attacking = true;
        this.attackingTicks = 1;
        this.attackInterval = 0;
        this.attackIntervalCounter = 0;
    }

    /**
     * 开始攻击（持续）
     * 参考 Carpet Mod: /player <name> attack continuous
     */
    public void startAttackContinuous() {
        this.attacking = true;
        this.attackingTicks = Integer.MAX_VALUE;
        this.attackInterval = 0;
        this.attackIntervalCounter = 0;
    }
    
    /**
     * 开始攻击（间隔）
     * 参考 Carpet Mod: /player <name> attack interval <ticks>
     * @param interval 攻击间隔（tick）
     */
    public void startAttackInterval(int interval) {
        this.attacking = true;
        this.attackingTicks = Integer.MAX_VALUE;
        this.attackInterval = interval;
        this.attackIntervalCounter = 0;
    }

    /**
     * 停止攻击
     */
    public void stopAttack() {
        this.attacking = false;
        this.attackingTicks = 0;
        this.attackInterval = 0;
        this.attackIntervalCounter = 0;
    }

    /**
     * 开始使用物品（单次）
     * 参考 Carpet Mod: /player <name> use once
     */
    public void startUseOnce() {
        this.using = true;
        this.usingTicks = 1;
        this.useInterval = 0;
        this.useIntervalCounter = 0;
    }

    /**
     * 开始使用物品（持续）
     * 参考 Carpet Mod: /player <name> use continuous
     */
    public void startUseContinuous() {
        this.using = true;
        this.usingTicks = Integer.MAX_VALUE;
        this.useInterval = 0;
        this.useIntervalCounter = 0;
    }
    
    /**
     * 开始使用物品（间隔）
     * 参考 Carpet Mod: /player <name> use interval <ticks>
     * @param interval 使用间隔（tick）
     */
    public void startUseInterval(int interval) {
        this.using = true;
        this.usingTicks = Integer.MAX_VALUE;
        this.useInterval = interval;
        this.useIntervalCounter = 0;
    }

    /**
     * 停止使用物品
     */
    public void stopUse() {
        this.using = false;
        this.usingTicks = 0;
        this.useInterval = 0;
        this.useIntervalCounter = 0;
    }

    /**
     * 设置潜行状态
     */
    public void setSneak(boolean sneak) {
        this.sneaking = sneak;
    }

    /**
     * 设置跳跃状态
     */
    public void setJump(boolean jump) {
        this.jumping = jump;
    }

    /**
     * 设置疾跑状态
     */
    public void setSprint(boolean sprint) {
        this.sprinting = sprint;
    }

    /**
     * 看向正上方（pitch = -90）
     */
    public void lookUp() {
        bot.setXRot(-90.0F);
    }

    /**
     * 看向正下方（pitch = 90）
     */
    public void lookDown() {
        bot.setXRot(90.0F);
    }

    /**
     * 向左转 90 度（相对当前朝向）
     */
    public void lookLeft() {
        float newYaw = bot.getYRot() - 90.0F;
        bot.setYRot(newYaw);
        bot.setYHeadRot(newYaw);
    }

    /**
     * 向右转 90 度（相对当前朝向）
     */
    public void lookRight() {
        float newYaw = bot.getYRot() + 90.0F;
        bot.setYRot(newYaw);
        bot.setYHeadRot(newYaw);
    }

    /**
     * 看向指定方向
     * @param yaw 偏航角
     * @param pitch 俯仰角
     */
    public void lookAt(float yaw, float pitch) {
        bot.setYRot(yaw);
        bot.setXRot(Math.max(-90.0F, Math.min(90.0F, pitch)));
        bot.setYHeadRot(yaw);
    }

    /**
     * 看向指定位置
     * @param target 目标位置
     */
    public void lookAt(Vec3 target) {
        Vec3 botPos = bot.getEyePosition();
        Vec3 direction = target.subtract(botPos).normalize();
        
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.atan2(-direction.y, horizontalDistance));
        
        lookAt(yaw, pitch);
    }

    /**
     * 向前移动
     */
    public void moveForward() {
        forward = 1.0F;
    }

    /**
     * 向后移动
     */
    public void moveBackward() {
        forward = -1.0F;
    }

    /**
     * 向左移动
     */
    public void moveLeft() {
        strafing = 1.0F;
    }

    /**
     * 向右移动
     */
    public void moveRight() {
        strafing = -1.0F;
    }

    /**
     * 停止移动
     */
    public void stopMovement() {
        forward = 0.0F;
        strafing = 0.0F;
    }

    /**
     * 停止所有动作
     */
    public void stopAll() {
        stopAttack();
        stopUse();
        setSneak(false);
        setJump(false);
        setSprint(false);
        stopMovement();
    }

    /**
     * 丢弃当前手持物品（一个）
     */
    public void dropItem() {
        bot.drop(false);
    }

    /**
     * 丢弃当前手持物品（整组）
     */
    public void dropStack() {
        bot.drop(true);
    }

    /**
     * 交换主副手物品
     */
    public void swapHands() {
        // 使用正确的方法交换主副手物品
        var inventory = bot.getInventory();
        var offhandItem = inventory.offhand.get(0);
        var mainhandItem = inventory.getSelected();
        
        inventory.offhand.set(0, mainhandItem);
        inventory.setItem(inventory.selected, offhandItem);
    }

    /**
     * 骑乘附近的实体
     */
    public boolean mount() {
        var config = name.modid.config.ModConfig.getInstance();
        
        // 查找附近可骑乘的实体
        var nearbyEntities = bot.level().getEntities(
            bot,
            bot.getBoundingBox().inflate(3.0D),
            entity -> {
                // 基本条件
                if (!entity.isPickable() || entity.isPassenger() || entity == bot) {
                    return false;
                }
                
                // 检查是否允许骑乘其他假人
                if (entity instanceof BotPlayer && !config.allowMountOtherBots) {
                    return false;
                }
                
                // 检查白名单
                String entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(entity.getType()).toString();
                return config.mountWhitelist.isEmpty() || config.mountWhitelist.contains(entityId);
            }
        );

        for (var entity : nearbyEntities) {
            if (bot.startRiding(entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 下马/离开当前骑乘的实体
     */
    public boolean dismount() {
        if (bot.isPassenger()) {
            bot.stopRiding();
            return true;
        }
        return false;
    }

    /**
     * 旋转视角（相对旋转）
     * @param yawDelta 偏航角增量
     * @param pitchDelta 俯仰角增量
     */
    public void turn(float yawDelta, float pitchDelta) {
        float newYaw = bot.getYRot() + yawDelta;
        float newPitch = Math.max(-90.0F, Math.min(90.0F, bot.getXRot() + pitchDelta));
        bot.setYRot(newYaw);
        bot.setXRot(newPitch);
        bot.setYHeadRot(newYaw);
    }
}
