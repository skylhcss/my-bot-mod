package name.modid.client;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端缓存的假人列表（由服务端 S2C bot_list 同步）
 * 供全局配置界面的"假人"标签页使用。
 */
public class BotClientData {

    /** 单个假人条目 */
    public record Entry(String name, String dimension) {}

    /** 不可变列表，set() 时整体替换；get() 直接返回，避免每帧拷贝 */
    private static volatile List<Entry> bots = List.of();

    /** 更新列表（在客户端主线程调用） */
    public static void set(List<Entry> list) {
        bots = (list == null || list.isEmpty()) ? List.of() : List.copyOf(list);
    }

    /** 获取当前列表（不可变，可安全共享，无拷贝开销） */
    public static List<Entry> get() {
        return bots;
    }

    /** 增量新增/更新一个假人（按名字去重，客户端主线程调用） */
    public static void addOrUpdate(Entry e) {
        List<Entry> cur = new ArrayList<>(bots);
        cur.removeIf(x -> x.name().equals(e.name()));
        cur.add(e);
        bots = List.copyOf(cur);
    }

    /** 增量移除一个假人（客户端主线程调用） */
    public static void remove(String name) {
        List<Entry> cur = new ArrayList<>(bots);
        cur.removeIf(x -> x.name().equals(name));
        bots = List.copyOf(cur);
    }
}
