package name.modid.client.screen;

import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 快捷键配置界面
 */
public class KeybindConfigScreen extends Screen {
    
    private final Screen parent;
    private final ModConfig config;
    
    public KeybindConfigScreen(Screen parent) {
        super(Component.literal("快捷键配置"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int startY = 80;
        
        // 当前快捷键显示
        String currentKey = config.configMenuKey.replace("key.keyboard.", "").toUpperCase();
        
        // 提示文本在 render 方法中绘制
        
        // 完成按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("完成"),
                button -> this.minecraft.setScreen(parent)
            )
            .bounds(centerX - buttonWidth / 2, this.height - 30, buttonWidth, buttonHeight)
            .build()
        );
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 当前快捷键
        String currentKey = config.configMenuKey.replace("key.keyboard.", "").toUpperCase();
        graphics.drawCenteredString(
            this.font, 
            Component.literal("打开配置菜单: §e" + currentKey), 
            this.width / 2, 
            60, 
            0xFFFFFF
        );
        
        // 说明文本
        graphics.drawCenteredString(
            this.font, 
            Component.literal("§7在游戏中按 " + currentKey + " 键打开配置菜单"), 
            this.width / 2, 
            100, 
            0xAAAAAA
        );
        
        graphics.drawCenteredString(
            this.font, 
            Component.literal("§7快捷键可以在 Minecraft 设置 > 控制 > 按键绑定 中修改"), 
            this.width / 2, 
            120, 
            0xAAAAAA
        );
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
