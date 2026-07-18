package name.modid.bot;

import name.modid.MyBotMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 假人寻路器
 * 基于 A* 算法，所有路径节点都是"站立位置"：脚踩固体方块 + 膝盖和头部有空间
 * 借鉴 Baritone / Minecraft 原生寻路的核心设计
 *
 * 支持的移动类型：
 * - 平地行走（4方向 + 4对角线）
 * - 跳跃上1格（4方向）
 * - 下落1-3格（4方向）
 */
public class BotPathfinder {

    /** 每次寻路最大迭代次数 */
    private static final int MAX_ITERATIONS = 15000;

    /** 开放集合最大大小 */
    private static final int MAX_OPEN_SET_SIZE = 25000;

    /** 到达路标点的水平距离阈值 */
    private static final double WAYPOINT_REACH_DISTANCE = 0.6;

    /** 到达终点的水平距离阈值 */
    private static final double TARGET_REACH_DISTANCE = 1.5;

    /** 路径重算间隔（tick） */
    private static final int PATH_RECALC_INTERVAL = 100;

    /** 卡住检测阈值（tick） */
    private static final int STUCK_THRESHOLD = 30;

    /** 卡住时的最小水平移动距离 */
    private static final double STUCK_MOVE_THRESHOLD = 0.15;

    private final BotPlayer bot;
    private List<BlockPos> currentPath;
    private int currentWaypointIndex;
    private BlockPos target;
    private boolean isPathfinding;
    private int ticksStuck;
    private Vec3 lastPos;
    private int tickCounter;

    public BotPathfinder(BotPlayer bot) {
        this.bot = bot;
    }

    /**
     * 开始寻路到指定位置
     * @param target 目标位置（会自动寻找最近的可站立位置）
     * @return 是否成功找到初始路径
     */
    public boolean pathTo(BlockPos target) {
        if (bot.isPassenger()) {
            bot.stopRiding();
        }

        this.target = target;
        this.isPathfinding = true;
        this.currentWaypointIndex = 0;
        this.ticksStuck = 0;
        this.tickCounter = 0;
        this.lastPos = bot.position();

        // 规范化起始和目标位置为有效站立位置
        BlockPos start = findStandingPos(bot.blockPosition());
        BlockPos end = findStandingPosNear(target);

        if (end == null) {
            MyBotMod.LOGGER.warn("寻路目标附近无可站立位置: {}", target);
            this.isPathfinding = false;
            return false;
        }

        this.currentPath = findPath(start, end);

        if (this.currentPath == null || this.currentPath.isEmpty()) {
            this.isPathfinding = false;
            return false;
        }

        return true;
    }

    public void cancelPath() {
        this.isPathfinding = false;
        this.currentPath = null;
        this.currentWaypointIndex = 0;
        this.target = null;
        this.ticksStuck = 0;
        this.tickCounter = 0;
        bot.getActionController().stopMovement();
        bot.getActionController().setSprint(false);
    }

    public boolean isPathfinding() { return isPathfinding; }
    public BlockPos getTarget() { return target; }
    public List<BlockPos> getCurrentPath() { return currentPath; }

    /**
     * 每 tick 更新寻路逻辑
     */
    public void tick() {
        if (!isPathfinding || target == null) return;
        tickCounter++;

        // 检查是否到达终点（仅水平距离）
        Vec3 targetCenter = Vec3.atCenterOf(target);
        double hDist = Math.sqrt(
            Math.pow(bot.getX() - targetCenter.x, 2) +
            Math.pow(bot.getZ() - targetCenter.z, 2));
        if (hDist < TARGET_REACH_DISTANCE) {
            cancelPath();
            return;
        }

        // 定期重新计算路径
        if (tickCounter % PATH_RECALC_INTERVAL == 0) {
            BlockPos start = findStandingPos(bot.blockPosition());
            BlockPos end = findStandingPosNear(target);
            if (end != null) {
                List<BlockPos> newPath = findPath(start, end);
                if (newPath != null && !newPath.isEmpty()) {
                    currentPath = newPath;
                    currentWaypointIndex = 0;
                } else {
                    cancelPath();
                    return;
                }
            }
        }

        followPath();
    }

    /**
     * 跟随当前路径
     */
    private void followPath() {
        if (currentPath == null || currentPath.isEmpty()) {
            cancelPath();
            return;
        }
        if (currentWaypointIndex >= currentPath.size()) {
            cancelPath();
            return;
        }

        BlockPos waypoint = currentPath.get(currentWaypointIndex);

        // 到达当前路标点（仅检测水平距离）
        if (hasReachedWaypoint(waypoint)) {
            currentWaypointIndex++;
            if (currentWaypointIndex >= currentPath.size()) {
                cancelPath();
                return;
            }
            waypoint = currentPath.get(currentWaypointIndex);
        }

        moveToWaypoint(waypoint);

        // 卡住检测（仅水平距离，避免跳跃/下落时误判）
        Vec3 currentPos = bot.position();
        if (lastPos != null) {
            double movedH = Math.sqrt(
                Math.pow(currentPos.x - lastPos.x, 2) +
                Math.pow(currentPos.z - lastPos.z, 2));
            if (movedH < STUCK_MOVE_THRESHOLD) {
                ticksStuck++;
                if (ticksStuck > STUCK_THRESHOLD) {
                    ticksStuck = 0;
                    BlockPos start = findStandingPos(bot.blockPosition());
                    BlockPos end = findStandingPosNear(target);
                    if (end != null) {
                        List<BlockPos> newPath = findPath(start, end);
                        if (newPath != null && !newPath.isEmpty()) {
                            currentPath = newPath;
                            currentWaypointIndex = 0;
                        } else {
                            cancelPath();
                        }
                    }
                }
            } else {
                ticksStuck = 0;
            }
        }
        lastPos = currentPos;
    }

    /**
     * 向路标点移动
     * 仅设置水平朝向（pitch=0），垂直运动由物理引擎和自动跳跃处理
     */
    private void moveToWaypoint(BlockPos waypoint) {
        BotActionController controller = bot.getActionController();

        // 仅设置水平朝向，不改变俯仰角
        Vec3 targetPos = new Vec3(
            waypoint.getX() + 0.5,
            bot.getEyePosition().y,
            waypoint.getZ() + 0.5);
        controller.lookAt(targetPos);
        controller.moveForward();

        // 路标点比当前位置高时疾跑（帮助跳上高台）
        if (waypoint.getY() > bot.blockPosition().getY()) {
            controller.setSprint(true);
        } else {
            controller.setSprint(false);
        }
    }

    /**
     * 判断是否到达路标点（仅水平距离）
     */
    private boolean hasReachedWaypoint(BlockPos waypoint) {
        double dx = bot.getX() - (waypoint.getX() + 0.5);
        double dz = bot.getZ() - (waypoint.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz) < WAYPOINT_REACH_DISTANCE;
    }

    // ==================== 位置规范化 ====================

    /**
     * 查找有效的站立位置
     * 如果当前位置有效直接返回，否则向下搜索最多 3 格
     */
    private BlockPos findStandingPos(BlockPos pos) {
        if (isValidStandingPos(pos)) return pos;
        for (int dy = -1; dy >= -3; dy--) {
            BlockPos check = pos.above(dy);
            if (isValidStandingPos(check)) return check;
        }
        return pos;
    }

    /**
     * 在目标位置附近查找有效的站立位置
     * 先检查目标本身，再向下搜索最多 10 格
     */
    private BlockPos findStandingPosNear(BlockPos target) {
        if (isValidStandingPos(target)) return target;
        for (int dy = -1; dy >= -10; dy--) {
            BlockPos check = target.above(dy);
            if (isValidStandingPos(check)) return check;
        }
        return null;
    }

    /**
     * 检查一个位置是否为有效站立位置
     * 条件：脚下方块固体 + 脚部空气 + 头部空气 + 脚下无流体
     */
    private boolean isValidStandingPos(BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = bot.level().getBlockState(below);
        if (!belowState.blocksMotion()) return false;

        BlockState footState = bot.level().getBlockState(pos);
        if (footState.blocksMotion()) return false;
        if (!footState.getFluidState().isEmpty()) return false;

        BlockState headState = bot.level().getBlockState(pos.above());
        return !headState.blocksMotion();
    }

    // ==================== A* 寻路算法 ====================

    private List<BlockPos> findPath(BlockPos start, BlockPos end) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, PathNode> nodeMap = new HashMap<>();

        PathNode startNode = new PathNode(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        nodeMap.put(start, startNode);

        int iterations = 0;
        PathNode bestNode = startNode;
        float bestH = startNode.hCost;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            if (openSet.size() > MAX_OPEN_SET_SIZE) break;

            PathNode current = openSet.poll();
            if (!current.active) continue;

            if (current.pos.equals(end)) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            if (current.hCost < bestH) {
                bestH = current.hCost;
                bestNode = current;
            }

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (closedSet.contains(neighbor)) continue;

                float cost = moveCost(current.pos, neighbor);
                float tentativeG = current.gCost + cost;

                PathNode existing = nodeMap.get(neighbor);
                if (existing != null && tentativeG >= existing.gCost) continue;

                if (existing != null) existing.active = false;

                PathNode newNode = new PathNode(neighbor, current, tentativeG, heuristic(neighbor, end));
                openSet.add(newNode);
                nodeMap.put(neighbor, newNode);
            }
        }

        // 未找到完整路径，返回部分路径
        if (bestNode != startNode && bestNode.hCost < heuristic(start, end) * 0.8F) {
            return reconstructPath(bestNode);
        }
        return null;
    }

    /**
     * 获取所有可达的邻居站立位置
     * 所有邻居都保证是有效的站立位置
     */
    private List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>(12);

        // 基数方向：+X, -X, +Z, -Z
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] d : dirs) {
            BlockPos adj = pos.offset(d[0], 0, d[1]);

            // 1. 平地行走
            if (isValidStandingPos(adj)) {
                neighbors.add(adj);
                continue;
            }

            // 2. 跳跃上1格
            BlockPos up = adj.above();
            if (isValidStandingPos(up) && canClimb(adj)) {
                neighbors.add(up);
                continue;
            }

            // 3. 下落1格
            BlockPos down1 = adj.below();
            if (isClear(adj) && isValidStandingPos(down1)) {
                neighbors.add(down1);
                continue;
            }

            // 4. 下落2-3格
            for (int dy = 2; dy <= 3; dy++) {
                BlockPos landing = adj.below(dy);
                if (!isValidStandingPos(landing)) continue;
                boolean clear = isClear(adj);
                for (int i = 1; i < dy && clear; i++) {
                    clear = isClear(adj.below(i));
                }
                if (clear) {
                    neighbors.add(landing);
                    break;
                }
            }
        }

        // 对角线方向（仅平地）
        int[][] diags = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        for (int[] d : diags) {
            BlockPos diag = pos.offset(d[0], 0, d[1]);
            BlockPos side1 = pos.offset(d[0], 0, 0);
            BlockPos side2 = pos.offset(0, 0, d[1]);

            if (isClear(side1) && isClear(side2) && isValidStandingPos(diag)) {
                neighbors.add(diag);
            }
        }

        return neighbors;
    }

    /** 检查位置是否可攀登（前方有固体方块作为台阶） */
    private boolean canClimb(BlockPos pos) {
        return bot.level().getBlockState(pos).blocksMotion();
    }

    /** 检查位置是否通畅（非阻挡方块 + 无流体） */
    private boolean isClear(BlockPos pos) {
        BlockState state = bot.level().getBlockState(pos);
        if (state.blocksMotion()) return false;
        return state.getFluidState().isEmpty();
    }

    // ==================== 代价和启发式 ====================

    private float moveCost(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dz = Math.abs(from.getZ() - to.getZ());
        int dy = to.getY() - from.getY();

        float hCost = (dx > 0 && dz > 0) ? 1.414F : 1.0F;
        float vCost = dy > 0 ? 1.5F : (dy < 0 ? 0.5F : 0);
        return hCost + vCost;
    }

    /** 启发式函数（3D Octile distance） */
    private float heuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        int[] s = {dx, dy, dz};
        Arrays.sort(s);
        return s[0] * 1.732F + s[1] * 1.414F + (s[2] - s[0] - s[1]) * 1.0F;
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
