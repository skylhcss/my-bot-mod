package name.modid.bot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.*;

/**
 * 假人寻路器（同步优化 A*）
 * 发起寻路时一次性完成搜索（以迭代/节点上限与高效数据结构约束最坏开销），随后逐格跟随。
 * 节点为可占据位置：脚踩固体（陆地站立）或处于水面（游泳）。
 * 借鉴 Baritone / Minecraft 原生寻路的核心设计。
 *
 * 支持的移动类型：
 * - 平地行走（4 方向 + 4 对角线）
 * - 跳跃上 1 格
 * - 下落（落到地面限高 3 格；落入水中不限高，无摔落伤害）
 * - 跨越跳跃 / 跑酷（跳过 1-3 格裂谷，滞空时保持移动，疾跑跳更远）
 * - 游泳（下水/上岸、水面与水下移动，倾向浮出水面避免溺水）
 */
public class BotPathfinder {

    /** 单次寻路最大迭代次数（硬上限，约束最坏搜索开销） */
    private static final int MAX_ITERATIONS = 15000;

    /** 开放集合最大大小 */
    private static final int MAX_OPEN_SET_SIZE = 25000;

    /** 到达路标点的水平距离阈值 */
    private static final double WAYPOINT_REACH_DISTANCE = 0.6;

    /** 到达终点的水平距离阈值（收紧：必须基本站到目标方块上） */
    private static final double TARGET_REACH_DISTANCE = 0.75;

    /** 到达终点的垂直容差（格） */
    private static final double TARGET_REACH_VERTICAL = 1.25;

    /** 路径重算间隔（tick） */
    private static final int PATH_RECALC_INTERVAL = 100;

    /** 卡住检测阈值（tick） */
    private static final int STUCK_THRESHOLD = 30;

    /** 卡住时的最小水平移动距离 */
    private static final double STUCK_MOVE_THRESHOLD = 0.15;

    /** 邻近危险方块的额外代价惩罚 */
    private static final float HAZARD_COST = 8.0F;

    /** 重算冷却（tick）：避免走到路径末端却未到终点时每 tick 反复重算 */
    private static final int RECOMPUTE_COOLDOWN = 20;

    /** 落到地面的最大安全下落高度（超过则可能摔伤，不走） */
    private static final int FALL_SAFE_LAND = 3;

    /** 向下扫描落点的最大深度（用于落水，水可缓冲任意高度） */
    private static final int MAX_FALL = 24;

    /** 跨越跳跃的最大水平距离（格）：4 表示可跨越 3 格裂谷 */
    private static final int PARKOUR_MAX_DIST = 4;

    /** 寻路器状态 */
    private enum State { IDLE, FOLLOWING }

    private final BotPlayer bot;

    private State state = State.IDLE;
    private BlockPos target;
    /** 规范化后的实际终点（可站立/可游泳位置），到达判定以此为准 */
    private BlockPos destination;

    // 当前正在跟随的路径
    private List<BlockPos> currentPath;
    private int currentWaypointIndex;

    // 跟随状态
    private int ticksStuck;
    private Vec3 lastPos;
    private int consecutiveStuckRecomputes;
    private final LongOpenHashSet avoidPositions = new LongOpenHashSet();
    private int tickCounter;
    private int recomputeCooldown;

    /** 单次搜索内的方块状态缓存，减少世界访问（fastutil 免装箱） */
    private final Long2ObjectOpenHashMap<BlockState> stateCache = new Long2ObjectOpenHashMap<>();

    /** 可复用的邻居列表，避免每次 getNeighbors 分配新 ArrayList */
    private final List<BlockPos> reusableNeighbors = new ArrayList<>(16);

    private static final Direction[] HORIZONTAL = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    /** 直线四方向偏移（static 复用，避免每次 getNeighbors 分配） */
    private static final int[][] STRAIGHT_DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** 对角四方向偏移（static 复用） */
    private static final int[][] DIAGONAL_DIRS = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};

    public BotPathfinder(BotPlayer bot) {
        this.bot = bot;
    }

    /**
     * 开始寻路到指定位置（同步执行一次完整 A* 搜索）
     * @param target 目标位置（会自动寻找最近的可占据位置）
     * @return 找到完整或部分路径并开始跟随则返回 true；目标不可解析/不可达返回 false
     */
    public boolean pathTo(BlockPos target) {
        // 最大寻路距离限制（可配置，超出直接拒绝）
        int maxDist = name.modid.config.ModConfig.getInstance().maxPathfindingDistance;
        if (bot.blockPosition().distSqr(target) > (double) maxDist * maxDist) {
            return false;
        }
        if (bot.isPassenger()) {
            bot.stopRiding();
        }

        this.target = target;
        this.currentPath = null;
        this.currentWaypointIndex = 0;
        this.ticksStuck = 0;
        this.tickCounter = 0;
        this.recomputeCooldown = 0;
        this.lastPos = bot.position();

        BlockPos start = findStandingPos(bot.blockPosition());
        BlockPos end = findStandingPosNear(target);
        if (end == null) {
            this.state = State.IDLE;
            return false;
        }
        this.destination = end;

        List<BlockPos> path = runSearch(start, end);
        if (path == null || path.isEmpty()) {
            this.state = State.IDLE;
            return false;
        }
        this.currentPath = path;
        this.currentWaypointIndex = 0;
        this.state = State.FOLLOWING;
        // 立即跳过已在身后/已越过的起始路标，避免新路径起点在身后导致回头
        advanceWaypoint();
        return true;
    }

    public void cancelPath() {
        this.state = State.IDLE;
        this.currentPath = null;
        this.currentWaypointIndex = 0;
        this.target = null;
        this.destination = null;
        this.ticksStuck = 0;
        this.consecutiveStuckRecomputes = 0;
        this.avoidPositions.clear();
        this.tickCounter = 0;
        this.recomputeCooldown = 0;
        this.stateCache.clear();
        bot.getActionController().stopMovement();
        bot.getActionController().setSprint(false);
        bot.getActionController().setJump(false);
    }

    public boolean isPathfinding() { return state != State.IDLE; }
    public BlockPos getTarget() { return target; }
    public List<BlockPos> getCurrentPath() { return currentPath; }

    /**
     * 每 tick 更新寻路逻辑
     */
    public void tick() {
        if (state == State.IDLE || target == null) return;
        tickCounter++;
        if (recomputeCooldown > 0) recomputeCooldown--;

        // 到达终点（基于规范化终点 + 收紧的水平/垂直容差）
        if (hasReachedDestination()) {
            cancelPath();
            return;
        }

        // 有路径就跟随
        if (currentPath != null && !currentPath.isEmpty()) {
            followPath();
            if (state == State.IDLE) return; // followPath 可能已取消
        }

        // 跟随期间定期重算（同步完成，新路径立即生效）
        if (state == State.FOLLOWING && tickCounter % PATH_RECALC_INTERVAL == 0) {
            tryRecompute();
        }
    }

    // ==================== 同步优化 A* ====================

    /**
     * 同步执行一次完整 A* 搜索，返回完整路径；未达终点时若最优节点已显著逼近则返回部分路径；不可达返回 null。
     * 优化要点：fastutil 长键哈希集合/映射（免装箱）、懒删除代替 decrease-key、
     * 单次搜索方块状态缓存（未加载区块按基岩处理）、可采纳启发式、迭代/节点硬上限。
     */
    private List<BlockPos> runSearch(BlockPos start, BlockPos end) {
        stateCache.clear();
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        LongOpenHashSet closedSet = new LongOpenHashSet();
        Long2ObjectOpenHashMap<PathNode> nodeMap = new Long2ObjectOpenHashMap<>();

        PathNode startNode = new PathNode(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        nodeMap.put(start.asLong(), startNode);
        PathNode bestNode = startNode;
        float bestH = startNode.hCost;
        int iterations = 0;
        PathNode endNode = null;

        while (!openSet.isEmpty()) {
            if (iterations++ >= MAX_ITERATIONS || openSet.size() > MAX_OPEN_SET_SIZE) {
                break; // 超限降级：尝试用最优部分路径
            }

            PathNode current = openSet.poll();
            if (!current.active) continue; // 懒删除：已被更优路径取代的旧节点

            if (current.pos.equals(end)) {
                endNode = current;
                break;
            }

            closedSet.add(current.pos.asLong());
            if (current.hCost < bestH) {
                bestH = current.hCost;
                bestNode = current;
            }

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                long nk = neighbor.asLong();
                if (closedSet.contains(nk)) continue;
                if (!avoidPositions.isEmpty() && avoidPositions.contains(nk)) continue;

                float tentativeG = current.gCost + moveCost(current.pos, neighbor);
                PathNode existing = nodeMap.get(nk);
                if (existing != null && tentativeG >= existing.gCost) continue;
                if (existing != null) existing.active = false;

                PathNode newNode = new PathNode(neighbor, current, tentativeG, heuristic(neighbor, end));
                openSet.add(newNode);
                nodeMap.put(nk, newNode);
            }
        }

        List<BlockPos> result = null;
        if (endNode != null) {
            result = reconstructPath(endNode);
        } else if (bestNode != startNode && bestNode.hCost < heuristic(start, end) * 0.8F) {
            // 部分路径降级：最优节点已把剩余距离缩短 20% 以上，朝目标逼近
            result = reconstructPath(bestNode);
        }
        // 路径平滑（可配置）：在方块缓存尚有效时合并可直行的路标点
        if (result != null && name.modid.config.ModConfig.getInstance().pathfindingSmooth) {
            result = smoothPath(result);
        }
        stateCache.clear();
        return result;
    }

    /** 定期/卡住触发的同步重算：成功则切换新路径，失败保留旧路径继续行走 */
    private void tryRecompute() {
        if (recomputeCooldown > 0) {
            return;
        }
        recomputeCooldown = RECOMPUTE_COOLDOWN;
        BlockPos start = findStandingPos(bot.blockPosition());
        BlockPos end = findStandingPosNear(target);
        if (end == null) {
            return; // 目标暂不可解析，继续走旧路径
        }
        this.destination = end;
        ticksStuck = 0;
        List<BlockPos> newPath = runSearch(start, end);
        if (newPath != null && !newPath.isEmpty()) {
            currentPath = newPath;
            currentWaypointIndex = 0;
            // 立即跳过已在身后/已越过的起始路标，避免重算后新路径起点在身后导致回头
            advanceWaypoint();
        }
        // 重算失败（null）：保留旧路径继续行走；旧路径也走完时由 followPath 处理
    }

    // ==================== 路径跟随 ====================

    private void followPath() {
        if (currentPath == null || currentPath.isEmpty()) {
            return;
        }
        // 打开路径上/身旁可徒手开启的门（避免被关闭的门挡住）
        openDoorsNearby();
        // 跳过已到达或已越过的路标点，避免因越过目标而回头
        advanceWaypoint();
        if (currentWaypointIndex >= currentPath.size()) {
            if (hasReachedDestination()) { cancelPath(); return; }
            // 已走完当前路径但未达终点：停止残留前进，同步重算（受冷却约束），避免失控前冲/漂移
            bot.getActionController().stopMovement();
            tryRecompute();
            return;
        }

        BlockPos waypoint = currentPath.get(currentWaypointIndex);
        moveToWaypoint(waypoint);

        // 卡住检测（仅水平距离，避免跳跃/下落/游泳时误判；自由落体中不计卡住，垂直下落属正常行进）
        Vec3 currentPos = bot.position();
        boolean airborne = !bot.onGround() && !bot.isInWater() && !bot.onClimbable();
        if (!airborne && lastPos != null) {
            double mdx = currentPos.x - lastPos.x;
            double mdz = currentPos.z - lastPos.z;
            double movedH = Math.sqrt(mdx * mdx + mdz * mdz);
            if (movedH < STUCK_MOVE_THRESHOLD) {
                ticksStuck++;
                onStuck();
            } else {
                ticksStuck = 0;
                consecutiveStuckRecomputes = 0;
            }
        }
        lastPos = currentPos;
    }

    /**
     * 分级卡住恢复：先尝试跳跃越过小障碍/台阶，仍卡住则重算路径。
     */
    private void onStuck() {
        if (ticksStuck == 12) {
            // 一级：尝试跳跃（越过 1 格小障碍/台阶）
            if (bot.onGround()) bot.getActionController().setJump(true);
        } else if (ticksStuck > STUCK_THRESHOLD) {
            // 二级：重算路径。每次卡住重算都把当前位置与当前路标加入禁区：
            // A* 是确定性的，不加禁区会反复算回同一条（最初的）路线而卡死；
            // 不能依赖 consecutiveStuckRecomputes 门槛，因为它在假人移动后会被清零
            avoidPositions.add(bot.blockPosition().asLong());
            if (currentPath != null && currentWaypointIndex < currentPath.size()) {
                avoidPositions.add(currentPath.get(currentWaypointIndex).asLong());
            }
            consecutiveStuckRecomputes++;
            // 连续多次卡住，或禁区累积过多（反复受挫说明目的地实际不可达）：放弃，避免无限循环
            if (consecutiveStuckRecomputes >= 4 || avoidPositions.size() > 48) {
                cancelPath();
                return;
            }
            ticksStuck = 0;
            tryRecompute();
        }
    }

    /** 打开身旁（脚部/头部四周）可徒手开启且当前关闭的门/栅栏门/活板门 */
    private void openDoorsNearby() {
        BlockPos base = bot.blockPosition();
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos cell = base.above(dy);
            openIfClosedDoor(cell);
            for (Direction d : HORIZONTAL) {
                openIfClosedDoor(cell.relative(d));
            }
        }
    }

    private void openIfClosedDoor(BlockPos pos) {
        BlockState s = bot.level().getBlockState(pos);
        if (!isPassableDoor(s)) return;
        if (!s.hasProperty(BlockStateProperties.OPEN) || s.getValue(BlockStateProperties.OPEN)) return;
        // 直接切换 OPEN 状态打开门/活板门（规避跨版本不稳定的 BlockState.use 签名；邻域更新使双开门同步打开）
        bot.level().setBlock(pos, s.setValue(BlockStateProperties.OPEN, Boolean.TRUE), 3);
    }

    /**
     * 向路标点移动。设置水平朝向，垂直运动由物理引擎处理。
     * 处理三种情形：游泳（水中上浮前进）、上台阶（疾跑+跳）、跨越裂谷（助跑起跳）。
     */
    private void moveToWaypoint(BlockPos waypoint) {
        BotActionController c = bot.getActionController();
        
        Vec3 look = new Vec3(waypoint.getX() + 0.5, bot.getEyePosition().y, waypoint.getZ() + 0.5);
        c.lookAt(look);
        c.moveForward();
        
        double dx = (waypoint.getX() + 0.5) - bot.getX();
        double dz = (waypoint.getZ() + 0.5) - bot.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        int dyStep = waypoint.getY() - bot.blockPosition().getY();
        
        // 在水中：向前游动；目标不低于当前位置就持续划水保持浮力（上浮/平游），
        // 仅目标在下方时松开下潜。不能依赖 isEyeInFluid：水面漂浮时眼睛出水会停止划水导致下沉
        if (bot.isInWater()) {
            c.setSprint(false);
            c.setJump(dyStep >= 0);
            return;
        }
            
        // 攻爬梯子/藤蔓/脚手架：贴住并按需上/下
        if (bot.onClimbable()) {
            c.setSprint(false);
            // 垂直攀爬：目标就在本列正上/正下时停止水平前进，
            // 避免在藤蔓上前后晃动导致脱落（跳跃输入即可上爬）
            if (horiz < 0.7) {
                c.stopMovement();
            }
            c.setJump(dyStep > 0); // 向上爬按跳；同层/向下不按（缓降）
            return;
        }
                
        boolean gapJump = horiz > 1.6 && gapAhead(dx, dz);
        if (gapJump) {
            // 跨越裂谷：助跑起跳（滞空时物理引擎保持水平移动）
            c.setSprint(true);
            c.setJump(bot.onGround());
        } else if (dyStep > 0) {
            // 上台阶：仅在实际顶到台阶（水平碰撞）时才跳，避免无谓跳跃
            c.setSprint(horiz > 2.5);
            c.setJump(bot.onGround() && bot.horizontalCollision);
        } else {
            // 平地/下坡：不跳，远距离直线时疾跑更快
            c.setSprint(horiz > 2.5);
            c.setJump(false);
        }
    }
    
    /** 判断前方 1 格是否为裂谷边缘（脚下悬空且可通行） */
    private boolean gapAhead(double dx, double dz) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-3) return false;
        int ox = (int) Math.round(dx / len);
        int oz = (int) Math.round(dz / len);
        if (ox == 0 && oz == 0) return false;
        BlockPos ahead = bot.blockPosition().offset(ox, 0, oz);
        return !bot.level().getBlockState(ahead).blocksMotion()
            && !bot.level().getBlockState(ahead.below()).blocksMotion();
    }
    
    private boolean hasReachedWaypoint(BlockPos waypoint) {
        double dx = bot.getX() - (waypoint.getX() + 0.5);
        double dz = bot.getZ() - (waypoint.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz) < WAYPOINT_REACH_DISTANCE;
    }

    /**
     * 前进到下一个仍在前方且未到达的路标点。
     * 跳过条件（任一）：已到达当前路标；或已沿"当前→下一路标"方向越过当前路标。
     * 后者对起始路标（index 0）同样生效，修复重算后新路径起点在身后导致的回头。
     */
    private void advanceWaypoint() {
        while (currentWaypointIndex < currentPath.size()) {
            BlockPos cur = currentPath.get(currentWaypointIndex);
            if (hasReachedWaypoint(cur)) {
                currentWaypointIndex++;
                continue;
            }
            if (currentWaypointIndex + 1 < currentPath.size()
                    && hasPassedToward(cur, currentPath.get(currentWaypointIndex + 1))) {
                currentWaypointIndex++;
                continue;
            }
            break;
        }
    }

    /** 假人是否已沿 cur-&gt;next 方向越过 cur（水平投影超过 cur 朝向 next） */
    private boolean hasPassedToward(BlockPos cur, BlockPos next) {
        double dx = next.getX() - cur.getX();
        double dz = next.getZ() - cur.getZ();
        if (dx == 0 && dz == 0) return false;
        double bx = bot.getX() - (cur.getX() + 0.5);
        double bz = bot.getZ() - (cur.getZ() + 0.5);
        return bx * dx + bz * dz > 0;
    }

    private boolean hasReachedDestination() {
        if (destination == null) return false;
        double dx = bot.getX() - (destination.getX() + 0.5);
        double dz = bot.getZ() - (destination.getZ() + 0.5);
        double h = Math.sqrt(dx * dx + dz * dz);
        double dyAbs = Math.abs(bot.getY() - destination.getY());
        return h < TARGET_REACH_DISTANCE && dyAbs <= TARGET_REACH_VERTICAL;
    }

    // ==================== 位置规范化 ====================

    private BlockPos findStandingPos(BlockPos pos) {
        if (isOccupiable(pos)) return pos;
        // 向下扫描可站立位（覆盖飞行/高处起点，深度与最大下落扫描一致）
        for (int dy = -1; dy >= -MAX_FALL; dy--) {
            BlockPos check = pos.above(dy);
            if (isOccupiable(check)) return check;
        }
        return pos;
    }

    /**
     * 在目标位置附近查找可占据位置：先向下搜 10 格，再向上搜 3 格。
     */
    private BlockPos findStandingPosNear(BlockPos target) {
        if (isOccupiable(target)) return target;
        for (int dy = -1; dy >= -10; dy--) {
            BlockPos check = target.above(dy);
            if (isOccupiable(check)) return check;
        }
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos check = target.above(dy);
            if (isOccupiable(check)) return check;
        }
        return null;
    }

    // ==================== 位置可占据判定 ====================

    /** 可占据 = 陆地站立 或 水面游泳 或 可攻爬（梯子/藤蔓/脚手架） */
    private boolean isOccupiable(BlockPos pos) {
        if (isValidStandingPos(pos) || isClimbablePos(pos)) {
            return true;
        }
        // 游泳路线（可配置关闭）
        return name.modid.config.ModConfig.getInstance().pathfindingAllowSwim && isWaterSwim(pos);
    }

    /**
     * 有效站立位置（陆地）：脚下固体且非危险 + 脚部空气 + 头部空气 + 脚部无流体/危险
     */
    private boolean isValidStandingPos(BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = stateAt(below);
        if (!belowState.blocksMotion()) return false;
        if (isHazardToStandOn(belowState)) return false;

        BlockState footState = stateAt(pos);
        if (footState.blocksMotion() && !isPassableDoor(footState)) return false;
        if (!footState.getFluidState().isEmpty()) return false;
        if (isHazardAt(footState)) return false;

        BlockState headState = stateAt(pos.above());
        if (headState.blocksMotion() && !isPassableDoor(headState)) return false;
        return !isHazardAt(headState);
    }

    /**
     * 可游泳位置（水中）：脚部为水且非固体，头部可容身（非固体、非危险）。
     * 允许完全没入水中的位置，以便水下假人也能开始寻路；游动时会优先浮出水面。
     */
    private boolean isWaterSwim(BlockPos pos) {
        BlockState footState = stateAt(pos);
        if (!footState.getFluidState().is(FluidTags.WATER)) return false;
        if (footState.blocksMotion()) return false;
        if (isHazardAt(footState)) return false;
    
        BlockState headState = stateAt(pos.above());
        if (headState.blocksMotion()) return false;
        return !isHazardAt(headState);
    }
    
    /** 该游泳位置是否完全没入水中（头顶也是水） */
    private boolean isSubmerged(BlockPos pos) {
        return !stateAt(pos.above()).getFluidState().isEmpty();
    }

    // ==================== 邻居生成 ====================

    private List<BlockPos> getNeighbors(BlockPos pos) {
        reusableNeighbors.clear();
        List<BlockPos> neighbors = reusableNeighbors;

        for (int[] d : STRAIGHT_DIRS) {
            BlockPos adj = pos.offset(d[0], 0, d[1]);

            // 1. 平地行走 / 下水
            if (isOccupiable(adj)) {
                neighbors.add(adj);
                continue;
            }

            // 2. 跳跃上 1 格（非栅栏/围墙 + 当前头顶有跳跃空间 + 目标可站）
            BlockPos up = adj.above();
            BlockState adjState = stateAt(adj);
            if (isOccupiable(up) && adjState.blocksMotion() && !isTallBarrier(adjState)
                    && !stateAt(pos.above(2)).blocksMotion()) {
                neighbors.add(up);
                continue;
            }

            // 3. 下落：向下扫描第一个落点（地面限高，水面不限高）
            if (isClear(adj)) {
                BlockPos landing = scanFallLanding(adj);
                if (landing != null) {
                    neighbors.add(landing);
                    continue;
                }
            }

            // 4. 跨越跳跃 / 跑酷：adj 为可通过的空缺（可配置关闭）
            if (name.modid.config.ModConfig.getInstance().pathfindingAllowParkour
                    && isClear(adj) && isClear(adj.above())) {
                addParkourLandings(pos, d[0], d[1], neighbors);
            }
        }

        // 对角线方向（仅平地/水面，且不切墙角）
        for (int[] d : DIAGONAL_DIRS) {
            BlockPos diag = pos.offset(d[0], 0, d[1]);
            BlockPos side1 = pos.offset(d[0], 0, 0);
            BlockPos side2 = pos.offset(0, 0, d[1]);
            if (isClear(side1) && isClear(side2) && isOccupiable(diag)) {
                neighbors.add(diag);
            }
        }

        // 攻爬：当前处于梯子/藤蔓/脚手架时，可垂直上下
        if (isClimbable(pos)) {
            BlockPos up = pos.above();
            if (isClimbable(up) || isOccupiable(up)) neighbors.add(up);
            BlockPos down = pos.below();
            if (isOccupiable(down)) neighbors.add(down);
        }

        return neighbors;
    }

    /**
     * 从 adj 向下扫描第一个落点：
     * - 落到地面：仅当下落高度 <= FALL_SAFE_LAND 时允许（避免摔伤）
     * - 落入水面：任意高度允许（水缓冲，无摔落伤害）
     * 遇到实心方块或障碍则停止。
     */
    private BlockPos scanFallLanding(BlockPos adj) {
        for (int depth = 1; depth <= MAX_FALL; depth++) {
            BlockPos cell = adj.below(depth);
            if (isWaterSwim(cell)) {
                // 落水：仅在允许游泳路线时才可作为落点，否则禁用游泳时路径会引入水中节点
                return name.modid.config.ModConfig.getInstance().pathfindingAllowSwim ? cell : null;
            }
            if (isValidStandingPos(cell)) {
                return depth <= FALL_SAFE_LAND ? cell : null; // 落地，限高
            }
            if (!isClear(cell)) {
                return null; // 被固体/流体挡住
            }
        }
        return null;
    }

    /**
     * 跨越跳跃落点：沿方向 d 从距离 2 到 PARKOUR_MAX_DIST 寻找落点，
     * 要求空中通道（脚+头两格空气）畅通，落点同高或低 1 格。
     */
    private void addParkourLandings(BlockPos pos, int dx, int dz, List<BlockPos> out) {
        for (int dist = 2; dist <= PARKOUR_MAX_DIST; dist++) {
            // 空中通道必须畅通
            BlockPos mid = pos.offset(dx * (dist - 1), 0, dz * (dist - 1));
            if (!isClear(mid) || !isClear(mid.above())) {
                break; // 通道被挡，更远也不行
            }
            // 落点：同高
            BlockPos flat = pos.offset(dx * dist, 0, dz * dist);
            if (isOccupiable(flat)) {
                out.add(flat);
            }
            // 落点：低 1 格
            BlockPos down = pos.offset(dx * dist, -1, dz * dist);
            if (isOccupiable(down)) {
                out.add(down);
            }
        }
    }

    /** 栅栏/围墙等 1.5 格高障碍（玩家无法从同层跳上，需绕行或从高处进入） */
    private boolean isTallBarrier(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.FENCES)
            || state.is(net.minecraft.tags.BlockTags.WALLS);
    }

    /** 位置是否通畅（非阻挡 + 无流体 + 非危险；可徒手开启的门视为可通过） */
    private boolean isClear(BlockPos pos) {
        BlockState state = stateAt(pos);
        if (state.blocksMotion() && !isPassableDoor(state)) return false;
        if (!state.getFluidState().isEmpty()) return false;
        return !isHazardAt(state);
    }

    // ==================== 危险方块 ====================

    /** 站在其上会造成伤害的方块（脚下） */
    private boolean isHazardToStandOn(BlockState below) {
        var block = below.getBlock();
        return block == Blocks.MAGMA_BLOCK
            || block == Blocks.CACTUS
            || block == Blocks.CAMPFIRE
            || block == Blocks.SOUL_CAMPFIRE;
    }

    /** 身处其中会造成伤害的方块（脚/头/路径穿过） */
    private boolean isHazardAt(BlockState state) {
        if (state.getFluidState().is(FluidTags.LAVA)) return true;
        var block = state.getBlock();
        // 细雪：穿皮革靴子可安全通过，否则视为危险
        if (block == Blocks.POWDER_SNOW) return !hasLeatherBoots();
        return block == Blocks.FIRE
            || block == Blocks.SOUL_FIRE
            || block == Blocks.SWEET_BERRY_BUSH
            || block == Blocks.WITHER_ROSE
            || block == Blocks.CACTUS;
    }

    /** 假人是否穿着皮革靴子（可安全踩细雪） */
    private boolean hasLeatherBoots() {
        return bot.getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS);
    }

    /** 可徒手开启的门/栅栏门/活板门（铁门/铁活板门需红石，不算） */
    private boolean isPassableDoor(BlockState state) {
        var b = state.getBlock();
        return (b instanceof DoorBlock && b != Blocks.IRON_DOOR)
            || b instanceof FenceGateBlock
            || (b instanceof TrapDoorBlock && b != Blocks.IRON_TRAPDOOR);
    }

    /** 位置是否为可攻爬方块（梯子/藤蔓/脚手架） */
    private boolean isClimbable(BlockPos pos) {
        return stateAt(pos).is(BlockTags.CLIMBABLE);
    }

    /** 可攻爬站位：处于梯子/藤蔓/脚手架中，且头部可容身 */
    private boolean isClimbablePos(BlockPos pos) {
        if (!isClimbable(pos)) return false;
        BlockState head = stateAt(pos.above());
        return !head.blocksMotion() || isClimbable(pos.above());
    }

    /** 缓慢/高代价通过的方块（蜘蛛网） */
    private boolean isSlowBlock(BlockPos pos) {
        return stateAt(pos).getBlock() == Blocks.COBWEB;
    }

    /** 邻近危险（岩浆/火等）的额外代价 */
    private float hazardPenalty(BlockPos pos) {
        for (Direction d : HORIZONTAL) {
            BlockPos side = pos.relative(d);
            BlockState s = stateAt(side);
            if (isHazardAt(s)) return HAZARD_COST;
            BlockState sideBelow = stateAt(side.below());
            if (sideBelow.getFluidState().is(FluidTags.LAVA)) return HAZARD_COST;
        }
        return 0;
    }

    // ==================== 代价和启发式 ====================

    private float moveCost(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dz = Math.abs(from.getZ() - to.getZ());
        int dy = to.getY() - from.getY();

        int hmax = Math.max(dx, dz);
        int hmin = Math.min(dx, dz);
        float horiz = 1.414F * hmin + (hmax - hmin); // 对角 + 直线

        float vCost = dy > 0 ? 1.5F * dy : (dy < 0 ? 0.5F * (-dy) : 0);
        float jumpPenalty = hmax >= 2 ? 3.0F : 0;      // 跨越跳跃
        float swimPenalty = 0;
        if (isWaterSwim(to)) {
            swimPenalty = isSubmerged(to) ? 4.0F : 2.0F; // 游泳较慢，潜水更不优先（倾向水面）
        }
        // 门：可通过但略增代价（优先普通路径）；蜘蛛网：穿越极慢，高代价；攻爬：中等代价
        float doorPenalty = (isPassableDoor(stateAt(to)) || isPassableDoor(stateAt(to.above()))) ? 2.0F : 0F;
        float cobwebPenalty = (isSlowBlock(to) || isSlowBlock(to.above())) ? 12.0F : 0F;
        float climbPenalty = isClimbable(to) ? 2.0F : 0F;
        return horiz + vCost + jumpPenalty + swimPenalty + doorPenalty + cobwebPenalty + climbPenalty + hazardPenalty(to);
    }

    /**
     * 启发式函数：XZ 平面 2D 八方向距离 + 垂直项。
     * 假人移动本质是水平行走 + 独立跳/落，故用 2D octile 更贴合、可采纳。
     */
    private float heuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dz = Math.abs(a.getZ() - b.getZ());
        int dy = Math.abs(a.getY() - b.getY());
        int dmax = Math.max(dx, dz);
        int dmin = Math.min(dx, dz);
        return dmax + 0.414F * dmin + 0.5F * dy;
    }

    private List<BlockPos> reconstructPath(PathNode endNode) {
        List<BlockPos> path = new ArrayList<>();
        PathNode current = endNode;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    // ==================== 路径平滑（宽度感知视线法） ====================

    /**
     * 智能路径平滑：贪心地从当前锚点向最远可直达的路标点合并，减少迁回抖动的中间路点。
     * 与早期 String-pulling 的区别（更智能、不偏航/不卡角）：
     * 1. 只在同一高度间合并，跳跃/下落/游泳等高度变化点作为锚点保留；
     * 2. 复用 A* 的同一套 isValidStandingPos 判定，保证与搜索时一致；
     * 3. 采样间隔加密到 0.25 格，并在中间采样点额外校验 ±0.3 侧向净空（约实体半宽），
     *    避免直行线贴墙角太近导致碰撞卡住。
     */
    private List<BlockPos> smoothPath(List<BlockPos> path) {
        if (path.size() <= 2) {
            return path;
        }
        List<BlockPos> result = new ArrayList<>();
        result.add(path.get(0));
        int anchor = 0;
        while (anchor < path.size() - 1) {
            int farthest = anchor + 1;
            // 从最远向近处找第一个可直达的点（比逐个向前探更少的判定次数）
            for (int j = path.size() - 1; j > anchor + 1; j--) {
                if (canShortcut(path.get(anchor), path.get(j))) {
                    farthest = j;
                    break;
                }
            }
            result.add(path.get(farthest));
            anchor = farthest;
        }
        return result;
    }

    /** 两点间能否安全直行（同高度 + 全程中心线与侧向净空均可站立） */
    private boolean canShortcut(BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) {
            return false; // 高度变化点（跳/落/游）不平滑
        }
        int y = a.getY();
        double x0 = a.getX() + 0.5, z0 = a.getZ() + 0.5;
        double x1 = b.getX() + 0.5, z1 = b.getZ() + 0.5;
        double dx = x1 - x0, dz = z1 - z0;
        double dist = Math.hypot(dx, dz);
        if (dist < 1e-3) {
            return true;
        }
        // 单位方向与垂直方向（用于侧向净空采样）
        double nx = dx / dist, nz = dz / dist;
        double px = -nz, pz = nx;
        int steps = (int) Math.ceil(dist / 0.25);
        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            double cx = x0 + dx * t, cz = z0 + dz * t;
            if (!walkableAt(cx, y, cz)) {
                return false;
            }
            // 中间采样点额外校验两侧净空，防止贴角；端点本身在路径上不查侧向
            if (s > 0 && s < steps) {
                if (!walkableAt(cx + px * 0.3, y, cz + pz * 0.3)) {
                    return false;
                }
                if (!walkableAt(cx - px * 0.3, y, cz - pz * 0.3)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 某连续水平坐标在指定高度是否为可站立位置（脚下固体可站、无危险、头部可容身） */
    private boolean walkableAt(double x, int y, double z) {
        return isValidStandingPos(new BlockPos(Mth.floor(x), y, Mth.floor(z)));
    }

    // ==================== 方块状态缓存 ====================

    private BlockState stateAt(BlockPos pos) {
        long key = pos.asLong();
        BlockState cached = stateCache.get(key);
        if (cached != null) return cached;
        // 未加载的区块返回“不可通行”状态，避免计算出穿越未加载区块的无效路径
        if (!bot.level().hasChunkAt(pos)) {
            BlockState barrier = Blocks.BEDROCK.defaultBlockState();
            stateCache.put(key, barrier);
            return barrier;
        }
        BlockState state = bot.level().getBlockState(pos);
        stateCache.put(key, state);
        return state;
    }

    // ==================== 内部类 ====================

    private static class PathNode implements Comparable<PathNode> {
        final BlockPos pos;
        final PathNode parent;
        final float gCost;
        final float hCost;
        final float fCost;
        boolean active = true;

        PathNode(BlockPos pos, PathNode parent, float gCost, float hCost) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }

        @Override
        public int compareTo(PathNode other) {
            return Float.compare(this.fCost, other.fCost);
        }
    }
}
