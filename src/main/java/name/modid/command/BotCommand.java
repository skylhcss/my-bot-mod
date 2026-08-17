package name.modid.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import name.modid.behavior.BehaviorManager;
import name.modid.bot.BotActionController;
import name.modid.bot.BotManager;
import name.modid.bot.BotPersistenceManager;
import name.modid.bot.BotPlayer;
import name.modid.menu.BotInventoryMenu;
import name.modid.net.BotNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
//? if <1.20.5 {
import net.minecraft.network.FriendlyByteBuf;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
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
            .requires(source -> {
                // 实时读取配置，避免 /botmod config reload|reset 替换单例后使用过期实例
                // 如果允许非 OP 创建假人，则所有玩家都可以使用
                if (name.modid.config.ModConfig.getInstance().allowNonOpControlBot) {
                    return true;
                }
                // 否则需要 OP 权限（等级 2）
                return source.hasPermission(2);
            })
            .then(Commands.argument("botName", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    for (BotPlayer b : BotManager.getAllBots()) {
                        builder.suggest(b.getName().getString());
                    }
                    return builder.buildFuture();
                })
                // /bot <name> spawn
                .then(Commands.literal("spawn")
                    .executes(BotCommand::spawnBot)
                    .then(Commands.literal("at")
                        .then(Commands.argument("position", Vec3Argument.vec3())
                            .executes(BotCommand::spawnBotAt)
                        )
                    )
                )
                // /bot <name> kill（confirm 子命令供 GUI 二次确认后直接执行，跳过命令行侧的确认窗口）
                .then(Commands.literal("kill")
                    .executes(BotCommand::killBot)
                    .then(Commands.literal("confirm")
                        .executes(ctx -> killBotConfirmed(ctx))
                    )
                )
                // /bot <name> attack
                .then(Commands.literal("attack")
                    .then(Commands.literal("once")
                        .executes(ctx -> controlBotAction(ctx, "attack", "once", 0))
                    )
                    .then(Commands.literal("continuous")
                        .executes(ctx -> controlBotAction(ctx, "attack", "continuous", 0))
                    )
                    .then(Commands.literal("interval")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                            .executes(ctx -> controlBotAction(ctx, "attack", "interval", 
                                IntegerArgumentType.getInteger(ctx, "ticks")))
                        )
                    )
                )
                // /bot <name> use
                .then(Commands.literal("use")
                    .then(Commands.literal("once")
                        .executes(ctx -> controlBotAction(ctx, "use", "once", 0))
                    )
                    .then(Commands.literal("continuous")
                        .executes(ctx -> controlBotAction(ctx, "use", "continuous", 0))
                    )
                    .then(Commands.literal("interval")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                            .executes(ctx -> controlBotAction(ctx, "use", "interval", 
                                IntegerArgumentType.getInteger(ctx, "ticks")))
                        )
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
                // /bot <name> behavior - 行为脚本管理
                .then(Commands.literal("behavior")
                    .then(Commands.literal("list")
                        .executes(BotCommand::behaviorList)
                    )
                    .then(Commands.literal("assign")
                        .then(Commands.argument("behaviorFile", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                for (String name : BehaviorManager.getBehaviorNames()) {
                                    builder.suggest(StringArgumentType.escapeIfRequired(name));
                                }
                                return builder.buildFuture();
                            })
                            .executes(BotCommand::behaviorAssign)
                        )
                    )
                    .then(Commands.literal("unassign")
                        .then(Commands.argument("behaviorFile", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                BotPlayer b = BotManager.getBot(StringArgumentType.getString(ctx, "botName"));
                                if (b != null) {
                                    for (String name : BehaviorManager.getAssigned(b)) {
                                        builder.suggest(StringArgumentType.escapeIfRequired(name));
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(BotCommand::behaviorUnassign)
                        )
                    )
                    .then(Commands.literal("start")
                        .executes(BotCommand::behaviorStart)
                    )
                    .then(Commands.literal("stop")
                        .executes(BotCommand::behaviorStop)
                    )
                    .then(Commands.literal("reload")
                        .executes(BotCommand::behaviorReload)
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
                // /bot <name> look up/down/left/right/north/south/east/west
                .then(Commands.literal("look")
                    .then(Commands.literal("up")
                        .executes(ctx -> lookDirection(ctx, "up"))
                    )
                    .then(Commands.literal("down")
                        .executes(ctx -> lookDirection(ctx, "down"))
                    )
                    .then(Commands.literal("left")
                        .executes(ctx -> lookDirection(ctx, "left"))
                    )
                    .then(Commands.literal("right")
                        .executes(ctx -> lookDirection(ctx, "right"))
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
                // /bot <name> goto <x> <y> <z> - 自动寻路
                .then(Commands.literal("goto")
                    .then(Commands.argument("position", BlockPosArgument.blockPos())
                        .executes(BotCommand::gotoPosition)
                    )
                    .then(Commands.literal("stop")
                        .executes(BotCommand::cancelGoto)
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
                // /bot <name> inventory - 打开假人背包
                .then(Commands.literal("inventory")
                    .executes(BotCommand::openInventory)
                )
                // /bot <name> enderchest - 打开假人末影箱
                .then(Commands.literal("enderchest")
                    .executes(BotCommand::openEnderChest)
                )
                // /bot <name> panel - 打开假人设置面板
                .then(Commands.literal("panel")
                    .executes(BotCommand::openPanel)
                )
                // /bot <name> slot <0-8> - 设置手持槽位
                .then(Commands.literal("slot")
                    .then(Commands.argument("index", IntegerArgumentType.integer(0, 8))
                        .executes(BotCommand::setHeldSlot)
                    )
                )
                // /bot <name> gamemode <mode> - 设置游戏模式
                .then(Commands.literal("gamemode")
                    .then(Commands.literal("survival")
                        .executes(ctx -> setBotGameMode(ctx, GameType.SURVIVAL)))
                    .then(Commands.literal("creative")
                        .executes(ctx -> setBotGameMode(ctx, GameType.CREATIVE)))
                    .then(Commands.literal("adventure")
                        .executes(ctx -> setBotGameMode(ctx, GameType.ADVENTURE)))
                    .then(Commands.literal("spectator")
                        .executes(ctx -> setBotGameMode(ctx, GameType.SPECTATOR)))
                )
                // /bot <name> tphere - 将假人传送到执行者身边
                .then(Commands.literal("tphere")
                    .executes(BotCommand::teleportHere)
                )
            )
            // /bot list
            .then(Commands.literal("list")
                .executes(BotCommand::listBots)
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
        if (!BotManager.isValidBotName(botName)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.spawn.invalid_name"));
            return 0;
        }

        if (BotManager.hasBot(botName)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.spawn.already_exists", botName));
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
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.spawn.success", botName), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.spawn.failed"));
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
        if (!BotManager.isValidBotName(botName)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.spawn.invalid_name"));
            return 0;
        }

        if (BotManager.hasBot(botName)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.spawn.already_exists", botName));
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
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.spawn.success_at",
                String.format("%.1f, %.1f, %.1f", position.x, position.y, position.z), botName), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.spawn.failed"));
            return 0;
        }
    }

    /**
     * 移除假人
     */
    /** 待确认的移除操作：key = 操作者名 + ":" + 假人名(小写)，value = 过期时间戳(ms) */
    private static final java.util.Map<String, Long> pendingKill = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long KILL_CONFIRM_WINDOW_MS = 15000L;

    private static int killBot(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");

        if (!BotManager.hasBot(botName)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        // 破坏性操作二次确认：首次仅提示，窗口期内再次执行才真正移除
        String key = ctx.getSource().getTextName() + ":" + botName.toLowerCase();
        long now = System.currentTimeMillis();
        Long expiry = pendingKill.get(key);
        if (expiry == null || now > expiry) {
            // 顺便清理已过期的条目，避免内存泄漏
            pendingKill.entrySet().removeIf(e -> System.currentTimeMillis() > e.getValue());
            pendingKill.put(key, now + KILL_CONFIRM_WINDOW_MS);
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.kill.confirm", botName, botName), false);
            return 0;
        }
        pendingKill.remove(key);
        return doKill(ctx, botName);
    }

    /** GUI 已在界面内二次确认，直接执行移除 */
    private static int killBotConfirmed(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        if (!BotManager.hasBot(botName)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }
        pendingKill.remove(ctx.getSource().getTextName() + ":" + botName.toLowerCase());
        return doKill(ctx, botName);
    }

    private static int doKill(CommandContext<CommandSourceStack> ctx, String botName) {
        if (BotManager.removeBot(botName)) {
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.kill.success", botName), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }
    }

    /**
     * 控制假人动作（攻击、使用）
     * 参考 Carpet Mod: /player <name> attack <once|continuous|interval <ticks>>
     */
    private static int controlBotAction(CommandContext<CommandSourceStack> ctx, String action, String mode, int interval) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        BotActionController controller = bot.getActionController();
        Component message = Component.empty();
        
        switch (action) {
            case "attack":
                switch (mode) {
                    case "once":
                        controller.startAttackOnce();
                        message = Component.translatable("msg.my-bot-mod.attack.once", botName);
                        break;
                    case "continuous":
                        controller.startAttackContinuous();
                        message = Component.translatable("msg.my-bot-mod.attack.continuous", botName);
                        break;
                    case "interval":
                        controller.startAttackInterval(interval);
                        message = Component.translatable("msg.my-bot-mod.attack.interval", botName, interval);
                        break;
                }
                break;
            case "use":
                switch (mode) {
                    case "once":
                        controller.startUseOnce();
                        message = Component.translatable("msg.my-bot-mod.use.once", botName);
                        break;
                    case "continuous":
                        controller.startUseContinuous();
                        message = Component.translatable("msg.my-bot-mod.use.continuous", botName);
                        break;
                    case "interval":
                        controller.startUseInterval(interval);
                        message = Component.translatable("msg.my-bot-mod.use.interval", botName, interval);
                        break;
                }
                break;
        }

        Component finalMessage = message;
        ctx.getSource().sendSuccess(() -> finalMessage, true);
        return 1;
    }

    /**
     * 停止假人动作
     */
    private static int stopBotAction(CommandContext<CommandSourceStack> ctx, String action) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        BotActionController controller = bot.getActionController();
        
        switch (action) {
            case "attack":
                controller.stopAttack();
                ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.attack.stop", botName), true);
                break;
            case "use":
                controller.stopUse();
                ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.use.stop", botName), true);
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
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        BotActionController controller = bot.getActionController();
        Component message = Component.empty();
        
        switch (state) {
            case "sneak":
                controller.setSneak(enable);
                message = Component.translatable(enable ? "msg.my-bot-mod.action.sneak_on" : "msg.my-bot-mod.action.sneak_off", botName);
                break;
            case "jump":
                controller.setJump(enable);
                message = Component.translatable(enable ? "msg.my-bot-mod.action.jump_on" : "msg.my-bot-mod.action.jump_off", botName);
                break;
            case "sprint":
                controller.setSprint(enable);
                message = Component.translatable(enable ? "msg.my-bot-mod.action.sprint_on" : "msg.my-bot-mod.action.sprint_off", botName);
                break;
        }

        Component finalMessage = message;
        ctx.getSource().sendSuccess(() -> finalMessage, true);
        return 1;
    }

    /**
     * 控制假人看向方向
     * 参考 Carpet Mod：up/down 看向正上/下方，left/right 转 90 度
     */
    private static int lookDirection(CommandContext<CommandSourceStack> ctx, String direction) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        BotActionController controller = bot.getActionController();
        Component message = Component.empty();
        
        switch (direction) {
            case "up":
                controller.lookUp();
                message = Component.translatable("msg.my-bot-mod.look.up", botName);
                break;
            case "down":
                controller.lookDown();
                message = Component.translatable("msg.my-bot-mod.look.down", botName);
                break;
            case "left":
                controller.lookLeft();
                message = Component.translatable("msg.my-bot-mod.look.left", botName);
                break;
            case "right":
                controller.lookRight();
                message = Component.translatable("msg.my-bot-mod.look.right", botName);
                break;
        }

        Component finalMessage = message;
        ctx.getSource().sendSuccess(() -> finalMessage, true);
        return 1;
    }

    /**
     * 控制假人看向基本方向
     */
    private static int lookCardinal(CommandContext<CommandSourceStack> ctx, float yaw) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        bot.getActionController().lookAt(yaw, bot.getXRot());
        
        String key = yaw == 180.0F ? "msg.my-bot-mod.look.north" : yaw == 0.0F ? "msg.my-bot-mod.look.south" : yaw == -90.0F ? "msg.my-bot-mod.look.east" : "msg.my-bot-mod.look.west";
        ctx.getSource().sendSuccess(() -> Component.translatable(key, botName), true);
        return 1;
    }

    /**
     * 控制假人移动
     */
    private static int moveBot(CommandContext<CommandSourceStack> ctx, String direction) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
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

        String key = switch (direction) {
            case "forward" -> "msg.my-bot-mod.move.forward";
            case "backward" -> "msg.my-bot-mod.move.backward";
            case "left" -> "msg.my-bot-mod.move.left";
            case "right" -> "msg.my-bot-mod.move.right";
            default -> "msg.my-bot-mod.move.stop";
        };
        ctx.getSource().sendSuccess(() -> Component.translatable(key, botName), true);
        return 1;
    }

    /**
     * 假人寻路到指定位置
     */
    private static int gotoPosition(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        BlockPos target = BlockPosArgument.getLoadedBlockPos(ctx, "position");

        // 远距离警告（不再硬限制距离，但提示用户可能较慢）
        double distance = bot.position().distanceTo(Vec3.atCenterOf(target));
        if (distance > 500) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                "msg.my-bot-mod.goto.far_warning", String.format("%.0f", distance)), false);
        }

        boolean success = bot.getActionController().pathTo(target);
        if (success) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                "msg.my-bot-mod.goto.start", botName, target.getX(), target.getY(), target.getZ()), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.translatable(
                "msg.my-bot-mod.goto.no_path", target.getX(), target.getY(), target.getZ()));
            return 0;
        }
    }

    /**
     * 取消假人寻路
     */
    private static int cancelGoto(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        bot.getActionController().cancelPath();
        ctx.getSource().sendSuccess(() -> Component.translatable(
            "msg.my-bot-mod.goto.cancel", botName), true);
        return 1;
    }

    /**
     * 列出所有假人
     */
    private static int listBots(CommandContext<CommandSourceStack> ctx) {
        var bots = BotManager.getAllBots();
        
        if (bots.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.list.empty"), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.list.header", bots.size()), false);
        for (BotPlayer bot : bots) {
            String botName = bot.getName().getString();
            String creatorName = bot.getCreatorName();
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.list.entry", botName, creatorName), false);
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
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        // 手动停止同时暂停行为脚本（避免脚本与手动指令抢控制权）
        BehaviorManager.stop(bot);
        bot.getActionController().stopAll();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.stop.all", botName), true);
        return 1;
    }

    // ==================== 行为脚本命令 ====================

    /** 解析 botName 参数并取假人，不存在时发送失败消息 */
    private static BotPlayer resolveBot(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
        }
        return bot;
    }

    /** /bot <name> behavior list — 列出可用行为与该假人的播放列表 */
    private static int behaviorList(CommandContext<CommandSourceStack> ctx) {
        BotPlayer bot = resolveBot(ctx);
        if (bot == null) {
            return 0;
        }
        var available = BehaviorManager.getBehaviorNames();
        var assigned = BehaviorManager.getAssigned(bot);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.behavior.list.available",
            available.size(), available.isEmpty() ? "-" : String.join(", ", available)), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.behavior.list.assigned",
            bot.getName().getString(), assigned.isEmpty() ? "-" : String.join(" -> ", assigned)), false);
        String running = BehaviorManager.currentBehaviorName(bot);
        if (running != null) {
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.behavior.list.running",
                running), false);
        }
        var errors = BehaviorManager.getErrors();
        if (!errors.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.behavior.list.errors",
                errors.size(), String.join(", ", errors.keySet())), false);
        }
        return 1;
    }

    /** /bot <name> behavior assign <file> */
    private static int behaviorAssign(CommandContext<CommandSourceStack> ctx) {
        BotPlayer bot = resolveBot(ctx);
        if (bot == null) {
            return 0;
        }
        String file = StringArgumentType.getString(ctx, "behaviorFile");
        if (!BehaviorManager.assign(bot, file)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.behavior.assign.fail", file));
            return 0;
        }
        BotPersistenceManager.saveBot(bot);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.behavior.assign.ok",
            file, bot.getName().getString()), true);
        return 1;
    }

    /** /bot <name> behavior unassign <file> */
    private static int behaviorUnassign(CommandContext<CommandSourceStack> ctx) {
        BotPlayer bot = resolveBot(ctx);
        if (bot == null) {
            return 0;
        }
        String file = StringArgumentType.getString(ctx, "behaviorFile");
        if (!BehaviorManager.unassign(bot, file)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.behavior.unassign.fail", file));
            return 0;
        }
        BotPersistenceManager.saveBot(bot);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.behavior.unassign.ok",
            file, bot.getName().getString()), true);
        return 1;
    }

    /** /bot <name> behavior start */
    private static int behaviorStart(CommandContext<CommandSourceStack> ctx) {
        BotPlayer bot = resolveBot(ctx);
        if (bot == null) {
            return 0;
        }
        if (!BehaviorManager.start(bot)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.behavior.start.fail",
                bot.getName().getString()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.behavior.start.ok",
            bot.getName().getString()), true);
        return 1;
    }

    /** /bot <name> behavior stop */
    private static int behaviorStop(CommandContext<CommandSourceStack> ctx) {
        BotPlayer bot = resolveBot(ctx);
        if (bot == null) {
            return 0;
        }
        BehaviorManager.stop(bot);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.behavior.stop.ok",
            bot.getName().getString()), true);
        return 1;
    }

    /** /bot <name> behavior reload — 重新扫描行为文件夹 */
    private static int behaviorReload(CommandContext<CommandSourceStack> ctx) {
        BehaviorManager.reload();
        int count = BehaviorManager.getBehaviorNames().size();
        int errors = BehaviorManager.getErrors().size();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.behavior.reload.ok",
            count, errors), true);
        return 1;
    }

    /**
     * 假人丢弃物品
     */
    private static int dropItem(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        bot.getActionController().dropItem();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.drop.item", botName), true);
        return 1;
    }

    /**
     * 假人丢弃整组物品
     */
    private static int dropStack(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        bot.getActionController().dropStack();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.drop.stack", botName), true);
        return 1;
    }

    /**
     * 假人交换主副手物品
     */
    private static int swapHands(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        bot.getActionController().swapHands();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.swap.hands", botName), true);
        return 1;
    }

    /**
     * 假人骑乘附近实体
     */
    private static int mountEntity(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);

        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        if (bot.getActionController().mount()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.mount.success", botName), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.mount.no_target", botName));
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
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        if (bot.getActionController().dismount()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.dismount.success", botName), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.dismount.not_riding", botName));
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
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }

        float yaw = FloatArgumentType.getFloat(ctx, "yaw");
        float pitch = FloatArgumentType.getFloat(ctx, "pitch");

        bot.getActionController().turn(yaw, pitch);
        ctx.getSource().sendSuccess(() -> Component.translatable(
            "msg.my-bot-mod.turn.success", botName, yaw, pitch), true);
        return 1;
    }

    /**
     * 打开假人背包（原版风格容器界面，可编辑，含盔甲/副手/手持槽位）
     */
    private static int openInventory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }
        //? if >=1.20.5 {
        /*player.openMenu(new ExtendedScreenHandlerFactory<name.modid.menu.BotInventoryMenu.BotInventoryData>() {
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new BotInventoryMenu(id, inv, bot.getInventory(), bot);
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.my-bot-mod.inventory.title", botName);
            }

            @Override
            public name.modid.menu.BotInventoryMenu.BotInventoryData getScreenOpeningData(ServerPlayer p) {
                return new name.modid.menu.BotInventoryMenu.BotInventoryData(bot.getUUID(), bot.getInventory().selected);
            }
        });
        *///?} else {
        player.openMenu(new ExtendedScreenHandlerFactory() {
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new BotInventoryMenu(id, inv, bot.getInventory(), bot);
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.my-bot-mod.inventory.title", botName);
            }

            @Override
            public void writeScreenOpeningData(ServerPlayer p, FriendlyByteBuf buf) {
                buf.writeUUID(bot.getUUID());
                buf.writeVarInt(bot.getInventory().selected);
            }
        });
        //?}
        return 1;
    }

    /**
     * 打开假人末影箱（命令入口）
     */
    private static int openEnderChest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }
        openBotEnderChest(player, bot);
        return 1;
    }

    /** 打开假人末影箱（原版三行箱子界面，直接绑定假人末影箱实时编辑） */
    private static void openBotEnderChest(ServerPlayer viewer, BotPlayer bot) {
        var enderChest = bot.getEnderChestInventory();
        viewer.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3,
                    id, inv, enderChest, 3) {
                @Override
                public void removed(Player pl) {
                    super.removed(pl);
                    // 关闭末影箱时保存假人数据（saveBot 内部按 botPersistence 判断）
                    name.modid.bot.BotPersistenceManager.saveBot(bot);
                }
            },
            Component.translatable("gui.my-bot-mod.enderchest.title", bot.getName().getString())
        ));
    }

    /**
     * 打开假人设置面板（通过网络包触发客户端界面）
     */
    private static int openPanel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }
        BotNetworking.sendOpenPanel(player, bot);
        return 1;
    }

    /**
     * 设置假人手持槽位（0-8）
     */
    private static int setHeldSlot(CommandContext<CommandSourceStack> ctx) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }
        int index = IntegerArgumentType.getInteger(ctx, "index");
        bot.getInventory().selected = index;
        // 切换手持槽位不弹出聊天提示
        return 1;
    }

    /**
     * 设置假人游戏模式
     */
    private static int setBotGameMode(CommandContext<CommandSourceStack> ctx, GameType mode) {
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }
        bot.setGameMode(mode);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.gamemode.set", botName, Component.translatable("gameMode." + mode.getName())), true);
        return 1;
    }

    /**
     * 将假人传送到执行者身边
     */
    private static int teleportHere(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String botName = StringArgumentType.getString(ctx, "botName");
        BotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.bot.not_exist", botName));
            return 0;
        }
        BotManager.teleportCrossLevel(bot, player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.tphere.success", botName), true);
        return 1;
    }
}
