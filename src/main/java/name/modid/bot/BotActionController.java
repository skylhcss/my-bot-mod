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
            bot.swing(InteractionHand.MAIN_HAND);
            
            // 查找最近的可攻击实体
            var nearestEntity = bot.level().getNearestEntity(
                net.minecraft.world.entity.LivingEntity.class,
                net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT,
                bot,
                bot.getX(),
                bot.getEyeY(),
                bot.getZ(),
                bot.getBoundingBox().inflate(3.0D)
            );
            
            // 如果找到实体，则攻击
            if (nearestEntity != null) {
                bot.attack(nearestEntity);
            }
            
            attackingTicks--;
            if (attackingTicks <= 0) {
                attacking = false;
            }
        }
        
        // 处理使用物品动作
        if (using && usingTicks > 0) {
            bot.gameMode.useItem(bot, bot.level(), bot.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND);
            usingTicks--;
            if (usingTicks <= 0) {
                using = false;
            }
        }
        
        // 更新潜行状态
        bot.setShiftKeyDown(sneaking);
        
        // 更新跳跃状态
        if (jumping && bot.onGround()) {
            bot.jumpFromGround();
        }
        
        // 更新疾跑状态
        bot.setSprinting(sprinting);
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
     * 开始攻击
     * @param continuous 是否持续攻击
     */
    public void startAttack(boolean continuous) {
        this.attacking = true;
        this.attackingTicks = continuous ? Integer.MAX_VALUE : 1;
    }

    /**
     * 停止攻击
     */
    public void stopAttack() {
        this.attacking = false;
        this.attackingTicks = 0;
    }

    /**
     * 开始使用物品
     * @param continuous 是否持续使用
     */
    public void startUse(boolean continuous) {
        this.using = true;
        this.usingTicks = continuous ? Integer.MAX_VALUE : 1;
    }

    /**
     * 停止使用物品
     */
    public void stopUse() {
        this.using = false;
        this.usingTicks = 0;
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
     * 向上看
     * @param angle 角度增量
     */
    public void lookUp(float angle) {
        float newPitch = Math.max(-90.0F, bot.getXRot() - angle);
        bot.setXRot(newPitch);
    }

    /**
     * 向下看
     * @param angle 角度增量
     */
    public void lookDown(float angle) {
        float newPitch = Math.min(90.0F, bot.getXRot() + angle);
        bot.setXRot(newPitch);
    }

    /**
     * 向左看
     * @param angle 角度增量
     */
    public void lookLeft(float angle) {
        bot.setYRot(bot.getYRot() - angle);
        bot.setYHeadRot(bot.getYRot());
    }

    /**
     * 向右看
     * @param angle 角度增量
     */
    public void lookRight(float angle) {
        bot.setYRot(bot.getYRot() + angle);
        bot.setYHeadRot(bot.getYRot());
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
        // 查找附近可骑乘的实体（排除假人）
        var nearbyEntities = bot.level().getEntities(
            bot,
            bot.getBoundingBox().inflate(3.0D),
            entity -> entity.isPickable() 
                && !entity.isPassenger() 
                && !(entity instanceof BotPlayer)  // 排除假人
                && entity != bot  // 排除自己
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
