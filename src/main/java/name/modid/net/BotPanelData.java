package name.modid.net;

import name.modid.bot.BotPlayer;
import name.modid.bot.BotSettings;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * 假人设置面板的快照数据
 * 服务端在玩家右键假人时采集并下发，客户端据此打开并显示面板。
 * 包含假人个人配置（覆盖全局配置）的三态值。
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
    int killAura
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
            s.killAura.id()
        );
    }

    /** 按 BotSettings.KEYS 顺序取得三态 id */
    public int overrideId(String key) {
        return switch (key) {
            case "takeDamage" -> takeDamage;
            case "hunger" -> hunger;
            case "autoRespawn" -> autoRespawn;
            case "autoJump" -> autoJump;
            case "killAura" -> killAura;
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
            buf.readVarInt()
        );
    }
}
