package name.modid.client.screen;

import name.modid.config.ModConfig;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 关于界面
 */
public class AboutScreen extends Screen {
    
    private final Screen parent;
    private final ModConfig config;
    
    public AboutScreen(Screen parent) {
        super(Component.literal("关于"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 250;
        int buttonHeight = 24;
        int centerX = this.width / 2;
        
        // 打开 GitHub 按钮 - 使用 Minecraft 的 Util.getPlatform().openUri() 方法
        this.addRenderableWidget(
            Button.builder(
                Component.literal("访问 GitHub 仓库"),
                button -> {
                    try {
                        // 使用 Minecraft 的跨平台 URI 打开方法
                        Util.getPlatform().openUri(config.githubRepo);
                    } catch (Exception e) {
                        System.err.println("无法打开 GitHub 链接: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            )
            .bounds(centerX - buttonWidth / 2, this.height - 70, buttonWidth, buttonHeight)
            .build()
        );
        
        // 完成按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("完成"),
                button -> this.minecraft.setScreen(parent)
            )
            .bounds(centerX - buttonWidth / 2, this.height - 35, buttonWidth, buttonHeight)
            .build()
        );
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        int centerX = this.width / 2;
        int startY = 40;
        int lineHeight = 16;
        
        // 标题
        graphics.drawCenteredString(this.font, this.title, centerX, 20, 0xFFFFFF);
        
        // 模组信息
        graphics.drawCenteredString(this.font, Component.literal("§e§l" + config.modName), centerX, startY, 0xFFFF55);
        graphics.drawCenteredString(this.font, Component.literal("§7版本: §f" + config.modVersion), centerX, startY + lineHeight, 0xAAAAAA);
        graphics.drawCenteredString(this.font, Component.literal("§7作者: §f" + config.author), centerX, startY + lineHeight * 2, 0xAAAAAA);
        graphics.drawCenteredString(this.font, Component.literal("§7邮箱: §f" + config.email), centerX, startY + lineHeight * 3, 0xAAAAAA);
        graphics.drawCenteredString(this.font, Component.literal("§7许可证: §f" + config.license), centerX, startY + lineHeight * 4, 0xAAAAAA);
        
        // 描述
        int descStartY = startY + lineHeight * 6;
        graphics.drawCenteredString(this.font, Component.literal("§7一个类似 Carpet Mod 的假人（机器人玩家）模组"), centerX, descStartY, 0xAAAAAA);
        graphics.drawCenteredString(this.font, Component.literal("§7用于 Minecraft 1.20.1 Fabric"), centerX, descStartY + lineHeight, 0xAAAAAA);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
