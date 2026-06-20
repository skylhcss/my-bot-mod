package name.modid.bot;

import name.modid.MyBotMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 假人寻路器
 * 基于 A* 算法实现自动寻路功能
 * 支持跳跃1格高障碍、下落最多3格、水平8方向移动
 */
public class BotPathfinder {

    /** 每次寻路最大迭代次数，防止卡顿（已大幅提升以支持远距离寻路） */
    private static final int MAX_ITERATIONS = 15000;
    
    /** 开放集合最大大小，防止内存溢出 */
    private static final int MAX_OPEN_SET_SIZE = 25000;
    
    /** 到达路标点的距离阈值 */
    private static final double WAYPOINT_REACH_DISTANCE = 0.8;
    
    /** 到达终点的距离阈值 */
    private static final double TARGET_REACH_DISTANCE = 1.5;
    
    /** 路径重算间隔（tick） */
    private static final int PATH_RECALC_INTERVAL = 100;
    
    /** 卡住检测阈值（tick） */
    private static final int STUCK_THRESHOLD = 30;
    
    /** 卡住时的最小移动距离 */
    private static final double STUCK_MOVE_THRESHOLD = 0.2;

    private final BotPlayer bot;
    
    /** 当前路径 */
    private List<BlockPos> currentPath;
    
    /** 当前路标索引 */
    private int currentWaypointIndex;
    
    /** 目标位置 */
    private BlockPos target;
    
    /** 是否正在寻路 */
    private boolean isPathfinding;
    
    /** 卡住计数器 */
    private int ticksStuck;
    
    /** 上一帧位置 */
    private Vec3 lastPos;
    
    /** tick 计数器 */
    private int tickCounter;

    public BotPathfinder(BotPlayer bot) {
        this.bot = bot;
    }

    /**
     * 开始寻路到指定位置
     * @param target 目标位置
     * @return 是否成功找到初始路径
     */
    public boolean pathTo(BlockPos target) {
        // 如果正在骑乘，先下马
        if (bot.isPassenger()) {
            bot.stopRiding();
        }
        
        this.target = target;
        this.isPathfinding = true;
        this.currentWaypointIndex = 0;
        this.ticksStuck = 0;
        this.tickCounter = 0;
        this.lastPos = bot.position();
        
        // 立即计算初始路径
        this.currentPath = findPath(bot.blockPosition(), target);
        
        if (this.currentPath == null || this.currentPath.isEmpty()) {
            this.isPathfinding = false;
            return false;
        }
        
        return true;
    }

    /**
     * 取消寻路
     */
    public void cancelPath() {
        this.isPathfinding = false;
        this.currentPath = null;
        this.currentWaypointIndex = 0;
        this.target = null;
        this.ticksStuck = 0;
        this.tickCounter = 0;
        
        // 停止移动
        bot.getActionController().stopMovement();
        bot.getActionController().setSprint(false);
    }

    /**
     * 是否正在寻路
     */
    public boolean isPathfinding() {
        return isPathfinding;
    }

    /**
     * 获取当前目标位置
     */
    public BlockPos getTarget() {
        return target;
    }

    /**
     * 获取当前路径
     */
    public List<BlockPos> getCurrentPath() {
        return currentPath;
    }

    /**
     * 每 tick 更新寻路逻辑
     * 由 BotActionController.tick() 调用
     */
    public void tick() {
        if (!isPathfinding || target == null) return;
        
        tickCounter++;
        
        // 检查是否到达终点
        if (bot.position().distanceTo(Vec3.atCenterOf(target)) < TARGET_REACH_DISTANCE) {
            cancelPath();
            return;
        }
        
        // 定期重新计算路径（处理环境变化）
        if (tickCounter % PATH_RECALC_INTERVAL == 0) {
            List<BlockPos> newPath = findPath(bot.blockPosition(), target);
            if (newPath != null && !newPath.isEmpty()) {
                currentPath = newPath;
                currentWaypointIndex = 0;
            } else {
                // 重算失败，可能目标已不可达
                cancelPath();
                return;
            }
        }
        
        // 跟随路径
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
        
        // 检测是否到达当前路标点
        if (hasReachedWaypoint(waypoint)) {
            currentWaypointIndex++;
            if (currentWaypointIndex >= currentPath.size()) {
                cancelPath();
                return;
            }
            waypoint = currentPath.get(currentWaypointIndex);
        }
        
        // 向路标点移动
        moveToWaypoint(waypoint);
        
        // 卡住检测
        Vec3 currentPos = bot.position();
        if (lastPos != null) {
            double moved = currentPos.distanceTo(lastPos);
            if (moved < STUCK_MOVE_THRESHOLD) {
                ticksStuck++;
                if (ticksStuck > STUCK_THRESHOLD) {
                    // 重新寻路
                    ticksStuck = 0;
                    List<BlockPos> newPath = findPath(bot.blockPosition(), target);
                    if (newPath != null && !newPath.isEmpty()) {
                        currentPath = newPath;
                        currentWaypointIndex = 0;
                    } else {
                        cancelPath();
                        return;
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
     */
    private void moveToWaypoint(BlockPos waypoint) {
        BotActionController controller = bot.getActionController();
        
        // 计算方向并设置视角
        Vec3 targetCenter = Vec3.atCenterOf(waypoint);
        controller.lookAt(targetCenter);
        
        // 设置向前移动
        controller.moveForward();
        
        // 如果路标点比当前位置高，启用疾跑（帮助跳上高台）
        if (waypoint.getY() > bot.blockPosition().getY()) {
            controller.setSprint(true);
        } else {
            // 不需要疾跑时恢复正常速度
            controller.setSprint(false);
        }
    }

    /**
     * 判断是否到达路标点
     */
    private boolean hasReachedWaypoint(BlockPos waypoint) {
        Vec3 botCenter = bot.position();
        Vec3 wpCenter = Vec3.atCenterOf(waypoint);
        
        // 水平距离 + 垂直距离
        double horizontalDist = Math.sqrt(
            Math.pow(botCenter.x - wpCenter.x, 2) + 
            Math.pow(botCenter.z - wpCenter.z, 2)
        );
        double verticalDist = Math.abs(botCenter.y - wpCenter.y);
        
        return horizontalDist < WAYPOINT_REACH_DISTANCE && verticalDist < 1.2;
    }

    // ==================== A* 寻路算法 ====================

    /**
     * A* 寻路核心算法
     * @param start 起点
     * @param end 终点
     * @return 路径（包含起点和终点），如果无法到达返回 null
     */
    private List<BlockPos> findPath(BlockPos start, BlockPos end) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, PathNode> nodeMap = new HashMap<>();
        
        PathNode startNode = new PathNode(start, null, 0, calculateHeuristic(start, end));
        openSet.add(startNode);
        nodeMap.put(start, startNode);
        
        int iterations = 0;
        PathNode bestNode = startNode; // 跟踪最接近目标的节点（用于部分路径）
        float bestH = startNode.hCost;
        
        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            
            // 限制开放集合大小
            if (openSet.size() > MAX_OPEN_SET_SIZE) {
                break;
            }
            
            PathNode current = openSet.poll();
            
            // 惰性删除：跳过已被替换的陈旧节点
            if (!current.active) continue;
            
            // 到达终点
            if (current.pos.equals(end)) {
                return reconstructPath(current);
            }
            
            closedSet.add(current.pos);
            
            // 跟踪最接近目标的节点
            if (current.hCost < bestH) {
                bestH = current.hCost;
                bestNode = current;
            }
            
            // 遍历邻居
            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (closedSet.contains(neighbor)) continue;
                
                // 计算移动代价
                float moveCost = getMoveCost(current.pos, neighbor);
                float tentativeG = current.gCost + moveCost;
                
                PathNode existing = nodeMap.get(neighbor);
                if (existing != null && tentativeG >= existing.gCost) continue;
                
                // 惰性删除：标记旧节点为不活跃（而非 O(n) 的 remove）
                if (existing != null) {
                    existing.active = false;
                }
                
                PathNode newNode = new PathNode(neighbor, current, tentativeG, 
                                                 calculateHeuristic(neighbor, end));
                
                openSet.add(newNode);
                nodeMap.put(neighbor, newNode);
            }
        }
        
        // 未找到完整路径，返回朝目标方向的部分路径
        if (bestNode != startNode && bestNode.hCost < calculateHeuristic(start, end) * 0.8F) {
            MyBotMod.LOGGER.info("寻路未找到完整路径，返回部分路径（最接近点距目标: {}）", bestNode.hCost);
            return reconstructPath(bestNode);
        }
        
        return null; // 完全无法到达
    }

    /**
     * 获取邻居节点
     * 支持水平8方向、跳跃1格、下落最多3格
     */
    private List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>(12);
        
        // 水平4方向
        int[][] horizontalOffsets = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        };
        
        // 对角线4方向
        int[][] diagonalOffsets = {
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
        };
        
        // 水平方向（包括同高度、上1格、下1-3格）
        for (int[] offset : horizontalOffsets) {
            BlockPos horizontal = pos.offset(offset[0], 0, offset[1]);
            addWalkableNeighbors(neighbors, pos, horizontal);
        }
        
        // 对角线方向（只检查同高度，避免复杂度过高）
        for (int[] offset : diagonalOffsets) {
            BlockPos diagonal = pos.offset(offset[0], 0, offset[1]);
            // 对角线移动需要两个相邻方向都通畅
            BlockPos side1 = pos.offset(offset[0], 0, 0);
            BlockPos side2 = pos.offset(0, 0, offset[1]);
            if (canWalkThrough(side1) && canWalkThrough(side2)) {
                addWalkableNeighbors(neighbors, pos, diagonal);
            }
        }
        
        return neighbors;
    }

    /**
     * 添加可行走的邻居节点（同高度、上方跳跃、下方下落）
     */
    private void addWalkableNeighbors(List<BlockPos> neighbors, BlockPos current, BlockPos horizontal) {
        // 同高度
        if (canWalkThrough(horizontal) && canWalkThrough(horizontal.above()) && canStandOn(horizontal.below())) {
            neighbors.add(horizontal);
        }
        
        // 上1格（跳跃）：前方有固体方块，上方有空间
        BlockPos above = horizontal.above();
        if (!canWalkThrough(horizontal) && canStandOn(horizontal) 
            && canWalkThrough(above) && canWalkThrough(above.above())) {
            neighbors.add(above);
        }
        
        // 下1格
        BlockPos below = horizontal.below();
        if (canWalkThrough(horizontal) && canWalkThrough(below) && canStandOn(below.below())
            && canWalkThrough(below.above())) {
            // 检查从当前是否能直接走过去（同高度通畅）
            if (canWalkThrough(current.below()) || current.equals(horizontal.below().above())) {
                neighbors.add(below.above());
            }
        }
        
        // 下2-3格（下落）
        for (int dy = 2; dy <= 3; dy++) {
            BlockPos fallTarget = horizontal.below(dy);
            if (canStandOn(fallTarget) && canWalkThrough(fallTarget.above()) 
                && canWalkThrough(fallTarget.above(2))) {
                // 确保下落路径通畅
                boolean pathClear = true;
                for (int i = 1; i <= dy; i++) {
                    BlockPos check = horizontal.below(i);
                    if (!canWalkThrough(check) || !canWalkThrough(check.above())) {
                        pathClear = false;
                        break;
                    }
                }
                if (pathClear && canWalkThrough(horizontal)) {
                    neighbors.add(fallTarget.above());
                    break; // 只取最近的落点
                }
            }
        }
    }

    /**
     * 检查方块是否可以通过
     */
    private boolean canWalkThrough(BlockPos pos) {
        BlockState state = bot.level().getBlockState(pos);
        if (state.isAir()) return true;
        // 允许通过非固体方块（草、花、火把等）
        return !state.blocksMotion();
    }

    /**
     * 检查方块是否可以站立在上面
     */
    private boolean canStandOn(BlockPos pos) {
        BlockState state = bot.level().getBlockState(pos);
        // 必须是固体方块
        return state.blocksMotion();
    }

    /**
     * 获取两个节点之间的移动代价
     */
    private float getMoveCost(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dz = Math.abs(from.getZ() - to.getZ());
        int dy = Math.abs(from.getY() - to.getY());
        
        // 对角线移动代价更高
        float horizontalCost = (dx > 0 && dz > 0) ? 1.414F : 1.0F;
        
        // 高度差惩罚
        float verticalPenalty = dy * 2.0F;
        
        // 水中额外代价
        BlockState targetState = bot.level().getBlockState(to);
        float waterPenalty = targetState.getFluidState().isEmpty() ? 0 : 3.0F;
        
        return horizontalCost + verticalPenalty + waterPenalty;
    }

    /**
     * 启发式函数（Octile distance 的 3D 版本）
     */
    private float calculateHeuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        
        // 排序
        int[] sorted = {dx, dy, dz};
        Arrays.sort(sorted);
        
        // Octile distance 3D
        return sorted[0] * 1.732F + sorted[1] * 1.414F + (sorted[2] - sorted[0] - sorted[1]) * 1.0F;
    }

    /**
     * 从终点节点回溯路径
     */
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

    /**
     * 寻路节点
     */
    private static class PathNode implements Comparable<PathNode> {
        final BlockPos pos;
        final PathNode parent;
        final float gCost;
        final float hCost;
        final float fCost;
        /** 惰性删除标记：当节点被更优路径替换时设为 false */
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
