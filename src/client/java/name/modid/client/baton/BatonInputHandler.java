package name.modid.client.baton;

import name.modid.item.ModItems;
import name.modid.net.BotNetworking;
import name.modid.client.BotClientNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <1.21.2 {
import net.minecraft.world.InteractionResultHolder;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 指挥棒客户端交互（全部基于"准星指向"的远距离射线，而非近战交互距离）：
 * - Alt + 右键（看向假人）→ 选中该假人
 * - 右键（无修饰键，看向任意位置）→ 对选中假人下令（寻路/传送）
 *
 * 通过 UseBlock/UseItem 两个回调覆盖"看向近处方块"与"看向远处/空气"两种情况，
 * 统一走 {@link #handleUse}，用自定义射线获取目标，从而实现"看向哪里就到哪里"。
 * UseEntity 回调用于处理近战距离内直接点到假人的情况（并阻止打开设置面板）。
 */
public class BatonInputHandler {

    /** 指挥棒射线距离（格）——远大于原版交互距离，实现"看向远处" */
    private static final double REACH = 160.0;

    public static void register() {
        // 近战距离内直接点到实体（避免打开设置面板，并支持选中/下令）
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!level.isClientSide()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!player.getItemInHand(hand).is(ModItems.COMMAND_BATON)) return InteractionResult.PASS;
            if (!(entity instanceof Player target) || !BatonClientState.isBot(target.getName().getString())) {
                return InteractionResult.PASS;
            }

            if (Screen.hasAltDown()) {
                // Alt+右键假人 → 选中
                BatonClientState.setSelectedBotName(target.getName().getString());
            } else if (!Screen.hasShiftDown() && !Screen.hasControlDown()) {
                commandTo(entity.position());
            }
            return InteractionResult.FAIL; // 手持指挥棒时不打开原版面板/交互
        });

        // 看向近处方块
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> handleUse(player, level, hand));

        // 看向远处/空气（1.21.2+ UseItemCallback 返回值改为 InteractionResult）
        //? if >=1.21.2 {
        /*UseItemCallback.EVENT.register((player, level, hand) -> handleUse(player, level, hand));
        *///?} else {
        UseItemCallback.EVENT.register((player, level, hand) ->
            new InteractionResultHolder<>(handleUse(player, level, hand), player.getItemInHand(hand)));
        //?}
    }

    private static InteractionResult handleUse(Player player, Level level, InteractionHand hand) {
        if (!level.isClientSide()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!player.getItemInHand(hand).is(ModItems.COMMAND_BATON)) return InteractionResult.PASS;
        // Ctrl 保留给切换模式等
        if (Screen.hasControlDown()) return InteractionResult.PASS;

        // Shift 保留给其他用途
        if (Screen.hasShiftDown()) return InteractionResult.PASS;

        HitResult hit = raycast(player, REACH);

        if (Screen.hasAltDown()) {
            // 选人：看向假人时选中
            if (hit.getType() == HitResult.Type.ENTITY) {
                Entity e = ((EntityHitResult) hit).getEntity();
                if (e instanceof Player p && BatonClientState.isBot(p.getName().getString())) {
                    BatonClientState.setSelectedBotName(p.getName().getString());
                }
            }
            return InteractionResult.FAIL;
        }

        // 寻路/传送模式：看向哪里就到哪里
        if (hit.getType() == HitResult.Type.MISS) return InteractionResult.FAIL;
        Vec3 target = BatonClientState.getMode() == BatonClientState.Mode.TELEPORT
            ? teleportTarget(player, hit)
            : hit.getLocation();
        commandTo(target);
        return InteractionResult.FAIL;
    }

    /**
     * 计算安全的传送落点，避免把假人传送进墙体：
     * 取命中面朝向玩家一侧的相邻方块作为落脚点，必要时上移找容身空间、向下贴地。
     */
    private static Vec3 teleportTarget(Player player, HitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK) return hit.getLocation();
        BlockHitResult bhr = (BlockHitResult) hit;
        Level level = player.level();

        // 命中面外侧（玩家一侧）的相邻方块
        BlockPos feet = bhr.getBlockPos().relative(bhr.getDirection());
        // 若无两格容身空间，向上找
        for (int i = 0; i < 4 && !canStand(level, feet); i++) {
            feet = feet.above();
        }
        // 贴地：向下最多 8 格找地面，避免悬空
        for (int i = 0; i < 8; i++) {
            BlockPos below = feet.below();
            if (blocks(level, below)) break;       // 脚下已是地面
            if (!canStand(level, below)) break;     // 下方无容身空间
            feet = below;
        }
        return new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
    }

    private static boolean blocks(Level level, BlockPos pos) {
        return level.getBlockState(pos).blocksMotion();
    }

    private static boolean canStand(Level level, BlockPos pos) {
        return !blocks(level, pos) && !blocks(level, pos.above());
    }

    /** 对当前选中假人下令到目标位置（寻路或传送由模式决定） */
    private static void commandTo(Vec3 target) {
        String botName = BatonClientState.getSelectedBotName();
        if (botName == null || !BatonClientState.isBot(botName)) return;
        int actionType = BatonClientState.getMode() == BatonClientState.Mode.TELEPORT ? 1 : 0;
        sendAction(actionType, botName, target);
    }

    /** 沿玩家视线做射线，优先返回命中的实体，否则返回命中的方块 */
    private static HitResult raycast(Player player, double reach) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = eye.add(view.x * reach, view.y * reach, view.z * reach);

        BlockHitResult blockHit = player.level().clip(
            new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 blockEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        EntityHitResult entityHit = clipEntities(player, eye, blockEnd);
        return entityHit != null ? entityHit : blockHit;
    }

    /** 在 eye→end 线段上寻找最近的可选中实体（限制在方块命中点之前） */
    private static EntityHitResult clipEntities(Player player, Vec3 eye, Vec3 end) {
        Level level = player.level();
        Vec3 delta = end.subtract(eye);
        AABB box = player.getBoundingBox().expandTowards(delta).inflate(1.0D);
        double closestSq = eye.distanceToSqr(end);
        Entity hitEntity = null;
        Vec3 hitVec = null;
        for (Entity e : level.getEntities(player, box, en -> !en.isSpectator() && en.isPickable())) {
            AABB eb = e.getBoundingBox().inflate(e.getPickRadius());
            Optional<Vec3> clip = eb.clip(eye, end);
            if (clip.isPresent()) {
                double d = eye.distanceToSqr(clip.get());
                if (d < closestSq) {
                    closestSq = d;
                    hitEntity = e;
                    hitVec = clip.get();
                }
            }
        }
        return hitEntity == null ? null : new EntityHitResult(hitEntity, hitVec);
    }

    private static void sendAction(int actionType, String botName, Vec3 pos) {
        FriendlyByteBuf buf = BotNetworking.c2s();
        buf.writeVarInt(actionType);
        buf.writeUtf(botName);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        BotClientNetworking.sendBatonAction(buf);
    }
}
