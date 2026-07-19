package name.modid.bot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 假人寻路器（非阻塞 / 分帧增量 A*）
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

    /** 单次寻路最大迭代次数（跨 tick 累计） */
    private static final int MAX_ITERATIONS = 15000;

    /** 每 tick 最多执行的迭代次数（分帧预算，避免卡服） */
    private static final int ITERATIONS_PER_TICK = 3000;

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
    private enum State { IDLE, COMPUTING, FOLLOWING }

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
    private int tickCounter;
    private int recomputeCooldown;

    // ==================== A* 搜索状态（COMPUTING 期间有效） ====================
    private PriorityQueue<PathNode> openSet;
    private Set<Long> closedSet;
    private Map<Long, PathNode> nodeMap;
    private BlockPos searchStart;
    private BlockPos searchEnd;
    private PathNode searchStartNode;
    private PathNode bestNode;
    private float bestH;
    private int iterations;
    /** 本次搜索是否为"重算"（重算期间保留旧路径继续行走） */
    private boolean recomputing;

    /** 单次搜索内的方块状态缓存，减少世界访问 */
    private final Map<Long, BlockState> stateCache = new HashMap<>();

    private static final Direction[] HORIZONTAL = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public BotPathfinder(BotPlayer bot) {
        this.bot = bot;
    }

    /**
     * 开始寻路到指定位置
     * @param target 目标位置（会自动寻找最近的可占据位置）
     * @return 目标附近存在可占据位置并已开始搜索则返回 true；否则 false
     */
    public boolean pathTo(BlockPos target) {
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

        initSearch(start, end, false);
        return true;
    }

    public void cancelPath() {
        this.state = State.IDLE;
        this.currentPath = null;
        this.currentWaypointIndex = 0;
        this.target = null;
        this.destination = null;
        this.ticksStuck = 0;
        this.tickCounter = 0;
        this.recomputeCooldown = 0;
        this.openSet = null;
        this.closedSet = null;
        this.nodeMap = null;
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

        // 有路径就跟随（重算期间也继续走旧路径，避免停顿）
        if (currentPath != null && !currentPath.isEmpty()) {
            followPath();
            if (state == State.IDLE) return; // followPath 可能已取消
        }

        // 推进分帧搜索
        if (state == State.COMPUTING) {
            stepSearch();
        }

        // 跟随期间定期重算
        if (state == State.FOLLOWING && tickCounter % PATH_RECALC_INTERVAL == 0) {
            tryRecompute();
        }
    }

    // ==================== 分帧 A* ====================

    private void initSearch(BlockPos start, BlockPos end, boolean recompute) {
        stateCache.clear();
        this.searchStart = start;
        this.searchEnd = end;
        this.openSet = new PriorityQueue<>();
        this.closedSet = new HashSet<>();
        this.nodeMap = new HashMap<>();
        this.searchStartNode = new PathNode(start, null, 0, heuristic(start, end));
        this.openSet.add(searchStartNode);
        this.nodeMap.put(start.asLong(), searchStartNode);
        this.bestNode = searchStartNode;
        this.bestH = searchStartNode.hCost;
        this.iterations = 0;
        this.recomputing = recompute;
        this.state = State.COMPUTING;
    }

    /** 开始一次重算：保留当前路径继续行走，新路径就绪后再切换 */
    private void startRecompute() {
        recomputeCooldown = RECOMPUTE_COOLDOWN;
        BlockPos start = findStandingPos(bot.blockPosition());
        BlockPos end = findStandingPosNear(target);
        if (end == null) {
            return; // 目标暂不可解析，继续走旧路径
        }
        this.destination = end;
        initSearch(start, end, true);
        ticksStuck = 0;
    }

    /** 受冷却与状态约束的重算入口，避免每 tick 重复触发 */
    private void tryRecompute() {
        if (state != State.COMPUTING && recomputeCooldown <= 0) {
            startRecompute();
        }
    }

    /** 推进 A* 搜索，最多执行 ITERATIONS_PER_TICK 次迭代 */
    private void stepSearch() {
        int budget = ITERATIONS_PER_TICK;
        while (!openSet.isEmpty() && budget-- > 0) {
            if (iterations++ >= MAX_ITERATIONS || openSet.size() > MAX_OPEN_SET_SIZE) {
                finishSearch(null);
                return;
            }

            PathNode current = openSet.poll();
            if (!current.active) continue;

            if (current.pos.equals(searchEnd)) {
                finishSearch(current);
                return;
            }

            closedSet.add(current.pos.asLong());
            if (current.hCost < bestH) {
                bestH = current.hCost;
                bestNode = current;
            }

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                long nk = neighbor.asLong();
                if (closedSet.contains(nk)) continue;

                float tentativeG = current.gCost + moveCost(current.pos, neighbor);
                PathNode existing = nodeMap.get(nk);
                if (existing != null && tentativeG >= existing.gCost) continue;
                if (existing != null) existing.active = false;

                PathNode newNode = new PathNode(neighbor, current, tentativeG, heuristic(neighbor, searchEnd));
                openSet.add(newNode);
                nodeMap.put(nk, newNode);
            }
        }

        if (openSet.isEmpty()) {
            finishSearch(null); // 搜索耗尽，尝试部分路径
        }
    }

    /**
     * 结束一次搜索。endNode 非空表示找到完整路径，否则尝试用最优部分路径。
     */
    private void finishSearch(PathNode endNode) {
        List<BlockPos> newPath = null;
        if (endNode != null) {
            newPath = reconstructPath(endNode);
        } else if (bestNode != null && bestNode != searchStartNode
                && bestNode.hCost < heuristic(searchStart, searchEnd) * 0.8F) {
            newPath = reconstructPath(bestNode);
        }

        // 释放搜索结构
        openSet = null;
        closedSet = null;
        nodeMap = null;
        stateCache.clear();

        if (newPath != null && !newPath.isEmpty()) {
            currentPath = smoothPath(newPath);
            currentWaypointIndex = 0;
            state = State.FOLLOWING;
        } else if (recomputing && currentPath != null && !currentPath.isEmpty()) {
            // 重算失败但仍有旧路径，继续走旧路径
            state = State.FOLLOWING;
        } else {
            cancelPath();
        }
    }

    // ==================== 路径跟随 ====================

    private void followPath() {
        if (currentPath == null || currentPath.isEmpty()) {
            return;
        }
        // 跳过已到达或已越过的路标点，避免因越过目标而回头
        advanceWaypoint();
        if (currentWaypointIndex >= currentPath.size()) {
            if (hasReachedDestination()) { cancelPath(); return; }
            if (state != State.COMPUTING) tryRecompute();
            return;
        }

        BlockPos waypoint = currentPath.get(currentWaypointIndex);
        moveToWaypoint(waypoint);

        // 卡住检测（仅水平距离，避免跳跃/下落/游泳时误判）
        Vec3 currentPos = bot.position();
        if (lastPos != null) {
            double movedH = Math.sqrt(
                Math.pow(currentPos.x - lastPos.x, 2) +
                Math.pow(currentPos.z - lastPos.z, 2));
            if (movedH < STUCK_MOVE_THRESHOLD) {
                ticksStuck++;
                if (ticksStuck > STUCK_THRESHOLD) {
                    ticksStuck = 0;
                    if (state != State.COMPUTING) tryRecompute();
                }
            } else {
                ticksStuck = 0;
            }
        }
        lastPos = currentPos;
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
    
        // 在水中：向前游动；上升/上岸或头部没入时上浮，避免下沉/溺水（下潜时不上浮）
        if (bot.isInWater()) {
            c.setSprint(false);
            c.setJump(dyStep > 0 || (dyStep == 0 && bot.isEyeInFluid(FluidTags.WATER)));
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

    /** 前进到下一个仍在前方且未到达的路标点（跳过已到达/已越过的点，避免回头） */
    private void advanceWaypoint() {
        while (currentWaypointIndex < currentPath.size()) {
            BlockPos b = currentPath.get(currentWaypointIndex);
            boolean passed = currentWaypointIndex > 0
                && hasPassed(currentPath.get(currentWaypointIndex - 1), b);
            if (hasReachedWaypoint(b) || passed) {
                currentWaypointIndex++;
            } else {
                break;
            }
        }
    }

    /** 假人是否已沿 a-&gt;b 方向越过 b（水平投影超过 b） */
    private boolean hasPassed(BlockPos a, BlockPos b) {
        double abx = b.getX() - a.getX();
        double abz = b.getZ() - a.getZ();
        if (abx == 0 && abz == 0) return false;
        double bpx = bot.getX() - (b.getX() + 0.5);
        double bpz = bot.getZ() - (b.getZ() + 0.5);
        return bpx * abx + bpz * abz > 0;
    }

    private boolean hasReachedDestination() {
        if (destination == null) return false;
        double dx = bot.getX() - (destination.getX() + 0.5);
        double dz = bot.getZ() - (destination.getZ() + 0.5);
        double h = Math.sqrt(dx * dx + dz * dz);
        double dyAbs = Math.abs(bot.getY() - destination.getY());
        return h < TARGET_REACH_DISTANCE && dyAbs <= TARGET_REACH_VERTICAL;
    }

    // ==================== 路径平滑 ====================

    /**
     * String-pulling：合并平地直线段以减少路点抖动。
     * 跳跃/下落/游泳过渡作为锚点，不会被跳过。
     */
    private List<BlockPos> smoothPath(List<BlockPos> path) {
        if (path.size() <= 2) return path;

        List<BlockPos> result = new ArrayList<>();
        result.add(path.get(0));
        int i = 0;
        while (i < path.size() - 1) {
            int farthest = i + 1;
            for (int j = i + 2; j < path.size(); j++) {
                if (canWalkStraight(path.get(i), path.get(j))) {
                    farthest = j;
                } else {
                    break;
                }
            }
            result.add(path.get(farthest));
            i = farthest;
        }
        return result;
    }

    /** 两点是否可在同一高度沿直线走过（全程为可占据位置） */
    private boolean canWalkStraight(BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) return false;
        int y = a.getY();
        double x0 = a.getX() + 0.5, z0 = a.getZ() + 0.5;
        double x1 = b.getX() + 0.5, z1 = b.getZ() + 0.5;
        double dist = Math.hypot(x1 - x0, z1 - z0);
        int steps = Math.max(1, (int) Math.ceil(dist * 2));
        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            int bx = Mth.floor(x0 + (x1 - x0) * t);
            int bz = Mth.floor(z0 + (z1 - z0) * t);
            if (!isOccupiable(new BlockPos(bx, y, bz))) return false;
        }
        return true;
    }

    // ==================== 位置规范化 ====================

    private BlockPos findStandingPos(BlockPos pos) {
        if (isOccupiable(pos)) return pos;
        for (int dy = -1; dy >= -3; dy--) {
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

    /** 可占据 = 陆地站立 或 水面游泳 */
    private boolean isOccupiable(BlockPos pos) {
        return isValidStandingPos(pos) || isWaterSwim(pos);
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
        if (footState.blocksMotion()) return false;
        if (!footState.getFluidState().isEmpty()) return false;
        if (isHazardAt(footState)) return false;

        BlockState headState = stateAt(pos.above());
        if (headState.blocksMotion()) return false;
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
        List<BlockPos> neighbors = new ArrayList<>(16);

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            BlockPos adj = pos.offset(d[0], 0, d[1]);

            // 1. 平地行走 / 下水
            if (isOccupiable(adj)) {
                neighbors.add(adj);
                continue;
            }

            // 2. 跳跃上 1 格
            BlockPos up = adj.above();
            if (isOccupiable(up) && canClimb(adj)) {
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

            // 4. 跨越跳跃 / 跑酷：adj 为可通过的空缺
            if (isClear(adj) && isClear(adj.above())) {
                addParkourLandings(pos, d[0], d[1], neighbors);
            }
        }

        // 对角线方向（仅平地/水面，且不切墙角）
        int[][] diags = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        for (int[] d : diags) {
            BlockPos diag = pos.offset(d[0], 0, d[1]);
            BlockPos side1 = pos.offset(d[0], 0, 0);
            BlockPos side2 = pos.offset(0, 0, d[1]);
            if (isClear(side1) && isClear(side2) && isOccupiable(diag)) {
                neighbors.add(diag);
            }
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
                return cell; // 落水，任意高度
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

    /** 前方是否有固体方块作为台阶（可攀登） */
    private boolean canClimb(BlockPos pos) {
        return stateAt(pos).blocksMotion();
    }

    /** 位置是否通畅（非阻挡 + 无流体 + 非危险） */
    private boolean isClear(BlockPos pos) {
        BlockState state = stateAt(pos);
        if (state.blocksMotion()) return false;
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
        return block == Blocks.FIRE
            || block == Blocks.SOUL_FIRE
            || block == Blocks.SWEET_BERRY_BUSH
            || block == Blocks.POWDER_SNOW
            || block == Blocks.WITHER_ROSE
            || block == Blocks.CACTUS;
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
        return horiz + vCost + jumpPenalty + swimPenalty + hazardPenalty(to);
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

    // ==================== 方块状态缓存 ====================

    private BlockState stateAt(BlockPos pos) {
        long key = pos.asLong();
        BlockState cached = stateCache.get(key);
        if (cached != null) return cached;
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
