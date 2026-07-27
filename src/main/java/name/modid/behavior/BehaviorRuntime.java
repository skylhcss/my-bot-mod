package name.modid.behavior;

import name.modid.MyBotMod;
import name.modid.behavior.BehaviorProgram.Bin;
import name.modid.behavior.BehaviorProgram.Bool;
import name.modid.behavior.BehaviorProgram.Expr;
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
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 行为脚本解释器（每假人每行为一个实例）
 *
 * 执行模型：
 * - 显式栈帧支持任意嵌套的 repeat/while/forever/if；
 * - wait / 定时移动 / goto 为"挂起"语义，挂起后本 tick 让出，后续 tick 恢复；
 * - 每 tick 语句执行预算 {@link #BUDGET_PER_TICK}，防止无 wait 的死循环卡服；
 * - 运行错误不抛出：记录 lastError 并跳过该语句。
 */
public class BehaviorRuntime {

    /** 每 tick 最多执行的语句数（超出自动让出到下一 tick） */
    private static final int BUDGET_PER_TICK = 1000;
    /** 容器交互最大距离（格） */
    private static final double CONTAINER_RANGE = 8.0;
    /** 超出此距离需先寻路靠近才能打开容器（格） */
    private static final double OPEN_RANGE = 4.0;
    /** 函数调用最大嵌套深度（防递归爆栈） */
    private static final int MAX_CALL_DEPTH = 64;

    private final BotPlayer bot;
    private final BehaviorProgram program;
    private final Map<String, BehaviorValue> vars = new HashMap<>();
    private final Deque<Frame> stack = new ArrayDeque<>();
    /** 顶层 def 自定义函数表：函数名 → def 语句 */
    private final Map<String, Stmt> functions = new HashMap<>();
    /** 主流程（普通顶层语句 + onStart 事件体，按出现顺序合并） */
    private final List<Stmt> mainBody;
    /** 聊天触发器（op=onChat 帽子语句） */
    private final List<Stmt> chatTriggers = new java.util.ArrayList<>();

    private int waitTicks;
    private Runnable afterWait;
    private boolean waitingForPath;
    /** 寻路结束（到达/放弃）后的回调（容器拟真打开序列用） */
    private Runnable afterPath;
    /** 当前已打开的容器位置（null=未打开） */
    private BlockPos openContainerPos;
    private boolean finished;
    private String lastError;

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
        // 顶层分拣：def 入函数表；onStart 体并入主流程；onChat 注册为触发器；其余为主流程（向后兼容）
        List<Stmt> main = new java.util.ArrayList<>();
        for (Stmt s : program.body) {
            switch (s.op()) {
                case "def" -> {
                    String fname = eval(s.arg("name")).asString();
                    if (!fname.isEmpty()) {
                        functions.put(fname, s);
                    }
                }
                case "onStart" -> main.addAll(s.block("body"));
                case "onChat" -> chatTriggers.add(s);
                default -> main.add(s);
            }
        }
        this.mainBody = List.copyOf(main);
        if (!mainBody.isEmpty()) {
            stack.push(new Frame(mainBody, Kind.SEQ, null, 0));
        }
    }

    /**
     * 玩家聊天事件分发：匹配任一 onChat 触发器（pattern 空=任意消息，否则大小写不敏感包含匹配）
     * 则打断当前执行并运行触发体；发送者/消息存入变量 chat_sender / chat_message。
     *
     * @return 是否触发
     */
    public boolean onChatMessage(String sender, String message) {
        if (finished || message == null) {
            return false;
        }
        for (Stmt trigger : chatTriggers) {
            String pattern = trigger.arg("text") == null ? "" : evalStr(trigger.arg("text"));
            if (pattern.isEmpty() || message.toLowerCase().contains(pattern.toLowerCase())) {
                vars.put("chat_sender", BehaviorValue.str(sender));
                vars.put("chat_message", BehaviorValue.str(message));
                // 打断当前执行（含挂起、寻路与容器交互），改跑触发体
                stack.clear();
                waitTicks = 0;
                afterWait = null;
                waitingForPath = false;
                afterPath = null;
                closeIfOpen();
                if (bot != null) {
                    bot.getActionController().cancelPath();
                }
                List<Stmt> body = trigger.block("body");
                if (!body.isEmpty()) {
                    stack.push(new Frame(body, Kind.SEQ, trigger, 0));
                }
                return true;
            }
        }
        return false;
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

    /** 每 tick 由 BehaviorManager 调用 */
    public void tick() {
        if (finished || (bot != null && bot.isRemoved())) {
            finished = true;
            return;
        }
        // 定时挂起
        if (waitTicks > 0) {
            waitTicks--;
            if (waitTicks == 0 && afterWait != null) {
                Runnable r = afterWait;
                afterWait = null;
                r.run();
            }
            return;
        }
        // 等待寻路结束（到达或放弃）
        if (waitingForPath) {
            if (bot.getActionController().isPathfinding()) {
                return;
            }
            waitingForPath = false;
            if (afterPath != null) {
                Runnable r = afterPath;
                afterPath = null;
                r.run();
                return; // 回调可能重新挂起（waitTicks），本 tick 让出
            }
        }

        int budget = BUDGET_PER_TICK;
        while (budget-- > 0 && !finished) {
            Frame f = stack.peek();
            if (f == null) {
                // 主流程结束：loop 则重跑；含聊天触发器则保持监听不结束；否则完成
                if (program.loop && !mainBody.isEmpty()) {
                    stack.push(new Frame(mainBody, Kind.SEQ, null, 0));
                    return; // 每轮循环至少让出一 tick
                }
                if (!chatTriggers.isEmpty()) {
                    return; // 驻留监听聊天事件
                }
                finished = true;
                return;
            }
            if (f.index >= f.stmts.size()) {
                // 帧末尾：循环回卷或弹栈
                switch (f.kind) {
                    case REPEAT -> {
                        if (--f.remaining > 0) {
                            f.index = 0;
                        } else {
                            stack.pop();
                        }
                    }
                    case WHILE -> {
                        if (evalBool(f.owner.arg("cond"))) {
                            f.index = 0;
                        } else {
                            stack.pop();
                        }
                    }
                    case FOREVER -> f.index = 0;
                    default -> stack.pop();
                }
                continue;
            }
            Stmt s = f.stmts.get(f.index++);
            try {
                if (execute(s)) {
                    return; // 挂起
                }
            } catch (RuntimeException e) {
                lastError = program.sourceFile + ": " + s.op() + " 执行出错 - " + e.getMessage();
                MyBotMod.LOGGER.warn("[行为] {}", lastError);
            }
        }
    }

    // ==================== 语句执行 ====================

    /** @return true = 已挂起，本 tick 结束 */
    private boolean execute(Stmt s) {
        // bot 在纯逻辑单元测试中可为 null（仅执行控制流/变量类语句）
        BotActionController c = bot == null ? null : bot.getActionController();
        switch (s.op()) {
            case "say" -> say(evalStr(s.arg("text")));
            case "wait" -> {
                waitTicks = Math.max(1, (int) evalNum(s.arg("ticks")));
                return true;
            }
            case "move" -> {
                setMove(c, evalStr(s.arg("dir")));
                if (s.arg("ticks") != null) {
                    waitTicks = Math.max(1, (int) evalNum(s.arg("ticks")));
                    afterWait = c::stopMovement;
                    return true;
                }
            }
            case "stopMove" -> c.stopMovement();
            case "jump" -> {
                c.setJump(true);
                waitTicks = 1;
                afterWait = () -> c.setJump(false);
                return true;
            }
            case "sneak" -> c.setSneak(s.arg("on") == null || evalBool(s.arg("on")));
            case "sprint" -> c.setSprint(s.arg("on") == null || evalBool(s.arg("on")));
            case "look" -> look(c, evalStr(s.arg("dir")));
            case "lookAt" -> c.lookAt(new Vec3(evalNum(s.arg("x")), evalNum(s.arg("y")), evalNum(s.arg("z"))));
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
            case "goto" -> {
                BlockPos target = BlockPos.containing(
                    evalNum(s.arg("x")), evalNum(s.arg("y")), evalNum(s.arg("z")));
                if (c.pathTo(target)) {
                    waitingForPath = true;
                    return true;
                }
                lastError = program.sourceFile + ": goto 无法到达 " + target.toShortString();
            }
            case "gotoStop" -> c.cancelPath();
            case "mount" -> c.mount();
            case "dismount" -> c.dismount();
            case "openContainer" -> {
                return containerOp(s, pos -> {
                });
            }
            case "closeContainer" -> closeIfOpen();
            case "takeFromContainer" -> {
                return containerOp(s, pos -> takeItems(s, pos));
            }
            case "putToContainer" -> {
                return containerOp(s, pos -> putItems(s, pos));
            }
            case "readContainer" -> {
                return containerOp(s, pos -> {
                    String var = evalStr(s.arg("var"));
                    Container container = rawContainer(pos);
                    vars.put(var, BehaviorValue.str(container == null ? "" : summarize(container)));
                });
            }
            case "dumpContainer" -> {
                return containerOp(s, pos -> {
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
                bot.getName().getString(), evalStr(s.arg("content")));
            case "set" -> vars.put(evalStr(s.arg("var")), eval(s.arg("value")));
            case "change" -> {
                String var = evalStr(s.arg("var"));
                double delta = evalNum(s.arg("value"));
                BehaviorValue old = vars.getOrDefault(var, BehaviorValue.num(0));
                vars.put(var, BehaviorValue.num(old.asNumber() + delta));
            }
            case "repeat" -> {
                int times = (int) evalNum(s.arg("times"));
                List<Stmt> body = s.block("body");
                if (times > 0 && !body.isEmpty()) {
                    stack.push(new Frame(body, Kind.REPEAT, s, times));
                }
            }
            case "while" -> {
                List<Stmt> body = s.block("body");
                if (!body.isEmpty() && evalBool(s.arg("cond"))) {
                    stack.push(new Frame(body, Kind.WHILE, s, 0));
                }
            }
            case "forever" -> {
                List<Stmt> body = s.block("body");
                if (!body.isEmpty()) {
                    stack.push(new Frame(body, Kind.FOREVER, s, 0));
                }
            }
            case "if" -> {
                List<Stmt> branch = evalBool(s.arg("cond")) ? s.block("then") : s.block("else");
                if (!branch.isEmpty()) {
                    stack.push(new Frame(branch, Kind.SEQ, s, 0));
                }
            }
            case "def", "onStart", "onChat" -> {
                // 帽子/定义语句已在构造时分拣，执行流中跳过
            }
            case "call" -> {
                Stmt def = functions.get(evalStr(s.arg("name")));
                if (def == null) {
                    lastError = program.sourceFile + ": 未定义的函数 \"" + evalStr(s.arg("name")) + "\"";
                    return false;
                }
                if (stack.size() >= MAX_CALL_DEPTH) {
                    lastError = program.sourceFile + ": 函数嵌套过深（>" + MAX_CALL_DEPTH + "）";
                    return false;
                }
                // 绑定参数：def.params 为逗号分隔参数名，实参为 arg0/arg1/...
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
                    stack.push(new Frame(fnBody, Kind.SEQ, s, 0));
                }
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
                // 解析器已拦截未知 op，此处兜底跳过
            }
        }
        return false;
    }

    /** 聊天：以系统消息广播 "<名字> 内容"（规避 1.19+ 签名聊天，跨版本安全） */
    private void say(String text) {
        if (bot.getServer() != null) {
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

    // ==================== 容器 ====================

    /**
     * 容器操作统一入口（拟真流程）：
     * 已打开该容器 → 直接执行；否则 远处先寻路靠近 → 看向容器+挥手 → 短暂停顿 → 真实打开（盖子动画/声音） → 执行操作
     *
     * @return true = 已挂起（寻路/动画中）
     */
    private boolean containerOp(Stmt s, java.util.function.Consumer<BlockPos> action) {
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
                waitingForPath = true;
                afterPath = () -> beginOpen(pos, action);
                return true;
            }
            lastError = program.sourceFile + ": 无法寻路到容器 " + pos.toShortString();
            return false;
        }
        beginOpen(pos, action);
        return true;
    }

    /** 拟真打开：旋转视角看向容器 + 挥手，短暂停顿后走原版 openMenu（盖子/声音/viewer 计数真实生效） */
    private void beginOpen(BlockPos pos, java.util.function.Consumer<BlockPos> action) {
        // 寻路可能放弃而未到达：仍超出交互距离则报错跳过
        if (bot.blockPosition().distSqr(pos) > CONTAINER_RANGE * CONTAINER_RANGE) {
            lastError = program.sourceFile + ": 到达不了容器 " + pos.toShortString() + "（寻路失败）";
            return;
        }
        bot.getActionController().lookAt(Vec3.atCenterOf(pos));
        bot.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        waitTicks = 8;
        afterWait = () -> {
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
            int movedThis = take - taking.getCount(); // add 后剩余未放入的留在 taking
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
        // 先叠加
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
        // 再占空格
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
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                items.merge(id, stack.getCount(), Integer::sum);
            }
        }
        return items;
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
        // and/or 短路
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
        switch (sensor.name()) {
            case "health":
                return BehaviorValue.num(bot.getHealth());
            case "food":
                return BehaviorValue.num(bot.getFoodData().getFoodLevel());
            case "posX":
                return BehaviorValue.num(bot.getX());
            case "posY":
                return BehaviorValue.num(bot.getY());
            case "posZ":
                return BehaviorValue.num(bot.getZ());
            case "dimension":
                return BehaviorValue.str(bot.level().dimension().location().toString());
            case "heldItem":
                return BehaviorValue.str(
                    BuiltInRegistries.ITEM.getKey(bot.getMainHandItem().getItem()).toString());
            case "invCount": {
                String filter = a.isEmpty() ? null : eval(a.get(0)).asString();
                int total = 0;
                for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
                    ItemStack stack = bot.getInventory().getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (filter == null || filter.isEmpty()
                            || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(filter)) {
                        total += stack.getCount();
                    }
                }
                return BehaviorValue.num(total);
            }
            case "nearbyEntities": {
                double range = a.isEmpty() ? 8 : Math.max(1, Math.min(32, eval(a.get(0)).asNumber()));
                String typeFilter = a.size() > 1 ? eval(a.get(1)).asString() : "";
                int count = bot.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    bot.getBoundingBox().inflate(range), e -> e != bot && e.isAlive()
                        && (typeFilter.isEmpty() || BuiltInRegistries.ENTITY_TYPE.getKey(e.getType())
                            .toString().equals(typeFilter))).size();
                return BehaviorValue.num(count);
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
                if (a.size() < 3) {
                    return BehaviorValue.num(0);
                }
                BlockPos pos = BlockPos.containing(
                    eval(a.get(0)).asNumber(), eval(a.get(1)).asNumber(), eval(a.get(2)).asNumber());
                if (bot.blockPosition().distSqr(pos) > CONTAINER_RANGE * CONTAINER_RANGE
                        || !bot.serverLevel().hasChunkAt(pos)
                        || !(bot.serverLevel().getBlockEntity(pos) instanceof Container container)) {
                    return BehaviorValue.num(0);
                }
                String filter = a.size() > 3 ? eval(a.get(3)).asString() : null;
                int total = 0;
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (filter == null || filter.isEmpty()
                            || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(filter)) {
                        total += stack.getCount();
                    }
                }
                return BehaviorValue.num(total);
            }
            case "isPathfinding":
                return BehaviorValue.bool(bot.getActionController().isPathfinding());
            case "timeOfDay":
                return BehaviorValue.num(bot.level().getDayTime() % 24000L);
            case "random": {
                double min = a.size() > 0 ? eval(a.get(0)).asNumber() : 0;
                double max = a.size() > 1 ? eval(a.get(1)).asNumber() : 100;
                if (max < min) {
                    double t = min;
                    min = max;
                    max = t;
                }
                // 整数闭区间取随机（Scratch 语义）
                return BehaviorValue.num(Math.floor(min + ThreadLocalRandom.current().nextDouble() * (max - min + 1)));
            }
            default:
                return BehaviorValue.num(0);
        }
    }

    /** 测试用：读取变量当前值 */
    public BehaviorValue getVar(String name) {
        return vars.getOrDefault(name, BehaviorValue.num(0));
    }
}
