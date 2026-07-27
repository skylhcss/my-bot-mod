package name.modid.net;

import name.modid.behavior.BehaviorManager;
import name.modid.bot.BotActionController;
import name.modid.bot.BotPlayer;
import name.modid.bot.BotSettings;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * 假人设置面板的快照数据
 * 服务端在玩家右键假人时采集并下发，客户端据此打开并显示面板。
 * 包含假人个人配置（覆盖全局配置）的三态值，以及当前活动状态与饥饿值等展示信息。
 */
public record BotPanelData(
    String name,
    UUID uuid,
    int gameMode,
    int selectedSlot,
    float health,
    float maxHealth,
    String dimension,
    double x,
    double y,
    double z,
    // 个人配置三态（0=继承 1=开 2=关），键顺序见 BotSettings.KEYS
    int takeDamage,
    int hunger,
    int autoRespawn,
    int autoJump,
    int killAura,
    int glowing,
    int fireImmune,
    // 当前活动状态：0=空闲 1=战斗中 2=寻路中 3=跟随中
    int status,
    // 饥饿值（0-20）
    int food,
    // 正在运行的行为名（空串=未运行）
    String behavior
) {

    /**
     * 从假人实体采集快照
     */
    public static BotPanelData fromBot(BotPlayer bot) {
        BotSettings s = bot.getSettings();
        return new BotPanelData(
            bot.getName().getString(),
            bot.getUUID(),
            bot.gameMode.getGameModeForPlayer().getId(),
            bot.getInventory().selected,
            bot.getHealth(),
            bot.getMaxHealth(),
            bot.level().dimension().location().toString(),
            bot.getX(),
            bot.getY(),
            bot.getZ(),
            s.takeDamage.id(),
            s.hunger.id(),
            s.autoRespawn.id(),
            s.autoJump.id(),
            s.killAura.id(),
            s.glowing.id(),
            s.fireImmune.id(),
            statusOf(bot),
            bot.getFoodData().getFoodLevel(),
            behaviorOf(bot)
        );
    }

    /** 正在运行的行为名（未运行返回空串） */
    private static String behaviorOf(BotPlayer bot) {
        String behaviorName = BehaviorManager.currentBehaviorName(bot);
        return behaviorName == null ? "" : behaviorName;
    }

    /** 计算假人当前活动状态：战斗中 &gt; 寻路中 &gt; 空闲（跟随功能未实现，暂不产生 3） */
    private static int statusOf(BotPlayer bot) {
        BotActionController c = bot.getActionController();
        if (c.isAttacking() || c.isUsing()) {
            return 1;
        }
        if (c.isPathfinding()) {
            return 2;
        }
        return 0;
    }

    /** 按 BotSettings.KEYS 顺序取得三态 id */
    public int overrideId(String key) {
        return switch (key) {
            case "takeDamage" -> takeDamage;
            case "hunger" -> hunger;
            case "autoRespawn" -> autoRespawn;
            case "autoJump" -> autoJump;
            case "killAura" -> killAura;
            case "glowing" -> glowing;
            case "fireImmune" -> fireImmune;
            default -> 0;
        };
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUUID(uuid);
        buf.writeVarInt(gameMode);
        buf.writeVarInt(selectedSlot);
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeUtf(dimension);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeVarInt(takeDamage);
        buf.writeVarInt(hunger);
        buf.writeVarInt(autoRespawn);
        buf.writeVarInt(autoJump);
        buf.writeVarInt(killAura);
        buf.writeVarInt(glowing);
        buf.writeVarInt(fireImmune);
        buf.writeVarInt(status);
        buf.writeVarInt(food);
        buf.writeUtf(behavior, 128);
    }

    public static BotPanelData read(FriendlyByteBuf buf) {
        return new BotPanelData(
            buf.readUtf(),
            buf.readUUID(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readUtf(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readUtf(128)
        );
    }
}
