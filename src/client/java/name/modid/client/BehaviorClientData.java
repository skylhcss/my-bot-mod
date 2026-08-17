package name.modid.client;

import java.util.List;
import java.util.Map;

/**
 * 客户端缓存的行为系统状态（由服务端 S2C behavior_list 同步）
 * 供全局配置界面的"行为"标签页使用。
 */
public class BehaviorClientData {

    /** 单个可用行为条目 */
    public record BehaviorEntry(String file, String displayName, String description, int blocks, boolean loop) {
    }

    /** 完整快照：可用行为 + 查询假人的播放列表/运行态/运行进度 + 解析错误 */
    public record State(List<BehaviorEntry> behaviors, String botName, List<String> assigned,
                        boolean running, String current, int queueIndex, int queueTotal,
                        Map<String, String> errors) {
    }

    private static volatile State state = new State(List.of(), "", List.of(), false, "", 0, 0, Map.of());

    /** 更新快照（在客户端主线程调用） */
    public static void set(State s) {
        state = s;
    }

    /** 获取当前快照（不可变，可安全共享） */
    public static State get() {
        return state;
    }
}
