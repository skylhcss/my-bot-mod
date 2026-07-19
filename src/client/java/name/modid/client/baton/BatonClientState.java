package name.modid.client.baton;

import name.modid.client.BotClientData;
import name.modid.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 指挥棒的客户端本地状态：当前模式与选中的假人。
 * 纯客户端状态，实际动作通过 C2S 数据包在服务端执行。
 */
public class BatonClientState {

    /** 指挥棒模式 */
    public enum Mode {
        PATHFIND("gui.my-bot-mod.baton.mode.pathfind"),
        TELEPORT("gui.my-bot-mod.baton.mode.teleport");

        public final String translationKey;

        Mode(String key) {
            this.translationKey = key;
        }
    }

    private static Mode mode = Mode.PATHFIND;
    private static String selectedBotName = null;

    public static Mode getMode() {
        return mode;
    }

    public static String getSelectedBotName() {
        return selectedBotName;
    }

    public static void setSelectedBotName(String name) {
        selectedBotName = name;
    }

    /** 重置状态（断开连接时调用） */
    public static void reset() {
        mode = Mode.PATHFIND;
        selectedBotName = null;
    }

    /** 按方向切换模式（dir>=0 下一个，否则上一个） */
    public static void cycleMode(int dir) {
        Mode[] values = Mode.values();
        int step = dir >= 0 ? 1 : -1;
        mode = values[Math.floorMod(mode.ordinal() + step, values.length)];
    }

    /** 在假人列表中按方向切换选中的假人 */
    public static void cycleBot(int dir) {
        List<BotClientData.Entry> bots = BotClientData.get();
        if (bots.isEmpty()) {
            selectedBotName = null;
            return;
        }
        int idx = -1;
        for (int i = 0; i < bots.size(); i++) {
            if (bots.get(i).name().equals(selectedBotName)) {
                idx = i;
                break;
            }
        }
        int step = dir >= 0 ? 1 : -1;
        idx = Math.floorMod(idx + step, bots.size());
        selectedBotName = bots.get(idx).name();
    }

    /** 玩家是否手持指挥棒（主手或副手） */
    public static boolean isHoldingBaton(Player player) {
        if (player == null) return false;
        return player.getMainHandItem().is(ModItems.COMMAND_BATON)
            || player.getOffhandItem().is(ModItems.COMMAND_BATON);
    }

    /** 本地玩家是否手持指挥棒 */
    public static boolean isHoldingBaton() {
        return isHoldingBaton(Minecraft.getInstance().player);
    }

    /** 判断名字是否为已知假人 */
    public static boolean isBot(String name) {
        if (name == null) return false;
        for (BotClientData.Entry e : BotClientData.get()) {
            if (e.name().equals(name)) return true;
        }
        return false;
    }

    /** 在客户端世界中按名字查找选中的假人实体（可能为 null，如不在附近/不同维度） */
    public static AbstractClientPlayer findSelectedClientPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (selectedBotName == null || mc.level == null) return null;
        for (AbstractClientPlayer p : mc.level.players()) {
            if (p.getName().getString().equals(selectedBotName)) return p;
        }
        return null;
    }
}
