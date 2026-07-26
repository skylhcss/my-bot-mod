package name.modid.client.screen.pages;

import name.modid.client.BotClientData;
import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.SectionCard;
import name.modid.config.ModConfig;
import name.modid.net.BotNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 假人列表页面
 * 展示当前所有假人，点击某个假人可打开其设置面板（发送 /bot &lt;name&gt; panel）。
 */
public class BotsPage extends ConfigPage {

    private final ModernConfigScreen parentScreen;

    public BotsPage(ModernConfigScreen parentScreen, ModConfig config) {
        super(config);
        this.parentScreen = parentScreen;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.my-bot-mod.page.bots");
    }

    @Override
    protected void buildPage() {
        SectionCard section = addSection(Component.translatable("gui.my-bot-mod.bots.section").getString());

        // 刷新按钮
        ModernButton refresh = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.translatable("gui.my-bot-mod.bots.refresh"), b -> requestList());
        section.addItem(refresh);

        List<BotClientData.Entry> bots = BotClientData.get();
        if (bots.isEmpty()) {
            ModernButton none = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                Component.translatable("gui.my-bot-mod.bots.empty"), b -> requestList());
            none.active = false;
            section.addItem(none);
        } else {
            for (BotClientData.Entry entry : bots) {
                String name = entry.name();
                ModernButton btn = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
                    Component.translatable("gui.my-bot-mod.bots.entry", name, shortDimension(entry.dimension())),
                    b -> openBotPanel(name));
                section.addItem(btn);
            }
        }
    }

    private void openBotPanel(String name) {
        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand("bot " + name + " panel");
        }
    }

    private void requestList() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ClientPlayNetworking.send(BotNetworking.REQUEST_BOT_LIST, BotNetworking.c2s());
        }
    }

    private static String shortDimension(String dim) {
        int idx = dim.indexOf(':');
        return idx >= 0 ? dim.substring(idx + 1) : dim;
    }
}
