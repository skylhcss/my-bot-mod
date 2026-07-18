package name.modid.bot;

/**
 * 假人个人配置（覆盖全局配置）
 *
 * 每一项为三态：
 * - INHERIT：继承全局配置
 * - ON：强制开启
 * - OFF：强制关闭
 *
 * 解析时优先于全局配置：resolve(override, globalValue)
 */
public class BotSettings {

    public enum Override {
        INHERIT, ON, OFF;

        public int id() {
            return this.ordinal();
        }

        public static Override byId(int id) {
            Override[] values = values();
            if (id < 0 || id >= values.length) {
                return INHERIT;
            }
            return values[id];
        }

        /** 循环切换：继承 -> 开 -> 关 -> 继承 */
        public Override next() {
            return switch (this) {
                case INHERIT -> ON;
                case ON -> OFF;
                case OFF -> INHERIT;
            };
        }
    }

    /** 可覆盖的配置项键（用于网络与界面遍历） */
    public static final String[] KEYS = {
        "takeDamage", "hunger", "autoRespawn", "autoJump", "killAura"
    };

    // 受到伤害
    public Override takeDamage = Override.INHERIT;
    // 会饥饿
    public Override hunger = Override.INHERIT;
    // 死亡自动重生
    public Override autoRespawn = Override.INHERIT;
    // 自动跳跃
    public Override autoJump = Override.INHERIT;
    // 杀戮光环
    public Override killAura = Override.INHERIT;

    /**
     * 解析三态覆盖：ON/OFF 优先，INHERIT 使用全局值
     */
    public static boolean resolve(Override override, boolean globalValue) {
        if (override == null) {
            return globalValue;
        }
        return switch (override) {
            case ON -> true;
            case OFF -> false;
            case INHERIT -> globalValue;
        };
    }

    public Override get(String key) {
        if (key == null) {
            return Override.INHERIT;
        }
        return switch (key) {
            case "takeDamage" -> takeDamage;
            case "hunger" -> hunger;
            case "autoRespawn" -> autoRespawn;
            case "autoJump" -> autoJump;
            case "killAura" -> killAura;
            default -> Override.INHERIT;
        };
    }

    public void set(String key, Override value) {
        if (key == null || value == null) {
            return;
        }
        switch (key) {
            case "takeDamage" -> takeDamage = value;
            case "hunger" -> hunger = value;
            case "autoRespawn" -> autoRespawn = value;
            case "autoJump" -> autoJump = value;
            case "killAura" -> killAura = value;
            default -> {
            }
        }
    }

    /** 从另一份设置复制所有字段（用于驻留恢复） */
    public void copyFrom(BotSettings other) {
        if (other == null) {
            return;
        }
        this.takeDamage = other.takeDamage != null ? other.takeDamage : Override.INHERIT;
        this.hunger = other.hunger != null ? other.hunger : Override.INHERIT;
        this.autoRespawn = other.autoRespawn != null ? other.autoRespawn : Override.INHERIT;
        this.autoJump = other.autoJump != null ? other.autoJump : Override.INHERIT;
        this.killAura = other.killAura != null ? other.killAura : Override.INHERIT;
    }

    /** 该键的中文显示名 */
    public static String displayName(String key) {
        return switch (key) {
            case "takeDamage" -> "受到伤害";
            case "hunger" -> "会饥饿";
            case "autoRespawn" -> "死亡自动重生";
            case "autoJump" -> "自动跳跃";
            case "killAura" -> "杀戮光环";
            default -> key;
        };
    }
}
