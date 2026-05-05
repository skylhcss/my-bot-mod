package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ResetButton;
import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 通用设置页面
 */
public class GeneralPage extends ConfigPage {
    
    private final ModernConfigScreen parentScreen;
    
    public GeneralPage(ModernConfigScreen parentScreen, ModConfig config) {
        super(config);
        this.parentScreen = parentScreen;
    }
    
    @Override
    public Component getTitle() {
        return Component.literal("通用");
    }
    
    @Override
    protected void initPage() {
        int currentY = contentY + 20;
        
        // 启用假人功能
        ModernCheckbox enableBotCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 40,
            ITEM_HEIGHT,
            Component.literal("启用假人功能"),
            config.enableBotFeature
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.enableBotFeature = this.selected();
                config.save();
            }
        };
        addWidget(enableBotCheckbox);
        
        // 重置按钮
        ResetButton resetEnableBot = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.enableBotFeature = true;
                config.save();
                enableBotCheckbox.setSelected(true);
            }
        );
        addWidget(resetEnableBot);
        currentY += ITEM_HEIGHT + 20;
        
        // Carpet Mod 兼容模式
        ModernCheckbox carpetCompatCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 40,
            ITEM_HEIGHT,
            Component.literal("Carpet Mod 兼容模式"),
            config.carpetModCompatibility
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.carpetModCompatibility = this.selected();
                config.save();
            }
        };
        addWidget(carpetCompatCheckbox);
        
        // 重置按钮
        ResetButton resetCarpetCompat = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.carpetModCompatibility = true;
                config.save();
                carpetCompatCheckbox.setSelected(true);
            }
        );
        addWidget(resetCarpetCompat);
        currentY += ITEM_HEIGHT + 20;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
