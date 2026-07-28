package name.modid.client.screen.pages;

import name.modid.client.BehaviorClientData;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.ResetButton;
import name.modid.client.screen.widget.SectionCard;
import name.modid.config.ModConfig;
import name.modid.net.BotNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

/**
 * 单个假人的行为管理内容页（内嵌于 BotBehaviorScreen，每假人各具一个）
 * 播放列表（多行为按顺序串联执行）、启动/停止、可用行为添加、重扫与解析错误展示。
 * 数据由服务端 BEHAVIOR_LIST 包驱动；宿主 Screen 负责轮询刷新。
 */
public class BehaviorPage extends ConfigPage {

    /** 本页面绑定的假人（不可切换） */
    private final String botName;

    public BehaviorPage(String botName, ModConfig config) {
        super(config);
        this.botName = botName;
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

        // ===== Section 1: 运行控制 =====
        SectionCard controlSection = addSection(
            Component.translatable("gui.my-bot-mod.behavior.section.control").getString());
        ModernButton runBtn = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.translatable(running
                ? "gui.my-bot-mod.behavior.stop" : "gui.my-bot-mod.behavior.start"),
            b -> sendCommand(running ? 3 : 2, ""));
        runBtn.active = running || !assigned.isEmpty();
        controlSection.addItem(runBtn);

        // ===== Section 2: 播放列表（串联执行；点击上移，右侧按钮移除） =====
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
                String file = assigned.get(i);
                ModernButton row = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                    Component.translatable("gui.my-bot-mod.behavior.playlist.entry", i + 1, file),
                    b -> sendCommand(5, file));
                playlistSection.addItem(row, new ResetButton(0, 0, () -> sendCommand(1, file)));
            }
        }

        // ===== Section 3: 可用行为（点击添加） =====
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
                ModernButton row = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                    Component.translatable("gui.my-bot-mod.behavior.available.entry",
                        label, entry.blocks(),
                        entry.loop() ? Component.translatable("gui.my-bot-mod.behavior.loop_tag").getString() : ""),
                    b -> sendCommand(0, entry.file()));
                availableSection.addItem(row);
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

    /** 是否正在运行（供宿主 Screen 显示状态行） */
    public boolean isRunning() {
        BehaviorClientData.State state = BehaviorClientData.get();
        return state.botName().equals(botName) && state.running();
    }

    /** 请求本假人的最新行为状态（宿主 Screen 打开与轮询时调用） */
    public void sendRequest() {
        if (minecraft != null && minecraft.getConnection() != null) {
            FriendlyByteBuf buf = BotNetworking.c2s();
            buf.writeUtf(botName, 16);
            ClientPlayNetworking.send(BotNetworking.REQUEST_BEHAVIOR_LIST, buf);
        }
    }

    /** 发送行为指令（0=分配 1=移除 2=启动 3=停止 4=重扫 5=上移），服务端回发最新列表触发重建 */
    private void sendCommand(int action, String behaviorFile) {
        if (minecraft != null && minecraft.getConnection() != null) {
            FriendlyByteBuf buf = BotNetworking.c2s();
            buf.writeVarInt(action);
            buf.writeUtf(botName, 16);
            buf.writeUtf(behaviorFile, 128);
            ClientPlayNetworking.send(BotNetworking.BEHAVIOR_COMMAND, buf);
        }
    }
}
