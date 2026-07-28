package name.modid.behavior;

import name.modid.MyBotMod;
import name.modid.behavior.BehaviorProgram.Bin;
import name.modid.behavior.BehaviorProgram.Bool;
import name.modid.behavior.BehaviorProgram.Expr;
import name.modid.behavior.BehaviorProgram.ListLit;
import name.modid.behavior.BehaviorProgram.Num;
import name.modid.behavior.BehaviorProgram.Sensor;
import name.modid.behavior.BehaviorProgram.Stmt;
import name.modid.behavior.BehaviorProgram.Str;
import name.modid.behavior.BehaviorProgram.Un;
import name.modid.behavior.BehaviorProgram.Var;
import name.modid.bot.BotActionController;
import name.modid.bot.BotPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * 行为脚本解释器（每假人每行为一个实例）—— Scratch 式多线程模型
 *
 * 执行模型：
 * - 每个帽子块（onStart/onChat/onBroadcast/onHealthBelow/onEntityNear）各自形成一个独立"线程"，
 *   多个帽子块并联执行；帽子块下的语句链串联执行；
 * - 挂起状态（wait/寻路/waitUntil/容器动画）按线程隔离，互不阻塞；
 * - 变量表全行为共享（Scratch 同款语义）；
 * - 每线程每 tick 有执行预算，防止无 wait 的死循环卡服；
 * - 运行错误不抛出：记录 lastError 并跳过该语句。
 */
public class BehaviorRuntime {

    /** 每线程每 tick 最多执行的语句数（超出自动让出到下一 tick） */
    private static final int BUDGET_PER_TICK = 400;
    /** 容器交互最大距离（格） */
    private static final double CONTAINER_RANGE = 8.0;
    /** 超出此距离需先寻路靠近才能打开容器（格） */
    private static final double OPEN_RANGE = 4.0;
    /** 单线程调用栈最大深度（防递归爆栈） */
    private static final int MAX_CALL_DEPTH = 64;
    /** 轮询型事件（血量/实体接近）的检查间隔（tick） */
    private static final int POLL_INTERVAL = 10;

    private final BotPlayer bot;
    private final BehaviorProgram program;
    private final Map<String, BehaviorValue> vars = new HashMap<>();
    /** 顶层 def 自定义函数表：函数名 → def 语句 */
    private final Map<String, Stmt> functions = new HashMap<>();
    /** 所有执行线程（主线程 + 事件线程） */
    private final List<ThreadState> threads = new ArrayList<>();
    /** 事件触发器（onChat/onBroadcast/onHealthBelow/onEntityNear 帽子语句） */
    private final List<Stmt> chatTriggers = new ArrayList<>();
    private final List<Stmt> broadcastTriggers = new ArrayList<>();
    private final List<Stmt> pollTriggers = new ArrayList<>();
    /** 触发器 → 当前活跃事件线程（重触发时重启该线程，Scratch 语义） */
    private final Map<Stmt, ThreadState> eventThreads = new IdentityHashMap<>();
    /** 轮询触发器的边沿状态（true=条件已满足过，需恢复 false 才能再次触发） */
    private final Map<Stmt, Boolean> pollArmed = new IdentityHashMap<>();

    /** 当前已打开的容器位置（物理上一个假人同时只开一个，runtime 级共享） */
    private BlockPos openContainerPos;
    private boolean finished;
    private String lastError;
    private int tickCounter;

    /** 单个执行线程：独立栈与挂起状态 */
    private static final class ThreadState {
        final Deque<Frame> stack = new ArrayDeque<>();
        final List<Stmt> body;
        /** 是否随 program.loop 在跑完后重启（主线程/onStart 线程） */
        final boolean loopRestart;
        int waitTicks;
        Runnable afterWait;
        boolean waitingForPath;
        Runnable afterPath;
        Expr waitUntilCond;
        boolean done;

        ThreadState(List<Stmt> body, boolean loopRestart) {
            this.body = body;
            this.loopRestart = loopRestart;
            if (!body.isEmpty()) {
                stack.push(new Frame(body, Kind.SEQ, null, 0));
            } else {
                done = true;
            }
        }

        /** 重置到起点（loop 重启 / 事件重触发） */
        void restart() {
            stack.clear();
            waitTicks = 0;
            afterWait = null;
            waitingForPath = false;
            afterPath = null;
            waitUntilCond = null;
            done = body.isEmpty();
            if (!body.isEmpty()) {
                stack.push(new Frame(body, Kind.SEQ, null, 0));
            }
        }
    }

    /** 栈帧类型 */
    private enum Kind { SEQ, REPEAT, WHILE, FOREVER }

    private static final class Frame {
        final List<Stmt> stmts;
        final Kind kind;
        final Stmt owner;
        int index;
        int remaining;

        Frame(List<Stmt> stmts, Kind kind, Stmt owner, int remaining) {
            this.stmts = stmts;
            this.kind = kind;
            this.owner = owner;
            this.remaining = remaining;
        }
    }

    public BehaviorRuntime(BotPlayer bot, BehaviorProgram program) {
        this.bot = bot;
        this.program = program;
        // 顶层分拣：def 入函数表；onStart 各成一线程；事件帽子注册为触发器；散置语句合并为主线程（向后兼容）
        List<Stmt> main = new ArrayList<>();
        for (Stmt s : program.body) {
            switch (s.op()) {
                case "def" -> {
                    String fname = eval(s.arg("name")).asString();
                    if (!fname.isEmpty()) {
                        functions.put(fname, s);
                    }
                }
                case "onStart" -> threads.add(new ThreadState(s.block("body"), true));
                case "onChat" -> chatTriggers.add(s);
                case "onBroadcast" -> broadcastTriggers.add(s);
                case "onHealthBelow", "onEntityNear" -> pollTriggers.add(s);
                default -> main.add(s);
            }
        }
        if (!main.isEmpty()) {
            threads.add(new ThreadState(List.copyOf(main), true));
        }
    }

    public BehaviorProgram getProgram() {
        return program;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getLastError() {
        return lastError;
    }

    /** 是否存在任何事件触发器（决定跑完后是否驻留监听） */
    private boolean hasTriggers() {
        return !chatTriggers.isEmpty() || !broadcastTriggers.isEmpty() || !pollTriggers.isEmpty();
    }

    // ==================== 事件分发 ====================

    /**
     * 玩家聊天事件：匹配 onChat 触发器（pattern 空=任意消息，否则大小写不敏感包含匹配）。
     * 触发仅（重）启动该触发器自己的线程，其余线程不受影响（Scratch 语义）。
     *
     * @return 是否有触发器命中
     */
    public boolean onChatMessage(String sender, String message) {
        if (finished || message == null) {
            return false;
        }
        boolean hit = false;
        for (Stmt trigger : chatTriggers) {
            String pattern = trigger.arg("text") == null ? "" : evalStr(trigger.arg("text"));
            if (pattern.isEmpty() || message.toLowerCase().contains(pattern.toLowerCase())) {
                vars.put("chat_sender", BehaviorValue.str(sender));
                vars.put("chat_message", BehaviorValue.str(message));
                fire(trigger);
                hit = true;
            }
        }
        return hit;
    }

    /** 广播事件：匹配 onBroadcast(name) 触发器（大小写不敏感全等） @return 是否命中 */
    public boolean onBroadcastMessage(String name) {
        if (finished || name == null) {
            return false;
        }
        boolean hit = false;
        for (Stmt trigger : broadcastTriggers) {
            String expect = trigger.arg("name") == null ? "" : evalStr(trigger.arg("name"));
            if (expect.equalsIgnoreCase(name)) {
                fire(trigger);
                hit = true;
            }
        }
        return hit;
    }

    /** （重）启动触发器专属线程 */
    private void fire(Stmt trigger) {
        ThreadState t = eventThreads.get(trigger);
        if (t == null) {
            t = new ThreadState(trigger.block("body"), false);
            eventThreads.put(trigger, t);
            threads.add(t);
        } else {
            t.restart();
        }
    }

    /** 轮询型触发器（血量低于/实体接近）：条件 false→true 边沿触发一次 */
    private void pollEdgeTriggers() {
        if (bot == null || pollTriggers.isEmpty() || tickCounter % POLL_INTERVAL != 0) {
            return;
        }
        for (Stmt trigger : pollTriggers) {
            boolean condition;
            if (trigger.op().equals("onHealthBelow")) {
                condition = bot.getHealth() < evalNum(trigger.arg("value"));
            } else { // onEntityNear
                double range = trigger.arg("range") == null ? 8 : Math.max(1, Math.min(32, evalNum(trigger.arg("range"))));
                String type = trigger.arg("type") == null ? "" : evalStr(trigger.arg("type"));
                condition = countNearby(range, type) > 0;
            }
            boolean wasArmed = pollArmed.getOrDefault(trigger, false);
            if (condition && !wasArmed) {
                fire(trigger);
            }
            pollArmed.put(trigger, condition);
        }
    }

    // ==================== tick 调度 ====================

    /** 每 tick 由 BehaviorManager 调用 */
    public void tick() {
        if (finished || (bot != null && bot.isRemoved())) {
            finished = true;
            return;
        }
        tickCounter++;
        pollEdgeTriggers();

        boolean anyAlive = false;
        for (ThreadState t : threads) {
            if (t.done) {
                continue;
            }
            tickThread(t);
            if (finished) {
                return; // stopSelf/stopAll 立即终止整个行为
            }
            if (!t.done) {
                anyAlive = true;
            }
        }
        // 全部线程跑完：无触发器且不循环则结束；有触发器则驻留监听
        if (!anyAlive && !hasTriggers()) {
            boolean restarted = false;
            if (program.loop) {
                for (ThreadState t : threads) {
                    if (t.loopRestart) {
                        t.restart();
                        restarted = true;
                    }
                }
            }
            if (!restarted) {
                closeIfOpen();
                finished = true;
            }
        }
    }

    /** 推进单个线程：处理挂起 → 预算内执行语句 */
    private void tickThread(ThreadState t) {
        // 定时挂起
        if (t.waitTicks > 0) {
            t.waitTicks--;
            if (t.waitTicks == 0 && t.afterWait != null) {
                Runnable r = t.afterWait;
                t.afterWait = null;
                r.run();
            }
            return;
        }
        // 等待寻路结束（到达或放弃）
        if (t.waitingForPath) {
            if (bot != null && bot.getActionController().isPathfinding()) {
                return;
            }
            t.waitingForPath = false;
            if (t.afterPath != null) {
                Runnable r = t.afterPath;
                t.afterPath = null;
                r.run();
                return; // 回调可能重新挂起，本 tick 让出
            }
        }
        // 等待条件成立
        if (t.waitUntilCond != null) {
            if (!evalBool(t.waitUntilCond)) {
                return;
            }
            t.waitUntilCond = null;
        }

        int budget = BUDGET_PER_TICK;
        while (budget-- > 0 && !t.done && !finished) {
            Frame f = t.stack.peek();
            if (f == null) {
                // 线程体结束：主线程按 loop 重启（至少让出一 tick），事件线程标记完成
                if (program.loop && t.loopRestart) {
                    t.restart();
                }
                t.done = !program.loop || !t.loopRestart;
                return;
            }
            if (f.index >= f.stmts.size()) {
                switch (f.kind) {
                    case REPEAT -> {
                        if (--f.remaining > 0) {
                            f.index = 0;
                        } else {
                            t.stack.pop();
                        }
                    }
                    case WHILE -> {
                        if (evalBool(f.owner.arg("cond"))) {
                            f.index = 0;
                        } else {
                            t.stack.pop();
                        }
                    }
                    case FOREVER -> f.index = 0;
                    default -> t.stack.pop();
                }
                continue;
            }
            Stmt s = f.stmts.get(f.index++);
            try {
                if (execute(s, t)) {
                    return; // 挂起
                }
            } catch (RuntimeException e) {
                lastError = program.sourceFile + ": " + s.op() + " 执行出错 - " + e.getMessage();
                MyBotMod.LOGGER.warn("[行为] {}", lastError);
            }
        }
    }

    // ==================== 语句执行 ====================

    /** @return true = 当前线程已挂起 */
    private boolean execute(Stmt s, ThreadState t) {
        // bot 在纯逻辑单元测试中可为 null（仅执行控制流/变量/列表类语句）
        BotActionController c = bot == null ? null : bot.getActionController();
        switch (s.op()) {
            case "say" -> say(evalStr(s.arg("text")));
            case "wait" -> {
                t.waitTicks = Math.max(1, (int) evalNum(s.arg("ticks")));
                return true;
            }
            case "waitUntil" -> {
                if (!evalBool(s.arg("cond"))) {
                    t.waitUntilCond = s.arg("cond");
                    return true;
                }
            }
            case "move" -> {
                setMove(c, evalStr(s.arg("dir")));
                if (s.arg("ticks") != null) {
                    t.waitTicks = Math.max(1, (int) evalNum(s.arg("ticks")));
                    t.afterWait = c::stopMovement;
                    return true;
                }
            }
            case "stopMove" -> c.stopMovement();
            case "jump" -> {
                c.setJump(true);
                t.waitTicks = 1;
                t.afterWait = () -> c.setJump(false);
                return true;
            }
            case "sneak" -> c.setSneak(s.arg("on") == null || evalBool(s.arg("on")));
            case "sprint" -> c.setSprint(s.arg("on") == null || evalBool(s.arg("on")));
            case "look" -> look(c, evalStr(s.arg("dir")));
            case "lookAt" -> c.lookAt(new Vec3(evalNum(s.arg("x")), evalNum(s.arg("y")), evalNum(s.arg("z"))));
            case "lookAtEntity" -> {
                LivingEntity target = nearestEntity(
                    s.arg("range") == null ? 16 : evalNum(s.arg("range")),
                    s.arg("type") == null ? "" : evalStr(s.arg("type")));
                if (target != null) {
                    c.lookAt(target.getEyePosition());
                }
            }
            case "turn" -> c.turn((float) evalNum(s.arg("yaw")), (float) evalNum(s.arg("pitch")));
            case "attack" -> {
                String mode = s.arg("mode") == null ? "once" : evalStr(s.arg("mode"));
                switch (mode) {
                    case "continuous" -> c.startAttackContinuous();
                    case "interval" -> c.startAttackInterval(Math.max(1, (int) evalNum(s.arg("interval"))));
                    default -> c.startAttackOnce();
                }
            }
            case "use" -> {
                String mode = s.arg("mode") == null ? "once" : evalStr(s.arg("mode"));
                switch (mode) {
                    case "continuous" -> c.startUseContinuous();
                    case "interval" -> c.startUseInterval(Math.max(1, (int) evalNum(s.arg("interval"))));
                    default -> c.startUseOnce();
                }
            }
            case "stopAttack" -> c.stopAttack();
            case "stopUse" -> c.stopUse();
            case "slot" -> bot.getInventory().selected =
                Math.max(0, Math.min(8, (int) evalNum(s.arg("n"))));
            case "swapHands" -> c.swapHands();
            case "drop" -> {
                if (s.arg("stack") != null && evalBool(s.arg("stack"))) {
                    c.dropStack();
                } else {
                    c.dropItem();
                }
            }
            case "equipItem" -> equipItem(evalStr(s.arg("item")));
            case "dropOf" -> dropOf(evalStr(s.arg("item")),
                s.arg("count") == null ? Integer.MAX_VALUE : (int) evalNum(s.arg("count")));
            case "goto" -> {
                BlockPos target = BlockPos.containing(
                    evalNum(s.arg("x")), evalNum(s.arg("y")), evalNum(s.arg("z")));
                if (c.pathTo(target)) {
                    t.waitingForPath = true;
                    return true;
                }
                lastError = program.sourceFile + ": goto 无法到达 " + target.toShortString();
            }
            case "gotoStop" -> c.cancelPath();
            case "mount" -> c.mount();
            case "dismount" -> c.dismount();
            case "openContainer" -> {
                return containerOp(s, t, pos -> {
                });
            }
            case "closeContainer" -> closeIfOpen();
            case "takeFromContainer" -> {
                return containerOp(s, t, pos -> takeItems(s, pos));
            }
            case "putToContainer" -> {
                return containerOp(s, t, pos -> putItems(s, pos));
            }
            case "readContainer" -> {
                return containerOp(s, t, pos -> {
                    String var = evalStr(s.arg("var"));
                    Container container = rawContainer(pos);
                    vars.put(var, BehaviorValue.str(container == null ? "" : summarize(container)));
                });
            }
            case "dumpContainer" -> {
                return containerOp(s, t, pos -> {
                    Container container = rawContainer(pos);
                    if (container != null) {
                        BotOutput.writeItems(evalStr(s.arg("file")), evalStr(s.arg("format")),
                            bot.getName().getString(), "container", collectItems(container));
                    }
                });
            }
            case "dumpInventory" -> BotOutput.writeItems(evalStr(s.arg("file")), evalStr(s.arg("format")),
                bot.getName().getString(), "inventory", collectItems(bot.getInventory()));
            case "output" -> BotOutput.writeText(evalStr(s.arg("file")), evalStr(s.arg("format")),
                bot == null ? "test" : bot.getName().getString(), evalStr(s.arg("content")));
            case "set" -> vars.put(evalStr(s.arg("var")), eval(s.arg("value")));
            case "change" -> {
                String var = evalStr(s.arg("var"));
                double delta = evalNum(s.arg("value"));
                BehaviorValue old = vars.getOrDefault(var, BehaviorValue.num(0));
                vars.put(var, BehaviorValue.num(old.asNumber() + delta));
            }
            case "listAdd" -> mutateList(s, (list, unused) -> list.add(eval(s.arg("value"))));
            case "listInsert" -> mutateList(s, (list, idx) -> {
                int i = clampIndex(idx, list.size() + 1);
                list.add(i, eval(s.arg("value")));
            });
            case "listRemove" -> mutateList(s, (list, idx) -> {
                int i = clampIndex(idx, list.size());
                if (i >= 0 && i < list.size()) {
                    list.remove(i);
                }
            });
            case "listSet" -> mutateList(s, (list, idx) -> {
                int i = clampIndex(idx, list.size());
                if (i >= 0 && i < list.size()) {
                    list.set(i, eval(s.arg("value")));
                }
            });
            case "listClear" -> mutateList(s, (list, unused) -> list.clear());
            case "broadcast" -> BehaviorManager.broadcastEvent(evalStr(s.arg("name")));
            case "repeat" -> {
                int times = (int) evalNum(s.arg("times"));
                List<Stmt> body = s.block("body");
                if (times > 0 && !body.isEmpty()) {
                    t.stack.push(new Frame(body, Kind.REPEAT, s, times));
                }
            }
            case "while" -> {
                List<Stmt> body = s.block("body");
                if (!body.isEmpty() && evalBool(s.arg("cond"))) {
                    t.stack.push(new Frame(body, Kind.WHILE, s, 0));
                }
            }
            case "forever" -> {
                List<Stmt> body = s.block("body");
                if (!body.isEmpty()) {
                    t.stack.push(new Frame(body, Kind.FOREVER, s, 0));
                }
            }
            case "if" -> {
                List<Stmt> branch = evalBool(s.arg("cond")) ? s.block("then") : s.block("else");
                if (!branch.isEmpty()) {
                    t.stack.push(new Frame(branch, Kind.SEQ, s, 0));
                }
            }
            case "def", "onStart", "onChat", "onBroadcast", "onHealthBelow", "onEntityNear" -> {
                // 帽子/定义语句已在构造时分拣，执行流中跳过
            }
            case "call" -> {
                Stmt def = functions.get(evalStr(s.arg("name")));
                if (def == null) {
                    lastError = program.sourceFile + ": 未定义的函数 \"" + evalStr(s.arg("name")) + "\"";
                    return false;
                }
                if (t.stack.size() >= MAX_CALL_DEPTH) {
                    lastError = program.sourceFile + ": 函数嵌套过深（>" + MAX_CALL_DEPTH + "）";
                    return false;
                }
                String params = def.arg("params") == null ? "" : evalStr(def.arg("params"));
                if (!params.isEmpty()) {
                    String[] names = params.split(",");
                    for (int i = 0; i < names.length; i++) {
                        Expr argExpr = s.arg("arg" + i);
                        vars.put(names[i].trim(), argExpr == null ? BehaviorValue.num(0) : eval(argExpr));
                    }
                }
                List<Stmt> fnBody = def.block("body");
                if (!fnBody.isEmpty()) {
                    t.stack.push(new Frame(fnBody, Kind.SEQ, s, 0));
                }
            }
            case "stopThread" -> {
                t.done = true;
                return true;
            }
            case "stopSelf" -> {
                closeIfOpen();
                finished = true;
                return true;
            }
            case "stopAll" -> {
                closeIfOpen();
                c.stopAll();
                finished = true;
                return true;
            }
            default -> {
                // 解析器已拦截未知 op，此处兑底跳过
            }
        }
        return false;
    }

    // ==================== 动作辅助 ====================

    /** 聊天：以系统消息广播 "<名字> 内容"（规避 1.19+ 签名聊天，跨版本安全） */
    private void say(String text) {
        if (bot != null && bot.getServer() != null) {
            bot.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("<" + bot.getName().getString() + "> " + text), false);
        }
    }

    private static void setMove(BotActionController c, String dir) {
        switch (dir) {
            case "backward" -> c.moveBackward();
            case "left" -> c.moveLeft();
            case "right" -> c.moveRight();
            default -> c.moveForward();
        }
    }

    /** 朝向：相对方向复用控制器；绝对方位换算 yaw（south=0, west=90, north=180, east=-90） */
    private void look(BotActionController c, String dir) {
        switch (dir) {
            case "up" -> c.lookUp();
            case "down" -> c.lookDown();
            case "left" -> c.lookLeft();
            case "right" -> c.lookRight();
            case "north" -> c.lookAt(180.0F, 0.0F);
            case "south" -> c.lookAt(0.0F, 0.0F);
            case "west" -> c.lookAt(90.0F, 0.0F);
            case "east" -> c.lookAt(-90.0F, 0.0F);
            default -> {
            }
        }
    }

    /** 把背包中指定物品换到当前手持槽 */
    private void equipItem(String itemId) {
        var inv = bot.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && itemId(stack).equals(itemId)) {
                if (i == inv.selected) {
                    return;
                }
                ItemStack held = inv.getItem(inv.selected);
                inv.setItem(inv.selected, stack);
                inv.setItem(i, held);
                return;
            }
        }
        lastError = program.sourceFile + ": 背包里没有 " + itemId;
    }

    /** 丢弃背包中指定物品（最多 count 个） */
    private void dropOf(String itemId, int count) {
        var inv = bot.getInventory();
        int remaining = Math.max(0, count);
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !itemId(stack).equals(itemId)) {
                continue;
            }
            int n = Math.min(remaining, stack.getCount());
            ItemStack dropped = stack.copy();
            dropped.setCount(n);
            stack.shrink(n);
            inv.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            bot.drop(dropped, false);
            remaining -= n;
        }
    }

    /** 找最近的匹配实体（type 空=任意生物） */
    private LivingEntity nearestEntity(double range, String type) {
        double r = Math.max(1, Math.min(32, range));
        List<LivingEntity> found = bot.serverLevel().getEntitiesOfClass(LivingEntity.class,
            bot.getBoundingBox().inflate(r), e -> e != bot && e.isAlive()
                && (type.isEmpty() || BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(type)));
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : found) {
            double d = e.distanceToSqr(bot);
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        return nearest;
    }

    private int countNearby(double range, String type) {
        return bot.serverLevel().getEntitiesOfClass(LivingEntity.class,
            bot.getBoundingBox().inflate(range), e -> e != bot && e.isAlive()
                && (type.isEmpty() || BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(type))).size();
    }

    // ==================== 列表语句辅助 ====================

    /** 取/建变量中的列表并就地修改（索引参数 1-based，Scratch 风格） */
    private void mutateList(Stmt s, java.util.function.BiConsumer<List<BehaviorValue>, Integer> op) {
        String var = evalStr(s.arg("var"));
        BehaviorValue value = vars.get(var);
        if (value == null || !value.isList()) {
            value = BehaviorValue.list(new ArrayList<>());
            vars.put(var, value);
        }
        int idx = s.arg("index") == null ? 0 : (int) evalNum(s.arg("index")) - 1;
        op.accept(value.asList(), idx);
    }

    private static int clampIndex(int idx, int sizeInclusive) {
        return Math.max(0, Math.min(idx, sizeInclusive - 1));
    }

    // ==================== 容器 ====================

    /**
     * 容器操作统一入口（拟真流程）：
     * 已打开该容器 → 直接执行；否则 远处先寻路靠近 → 看向容器+挥手 → 短暂停顿 → 真实打开 → 执行
     *
     * @return true = 当前线程已挂起（寻路/动画中）
     */
    private boolean containerOp(Stmt s, ThreadState t, Consumer<BlockPos> action) {
        if (bot == null) {
            return false; // 纯逻辑测试模式跳过实体交互
        }
        BlockPos pos = BlockPos.containing(evalNum(s.arg("x")), evalNum(s.arg("y")), evalNum(s.arg("z")));
        if (!bot.serverLevel().hasChunkAt(pos)
                || !(bot.serverLevel().getBlockEntity(pos) instanceof Container)) {
            lastError = program.sourceFile + ": " + pos.toShortString() + " 处不是容器或区块未加载";
            return false;
        }
        if (pos.equals(openContainerPos)) {
            action.accept(pos);
            return false;
        }
        // 换容器前先关掉上一个
        closeIfOpen();
        if (bot.blockPosition().distSqr(pos) > OPEN_RANGE * OPEN_RANGE) {
            // 够不着：先寻路到容器附近，到达后继续打开序列
            if (bot.getActionController().pathTo(pos)) {
                t.waitingForPath = true;
                t.afterPath = () -> beginOpen(pos, t, action);
                return true;
            }
            lastError = program.sourceFile + ": 无法寻路到容器 " + pos.toShortString();
            return false;
        }
        beginOpen(pos, t, action);
        return true;
    }

    /** 拟真打开：旋转视角看向容器 + 挥手，短暂停顿后走原版 openMenu（盖子/声音/viewer 计数真实生效） */
    private void beginOpen(BlockPos pos, ThreadState t, Consumer<BlockPos> action) {
        // 寻路可能放弃而未到达：仍超出交互距离则报错跳过
        if (bot.blockPosition().distSqr(pos) > CONTAINER_RANGE * CONTAINER_RANGE) {
            lastError = program.sourceFile + ": 到达不了容器 " + pos.toShortString() + "（寻路失败）";
            return;
        }
        bot.getActionController().lookAt(Vec3.atCenterOf(pos));
        bot.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        t.waitTicks = 8;
        t.afterWait = () -> {
            var state = bot.serverLevel().getBlockState(pos);
            var provider = state.getMenuProvider(bot.serverLevel(), pos);
            if (provider != null) {
                bot.openMenu(provider);
            }
            openContainerPos = pos;
            action.accept(pos);
        };
    }

    /** 关闭当前打开的容器（行为结束/切换容器/显式关闭时调用） */
    public void closeIfOpen() {
        if (openContainerPos != null && bot != null && !bot.isRemoved()) {
            bot.closeContainer();
        }
        openContainerPos = null;
    }

    /** 取已校验位置的容器（不做拟真流程，供 action 内部使用） */
    private Container rawContainer(BlockPos pos) {
        if (bot != null && bot.serverLevel().getBlockEntity(pos) instanceof Container container) {
            return container;
        }
        return null;
    }

    /** 从容器取物到背包：item 空串=任意物品，count 缺省=全部；实际移动数可存入 var */
    private void takeItems(Stmt s, BlockPos pos) {
        Container container = rawContainer(pos);
        if (container == null) {
            return;
        }
        String filter = s.arg("item") == null ? "" : evalStr(s.arg("item"));
        int want = s.arg("count") == null ? Integer.MAX_VALUE : Math.max(0, (int) evalNum(s.arg("count")));
        int moved = 0;
        for (int i = 0; i < container.getContainerSize() && want > 0; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!filter.isEmpty() && !itemId(stack).equals(filter)) {
                continue;
            }
            int take = Math.min(want, stack.getCount());
            ItemStack taking = stack.copy();
            taking.setCount(take);
            bot.getInventory().add(taking);
            int movedThis = take - taking.getCount();
            if (movedThis <= 0) {
                break; // 背包已满
            }
            stack.shrink(movedThis);
            container.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            moved += movedThis;
            want -= movedThis;
        }
        container.setChanged();
        if (s.arg("var") != null) {
            vars.put(evalStr(s.arg("var")), BehaviorValue.num(moved));
        }
    }

    /** 从背包放物到容器：item 空串=任意物品，count 缺省=全部；实际移动数可存入 var */
    private void putItems(Stmt s, BlockPos pos) {
        Container container = rawContainer(pos);
        if (container == null) {
            return;
        }
        String filter = s.arg("item") == null ? "" : evalStr(s.arg("item"));
        int want = s.arg("count") == null ? Integer.MAX_VALUE : Math.max(0, (int) evalNum(s.arg("count")));
        int moved = 0;
        var inv = bot.getInventory();
        for (int i = 0; i < inv.getContainerSize() && want > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!filter.isEmpty() && !itemId(stack).equals(filter)) {
                continue;
            }
            int give = Math.min(want, stack.getCount());
            ItemStack giving = stack.copy();
            giving.setCount(give);
            int inserted = insertInto(container, giving);
            if (inserted <= 0) {
                break; // 容器已满
            }
            stack.shrink(inserted);
            inv.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            moved += inserted;
            want -= inserted;
        }
        container.setChanged();
        if (s.arg("var") != null) {
            vars.put(evalStr(s.arg("var")), BehaviorValue.num(moved));
        }
    }

    /** 将物品堆插入容器（先叠加同类未满格，再用空格） @return 实际插入数量 */
    private static int insertInto(Container container, ItemStack stack) {
        int inserted = 0;
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, stack)
                    && slot.getCount() < slot.getMaxStackSize()) {
                int room = slot.getMaxStackSize() - slot.getCount();
                int add = Math.min(room, stack.getCount());
                slot.grow(add);
                stack.shrink(add);
                inserted += add;
            }
        }
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stack.copy());
                inserted += stack.getCount();
                stack.setCount(0);
            }
        }
        return inserted;
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** 容器内容摘要："id*数量;id*数量"（空容器为空串） */
    private static String summarize(Container container) {
        Map<String, Integer> items = collectItems(container);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : items.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(e.getKey()).append('*').append(e.getValue());
        }
        return sb.toString();
    }

    /** 汇总容器物品：物品 ID → 总数（保持遍历序） */
    private static Map<String, Integer> collectItems(Container container) {
        Map<String, Integer> items = new java.util.LinkedHashMap<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                items.merge(itemId(stack), stack.getCount(), Integer::sum);
            }
        }
        return items;
    }

    /** 读取传感器用容器（x/y/z 参数式，8 格限制，不触发拟真打开） */
    private Container sensorContainer(List<Expr> a) {
        if (bot == null || a.size() < 3) {
            return null;
        }
        BlockPos pos = BlockPos.containing(
            eval(a.get(0)).asNumber(), eval(a.get(1)).asNumber(), eval(a.get(2)).asNumber());
        if (bot.blockPosition().distSqr(pos) > CONTAINER_RANGE * CONTAINER_RANGE
                || !bot.serverLevel().hasChunkAt(pos)) {
            return null;
        }
        return bot.serverLevel().getBlockEntity(pos) instanceof Container container ? container : null;
    }

    // ==================== 表达式求值 ====================

    BehaviorValue eval(Expr expr) {
        if (expr == null) {
            return BehaviorValue.num(0);
        }
        if (expr instanceof Num n) {
            return BehaviorValue.num(n.v());
        }
        if (expr instanceof Str st) {
            return BehaviorValue.str(st.v());
        }
        if (expr instanceof Bool b) {
            return BehaviorValue.bool(b.v());
        }
        if (expr instanceof ListLit ll) {
            List<BehaviorValue> items = new ArrayList<>(ll.items().size());
            for (Expr item : ll.items()) {
                items.add(eval(item));
            }
            return BehaviorValue.list(items);
        }
        if (expr instanceof Var v) {
            return vars.getOrDefault(v.name(), BehaviorValue.num(0));
        }
        if (expr instanceof Un un) {
            return switch (un.op()) {
                case "not" -> BehaviorValue.bool(!eval(un.operand()).asBool());
                case "neg" -> BehaviorValue.num(-eval(un.operand()).asNumber());
                case "abs" -> BehaviorValue.num(Math.abs(eval(un.operand()).asNumber()));
                case "floor" -> BehaviorValue.num(Math.floor(eval(un.operand()).asNumber()));
                case "ceil" -> BehaviorValue.num(Math.ceil(eval(un.operand()).asNumber()));
                case "round" -> BehaviorValue.num(Math.round(eval(un.operand()).asNumber()));
                case "sqrt" -> BehaviorValue.num(Math.sqrt(Math.max(0, eval(un.operand()).asNumber())));
                default -> BehaviorValue.num(0);
            };
        }
        if (expr instanceof Bin bin) {
            return evalBin(bin);
        }
        if (expr instanceof Sensor sensor) {
            return evalSensor(sensor);
        }
        return BehaviorValue.num(0);
    }

    private double evalNum(Expr expr) {
        return eval(expr).asNumber();
    }

    private boolean evalBool(Expr expr) {
        return expr != null && eval(expr).asBool();
    }

    private String evalStr(Expr expr) {
        return eval(expr).asString();
    }

    private BehaviorValue evalBin(Bin bin) {
        if (bin.op().equals("and")) {
            return BehaviorValue.bool(eval(bin.left()).asBool() && eval(bin.right()).asBool());
        }
        if (bin.op().equals("or")) {
            return BehaviorValue.bool(eval(bin.left()).asBool() || eval(bin.right()).asBool());
        }
        BehaviorValue l = eval(bin.left());
        BehaviorValue r = eval(bin.right());
        return switch (bin.op()) {
            case "+" -> BehaviorValue.num(l.asNumber() + r.asNumber());
            case "-" -> BehaviorValue.num(l.asNumber() - r.asNumber());
            case "*" -> BehaviorValue.num(l.asNumber() * r.asNumber());
            case "/" -> BehaviorValue.num(r.asNumber() == 0 ? 0 : l.asNumber() / r.asNumber());
            case "%" -> BehaviorValue.num(r.asNumber() == 0 ? 0 : l.asNumber() % r.asNumber());
            case "==" -> BehaviorValue.bool(l.looseEquals(r));
            case "!=" -> BehaviorValue.bool(!l.looseEquals(r));
            case "<" -> BehaviorValue.bool(l.asNumber() < r.asNumber());
            case ">" -> BehaviorValue.bool(l.asNumber() > r.asNumber());
            case "<=" -> BehaviorValue.bool(l.asNumber() <= r.asNumber());
            case ">=" -> BehaviorValue.bool(l.asNumber() >= r.asNumber());
            case "concat" -> BehaviorValue.str(l.asString() + r.asString());
            case "min" -> BehaviorValue.num(Math.min(l.asNumber(), r.asNumber()));
            case "max" -> BehaviorValue.num(Math.max(l.asNumber(), r.asNumber()));
            case "pow" -> BehaviorValue.num(Math.pow(l.asNumber(), r.asNumber()));
            default -> BehaviorValue.num(0);
        };
    }

    private BehaviorValue evalSensor(Sensor sensor) {
        List<Expr> a = sensor.args();
        // 纯函数（不依赖实体，单元测试可用）
        switch (sensor.name()) {
            case "random": {
                double min = a.size() > 0 ? eval(a.get(0)).asNumber() : 0;
                double max = a.size() > 1 ? eval(a.get(1)).asNumber() : 100;
                if (max < min) {
                    double tmp = min;
                    min = max;
                    max = tmp;
                }
                return BehaviorValue.num(Math.floor(min + ThreadLocalRandom.current().nextDouble() * (max - min + 1)));
            }
            case "sin": return BehaviorValue.num(Math.sin(Math.toRadians(argNum(a, 0, 0))));
            case "cos": return BehaviorValue.num(Math.cos(Math.toRadians(argNum(a, 0, 0))));
            case "tan": return BehaviorValue.num(Math.tan(Math.toRadians(argNum(a, 0, 0))));
            case "strLen": return BehaviorValue.num(argStr(a, 0).length());
            case "strContains": return BehaviorValue.bool(
                argStr(a, 0).toLowerCase().contains(argStr(a, 1).toLowerCase()));
            case "strUpper": return BehaviorValue.str(argStr(a, 0).toUpperCase());
            case "strLower": return BehaviorValue.str(argStr(a, 0).toLowerCase());
            case "strTrim": return BehaviorValue.str(argStr(a, 0).trim());
            case "strCharAt": {
                String str = argStr(a, 0);
                int i = (int) argNum(a, 1, 1) - 1;
                return BehaviorValue.str(i >= 0 && i < str.length() ? String.valueOf(str.charAt(i)) : "");
            }
            case "strIndexOf": return BehaviorValue.num(
                argStr(a, 0).toLowerCase().indexOf(argStr(a, 1).toLowerCase()) + 1);
            case "strSub": {
                String str = argStr(a, 0);
                int from = Math.max(1, (int) argNum(a, 1, 1));
                int to = Math.min(str.length(), (int) argNum(a, 2, str.length()));
                return BehaviorValue.str(from <= to ? str.substring(from - 1, to) : "");
            }
            case "listGet": {
                List<BehaviorValue> list = argList(a, 0);
                int i = (int) argNum(a, 1, 1) - 1;
                return i >= 0 && i < list.size() ? list.get(i) : BehaviorValue.str("");
            }
            case "listLen": return BehaviorValue.num(argList(a, 0).size());
            case "listContains": {
                BehaviorValue needle = a.size() > 1 ? eval(a.get(1)) : BehaviorValue.str("");
                for (BehaviorValue item : argList(a, 0)) {
                    if (item.looseEquals(needle)) {
                        return BehaviorValue.bool(true);
                    }
                }
                return BehaviorValue.bool(false);
            }
            case "listIndexOf": {
                List<BehaviorValue> list = argList(a, 0);
                BehaviorValue needle = a.size() > 1 ? eval(a.get(1)) : BehaviorValue.str("");
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).looseEquals(needle)) {
                        return BehaviorValue.num(i + 1);
                    }
                }
                return BehaviorValue.num(0);
            }
            case "listJoin": {
                String sep = a.size() > 1 ? argStr(a, 1) : ", ";
                StringBuilder sb = new StringBuilder();
                for (BehaviorValue item : argList(a, 0)) {
                    if (sb.length() > 0) {
                        sb.append(sep);
                    }
                    sb.append(item.asString());
                }
                return BehaviorValue.str(sb.toString());
            }
            case "listSplit": {
                String sep = a.size() > 1 ? argStr(a, 1) : ",";
                List<BehaviorValue> parts = new ArrayList<>();
                if (!argStr(a, 0).isEmpty()) {
                    for (String piece : argStr(a, 0).split(java.util.regex.Pattern.quote(sep), -1)) {
                        parts.add(BehaviorValue.str(piece));
                    }
                }
                return BehaviorValue.list(parts);
            }
            case "listRandom": {
                List<BehaviorValue> list = argList(a, 0);
                return list.isEmpty() ? BehaviorValue.str("")
                    : list.get(ThreadLocalRandom.current().nextInt(list.size()));
            }
            default:
                break;
        }
        // 以下依赖实体（测试模式返回默认值）
        if (bot == null) {
            return BehaviorValue.num(0);
        }
        switch (sensor.name()) {
            case "health": return BehaviorValue.num(bot.getHealth());
            case "maxHealth": return BehaviorValue.num(bot.getMaxHealth());
            case "food": return BehaviorValue.num(bot.getFoodData().getFoodLevel());
            case "xpLevel": return BehaviorValue.num(bot.experienceLevel);
            case "armor": return BehaviorValue.num(bot.getArmorValue());
            case "air": return BehaviorValue.num(bot.getAirSupply());
            case "posX": return BehaviorValue.num(bot.getX());
            case "posY": return BehaviorValue.num(bot.getY());
            case "posZ": return BehaviorValue.num(bot.getZ());
            case "dimension": return BehaviorValue.str(bot.level().dimension().location().toString());
            case "botName": return BehaviorValue.str(bot.getName().getString());
            case "heldItem": return BehaviorValue.str(itemId(bot.getMainHandItem()));
            case "onGround": return BehaviorValue.bool(bot.onGround());
            case "inWater": return BehaviorValue.bool(bot.isInWater());
            case "onFire": return BehaviorValue.bool(bot.isOnFire());
            case "sneaking": return BehaviorValue.bool(bot.isShiftKeyDown());
            case "isRaining": return BehaviorValue.bool(bot.level().isRaining());
            case "isDay": return BehaviorValue.bool(bot.level().isDay());
            case "isPathfinding": return BehaviorValue.bool(bot.getActionController().isPathfinding());
            case "timeOfDay": return BehaviorValue.num(bot.level().getDayTime() % 24000L);
            case "blockAt": {
                BlockPos pos = BlockPos.containing(argNum(a, 0, 0), argNum(a, 1, 0), argNum(a, 2, 0));
                if (!bot.serverLevel().hasChunkAt(pos)) {
                    return BehaviorValue.str("minecraft:air");
                }
                return BehaviorValue.str(BuiltInRegistries.BLOCK.getKey(
                    bot.serverLevel().getBlockState(pos).getBlock()).toString());
            }
            case "distanceTo": {
                double dx = bot.getX() - argNum(a, 0, 0);
                double dy = bot.getY() - argNum(a, 1, 0);
                double dz = bot.getZ() - argNum(a, 2, 0);
                return BehaviorValue.num(Math.sqrt(dx * dx + dy * dy + dz * dz));
            }
            case "nearestPlayerName": {
                var p = nearestRealPlayer();
                return BehaviorValue.str(p == null ? "" : p.getName().getString());
            }
            case "nearestPlayerDistance": {
                var p = nearestRealPlayer();
                return BehaviorValue.num(p == null ? -1 : p.distanceTo(bot));
            }
            case "invCount": {
                String filter = a.isEmpty() ? null : eval(a.get(0)).asString();
                int total = 0;
                for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
                    ItemStack stack = bot.getInventory().getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (filter == null || filter.isEmpty() || itemId(stack).equals(filter)) {
                        total += stack.getCount();
                    }
                }
                return BehaviorValue.num(total);
            }
            case "nearbyEntities": {
                double range = a.isEmpty() ? 8 : Math.max(1, Math.min(32, eval(a.get(0)).asNumber()));
                String typeFilter = a.size() > 1 ? eval(a.get(1)).asString() : "";
                return BehaviorValue.num(countNearby(range, typeFilter));
            }
            case "containerSlots": {
                Container container = sensorContainer(a);
                return BehaviorValue.num(container == null ? 0 : container.getContainerSize());
            }
            case "containerItem": {
                Container container = sensorContainer(a);
                int slot = a.size() > 3 ? (int) eval(a.get(3)).asNumber() : 0;
                if (container == null || slot < 0 || slot >= container.getContainerSize()) {
                    return BehaviorValue.str("minecraft:air");
                }
                ItemStack stack = container.getItem(slot);
                return BehaviorValue.str(stack.isEmpty() ? "minecraft:air" : itemId(stack));
            }
            case "containerSlotCount": {
                Container container = sensorContainer(a);
                int slot = a.size() > 3 ? (int) eval(a.get(3)).asNumber() : 0;
                if (container == null || slot < 0 || slot >= container.getContainerSize()) {
                    return BehaviorValue.num(0);
                }
                return BehaviorValue.num(container.getItem(slot).getCount());
            }
            case "containerCount": {
                Container container = sensorContainer(a);
                if (container == null) {
                    return BehaviorValue.num(0);
                }
                String filter = a.size() > 3 ? eval(a.get(3)).asString() : null;
                int total = 0;
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (filter == null || filter.isEmpty() || itemId(stack).equals(filter)) {
                        total += stack.getCount();
                    }
                }
                return BehaviorValue.num(total);
            }
            default:
                return BehaviorValue.num(0);
        }
    }

    /** 最近的真人玩家（64 格内，排除假人） */
    private net.minecraft.world.entity.player.Player nearestRealPlayer() {
        net.minecraft.world.entity.player.Player nearest = null;
        double best = Double.MAX_VALUE;
        for (var p : bot.serverLevel().players()) {
            if (p == bot || p instanceof BotPlayer || p.isSpectator()) {
                continue;
            }
            double d = p.distanceToSqr(bot);
            if (d < best && d <= 64 * 64) {
                best = d;
                nearest = p;
            }
        }
        return nearest;
    }

    private double argNum(List<Expr> a, int i, double fallback) {
        return a.size() > i ? eval(a.get(i)).asNumber() : fallback;
    }

    private String argStr(List<Expr> a, int i) {
        return a.size() > i ? eval(a.get(i)).asString() : "";
    }

    private List<BehaviorValue> argList(List<Expr> a, int i) {
        return a.size() > i ? eval(a.get(i)).asList() : new ArrayList<>();
    }

    /** 测试用：读取变量当前值 */
    public BehaviorValue getVar(String name) {
        return vars.getOrDefault(name, BehaviorValue.num(0));
    }
}
