package name.modid.client.screen;

import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 关于 & 帮助界面
 */
public class AboutScreen extends Screen {
    
    private final Screen parent;
    private final ModConfig config;
    
    public AboutScreen(Screen parent) {
        super(Component.literal("关于 & 帮助"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 250;
        int buttonHeight = 24;
        int centerX = this.width / 2;
        
        // 打开 GitHub 按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("访问 GitHub 仓库"),
                button -> {
                    // 在浏览器中打开 GitHub 链接
                    try {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(config.githubRepo));
                    } catch (Exception e) {
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
        
        // 帮助信息
        int helpStartY = descStartY + lineHeight * 3;
        graphics.drawCenteredString(this.font, Component.literal("§e§l快速开始"), centerX, helpStartY, 0xFFFF55);
        graphics.drawCenteredString(this.font, Component.literal("§71. 使用 §f/bot <名字> spawn §7创建假人"), centerX, helpStartY + lineHeight * 1 + 4, 0xAAAAAA);
        graphics.drawCenteredString(this.font, Component.literal("§72. 使用 §f/bot <名字> <动作> §7控制假人"), centerX, helpStartY + lineHeight * 2 + 4, 0xAAAAAA);
        graphics.drawCenteredString(this.font, Component.literal("§73. 使用 §f/bot list §7查看所有假人"), centerX, helpStartY + lineHeight * 3 + 4, 0xAAAAAA);
        graphics.drawCenteredString(this.font, Component.literal("§74. 使用 §f/bot test §7运行测试"), centerX, helpStartY + lineHeight * 4 + 4, 0xAAAAAA);
        
        // 命令示例
        int exampleStartY = helpStartY + lineHeight * 6;
        graphics.drawCenteredString(this.font, Component.literal("§e§l命令示例"), centerX, exampleStartY, 0xFFFF55);
        graphics.drawCenteredString(this.font, Component.literal("§f/bot TestBot attack continuous"), centerX, exampleStartY + lineHeight * 1 + 4, 0xCCCCCC);
        graphics.drawCenteredString(this.font, Component.literal("§f/bot TestBot move forward"), centerX, exampleStartY + lineHeight * 2 + 4, 0xCCCCCC);
        graphics.drawCenteredString(this.font, Component.literal("§f/bot TestBot look up"), centerX, exampleStartY + lineHeight * 3 + 4, 0xCCCCCC);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
