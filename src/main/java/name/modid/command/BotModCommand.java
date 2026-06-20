package name.modid.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import name.modid.MyBotMod;
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
                    .then(Commands.literal("allowNonOpCreateBot")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "allowNonOpCreateBot"))
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
                    .then(Commands.literal("carpetModCompatibility")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "carpetModCompatibility"))
                        )
                    )
                    .then(Commands.literal("allowBotAutoJump")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setBoolConfig(ctx, "allowBotAutoJump"))
                        )
                    )
                    
                    // 数值配置
                    .then(Commands.literal("attackReachDistance")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 10.0))
                            .executes(ctx -> setDoubleConfig(ctx, "attackReachDistance"))
                        )
                    )
                    .then(Commands.literal("creativeAttackReachDistance")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 10.0))
                            .executes(ctx -> setDoubleConfig(ctx, "creativeAttackReachDistance"))
                        )
                    )
                    .then(Commands.literal("killAuraRange")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 10.0))
                            .executes(ctx -> setDoubleConfig(ctx, "killAuraRange"))
                        )
                    )
                    .then(Commands.literal("maxBotCount")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(ctx -> setIntConfig(ctx, "maxBotCount"))
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
                            builder.suggest("allowNonOpCreateBot");
                            builder.suggest("autoRespawnOnDeath");
                            builder.suggest("botTakeDamage");
                            builder.suggest("botHunger");
                            builder.suggest("botPersistence");
                            builder.suggest("preserveBotState");
                            builder.suggest("carpetModCompatibility");
                            builder.suggest("allowBotAutoJump");
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
        
        ctx.getSource().sendSuccess(() -> Component.literal("§e§l=== 我的机器人 - 配置 ==="), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 总开关
        ctx.getSource().sendSuccess(() -> Component.literal("§6总开关:"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  enableBotFeature: " + formatBool(config.enableBotFeature)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 攻击设置
        ctx.getSource().sendSuccess(() -> Component.literal("§6攻击设置:"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  attackReachDistance: " + config.attackReachDistance), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  creativeAttackReachDistance: " + config.creativeAttackReachDistance), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  enableKillAura: " + formatBool(config.enableKillAura)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  killAuraRange: " + config.killAuraRange), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 骑乘设置
        ctx.getSource().sendSuccess(() -> Component.literal("§6骑乘设置:"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  allowMountOtherBots: " + formatBool(config.allowMountOtherBots)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  mountWhitelist: " + config.mountWhitelist.size() + " 个实体"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 生存设置
        ctx.getSource().sendSuccess(() -> Component.literal("§6生存设置:"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  maxBotCount: " + config.maxBotCount + " (0=无限)"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  allowNonOpCreateBot: " + formatBool(config.allowNonOpCreateBot)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  autoRespawnOnDeath: " + formatBool(config.autoRespawnOnDeath)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  botTakeDamage: " + formatBool(config.botTakeDamage)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  botHunger: " + formatBool(config.botHunger)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 动作设置
        ctx.getSource().sendSuccess(() -> Component.literal("§6动作设置:"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  allowBotAutoJump: " + formatBool(config.allowBotAutoJump)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 驻留设置
        ctx.getSource().sendSuccess(() -> Component.literal("§6驻留设置:"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  botPersistence: " + formatBool(config.botPersistence)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  preserveBotState: " + formatBool(config.preserveBotState)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        // 兼容性设置
        ctx.getSource().sendSuccess(() -> Component.literal("§6兼容性设置:"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  carpetModCompatibility: " + formatBool(config.carpetModCompatibility)), false);
        if (MyBotMod.isCarpetModLoaded()) {
            ctx.getSource().sendSuccess(() -> Component.literal("  §e检测到 Carpet Mod 已加载"), false);
        }
        
        return 1;
    }

    /**
     * 重新加载配置
     */
    private static int reloadConfig(CommandContext<CommandSourceStack> ctx) {
        // 重新从磁盘加载配置
        ModConfig.reload();
        ctx.getSource().sendSuccess(() -> Component.literal("§a配置已重新加载"), true);
        return 1;
    }

    /**
     * 重置配置
     */
    private static int resetConfig(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();
        config.reset();
        ctx.getSource().sendSuccess(() -> Component.literal("§a配置已重置为默认值"), true);
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
            case "allowNonOpCreateBot":
                config.allowNonOpCreateBot = value;
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
            case "carpetModCompatibility":
                config.carpetModCompatibility = value;
                // 立即应用兼容模式设置
                if (value && MyBotMod.isCarpetModLoaded()) {
                    // 启用兼容模式且检测到 Carpet Mod，禁用假人功能
                    config.enableBotFeature = false;
                    ctx.getSource().sendSuccess(() -> Component.literal("§e检测到 Carpet Mod，已自动禁用假人功能"), false);
                } else if (!value && MyBotMod.isCarpetModLoaded()) {
                    // 禁用兼容模式，提示用户可以手动启用假人功能
                    ctx.getSource().sendSuccess(() -> Component.literal("§e兼容模式已禁用，可使用 /botmod config set enableBotFeature true 启用假人功能"), false);
                }
                break;
            case "allowBotAutoJump":
                config.allowBotAutoJump = value;
                break;
        }
        
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§a已设置 " + key + " = " + formatBool(value)), true);
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
        ctx.getSource().sendSuccess(() -> Component.literal("§a已设置 " + key + " = " + value), true);
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
        }
        
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§a已设置 " + key + " = " + value), true);
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
            case "allowNonOpCreateBot" -> formatBool(config.allowNonOpCreateBot);
            case "autoRespawnOnDeath" -> formatBool(config.autoRespawnOnDeath);
            case "botTakeDamage" -> formatBool(config.botTakeDamage);
            case "botHunger" -> formatBool(config.botHunger);
            case "botPersistence" -> formatBool(config.botPersistence);
            case "preserveBotState" -> formatBool(config.preserveBotState);
            case "carpetModCompatibility" -> formatBool(config.carpetModCompatibility);
            case "allowBotAutoJump" -> formatBool(config.allowBotAutoJump);
            default -> "§c未知配置项";
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
            ctx.getSource().sendSuccess(() -> Component.literal("§e骑乘白名单为空"), false);
            return 0;
        }
        
        ctx.getSource().sendSuccess(() -> Component.literal("§e骑乘白名单 (" + config.mountWhitelist.size() + "):"), false);
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
            ctx.getSource().sendFailure(Component.literal("§c" + entityType + " 已在白名单中"));
            return 0;
        }
        
        config.mountWhitelist.add(entityType);
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§a已添加 " + entityType + " 到骑乘白名单"), true);
        return 1;
    }

    /**
     * 从骑乘白名单移除
     */
    private static int removeFromWhitelist(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();
        String entityType = StringArgumentType.getString(ctx, "entityType");
        
        if (!config.mountWhitelist.contains(entityType)) {
            ctx.getSource().sendFailure(Component.literal("§c" + entityType + " 不在白名单中"));
            return 0;
        }
        
        config.mountWhitelist.remove(entityType);
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§a已从骑乘白名单移除 " + entityType), true);
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
        ctx.getSource().sendSuccess(() -> Component.literal("§a已清空骑乘白名单（移除了 " + count + " 个实体）"), true);
        return 1;
    }

    /**
     * 显示模组信息
     */
    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();
        
        ctx.getSource().sendSuccess(() -> Component.literal("§e§l=== 我的机器人 ==="), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§6版本: §f" + config.modVersion), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§6作者: §f" + config.author), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§6邮箱: §f" + config.email), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§6许可证: §f" + config.license), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§6GitHub: §f" + config.githubRepo), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7" + config.description), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        
        if (MyBotMod.isCarpetModLoaded()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§e检测到 Carpet Mod 已加载"), false);
            if (config.carpetModCompatibility) {
                ctx.getSource().sendSuccess(() -> Component.literal("§a兼容模式已启用"), false);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("§c兼容模式已禁用"), false);
            }
        }
        
        return 1;
    }

    /**
     * 格式化布尔值
     */
    private static String formatBool(boolean value) {
        return value ? "§atrue" : "§cfalse";
    }
}
