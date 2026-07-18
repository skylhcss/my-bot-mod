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

    private static final List<Entry> BOTS = new ArrayList<>();

    /** 更新列表（在客户端主线程调用） */
    public static void set(List<Entry> list) {
        BOTS.clear();
        if (list != null) {
            BOTS.addAll(list);
        }
    }

    /** 获取当前列表副本 */
    public static List<Entry> get() {
        return new ArrayList<>(BOTS);
    }
}
