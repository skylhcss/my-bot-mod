package name.modid.behavior;

/**
 * 行为脚本的动态类型值：数字 / 字符串 / 布尔
 *
 * 参考 Scratch 的宽松类型语义：三种类型可按需要隐式互转，
 * 便于图形化编程用户自由拼接模块而不必关心类型。
 */
public final class BehaviorValue {

    /** 实际值：Double | String | Boolean 三者之一 */
    private final Object raw;

    private BehaviorValue(Object raw) {
        this.raw = raw;
    }

    public static BehaviorValue num(double v) {
        return new BehaviorValue(v);
    }

    public static BehaviorValue str(String v) {
        return new BehaviorValue(v == null ? "" : v);
    }

    public static BehaviorValue bool(boolean v) {
        return new BehaviorValue(v);
    }

    /** 数字视图：布尔转 0/1；字符串尝试解析，失败为 0 */
    public double asNumber() {
        if (raw instanceof Double d) {
            return d;
        }
        if (raw instanceof Boolean b) {
            return b ? 1 : 0;
        }
        try {
            return Double.parseDouble(((String) raw).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 布尔视图：数字非 0 为真；字符串非空且非 "false"/"0" 为真 */
    public boolean asBool() {
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw instanceof Double d) {
            return d != 0;
        }
        String s = (String) raw;
        return !s.isEmpty() && !s.equalsIgnoreCase("false") && !s.equals("0");
    }

    /** 字符串视图：整数值不带小数点 */
    public String asString() {
        if (raw instanceof Double d) {
            if (d == Math.floor(d) && !d.isInfinite() && Math.abs(d) < 1e15) {
                return String.valueOf((long) (double) d);
            }
            return String.valueOf(d);
        }
        if (raw instanceof Boolean b) {
            return b ? "true" : "false";
        }
        return (String) raw;
    }

    public boolean isNumber() {
        return raw instanceof Double;
    }

    /** Scratch 式宽松相等：两侧均可为数字时按数值比较，否则忽略大小写比较字符串 */
    public boolean looseEquals(BehaviorValue other) {
        Double a = tryNumber(this);
        Double b = tryNumber(other);
        if (a != null && b != null) {
            return a.doubleValue() == b.doubleValue();
        }
        return asString().equalsIgnoreCase(other.asString());
    }

    private static Double tryNumber(BehaviorValue v) {
        if (v.raw instanceof Double d) {
            return d;
        }
        if (v.raw instanceof Boolean) {
            return null;
        }
        String s = ((String) v.raw).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return asString();
    }
}
