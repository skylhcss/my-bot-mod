package name.modid.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import name.modid.bot.BotActionController;
import name.modid.bot.BotManager;
import name.modid.bot.BotPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * /bot 命令实现
 * 提供假人的创建、控制和管理功能
 */
public class BotCommand {

    /**
     * 注册命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bot")
            .requires(source -> source.hasPermission(2)) // 需要OP权限
            .then(Commands.argument("botName", StringArgumentType.word())
                // /bot <name> spawn
                .then(Commands.literal("spawn")
                    .executes(BotCommand::spawnBot)
                    .then(Commands.literal("at")
                        .then(Commands.argument("position", Vec3Argument.vec3())
                            .executes(BotCommand::spawnBotAt)
                        )
                    )
                )
                // /bot <name> kill
                .then(Commands.literal("kill")
                    .executes(BotCommand::killBot)
                )
                // /bot <name> attack
                .then(Commands.literal("attack")
                    .executes(ctx -> controlBotAction(ctx, "attack", false))
                    .then(Commands.literal("continuous")
                        .executes(ctx -> controlBotAction(ctx, "attack", true))
                    )
                )
                // /bot <name> use
                .then(Commands.literal("use")
                    .executes(ctx -> controlBotAction(ctx, "use", false))
                    .then(Commands.literal("continuous")
                        .executes(ctx -> controlBotAction(ctx, "use", true))
                    )
                )
                // /bot <name> stop - 停止所有动作或特定动作
                .then(Commands.literal("stop")
                    .executes(BotCommand::stopAllActions)
                    .then(Commands.literal("attack")
                        .executes(ctx -> stopBotAction(ctx, "attack"))
                    )
                    .then(Commands.literal("use")
                        .executes(ctx -> stopBotAction(ctx, "use"))
                    )
                    .then(Commands.literal("jump")
                        .executes(ctx -> toggleBotState(ctx, "jump", false))
                    )
                )
                // /bot <name> sneak
                .then(Commands.literal("sneak")
                    .executes(ctx -> toggleBotState(ctx, "sneak", true))
                )
                // /bot <name> unsneak
                .then(Commands.literal("unsneak")
                    .executes(ctx -> toggleBotState(ctx, "sneak", false))
                )
                // /bot <name> jump
                .then(Commands.literal("jump")
                    .executes(ctx -> toggleBotState(ctx, "jump", true))
                )
                // /bot <name> sprint
                .then(Commands.literal("sprint")
                    .executes(ctx -> toggleBotState(ctx, "sprint", true))
                )
                // /bot <name> unsprint
                .then(Commands.literal("unsprint")
                    .executes(ctx -> toggleBotState(ctx, "sprint", false))
                )
                // /bot <name> look up
                .then(Commands.literal("look")
                    .then(Commands.literal("up")
                        .executes(ctx -> lookDirection(ctx, "up", 15.0F))
                        .then(Commands.argument("angle", FloatArgumentType.floatArg(0.0F, 90.0F))
                            .executes(ctx -> lookDirection(ctx, "up", FloatArgumentType.getFloat(ctx, "angle")))
                        )
                    )
                    .then(Commands.literal("down")
                        .executes(ctx -> lookDirection(ctx, "down", 15.0F))
                        .then(Commands.argument("angle", FloatArgumentType.floatArg(0.0F, 90.0F))
                            .executes(ctx -> lookDirection(ctx, "down", FloatArgumentType.getFloat(ctx, "angle")))
                        )
                    )
                    .then(Commands.literal("left")
                        .executes(ctx -> lookDirection(ctx, "left", 15.0F))
                        .then(Commands.argument("angle", FloatArgumentType.floatArg(0.0F, 180.0F))
                            .executes(ctx -> lookDirection(ctx, "left", FloatArgumentType.getFloat(ctx, "angle")))
                        )
                    )
                    .then(Commands.literal("right")
                        .executes(ctx -> lookDirection(ctx, "right", 15.0F))
                        .then(Commands.argument("angle", FloatArgumentType.floatArg(0.0F, 180.0F))
                            .executes(ctx -> lookDirection(ctx, "right", FloatArgumentType.getFloat(ctx, "angle")))
                        )
                    )
                    .then(Commands.literal("north")
                        .executes(ctx -> lookCardinal(ctx, 180.0F))
                    )
                    .then(Commands.literal("south")
                        .executes(ctx -> lookCardinal(ctx, 0.0F))
                    )
                    .then(Commands.literal("east")
                        .executes(ctx -> lookCardinal(ctx, -90.0F))
                    )
                    .then(Commands.literal("west")
                        .executes(ctx -> lookCardinal(ctx, 90.0F))
                    )
                )
                // /bot <name> move forward/backward/left/right/stop
                .then(Commands.literal("move")
                    .then(Commands.literal("forward")
                        .executes(ctx -> moveBot(ctx, "forward"))
                    )
                    .then(Commands.literal("backward")
                        .executes(ctx -> moveBot(ctx, "backward"))
                    )
                    .then(Commands.literal("left")
                        .executes(ctx -> moveBot(ctx, "left"))
                    )
                    .then(Commands.literal("right")
                        .executes(ctx -> moveBot(ctx, "right"))
                    )
                    .then(Commands.literal("stop")
                        .executes(ctx -> moveBot(ctx, "stop"))
                    )
                )
                // /bot <name> drop - 丢弃物品
                .then(Commands.literal("drop")
                    .executes(BotCommand::dropItem)
                )
                // /bot <name> dropStack - 丢弃整组物品
                .then(Commands.literal("dropStack")
                    .executes(BotCommand::dropStack)
                )
                // /bot <name> swapHands - 交换主副手
                .then(Commands.literal("swapHands")
                    .executes(BotCommand::swapHands)
                )
                // /bot <name> mount - 骑乘附近实体
                .then(Commands.literal("mount")
                    .executes(BotCommand::mountEntity)
                )
                // /bot <name> dismount - 下马
                .then(Commands.literal("dismount")
                    .executes(BotCommand::dismountEntity)
                )
                // /bot <name> turn <yaw> <pitch> - 旋转视角
                .then(Commands.literal("turn")
                    .then(Commands.argument("yaw", FloatArgumentType.floatArg(-180.0F, 180.0F))
                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90.0F, 90.0F))
                            .executes(BotCommand::turnBot)
                        )
                    )
                )
            )
            // /bot list
            .then(Commands.literal("list")
                .executes(BotCommand::listBots)
            )
            // /bot test - 快速测试所有功能
            .then(Commands.literal("test")
                .executes(BotCommand::runTests)
                .then(Commands.literal("movement")
                    .executes(ctx -> runSpecificTest(ctx, "movement"))
                )
                .then(Commands.literal("actions")
                    .executes(ctx -> runSpecificTest(ctx, "actions"))
                )
                .then(Commands.literal("skin")
                    .executes(ctx -> runSpecificTest(ctx, "skin"))
                )
                .then(Commands.literal("all")
                    .executes(BotCommand::runTests)
                )
            )
        );
    }

    /**
     * 召唤假人（在玩家位置）
     */
    private static int spawnBot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String botName = StringArgumentType.getString(ctx, "botName");

        // 验证名字格式
        if (!isValidBotName(botName)) {
            ctx.getSource().sendFailure(Component.literal(
                "无效的假人名字！名字必须：\n" +
                "- 长度 3-16 个字符\n" +
                "- 只包含字母、数字和下划线\n" +
                "- 例如：Bot_1, TestBot, Steve123"
            ));
            return 0;
        }

        if (BotManager.hasBot(botName)) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 已存在！"));
            return 0;
        }

        BotPlayer bot = BotManager.createBot(
            ctx.getSource().getServer(),
            player,
            botName,
            null, // 使用玩家位置
            null  // 使用玩家游戏模式
        );

        if (bot != null) {
            ctx.getSource().sendSuccess(() -> Component.literal("成功召唤假人 " + botName), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("召唤假人失败！"));
            return 0;
        }
    }

    /**
     * 在指定位置召唤假人
     */
    private static int spawnBotAt(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String botName = StringArgumentType.getString(ctx, "botName");
        Vec3 position = Vec3Argument.getVec3(ctx, "position");

        // 验证名字格式
        if (!isValidBotName(botName)) {
            ctx.getSource().sendFailure(Component.literal(
                "无效的假人名字！名字必须：\n" +
                "- 长度 3-16 个字符\n" +
                "- 只包含字母、数字和下划线\n" +
                "- 例如：Bot_1, TestBot, Steve123"
            ));
            return 0;
        }

        if (BotManager.hasBot(botName)) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 已存在！"));
            return 0;
        }

        BotPlayer bot = BotManager.createBot(
            ctx.getSource().getServer(),
            player,
            botName,
            position,
            null
        );

        if (bot != null) {
            ctx.getSource().sendSuccess(() -> Component.literal("成功在 " + 
                String.format("%.1f, %.1f, %.1f", position.x, position.y, position.z) + 
                " 召唤假人 " + botName), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("召唤假人失败！"));
            return 0;
        }
    }
    
    /**
     * 验证假人名字是否有效
     */
    private static boolean isValidBotName(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) {
            return false;
        }
        return name.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * 移除假人
     */
    private static int killBot(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");

        if (BotManager.removeBot(botName)) {
            ctx.getSource().sendSuccess(() -> Component.literal("已移除假人 " + botName), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }
    }

    /**
     * 控制假人动作（攻击、使用）
     */
    private static int controlBotAction(CommandContext<CommandSourceStack> ctx, String action, boolean continuous) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        BotActionController controller = bot.getActionController();
        
        switch (action) {
            case "attack":
                controller.startAttack(continuous);
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "假人 " + botName + (continuous ? " 开始持续攻击" : " 攻击一次")), true);
                break;
            case "use":
                controller.startUse(continuous);
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "假人 " + botName + (continuous ? " 开始持续使用物品" : " 使用物品一次")), true);
                break;
        }

        return 1;
    }

    /**
     * 停止假人动作
     */
    private static int stopBotAction(CommandContext<CommandSourceStack> ctx, String action) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        BotActionController controller = bot.getActionController();
        
        switch (action) {
            case "attack":
                controller.stopAttack();
                ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " 停止攻击"), true);
                break;
            case "use":
                controller.stopUse();
                ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " 停止使用物品"), true);
                break;
        }

        return 1;
    }

    /**
     * 切换假人状态（潜行、跳跃、疾跑）
     */
    private static int toggleBotState(CommandContext<CommandSourceStack> ctx, String state, boolean enable) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        BotActionController controller = bot.getActionController();
        String actionName = "";
        
        switch (state) {
            case "sneak":
                controller.setSneak(enable);
                actionName = enable ? "开始潜行" : "停止潜行";
                break;
            case "jump":
                controller.setJump(enable);
                actionName = enable ? "开始跳跃" : "停止跳跃";
                break;
            case "sprint":
                controller.setSprint(enable);
                actionName = enable ? "开始疾跑" : "停止疾跑";
                break;
        }

        String finalActionName = actionName;
        ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " " + finalActionName), true);
        return 1;
    }

    /**
     * 控制假人看向方向
     */
    private static int lookDirection(CommandContext<CommandSourceStack> ctx, String direction, float angle) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        BotActionController controller = bot.getActionController();
        
        switch (direction) {
            case "up":
                controller.lookUp(angle);
                break;
            case "down":
                controller.lookDown(angle);
                break;
            case "left":
                controller.lookLeft(angle);
                break;
            case "right":
                controller.lookRight(angle);
                break;
        }

        String directionName = getDirectionName(direction);
        ctx.getSource().sendSuccess(() -> Component.literal(
            "假人 " + botName + " 向" + directionName + "看 " + angle + "°"), true);
        return 1;
    }

    /**
     * 控制假人看向基本方向
     */
    private static int lookCardinal(CommandContext<CommandSourceStack> ctx, float yaw) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        bot.getActionController().lookAt(yaw, bot.getXRot());
        
        String direction = yaw == 180.0F ? "北" : yaw == 0.0F ? "南" : yaw == -90.0F ? "东" : "西";
        ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " 看向" + direction + "方"), true);
        return 1;
    }

    /**
     * 控制假人移动
     */
    private static int moveBot(CommandContext<CommandSourceStack> ctx, String direction) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        BotActionController controller = bot.getActionController();
        
        switch (direction) {
            case "forward":
                controller.moveForward();
                break;
            case "backward":
                controller.moveBackward();
                break;
            case "left":
                controller.moveLeft();
                break;
            case "right":
                controller.moveRight();
                break;
            case "stop":
                controller.stopMovement();
                break;
        }

        String actionName = direction.equals("stop") ? "停止移动" : "向" + getDirectionName(direction) + "移动";
        String finalActionName = actionName;
        ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " " + finalActionName), true);
        return 1;
    }

    /**
     * 列出所有假人
     */
    private static int listBots(CommandContext<CommandSourceStack> ctx) {
        var bots = BotManager.getAllBots();
        
        if (bots.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("当前没有假人"), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("当前假人列表 (" + bots.size() + "):"), false);
        for (BotPlayer bot : bots) {
            String botName = bot.getName().getString();
            String creatorName = bot.getCreatorName();
            ctx.getSource().sendSuccess(() -> Component.literal("  - " + botName + 
                " (创建者: " + creatorName + ")"), false);
        }
        
        return bots.size();
    }

    /**
     * 停止假人的所有动作
     */
    private static int stopAllActions(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        bot.getActionController().stopAll();
        ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " 已停止所有动作"), true);
        return 1;
    }

    /**
     * 假人丢弃物品
     */
    private static int dropItem(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        bot.getActionController().dropItem();
        ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " 丢弃了物品"), true);
        return 1;
    }

    /**
     * 假人丢弃整组物品
     */
    private static int dropStack(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        bot.getActionController().dropStack();
        ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " 丢弃了整组物品"), true);
        return 1;
    }

    /**
     * 假人交换主副手物品
     */
    private static int swapHands(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        bot.getActionController().swapHands();
        ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " 交换了主副手物品"), true);
        return 1;
    }

    /**
     * 假人骑乘附近实体
     */
    private static int mountEntity(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        if (bot.getActionController().mount()) {
            ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " 骑乘了附近的实体"), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 附近没有可骑乘的实体"));
            return 0;
        }
    }

    /**
     * 假人下马
     */
    private static int dismountEntity(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        if (bot.getActionController().dismount()) {
            ctx.getSource().sendSuccess(() -> Component.literal("假人 " + botName + " 下马了"), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 当前没有骑乘任何实体"));
            return 0;
        }
    }

    /**
     * 假人旋转视角
     */
    private static int turnBot(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("假人 " + botName + " 不存在！"));
            return 0;
        }

        float yaw = FloatArgumentType.getFloat(ctx, "yaw");
        float pitch = FloatArgumentType.getFloat(ctx, "pitch");

        bot.getActionController().turn(yaw, pitch);
        ctx.getSource().sendSuccess(() -> Component.literal(
            "假人 " + botName + " 旋转了视角 (偏航: " + yaw + "°, 俯仰: " + pitch + "°)"), true);
        return 1;
    }

    /**
     * 获取方向的中文名称
     */
    private static String getDirectionName(String direction) {
        switch (direction) {
            case "up": return "上";
            case "down": return "下";
            case "left": return "左";
            case "right": return "右";
            case "forward": return "前";
            case "backward": return "后";
            default: return direction;
        }
    }
    
    /**
     * 运行所有测试
     */
    private static int runTests(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        
        ctx.getSource().sendSuccess(() -> Component.literal("=== 开始假人功能测试 ==="), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        int totalTests = 0;
        int passedTests = 0;
        
        // 测试 1: 名字验证
        ctx.getSource().sendSuccess(() -> Component.literal("§e[测试 1/6] 名字验证"), false);
        totalTests++;
        if (testNameValidation(ctx)) {
            passedTests++;
            ctx.getSource().sendSuccess(() -> Component.literal("§a✓ 通过：名字验证正常工作"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§c✗ 失败：名字验证有问题"), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 测试 2: 假人创建和删除
        ctx.getSource().sendSuccess(() -> Component.literal("§e[测试 2/6] 假人创建和删除"), false);
        totalTests++;
        if (testBotCreationAndRemoval(ctx, player)) {
            passedTests++;
            ctx.getSource().sendSuccess(() -> Component.literal("§a✓ 通过：假人创建和删除正常"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§c✗ 失败：假人创建或删除有问题"), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 测试 3: 移动功能
        ctx.getSource().sendSuccess(() -> Component.literal("§e[测试 3/6] 移动功能"), false);
        totalTests++;
        if (testMovement(ctx, player)) {
            passedTests++;
            ctx.getSource().sendSuccess(() -> Component.literal("§a✓ 通过：移动功能正常"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§c✗ 失败：移动功能有问题"), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 测试 4: 动作控制
        ctx.getSource().sendSuccess(() -> Component.literal("§e[测试 4/6] 动作控制"), false);
        totalTests++;
        if (testActions(ctx, player)) {
            passedTests++;
            ctx.getSource().sendSuccess(() -> Component.literal("§a✓ 通过：动作控制正常"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§c✗ 失败：动作控制有问题"), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 测试 5: 皮肤系统
        ctx.getSource().sendSuccess(() -> Component.literal("§e[测试 5/6] 皮肤系统"), false);
        totalTests++;
        if (testSkinSystem(ctx, player)) {
            passedTests++;
            ctx.getSource().sendSuccess(() -> Component.literal("§a✓ 通过：皮肤系统正常"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§c✗ 失败：皮肤系统有问题"), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 测试 6: 骑乘功能
        ctx.getSource().sendSuccess(() -> Component.literal("§e[测试 6/6] 骑乘功能"), false);
        totalTests++;
        if (testMounting(ctx, player)) {
            passedTests++;
            ctx.getSource().sendSuccess(() -> Component.literal("§a✓ 通过：骑乘功能正常"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§c✗ 失败：骑乘功能有问题"), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 总结
        ctx.getSource().sendSuccess(() -> Component.literal("=== 测试完成 ==="), false);
        final String resultColor = passedTests == totalTests ? "§a" : (passedTests > 0 ? "§e" : "§c");
        final int finalPassedTests = passedTests;
        final int finalTotalTests = totalTests;
        ctx.getSource().sendSuccess(() -> Component.literal(
            resultColor + "通过: " + finalPassedTests + "/" + finalTotalTests + " 个测试"
        ), false);
        
        return passedTests;
    }
    
    /**
     * 运行特定类型的测试
     */
    private static int runSpecificTest(CommandContext<CommandSourceStack> ctx, String testType) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        
        switch (testType) {
            case "movement":
                ctx.getSource().sendSuccess(() -> Component.literal("§e=== 测试移动功能 ==="), false);
                if (testMovement(ctx, player)) {
                    ctx.getSource().sendSuccess(() -> Component.literal("§a✓ 移动功能测试通过"), true);
                    return 1;
                } else {
                    ctx.getSource().sendFailure(Component.literal("§c✗ 移动功能测试失败"));
                    return 0;
                }
                
            case "actions":
                ctx.getSource().sendSuccess(() -> Component.literal("§e=== 测试动作控制 ==="), false);
                if (testActions(ctx, player)) {
                    ctx.getSource().sendSuccess(() -> Component.literal("§a✓ 动作控制测试通过"), true);
                    return 1;
                } else {
                    ctx.getSource().sendFailure(Component.literal("§c✗ 动作控制测试失败"));
                    return 0;
                }
                
            case "skin":
                ctx.getSource().sendSuccess(() -> Component.literal("§e=== 测试皮肤系统 ==="), false);
                if (testSkinSystem(ctx, player)) {
                    ctx.getSource().sendSuccess(() -> Component.literal("§a✓ 皮肤系统测试通过"), true);
                    return 1;
                } else {
                    ctx.getSource().sendFailure(Component.literal("§c✗ 皮肤系统测试失败"));
                    return 0;
                }
                
            default:
                return runTests(ctx);
        }
    }
    
    /**
     * 测试名字验证
     */
    private static boolean testNameValidation(CommandContext<CommandSourceStack> ctx) {
        // 测试有效名字
        boolean test1 = isValidBotName("TestBot");
        boolean test2 = isValidBotName("Bot_123");
        boolean test3 = isValidBotName("ABC");
        
        // 测试无效名字
        boolean test4 = !isValidBotName("ab");  // 太短
        boolean test5 = !isValidBotName("verylongbotname123");  // 太长
        boolean test6 = !isValidBotName("bot.test");  // 包含非法字符
        boolean test7 = !isValidBotName("bot-test");  // 包含连字符
        
        return test1 && test2 && test3 && test4 && test5 && test6 && test7;
    }
    
    /**
     * 测试假人创建和删除
     */
    private static boolean testBotCreationAndRemoval(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String testBotName = "TestBot_" + System.currentTimeMillis() % 10000;
        
        try {
            // 创建假人
            BotPlayer bot = BotManager.createBot(
                ctx.getSource().getServer(),
                player,
                testBotName,
                null,
                null
            );
            
            if (bot == null) {
                return false;
            }
            
            // 检查假人是否存在
            if (!BotManager.hasBot(testBotName)) {
                return false;
            }
            
            // 删除假人
            boolean removed = BotManager.removeBot(testBotName);
            if (!removed) {
                return false;
            }
            
            // 检查假人是否已删除
            return !BotManager.hasBot(testBotName);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 测试移动功能
     */
    private static boolean testMovement(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String testBotName = "MoveBot_" + System.currentTimeMillis() % 10000;
        
        try {
            // 创建假人
            BotPlayer bot = BotManager.createBot(
                ctx.getSource().getServer(),
                player,
                testBotName,
                null,
                null
            );
            
            if (bot == null) {
                return false;
            }
            
            BotActionController controller = bot.getActionController();
            
            // 测试移动输入
            controller.moveForward();
            boolean test1 = bot.zza == 1.0F;
            
            controller.moveBackward();
            boolean test2 = bot.zza == -1.0F;
            
            controller.moveLeft();
            boolean test3 = bot.xxa == 1.0F;
            
            controller.moveRight();
            boolean test4 = bot.xxa == -1.0F;
            
            controller.stopMovement();
            boolean test5 = bot.zza == 0.0F && bot.xxa == 0.0F;
            
            // 清理
            BotManager.removeBot(testBotName);
            
            return test1 && test2 && test3 && test4 && test5;
            
        } catch (Exception e) {
            BotManager.removeBot(testBotName);
            return false;
        }
    }
    
    /**
     * 测试动作控制
     */
    private static boolean testActions(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String testBotName = "ActionBot_" + System.currentTimeMillis() % 10000;
        
        try {
            // 创建假人
            BotPlayer bot = BotManager.createBot(
                ctx.getSource().getServer(),
                player,
                testBotName,
                null,
                null
            );
            
            if (bot == null) {
                return false;
            }
            
            BotActionController controller = bot.getActionController();
            
            // 测试潜行
            controller.setSneak(true);
            bot.tick();  // 需要 tick 才能应用状态
            boolean test1 = bot.isShiftKeyDown();
            
            controller.setSneak(false);
            bot.tick();
            boolean test2 = !bot.isShiftKeyDown();
            
            // 测试疾跑
            controller.setSprint(true);
            bot.tick();
            boolean test3 = bot.isSprinting();
            
            controller.setSprint(false);
            bot.tick();
            boolean test4 = !bot.isSprinting();
            
            // 测试视角旋转（不需要 tick）
            float originalYaw = bot.getYRot();
            controller.lookRight(45.0F);
            boolean test5 = Math.abs(bot.getYRot() - (originalYaw + 45.0F)) < 0.1F;
            
            // 清理
            BotManager.removeBot(testBotName);
            
            return test1 && test2 && test3 && test4 && test5;
            
        } catch (Exception e) {
            BotManager.removeBot(testBotName);
            return false;
        }
    }
    
    /**
     * 测试皮肤系统
     */
    private static boolean testSkinSystem(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String testBotName1 = "SkinBot1";
        String testBotName2 = "Steve";  // 正版玩家名
        
        try {
            // 测试 1: 创建假人并检查是否有皮肤属性
            BotPlayer bot1 = BotManager.createBot(
                ctx.getSource().getServer(),
                player,
                testBotName1,
                null,
                null
            );
            
            if (bot1 == null) {
                return false;
            }
            
            boolean test1 = bot1.getGameProfile().getProperties().containsKey("textures");
            
            // 测试 2: 尝试获取正版玩家皮肤
            BotPlayer bot2 = BotManager.createBot(
                ctx.getSource().getServer(),
                player,
                testBotName2,
                player.position().add(2, 0, 0),
                null
            );
            
            boolean test2 = bot2 != null && bot2.getGameProfile().getProperties().containsKey("textures");
            
            // 清理
            BotManager.removeBot(testBotName1);
            if (bot2 != null) {
                BotManager.removeBot(testBotName2);
            }
            
            return test1 && test2;
            
        } catch (Exception e) {
            BotManager.removeBot(testBotName1);
            BotManager.removeBot(testBotName2);
            return false;
        }
    }
    
    /**
     * 测试骑乘功能
     */
    private static boolean testMounting(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String testBotName1 = "MountBot1";
        String testBotName2 = "MountBot2";
        
        try {
            // 创建两个假人在远离玩家的位置，确保它们彼此靠近但周围没有其他可骑乘实体
            Vec3 testPos = player.position().add(10, 0, 10);  // 远离玩家
            
            BotPlayer bot1 = BotManager.createBot(
                ctx.getSource().getServer(),
                player,
                testBotName1,
                testPos,
                null
            );
            
            BotPlayer bot2 = BotManager.createBot(
                ctx.getSource().getServer(),
                player,
                testBotName2,
                testPos.add(1, 0, 0),  // 在 bot1 旁边 1 格
                null
            );
            
            if (bot1 == null || bot2 == null) {
                BotManager.removeBot(testBotName1);
                BotManager.removeBot(testBotName2);
                return false;
            }
            
            // 测试：bot1 尝试骑乘（附近只有 bot2，应该失败因为过滤了假人）
            boolean mounted = bot1.getActionController().mount();
            
            // 检查 bot1 是否骑到了 bot2 上
            boolean ridingBot2 = bot1.isPassenger() && bot1.getVehicle() == bot2;
            
            // 测试通过条件：
            // 1. 没有骑乘成功（因为附近只有假人，被过滤了）
            // 2. 或者骑乘成功但不是骑到 bot2 上（说明骑到了其他实体，这也是可以接受的）
            boolean test1 = !mounted || !ridingBot2;
            
            // 清理
            BotManager.removeBot(testBotName1);
            BotManager.removeBot(testBotName2);
            
            return test1;
            
        } catch (Exception e) {
            BotManager.removeBot(testBotName1);
            BotManager.removeBot(testBotName2);
            return false;
        }
    }
}
