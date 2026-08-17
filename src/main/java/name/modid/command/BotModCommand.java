package name.modid.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import name.modid.config.ModConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /botmod 命令实现
 * 提供配置管理功能
 */
public class BotModCommand {

    /**
     * 注册命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("botmod")
            .requires(source -> source.hasPermission(2)) // 需要 OP 权限
            
            // /botmod config - 显示所有配置
            .then(Commands.literal("config")
                .executes(BotModCommand::showConfig)
                
                // /botmod config reload - 重新加载配置
                .then(Commands.literal("reload")
                    .executes(BotModCommand::reloadConfig)
                )
                
                // /botmod config reset - 重置配置
                .then(Commands.literal("reset")
                    .executes(BotModCommand::resetConfig)
                )
                
                // /botmod config set <key> <value> - 设置配置
                .then(Commands.literal("set")
                    // 布尔值配置
                    .then(Commands.literal("enableBotFeature")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "enableBotFeature"))
                        )
                    )
                    .then(Commands.literal("enableKillAura")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "enableKillAura"))
                        )
                    )
                    .then(Commands.literal("allowMountOtherBots")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "allowMountOtherBots"))
                        )
                    )
                    .then(Commands.literal("allowNonOpControlBot")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "allowNonOpControlBot"))
                        )
                    )
                    .then(Commands.literal("autoRespawnOnDeath")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "autoRespawnOnDeath"))
                        )
                    )
                    .then(Commands.literal("botTakeDamage")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "botTakeDamage"))
                        )
                    )
                    .then(Commands.literal("botHunger")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "botHunger"))
                        )
                    )
                    .then(Commands.literal("botPersistence")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "botPersistence"))
                        )
                    )
                    .then(Commands.literal("preserveBotState")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "preserveBotState"))
                        )
                    )
                    .then(Commands.literal("allowBotAutoJump")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "allowBotAutoJump"))
                        )
                    )
                    .then(Commands.literal("batonRequiresOp")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "batonRequiresOp"))
                        )
                    )
                    .then(Commands.literal("botGlowing")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "botGlowing"))
                        )
                    )
                    .then(Commands.literal("botFireImmune")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "botFireImmune"))
                        )
                    )
                    .then(Commands.literal("pathfindingAllowParkour")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "pathfindingAllowParkour"))
                        )
                    )
                    .then(Commands.literal("pathfindingAllowSwim")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "pathfindingAllowSwim"))
                        )
                    )
                    .then(Commands.literal("pathfindingSmooth")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "pathfindingSmooth"))
                        )
                    )
                    
                    // 数值配置（范围与 ModConfig.validate() 对齐）
                    .then(Commands.literal("attackReachDistance")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 64.0))
                            .executes(ctx -> setDoubleConfig(ctx, "attackReachDistance"))
                        )
                    )
                    .then(Commands.literal("creativeAttackReachDistance")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 128.0))
                            .executes(ctx -> setDoubleConfig(ctx, "creativeAttackReachDistance"))
                        )
                    )
                    .then(Commands.literal("killAuraRange")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 64.0))
                            .executes(ctx -> setDoubleConfig(ctx, "killAuraRange"))
                        )
                    )
                    .then(Commands.literal("maxBotCount")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(ctx -> setIntConfig(ctx, "maxBotCount"))
                        )
                    )
                    .then(Commands.literal("maxBotsPerPlayer")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(ctx -> setIntConfig(ctx, "maxBotsPerPlayer"))
                        )
                    )
                    .then(Commands.literal("maxPathfindingDistance")
                        .then(Commands.argument("value", IntegerArgumentType.integer(32, 1024))
                            .executes(ctx -> setIntConfig(ctx, "maxPathfindingDistance"))
                        )
                    )
                )
                
                // /botmod config get <key> - 获取配置
                .then(Commands.literal("get")
                    .then(Commands.argument("key", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("enableBotFeature");
                            builder.suggest("attackReachDistance");
                            builder.suggest("creativeAttackReachDistance");
                            builder.suggest("enableKillAura");
                            builder.suggest("killAuraRange");
                            builder.suggest("allowMountOtherBots");
                            builder.suggest("maxBotCount");
                            builder.suggest("allowNonOpControlBot");
                            builder.suggest("autoRespawnOnDeath");
                            builder.suggest("botTakeDamage");
                            builder.suggest("botHunger");
                            builder.suggest("botPersistence");
                            builder.suggest("preserveBotState");
                            builder.suggest("allowBotAutoJump");
                            builder.suggest("batonRequiresOp");
                            builder.suggest("botGlowing");
                            builder.suggest("botFireImmune");
                            builder.suggest("maxBotsPerPlayer");
                            builder.suggest("maxPathfindingDistance");
                            builder.suggest("pathfindingAllowParkour");
                            builder.suggest("pathfindingAllowSwim");
                            builder.suggest("pathfindingSmooth");
                            return builder.buildFuture();
                        })
                        .executes(BotModCommand::getConfig)
                    )
                )
            )
            
            // /botmod whitelist - 管理骑乘白名单
            .then(Commands.literal("whitelist")
                .then(Commands.literal("list")
                    .executes(BotModCommand::listWhitelist)
                )
                .then(Commands.literal("add")
                    .then(Commands.argument("entityType", StringArgumentType.greedyString())
                        .executes(BotModCommand::addToWhitelist)
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("entityType", StringArgumentType.greedyString())
                        .executes(BotModCommand::removeFromWhitelist)
                    )
                )
                .then(Commands.literal("clear")
                    .executes(BotModCommand::clearWhitelist)
                )
            )
            
            // /botmod info - 显示模组信息
            .then(Commands.literal("info")
                .executes(BotModCommand::showInfo)
            )
        );
    }

    /**
     * 显示所有配置
     */
    private static int showConfig(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();
        
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.title"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 总开关
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.section.master"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  enableBotFeature: " + formatBool(config.enableBotFeature)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 攻击设置
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.section.attack"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  attackReachDistance: " + config.attackReachDistance), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  creativeAttackReachDistance: " + config.creativeAttackReachDistance), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  enableKillAura: " + formatBool(config.enableKillAura)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  killAuraRange: " + config.killAuraRange), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 骑乘设置
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.section.mount"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  allowMountOtherBots: " + formatBool(config.allowMountOtherBots)), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.mount_whitelist_count", config.mountWhitelist.size()), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 生存设置
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.section.survival"), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.max_bot_count", config.maxBotCount), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  maxBotsPerPlayer: " + config.maxBotsPerPlayer + " (0=无限)"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  allowNonOpControlBot: " + formatBool(config.allowNonOpControlBot)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  autoRespawnOnDeath: " + formatBool(config.autoRespawnOnDeath)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  botTakeDamage: " + formatBool(config.botTakeDamage)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  botHunger: " + formatBool(config.botHunger)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 动作设置
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.section.action"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  allowBotAutoJump: " + formatBool(config.allowBotAutoJump)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 指挥棒设置
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.section.baton"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  batonRequiresOp: " + formatBool(config.batonRequiresOp)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 外观与防护
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.section.look"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  botGlowing: " + formatBool(config.botGlowing)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  botFireImmune: " + formatBool(config.botFireImmune)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 寻路设置
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.section.pathfinding"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  maxPathfindingDistance: " + config.maxPathfindingDistance), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  pathfindingAllowParkour: " + formatBool(config.pathfindingAllowParkour)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  pathfindingAllowSwim: " + formatBool(config.pathfindingAllowSwim)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  pathfindingSmooth: " + formatBool(config.pathfindingSmooth)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 驻留设置
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.section.persistence"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  botPersistence: " + formatBool(config.botPersistence)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  preserveBotState: " + formatBool(config.preserveBotState)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        return 1;
    }

    /**
     * 重新加载配置
     */
    private static int reloadConfig(CommandContext<CommandSourceStack> ctx) {
        // 重新从磁盘加载配置
        ModConfig.reload();
        // 重读皮肤文件夹，使新增皮肤生效（运行态回传）
        name.modid.bot.BotSkinManager.reloadDefaultSkins();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.reloaded"), true);
        return 1;
    }

    /**
     * 重置配置
     */
    /** 待确认的配置重置：key = 操作者名，value = 过期时间戳(ms) */
    private static final java.util.Map<String, Long> pendingReset = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long RESET_CONFIRM_WINDOW_MS = 15000L;

    private static int resetConfig(CommandContext<CommandSourceStack> ctx) {
        // 破坏性操作二次确认：首次仅提示，窗口期内再次执行才真正重置
        String key = ctx.getSource().getTextName();
        long now = System.currentTimeMillis();
        Long expiry = pendingReset.get(key);
        if (expiry == null || now > expiry) {
            // 顺便清理已过期的条目，避免内存泄漏
            pendingReset.entrySet().removeIf(e -> System.currentTimeMillis() > e.getValue());
            pendingReset.put(key, now + RESET_CONFIRM_WINDOW_MS);
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.reset_confirm"), false);
            return 0;
        }
        pendingReset.remove(key);
        ModConfig config = ModConfig.getInstance();
        config.reset();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.reset"), true);
        return 1;
    }

    /**
     * 设置布尔值配置
     */
    private static int setBoolConfig(CommandContext<CommandSourceStack> ctx, String key) {
        ModConfig config = ModConfig.getInstance();
        boolean value = BoolArgumentType.getBool(ctx, "value");
        
        switch (key) {
            case "enableBotFeature":
                config.enableBotFeature = value;
                break;
            case "enableKillAura":
                config.enableKillAura = value;
                break;
            case "allowMountOtherBots":
                config.allowMountOtherBots = value;
                break;
            case "allowNonOpControlBot":
                config.allowNonOpControlBot = value;
                break;
            case "autoRespawnOnDeath":
                config.autoRespawnOnDeath = value;
                break;
            case "botTakeDamage":
                config.botTakeDamage = value;
                break;
            case "botHunger":
                config.botHunger = value;
                break;
            case "botPersistence":
                config.botPersistence = value;
                break;
            case "preserveBotState":
                config.preserveBotState = value;
                break;
            case "allowBotAutoJump":
                config.allowBotAutoJump = value;
                break;
            case "batonRequiresOp":
                config.batonRequiresOp = value;
                break;
            case "botGlowing":
                config.botGlowing = value;
                break;
            case "botFireImmune":
                config.botFireImmune = value;
                break;
            case "pathfindingAllowParkour":
                config.pathfindingAllowParkour = value;
                break;
            case "pathfindingAllowSwim":
                config.pathfindingAllowSwim = value;
                break;
            case "pathfindingSmooth":
                config.pathfindingSmooth = value;
                break;
        }
        
        config.save();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.set_ok", key, formatBool(value)), true);
        return 1;
    }

    /**
     * 设置浮点数配置
     */
    private static int setDoubleConfig(CommandContext<CommandSourceStack> ctx, String key) {
        ModConfig config = ModConfig.getInstance();
        double value = DoubleArgumentType.getDouble(ctx, "value");
        
        switch (key) {
            case "attackReachDistance":
                config.attackReachDistance = value;
                break;
            case "creativeAttackReachDistance":
                config.creativeAttackReachDistance = value;
                break;
            case "killAuraRange":
                config.killAuraRange = value;
                break;
        }
        
        config.save();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.set_ok", key, value), true);
        return 1;
    }

    /**
     * 设置整数配置
     */
    private static int setIntConfig(CommandContext<CommandSourceStack> ctx, String key) {
        ModConfig config = ModConfig.getInstance();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        
        if (key.equals("maxBotCount")) {
            config.maxBotCount = value;
        } else if (key.equals("maxBotsPerPlayer")) {
            config.maxBotsPerPlayer = value;
        } else if (key.equals("maxPathfindingDistance")) {
            config.maxPathfindingDistance = value;
        }
        
        config.save();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.config.set_ok", key, value), true);
        return 1;
    }

    /**
     * 获取配置值
     */
    private static int getConfig(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();
        String key = StringArgumentType.getString(ctx, "key");
        
        String value = switch (key) {
            case "enableBotFeature" -> formatBool(config.enableBotFeature);
            case "attackReachDistance" -> String.valueOf(config.attackReachDistance);
            case "creativeAttackReachDistance" -> String.valueOf(config.creativeAttackReachDistance);
            case "enableKillAura" -> formatBool(config.enableKillAura);
            case "killAuraRange" -> String.valueOf(config.killAuraRange);
            case "allowMountOtherBots" -> formatBool(config.allowMountOtherBots);
            case "maxBotCount" -> String.valueOf(config.maxBotCount);
            case "allowNonOpControlBot" -> formatBool(config.allowNonOpControlBot);
            case "autoRespawnOnDeath" -> formatBool(config.autoRespawnOnDeath);
            case "botTakeDamage" -> formatBool(config.botTakeDamage);
            case "botHunger" -> formatBool(config.botHunger);
            case "botPersistence" -> formatBool(config.botPersistence);
            case "preserveBotState" -> formatBool(config.preserveBotState);
            case "allowBotAutoJump" -> formatBool(config.allowBotAutoJump);
            case "batonRequiresOp" -> formatBool(config.batonRequiresOp);
            case "botGlowing" -> formatBool(config.botGlowing);
            case "botFireImmune" -> formatBool(config.botFireImmune);
            case "maxBotsPerPlayer" -> String.valueOf(config.maxBotsPerPlayer);
            case "maxPathfindingDistance" -> String.valueOf(config.maxPathfindingDistance);
            case "pathfindingAllowParkour" -> formatBool(config.pathfindingAllowParkour);
            case "pathfindingAllowSwim" -> formatBool(config.pathfindingAllowSwim);
            case "pathfindingSmooth" -> formatBool(config.pathfindingSmooth);
            default -> Component.translatable("msg.my-bot-mod.config.unknown").getString();
        };
        
        ctx.getSource().sendSuccess(() -> Component.literal(key + " = " + value), false);
        return 1;
    }

    /**
     * 列出骑乘白名单
     */
    private static int listWhitelist(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();
        
        if (config.mountWhitelist.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.mount.whitelist_empty"), false);
            return 0;
        }
        
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.mount.whitelist_header", config.mountWhitelist.size()), false);
        for (String entityType : config.mountWhitelist) {
            ctx.getSource().sendSuccess(() -> Component.literal("  - " + entityType), false);
        }
        
        return config.mountWhitelist.size();
    }

    /**
     * 添加到骑乘白名单
     */
    private static int addToWhitelist(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();
        String entityType = StringArgumentType.getString(ctx, "entityType");
        
        if (config.mountWhitelist.contains(entityType)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.mount.already_in", entityType));
            return 0;
        }
        
        config.mountWhitelist.add(entityType);
        config.save();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.mount.added", entityType), true);
        return 1;
    }

    /**
     * 从骑乘白名单移除
     */
    private static int removeFromWhitelist(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();
        String entityType = StringArgumentType.getString(ctx, "entityType");
        
        if (!config.mountWhitelist.contains(entityType)) {
            ctx.getSource().sendFailure(Component.translatable("msg.my-bot-mod.mount.not_in", entityType));
            return 0;
        }
        
        config.mountWhitelist.remove(entityType);
        config.save();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.mount.removed", entityType), true);
        return 1;
    }

    /**
     * 清空骑乘白名单
     */
    private static int clearWhitelist(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();
        int count = config.mountWhitelist.size();
        config.mountWhitelist.clear();
        config.save();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.mount.cleared", count), true);
        return 1;
    }

    /**
     * 显示模组信息
     */
    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();
        
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.info.title"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.info.version", config.modVersion), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.info.author", config.author), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.info.email", config.email), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.info.license", config.license), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.info.github", config.githubRepo), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.my-bot-mod.info.description", config.description), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        return 1;
    }

    /**
     * 格式化布尔值
     */
    private static String formatBool(boolean value) {
        return value ? "§atrue" : "§cfalse";
    }
}
