package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ModernSlider;
import name.modid.client.screen.widget.ResetButton;
import name.modid.client.screen.widget.SectionCard;
import name.modid.config.ModConfig;
import net.minecraft.network.chat.Component;

/**
 * 基础设置页面
 */
public class GeneralPage extends ConfigPage {
    
    private final ModernConfigScreen parentScreen;
    
    public GeneralPage(ModernConfigScreen parentScreen, ModConfig config) {
        super(config);
        this.parentScreen = parentScreen;
    }
    
    @Override
    public Component getTitle() {
        return Component.literal("基础");
    }
    
    @Override
    protected void buildPage() {
        // Section 1: 模组开关
        SectionCard switchSection = addSection("模组开关");
        
        ModernCheckbox enableBot = new ModernCheckbox(0, 0, 0, 22, Component.literal("启用假人功能"), config.enableBotFeature);
        enableBot.setOnChanged(() -> { config.enableBotFeature = enableBot.selected(); config.save(); });
        switchSection.addItem(enableBot, new ResetButton(0, 0, () -> {
            config.enableBotFeature = true; config.save(); enableBot.setSelected(true);
        }));
        
        ModernCheckbox carpetCompat = new ModernCheckbox(0, 0, 0, 22, Component.literal("Carpet Mod 兼容模式"), config.carpetModCompatibility);
        carpetCompat.setOnChanged(() -> { config.carpetModCompatibility = carpetCompat.selected(); config.save(); });
        switchSection.addItem(carpetCompat, new ResetButton(0, 0, () -> {
            config.carpetModCompatibility = true; config.save(); carpetCompat.setSelected(true);
        }));
        
        // Section 2: 服务器设置
        SectionCard serverSection = addSection("服务器设置");
        
        ModernSlider maxBotSlider = new ModernSlider(0, 0, 0, 22, 0, 50, config.maxBotCount,
            value -> {
                int v = (int) Math.round(value);
                return v == 0 ? Component.literal("假人最大数量: 无限制") : Component.literal("假人最大数量: " + v);
            },
            value -> { config.maxBotCount = (int) Math.round(value); config.save(); });
        serverSection.addItem(maxBotSlider, new ResetButton(0, 0, () -> {
            config.maxBotCount = 0; config.save(); maxBotSlider.setCurrentValue(0);
        }));
        
        ModernCheckbox nonOpCreate = new ModernCheckbox(0, 0, 0, 22, Component.literal("允许非 OP 创建假人"), config.allowNonOpCreateBot);
        nonOpCreate.setOnChanged(() -> { config.allowNonOpCreateBot = nonOpCreate.selected(); config.save(); });
        serverSection.addItem(nonOpCreate, new ResetButton(0, 0, () -> {
            config.allowNonOpCreateBot = false; config.save(); nonOpCreate.setSelected(false);
        }));
    }
}
