package name.modid.client.screen.pages;

import name.modid.client.BehaviorClientData;
import name.modid.client.BotClientData;
import name.modid.client.screen.ModernConfigScreen;
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
 * 行为管理页面
 * 选择目标假人 → 从可用行为中添加到其播放列表（可排序/移除）→ 启动/停止执行。
 * 行为文件由外置 Blockly 编辑器生成，放入 config/my-bot-mod/behaviors/。
 */
public class BehaviorPage extends ConfigPage {

    /** 记忆选中的假人（static 以便页面重建/重开时保留） */
    private static String selectedBot = "";
    /** 本次进入世界是否已请求过（防止 请求→响应→重建→再请求 死循环） */
    private static boolean requestedOnce;

    /** 断开连接时重置会话状态（由客户端 DISCONNECT 事件调用） */
    public static void resetSession() {
        selectedBot = "";
        requestedOnce = false;
    }

    private final ModernConfigScreen parentScreen;

    public BehaviorPage(ModernConfigScreen parentScreen, ModConfig config) {
        super(config);
        this.parentScreen = parentScreen;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.my-bot-mod.behavior.title");
    }

    @Override
    protected void buildPage() {
        List<BotClientData.Entry> bots = BotClientData.get();
        // 选中假人失效时回退到第一个假人
        if (selectedBot.isEmpty() || bots.stream().noneMatch(b -> b.name().equals(selectedBot))) {
            selectedBot = bots.isEmpty() ? "" : bots.get(0).name();
        }
        maybeRequest();

        BehaviorClientData.State state = BehaviorClientData.get();
        boolean stateMatches = state.botName().equals(selectedBot);

        // ===== Section 1: 目标假人 =====
        SectionCard botSection = addSection(Component.translatable("gui.my-bot-mod.behavior.section.bot").getString());
        if (bots.isEmpty()) {
            ModernButton none = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                Component.translatable("gui.my-bot-mod.bots.empty"), b -> {
                });
            none.active = false;
            botSection.addItem(none);
        } else {
            // 循环切换选中假人
            ModernButton botBtn = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                Component.translatable("gui.my-bot-mod.behavior.selected_bot", selectedBot,
                    stateMatches && state.running()
                        ? Component.translatable("gui.my-bot-mod.behavior.state.running").getString()
                        : Component.translatable("gui.my-bot-mod.behavior.state.idle").getString()),
                b -> cycleBot(bots));
            botSection.addItem(botBtn);

            // 启动 / 停止
            boolean running = stateMatches && state.running();
            ModernButton runBtn = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                Component.translatable(running
                    ? "gui.my-bot-mod.behavior.stop" : "gui.my-bot-mod.behavior.start"),
                b -> sendCommand(running ? 3 : 2, ""));
            runBtn.active = !selectedBot.isEmpty() && (running || !state.assigned().isEmpty());
            botSection.addItem(runBtn);
        }

        // ===== Section 2: 播放列表（点击上移，右侧按钮移除） =====
        SectionCard playlistSection = addSection(
            Component.translatable("gui.my-bot-mod.behavior.section.playlist").getString());
        List<String> assigned = stateMatches ? state.assigned() : List.of();
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

        // ===== Section 3: 可用行为（点击添加到播放列表） =====
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
                row.active = !selectedBot.isEmpty();
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

    /** 循环切换选中假人并向服务端请求其状态 */
    private void cycleBot(List<BotClientData.Entry> bots) {
        int idx = 0;
        for (int i = 0; i < bots.size(); i++) {
            if (bots.get(i).name().equals(selectedBot)) {
                idx = (i + 1) % bots.size();
                break;
            }
        }
        selectedBot = bots.get(idx).name();
        sendRequest();
    }

    /** 未同步过或选中假人变化时请求最新状态 */
    private void maybeRequest() {
        BehaviorClientData.State state = BehaviorClientData.get();
        if (!requestedOnce || !state.botName().equals(selectedBot)) {
            requestedOnce = true;
            sendRequest();
        }
    }

    private void sendRequest() {
        if (minecraft != null && minecraft.getConnection() != null) {
            FriendlyByteBuf buf = BotNetworking.c2s();
            buf.writeUtf(selectedBot, 16);
            ClientPlayNetworking.send(BotNetworking.REQUEST_BEHAVIOR_LIST, buf);
        }
    }

    /** 发送行为指令（0=分配 1=移除 2=启动 3=停止 4=重扫 5=上移），服务端回发最新列表触发重建 */
    private void sendCommand(int action, String behaviorFile) {
        if (minecraft != null && minecraft.getConnection() != null) {
            FriendlyByteBuf buf = BotNetworking.c2s();
            buf.writeVarInt(action);
            buf.writeUtf(selectedBot, 16);
            buf.writeUtf(behaviorFile, 128);
            ClientPlayNetworking.send(BotNetworking.BEHAVIOR_COMMAND, buf);
        }
    }
}
