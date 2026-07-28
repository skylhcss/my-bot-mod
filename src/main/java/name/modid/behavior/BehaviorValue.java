package name.modid.behavior;

/**
 * 行为脚本的动态类型值：数字 / 字符串 / 布尔
 *
 * 参考 Scratch 的宽松类型语义：三种类型可按需要隐式互转，
 * 便于图形化编程用户自由拼接模块而不必关心类型。
 */
public final class BehaviorValue {

    /** 实际值：Double | String | Boolean | List&lt;BehaviorValue&gt; 四者之一 */
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

    /** 列表值（内部持有可变副本，供列表语句就地修改） */
    public static BehaviorValue list(java.util.List<BehaviorValue> items) {
        return new BehaviorValue(new java.util.ArrayList<>(items == null ? java.util.List.of() : items));
    }

    public boolean isList() {
        return raw instanceof java.util.List;
    }

    /** 列表视图：非列表值视作单元素列表（宽松语义，返回可变引用） */
    @SuppressWarnings("unchecked")
    public java.util.List<BehaviorValue> asList() {
        if (raw instanceof java.util.List) {
            return (java.util.List<BehaviorValue>) raw;
        }
        java.util.List<BehaviorValue> single = new java.util.ArrayList<>(1);
        single.add(this);
        return single;
    }

    /** 数字视图：布尔转 0/1；列表转长度；字符串尝试解析，失败为 0 */
    public double asNumber() {
        if (raw instanceof Double d) {
            return d;
        }
        if (raw instanceof Boolean b) {
            return b ? 1 : 0;
        }
        if (raw instanceof java.util.List<?> l) {
            return l.size();
        }
        try {
            return Double.parseDouble(((String) raw).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 布尔视图：数字非 0 为真；列表非空为真；字符串非空且非 "false"/"0" 为真 */
    public boolean asBool() {
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw instanceof Double d) {
            return d != 0;
        }
        if (raw instanceof java.util.List<?> l) {
            return !l.isEmpty();
        }
        String s = (String) raw;
        return !s.isEmpty() && !s.equalsIgnoreCase("false") && !s.equals("0");
    }

    /** 字符串视图：整数值不带小数点；列表为 "[a, b]" */
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
        if (raw instanceof java.util.List<?> l) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(l.get(i));
            }
            return sb.append(']').toString();
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
        if (v.raw instanceof Boolean || v.raw instanceof java.util.List) {
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
