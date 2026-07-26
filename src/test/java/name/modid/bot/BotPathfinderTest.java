package name.modid.bot;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BotPathfinder 寻路核心逻辑单元测试。
 * 通过反射测试内部算法纯函数（heuristic、PathNode、reconstructPath），
 * 验证 A* 算法的数学正确性和边界条件。
 */
class BotPathfinderTest {

    // ==================== 反射辅助 ====================

    /** 获取 heuristic 方法的反射引用 */
    private static Method getHeuristicMethod() throws Exception {
        Method m = BotPathfinder.class.getDeclaredMethod("heuristic", BlockPos.class, BlockPos.class);
        m.setAccessible(true);
        return m;
    }

    /** 调用 heuristic(BlockPos a, BlockPos b) */
    private float invokeHeuristic(BotPathfinder instance, BlockPos a, BlockPos b) throws Exception {
        return (float) getHeuristicMethod().invoke(instance, a, b);
    }

    /** 获取 PathNode 内部类 */
    private static Class<?> getPathNodeClass() throws Exception {
        for (Class<?> inner : BotPathfinder.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("PathNode")) return inner;
        }
        throw new ClassNotFoundException("PathNode not found");
    }

    /** 创建 PathNode 实例 */
    private static Object createPathNode(BlockPos pos, Object parent, float gCost, float hCost) throws Exception {
        Class<?> nodeClass = getPathNodeClass();
        Constructor<?> ctor = nodeClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return ctor.newInstance(pos, parent, gCost, hCost);
    }

    /** 获取 PathNode 的 fCost */
    private static float getNodeFCost(Object node) throws Exception {
        var field = node.getClass().getDeclaredField("fCost");
        field.setAccessible(true);
        return field.getFloat(node);
    }

    /** 获取 PathNode 的 pos */
    private static BlockPos getNodePos(Object node) throws Exception {
        var field = node.getClass().getDeclaredField("pos");
        field.setAccessible(true);
        return (BlockPos) field.get(node);
    }

    /** 调用 reconstructPath(PathNode) */
    @SuppressWarnings("unchecked")
    private static List<BlockPos> invokeReconstructPath(BotPathfinder instance, Object endNode) throws Exception {
        Method m = BotPathfinder.class.getDeclaredMethod("reconstructPath", getPathNodeClass());
        m.setAccessible(true);
        return (List<BlockPos>) m.invoke(instance, endNode);
    }

    /** 创建一个 BotPathfinder 实例（bot 参数为 null，仅用于反射调用纯函数） */
    private static BotPathfinder createPathfinderForUnitTest() throws Exception {
        Constructor<BotPathfinder> ctor = BotPathfinder.class.getDeclaredConstructor(BotPlayer.class);
        ctor.setAccessible(true);
        return ctor.newInstance((BotPlayer) null);
    }

    // ==================== 启发式函数测试 ====================

    @Nested
    @DisplayName("heuristic 函数")
    class HeuristicTests {

        @Test
        @DisplayName("相同位置的启发值应为 0")
        void testSamePosition() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            BlockPos pos = new BlockPos(10, 64, -20);
            assertEquals(0.0f, invokeHeuristic(pf, pos, pos), 1e-6f);
        }

        @Test
        @DisplayName("启发值应为非负")
        void testNonNegative() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            BlockPos a = new BlockPos(-100, 0, 200);
            BlockPos b = new BlockPos(300, 128, -500);
            float h = invokeHeuristic(pf, a, b);
            assertTrue(h >= 0, "heuristic should be non-negative, got: " + h);
        }

        @ParameterizedTest
        @CsvSource({
            "0,0,0, 5,0,0, 5.0",        // 纯东向
            "0,0,0, 0,0,7, 7.0",        // 纯北向
            "0,0,0, 3,0,3, 4.242",      // 对角 (3 + 0.414*3 ≈ 4.242)
            "0,0,0, 0,10,0, 5.0",       // 纯垂直 (0.5*10 = 5)
            "0,0,0, 4,6,4, 8.656"       // 混合 (max(4,4) + 0.414*min(4,4) + 0.5*6 = 4+1.656+3 = 8.656) 
        })
        @DisplayName("水平/垂直/对角方向启发值验证")
        void testDirectionalHeuristic(int ax, int ay, int az, int bx, int by, int bz,
                                       double expected) throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            float h = invokeHeuristic(pf, new BlockPos(ax, ay, az), new BlockPos(bx, by, bz));
            // 使用宽容的 delta：仅验证数量级正确
            assertEquals((float) expected, h, 0.5f,
                    String.format("heuristic(%d,%d,%d -> %d,%d,%d) = %.3f, expected ~%.3f",
                            ax, ay, az, bx, by, bz, h, expected));
        }

        @Test
        @DisplayName("启发式函数应满足对称性 h(a,b) == h(b,a)")
        void testSymmetry() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            BlockPos a = new BlockPos(10, 64, -30);
            BlockPos b = new BlockPos(-5, 80, 42);
            float hab = invokeHeuristic(pf, a, b);
            float hba = invokeHeuristic(pf, b, a);
            assertEquals(hab, hba, 1e-6f, "heuristic should be symmetric");
        }

        @Test
        @DisplayName("启发式函数应满足三角不等式 h(a,c) <= h(a,b) + h(b,c)")
        void testTriangleInequality() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            BlockPos a = new BlockPos(0, 64, 0);
            BlockPos b = new BlockPos(5, 64, 5);
            BlockPos c = new BlockPos(10, 64, 0);

            float hac = invokeHeuristic(pf, a, c);
            float hab = invokeHeuristic(pf, a, b);
            float hbc = invokeHeuristic(pf, b, c);

            assertTrue(hac <= hab + hbc + 1e-5f,
                    String.format("Triangle inequality violated: h(a,c)=%.3f > h(a,b)+h(b,c)=%.3f",
                            hac, hab + hbc));
        }

        @Test
        @DisplayName("极大距离下启发值不应溢出")
        void testLargeDistanceNoOverflow() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            BlockPos a = new BlockPos(-30000000, 0, -30000000);
            BlockPos b = new BlockPos(30000000, 256, 30000000);
            float h = invokeHeuristic(pf, a, b);
            assertTrue(Float.isFinite(h), "heuristic overflowed for large distance");
            assertTrue(h > 0, "heuristic should be positive for different positions");
        }

        @Test
        @DisplayName("启发式应为可采纳的（不大于实际水平+垂直最短距离）")
        void testAdmissibility() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            // 对于纯水平直线移动，实际代价 = 曼哈顿距离
            // 启发值应 <= 实际代价
            BlockPos a = new BlockPos(0, 64, 0);
            BlockPos b = new BlockPos(10, 64, 0);
            float h = invokeHeuristic(pf, a, b);
            // 实际最短直线路径代价：10 格平走 = 10.0
            assertTrue(h <= 10.0f + 1e-5f,
                    "heuristic " + h + " exceeds actual straight-line cost 10.0");
        }
    }

    // ==================== PathNode 测试 ====================

    @Nested
    @DisplayName("PathNode 排序和属性")
    class PathNodeTests {

        @Test
        @DisplayName("fCost = gCost + hCost")
        void testFCostCalculation() throws Exception {
            Object node = createPathNode(new BlockPos(0, 0, 0), null, 5.0f, 3.0f);
            assertEquals(8.0f, getNodeFCost(node), 1e-6f);
        }

        @Test
        @DisplayName("优先队列应按 fCost 升序排列节点")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void testPriorityQueueOrdering() throws Exception {
            Object nodeA = createPathNode(new BlockPos(0, 0, 0), null, 2.0f, 8.0f); // f=10
            Object nodeB = createPathNode(new BlockPos(1, 0, 0), null, 1.0f, 4.0f); // f=5
            Object nodeC = createPathNode(new BlockPos(2, 0, 0), null, 3.0f, 12.0f); // f=15
            Object nodeD = createPathNode(new BlockPos(3, 0, 0), null, 0.5f, 2.0f); // f=2.5

            PriorityQueue pq = new PriorityQueue();
            pq.add(nodeA);
            pq.add(nodeB);
            pq.add(nodeC);
            pq.add(nodeD);

            // 应按 fCost 升序出队：2.5, 5, 10, 15
            assertEquals(2.5f, getNodeFCost(pq.poll()), 1e-6f);
            assertEquals(5.0f, getNodeFCost(pq.poll()), 1e-6f);
            assertEquals(10.0f, getNodeFCost(pq.poll()), 1e-6f);
            assertEquals(15.0f, getNodeFCost(pq.poll()), 1e-6f);
        }

        @Test
        @DisplayName("相同 fCost 的节点在优先队列中不应异常")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void testEqualFCostNoException() throws Exception {
            Object node1 = createPathNode(new BlockPos(0, 0, 0), null, 3.0f, 7.0f); // f=10
            Object node2 = createPathNode(new BlockPos(1, 0, 0), null, 5.0f, 5.0f); // f=10

            PriorityQueue pq = new PriorityQueue();
            assertDoesNotThrow(() -> {
                pq.add(node1);
                pq.add(node2);
                pq.poll();
                pq.poll();
            });
        }

        @Test
        @DisplayName("零代价节点 (fCost=0) 应排在最前")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void testZeroCostFirst() throws Exception {
            Object nodeZero = createPathNode(new BlockPos(0, 0, 0), null, 0.0f, 0.0f);
            Object nodeNormal = createPathNode(new BlockPos(1, 0, 0), null, 1.0f, 1.0f);

            PriorityQueue pq = new PriorityQueue();
            pq.add(nodeNormal);
            pq.add(nodeZero);

            assertEquals(0.0f, getNodeFCost(pq.poll()), 1e-6f);
        }
    }

    // ==================== 路径重建测试 ====================

    @Nested
    @DisplayName("reconstructPath 路径重建")
    class PathReconstructTests {

        @Test
        @DisplayName("单节点路径应只包含起点")
        void testSingleNodePath() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            BlockPos pos = new BlockPos(5, 64, 5);
            Object node = createPathNode(pos, null, 0, 0);

            List<BlockPos> path = invokeReconstructPath(pf, node);
            assertEquals(1, path.size());
            assertEquals(pos, path.get(0));
        }

        @Test
        @DisplayName("直线路径重建应保持正确顺序（起点→终点）")
        void testStraightLinePath() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();

            // 构建链 A -> B -> C -> D
            BlockPos posA = new BlockPos(0, 64, 0);
            BlockPos posB = new BlockPos(1, 64, 0);
            BlockPos posC = new BlockPos(2, 64, 0);
            BlockPos posD = new BlockPos(3, 64, 0);

            Object nodeA = createPathNode(posA, null, 0, 3);
            Object nodeB = createPathNode(posB, nodeA, 1, 2);
            Object nodeC = createPathNode(posC, nodeB, 2, 1);
            Object nodeD = createPathNode(posD, nodeC, 3, 0);

            List<BlockPos> path = invokeReconstructPath(pf, nodeD);

            assertEquals(4, path.size());
            assertEquals(posA, path.get(0)); // 起点
            assertEquals(posB, path.get(1));
            assertEquals(posC, path.get(2));
            assertEquals(posD, path.get(3)); // 终点
        }

        @Test
        @DisplayName("复杂3D路径重建应保持正确顺序")
        void testComplex3DPath() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();

            // 模拟 3D 路径：走 → 跳上 → 走 → 下落
            BlockPos p0 = new BlockPos(0, 64, 0);   // 起点
            BlockPos p1 = new BlockPos(1, 64, 0);   // 走
            BlockPos p2 = new BlockPos(2, 65, 0);   // 跳上
            BlockPos p3 = new BlockPos(3, 65, 1);   // 对角走
            BlockPos p4 = new BlockPos(4, 63, 1);   // 下落

            Object n0 = createPathNode(p0, null, 0, 5);
            Object n1 = createPathNode(p1, n0, 1, 4);
            Object n2 = createPathNode(p2, n1, 2.5f, 3);
            Object n3 = createPathNode(p3, n2, 3.9f, 1.5f);
            Object n4 = createPathNode(p4, n3, 4.4f, 0);

            List<BlockPos> path = invokeReconstructPath(pf, n4);

            assertEquals(5, path.size());
            assertEquals(p0, path.get(0));
            assertEquals(p1, path.get(1));
            assertEquals(p2, path.get(2));
            assertEquals(p3, path.get(3));
            assertEquals(p4, path.get(4));
        }

        @Test
        @DisplayName("长路径（100+ 节点）重建性能和正确性")
        void testLongPathReconstruct() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();

            // 构建 200 节点的直线路径
            int pathLength = 200;
            Object previousNode = null;
            BlockPos[] positions = new BlockPos[pathLength];

            for (int i = 0; i < pathLength; i++) {
                positions[i] = new BlockPos(i, 64, 0);
                previousNode = createPathNode(positions[i], previousNode, (float) i, (float) (pathLength - i));
            }

            long startTime = System.nanoTime();
            List<BlockPos> path = invokeReconstructPath(pf, previousNode);
            long elapsed = System.nanoTime() - startTime;

            assertEquals(pathLength, path.size());
            // 验证顺序
            for (int i = 0; i < pathLength; i++) {
                assertEquals(positions[i], path.get(i),
                        "Mismatch at index " + i);
            }
            // 性能：200 节点路径重建应在 10ms 内完成
            assertTrue(elapsed < 10_000_000L,
                    "Path reconstruction took too long: " + (elapsed / 1_000_000) + "ms");
        }
    }

    // ==================== 极端/边界场景测试 ====================

    @Nested
    @DisplayName("极端和特殊场景")
    class EdgeCaseTests {

        @Test
        @DisplayName("负坐标位置的启发值应正确计算")
        void testNegativeCoordinates() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            BlockPos a = new BlockPos(-10, -64, -20);
            BlockPos b = new BlockPos(-5, -60, -15);

            float h = invokeHeuristic(pf, a, b);
            assertTrue(h > 0, "Heuristic for different positions should be > 0");
            // dx=5, dz=5, dy=4 → octile = max(5,5) + 0.414*min(5,5) + 0.5*4 = 5+2.07+2 = 9.07
            assertEquals(9.07f, h, 0.5f);
        }

        @Test
        @DisplayName("纯垂直移动的启发值")
        void testPureVerticalHeuristic() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            BlockPos a = new BlockPos(0, 0, 0);
            BlockPos b = new BlockPos(0, 100, 0);

            float h = invokeHeuristic(pf, a, b);
            // 纯垂直：0.5 * 100 = 50
            assertEquals(50.0f, h, 1e-5f);
        }

        @Test
        @DisplayName("相邻位置（距离=1）的启发值应为 1.0")
        void testAdjacentHeuristic() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            BlockPos a = new BlockPos(0, 64, 0);

            // 四个水平相邻方向
            assertEquals(1.0f, invokeHeuristic(pf, a, new BlockPos(1, 64, 0)), 1e-5f);
            assertEquals(1.0f, invokeHeuristic(pf, a, new BlockPos(-1, 64, 0)), 1e-5f);
            assertEquals(1.0f, invokeHeuristic(pf, a, new BlockPos(0, 64, 1)), 1e-5f);
            assertEquals(1.0f, invokeHeuristic(pf, a, new BlockPos(0, 64, -1)), 1e-5f);
        }

        @Test
        @DisplayName("对角相邻位置的启发值应约为 1.414")
        void testDiagonalAdjacentHeuristic() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            BlockPos a = new BlockPos(0, 64, 0);
            BlockPos diag = new BlockPos(1, 64, 1);

            float h = invokeHeuristic(pf, a, diag);
            // max(1,1) + 0.414*min(1,1) = 1 + 0.414 = 1.414
            assertEquals(1.414f, h, 0.01f);
        }

        @Test
        @DisplayName("PathNode active 标志应默认为 true")
        void testNodeActiveDefault() throws Exception {
            Object node = createPathNode(new BlockPos(0, 0, 0), null, 1.0f, 1.0f);
            var field = node.getClass().getDeclaredField("active");
            field.setAccessible(true);
            assertTrue(field.getBoolean(node));
        }

        @Test
        @DisplayName("PathNode active 标志可被设为 false（用于惰性删除）")
        void testNodeDeactivation() throws Exception {
            Object node = createPathNode(new BlockPos(0, 0, 0), null, 1.0f, 1.0f);
            var field = node.getClass().getDeclaredField("active");
            field.setAccessible(true);
            field.setBoolean(node, false);
            assertFalse(field.getBoolean(node));
        }

        @Test
        @DisplayName("启发式函数对大量随机点应保持一致性（h(a,c) <= h(a,b) + h(b,c)）")
        void testConsistencyOnRandomPoints() throws Exception {
            BotPathfinder pf = createPathfinderForUnitTest();
            java.util.Random rng = new java.util.Random(42);

            for (int trial = 0; trial < 100; trial++) {
                BlockPos a = new BlockPos(rng.nextInt(1000) - 500, rng.nextInt(256), rng.nextInt(1000) - 500);
                BlockPos b = new BlockPos(rng.nextInt(1000) - 500, rng.nextInt(256), rng.nextInt(1000) - 500);
                BlockPos c = new BlockPos(rng.nextInt(1000) - 500, rng.nextInt(256), rng.nextInt(1000) - 500);

                float hac = invokeHeuristic(pf, a, c);
                float hab = invokeHeuristic(pf, a, b);
                float hbc = invokeHeuristic(pf, b, c);

                assertTrue(hac <= hab + hbc + 1e-3f,
                        String.format("Consistency violated at trial %d: h(a,c)=%.3f > h(a,b)+h(b,c)=%.3f+%.3f",
                                trial, hac, hab, hbc));
            }
        }
    }
}
