package name.modid.client.screen.pages;

import name.modid.client.BehaviorClientData;
import name.modid.client.editor.BehaviorEditorScreen;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.SectionCard;
import name.modid.config.ModConfig;
import name.modid.net.BotNetworking;
import name.modid.client.BotClientNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

/**
 * 单个假人的行为管理内容页（内嵌于 BotBehaviorScreen，每假人各具一个）
 *
 * 运行控制（状态强化 + 启动/停止分离）、播放列表（双向排序/移除/清空/编辑）、
 * 可用行为（添加/快速执行/编辑）、解析错误。数据由服务端 BEHAVIOR_LIST 包驱动。
 */
public class BehaviorPage extends ConfigPage {

    /** 行尾小按钮宽度 */
    private static final int MINI_BTN = 14;

    /** 本页面绑定的假人（不可切换） */
    private final String botName;
    /** 宿主屏幕（打开游戏内编辑器时作为父界面） */
    private Screen host;

    public BehaviorPage(String botName, ModConfig config) {
        super(config);
        this.botName = botName;
    }

    /** 设置宿主屏幕（编辑器返回目标） */
    public void setHost(Screen host) {
        this.host = host;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.my-bot-mod.behavior.title");
    }

    @Override
    protected void buildPage() {
        BehaviorClientData.State state = BehaviorClientData.get();
        boolean stateMatches = state.botName().equals(botName);
        boolean running = stateMatches && state.running();
        List<String> assigned = stateMatches ? state.assigned() : List.of();
        String current = stateMatches ? state.current() : "";
        int queueIndex = stateMatches ? state.queueIndex() : 0;
        int queueTotal = stateMatches ? state.queueTotal() : 0;

        // ===== Section 1: 运行控制 =====
        SectionCard controlSection = addSection(
            Component.translatable("gui.my-bot-mod.behavior.section.control").getString());
        // 状态行：正在执行的行为名与队列进度
        String statusText;
        if (running) {
            statusText = "▶ " + (current.isEmpty() ? "?" : current);
            if (queueTotal > 1) {
                statusText += "  (" + queueIndex + "/" + queueTotal + ")";
            }
        } else {
            statusText = Component.translatable("gui.my-bot-mod.behavior.state.idle").getString();
        }
        ModernButton statusRow = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.literal(statusText), b -> {
        });
        statusRow.active = false;
        controlSection.addItem(statusRow);

        ModernButton startBtn = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.translatable("gui.my-bot-mod.behavior.start"), b -> sendCommand(2, ""));
        startBtn.active = !running && !assigned.isEmpty();
        controlSection.addItem(startBtn);

        ModernButton stopBtn = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.translatable("gui.my-bot-mod.behavior.stop"), b -> sendCommand(3, ""));
        stopBtn.active = running;
        controlSection.addItem(stopBtn);

        controlSection.addItem(new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.translatable("gui.my-bot-mod.behavior.new"), b -> openEditor(null)));

        // ===== Section 2: 播放列表（▲▼ 双向排序，✕ 移除，点击行编辑行为） =====
        SectionCard playlistSection = addSection(
            Component.translatable("gui.my-bot-mod.behavior.section.playlist").getString());
        if (assigned.isEmpty()) {
            ModernButton empty = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                Component.translatable("gui.my-bot-mod.behavior.playlist.empty"), b -> {
            });
            empty.active = false;
            playlistSection.addItem(empty);
        } else {
            for (int i = 0; i < assigned.size(); i++) {
                final String file = assigned.get(i);
                boolean executing = running && queueIndex == i + 1;
                String prefix = executing ? "▶ " : (i + 1) + ". ";
                ModernButton row = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                    Component.literal(prefix + file), b -> openEditor(file));
                playlistSection.addItemWithTrailing(row,
                    mini("▲", () -> sendCommand(5, file)),
                    mini("▼", () -> sendCommand(7, file)),
                    mini("✕", () -> sendCommand(1, file)));
            }
            playlistSection.addItem(new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                Component.translatable("gui.my-bot-mod.behavior.playlist.clear"),
                b -> sendCommand(8, "")));
        }

        // ===== Section 3: 可用行为（+ 加入，▶ 只跑这个，✎ 编辑；搜索框在宿主屏幕） =====
        SectionCard availableSection = addSection(
            Component.translatable("gui.my-bot-mod.behavior.section.available").getString());
        ModernButton refresh = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.translatable("gui.my-bot-mod.behavior.refresh"), b -> sendCommand(4, ""));
        availableSection.addItem(refresh);
        if (state.behaviors().isEmpty()) {
            ModernButton none = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                Component.translatable("gui.my-bot-mod.behavior.available.empty"), b -> {
            });
            none.active = false;
            availableSection.addItem(none);
        } else {
            for (BehaviorClientData.BehaviorEntry entry : state.behaviors()) {
                String label = entry.displayName().isEmpty() ? entry.file() : entry.displayName();
                String suffix = (entry.loop() ? Component.translatable("gui.my-bot-mod.behavior.loop_tag").getString() : "")
                    + " (" + entry.blocks() + ")";
                ModernButton row = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                    Component.literal("+ " + label + " " + suffix), b -> sendCommand(0, entry.file()));
                availableSection.addItemWithTrailing(row,
                    mini("▶", () -> sendCommand(6, entry.file())),
                    mini("✎", () -> openEditor(entry.file())));
            }
        }

        // ===== Section 4: 解析错误（仅有错误时显示） =====
        if (!state.errors().isEmpty()) {
            SectionCard errorSection = addSection(
                Component.translatable("gui.my-bot-mod.behavior.section.errors").getString());
            for (Map.Entry<String, String> e : state.errors().entrySet()) {
                ModernButton row = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                    Component.literal(e.getKey() + " - " + e.getValue()), b -> {
                });
                row.active = false;
                errorSection.addItem(row);
            }
        }
    }

    /** 行尾小按钮 */
    private ModernButton mini(String label, Runnable action) {
        return new ModernButton(0, 0, MINI_BTN, DesignTokens.ROW_HEIGHT,
            Component.literal(label), b -> action.run());
    }

    /** 打开游戏内行为编辑器（file 为 null = 新建） */
    private void openEditor(String file) {
        if (minecraft != null) {
            minecraft.setScreen(new BehaviorEditorScreen(host, file));
        }
    }

    /** 是否正在运行（供宿主 Screen 显示状态行） */
    public boolean isRunning() {
        BehaviorClientData.State state = BehaviorClientData.get();
        return state.botName().equals(botName) && state.running();
    }

    /** 当前执行行为名与队列进度（供宿主 Screen 显示状态行） */
    public String statusLine() {
        BehaviorClientData.State state = BehaviorClientData.get();
        if (!state.botName().equals(botName) || !state.running()) {
            return "";
        }
        String line = state.current();
        if (state.queueTotal() > 1) {
            line += " (" + state.queueIndex() + "/" + state.queueTotal() + ")";
        }
        return line;
    }

    /** 请求本假人的最新行为状态（宿主 Screen 打开与轮询时调用） */
    public void sendRequest() {
        if (minecraft != null && minecraft.getConnection() != null) {
            FriendlyByteBuf buf = BotNetworking.c2s();
            buf.writeUtf(botName, 16);
            BotClientNetworking.sendRequestBehaviorList(buf);
        }
    }

    /** 发送行为指令（0=分配 1=移除 2=启动 3=停止 4=重扫 5=上移 6=快速执行 7=下移 8=清空） */
    private void sendCommand(int action, String behaviorFile) {
        if (minecraft != null && minecraft.getConnection() != null) {
            FriendlyByteBuf buf = BotNetworking.c2s();
            buf.writeVarInt(action);
            buf.writeUtf(botName, 16);
            buf.writeUtf(behaviorFile, 128);
            BotClientNetworking.sendBehaviorCommand(buf);
        }
    }
}
