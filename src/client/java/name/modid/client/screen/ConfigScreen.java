package name.modid.client.screen;

import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 配置界面主屏幕
 * 提供配置选项的分类入口
 */
public class ConfigScreen extends Screen {
    
    private final Screen parent;
    private final ModConfig config;
    
    public ConfigScreen(Screen parent) {
        super(Component.literal("My Bot Mod - 配置"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 300;
        int buttonHeight = 24;
        int centerX = this.width / 2;
        int startY = 80;
        int spacing = 30;
        
        // 总开关
        this.addRenderableWidget(
            new Checkbox(
                centerX - buttonWidth / 2, 
                startY, 
                buttonWidth, 
                buttonHeight,
                Component.literal("启用假人功能"),
                config.enableBotFeature,
                false
            ) {
                @Override
                public void onPress() {
                    super.onPress();
                    config.enableBotFeature = this.selected();
                    config.save();
                }
            }
        );
        
        // 功能配置按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("假人功能配置"),
                button -> this.minecraft.setScreen(new BotFeaturesConfigScreen(this))
            )
            .bounds(centerX - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight)
            .build()
        );
        
        // 快捷键配置按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("快捷键配置"),
                button -> this.minecraft.setScreen(new KeybindConfigScreen(this))
            )
            .bounds(centerX - buttonWidth / 2, startY + spacing * 3, buttonWidth, buttonHeight)
            .build()
        );
        
        // 关于按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("关于 & 帮助"),
                button -> this.minecraft.setScreen(new AboutScreen(this))
            )
            .bounds(centerX - buttonWidth / 2, startY + spacing * 4, buttonWidth, buttonHeight)
            .build()
        );
        
        // 重置配置按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("重置为默认配置"),
                button -> {
                    config.reset();
                    this.minecraft.setScreen(new ConfigScreen(parent));
                }
            )
            .bounds(centerX - buttonWidth / 2, startY + spacing * 6, buttonWidth, buttonHeight)
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
        // 绘制背景
        this.renderBackground(graphics);
        
        // 绘制标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        
        // 绘制提示文本
        if (!config.enableBotFeature) {
            graphics.drawCenteredString(
                this.font, 
                Component.literal("§c假人功能已禁用"), 
                this.width / 2, 
                55, 
                0xFF5555
            );
        } else {
            graphics.drawCenteredString(
                this.font, 
                Component.literal("§a假人功能已启用"), 
                this.width / 2, 
                55, 
                0x55FF55
            );
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
