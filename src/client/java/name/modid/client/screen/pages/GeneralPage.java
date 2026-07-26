package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.DesignTokens;
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
        return Component.translatable("gui.my-bot-mod.general.title");
    }
    
    @Override
    protected void buildPage() {
        // Section 1: 模组开关
        SectionCard switchSection = addSection(Component.translatable("gui.my-bot-mod.general.section.switch").getString());
        
        ModernCheckbox enableBot = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.translatable("gui.my-bot-mod.general.enable_bot"), config.enableBotFeature);
        enableBot.setOnChanged(() -> { config.enableBotFeature = enableBot.selected(); config.save(); });
        switchSection.addItem(enableBot, new ResetButton(0, 0, () -> {
            config.enableBotFeature = true; config.save(); enableBot.setSelected(true);
        }));
        
        ModernCheckbox carpetCompat = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.translatable("gui.my-bot-mod.general.carpet_compat"), config.carpetModCompatibility);
        carpetCompat.setOnChanged(() -> { config.carpetModCompatibility = carpetCompat.selected(); config.save(); });
        switchSection.addItem(carpetCompat, new ResetButton(0, 0, () -> {
            config.carpetModCompatibility = true; config.save(); carpetCompat.setSelected(true);
        }));
        
        // Section 2: 服务器设置
        SectionCard serverSection = addSection(Component.translatable("gui.my-bot-mod.general.section.server").getString());
        
        ModernSlider maxBotSlider = new ModernSlider(0, 0, 0, DesignTokens.ROW_HEIGHT, 0, 50, config.maxBotCount,
            value -> {
                int v = (int) Math.round(value);
                return v == 0 ? Component.translatable("gui.my-bot-mod.general.max_bot_unlimited") : Component.translatable("gui.my-bot-mod.general.max_bot", v);
            },
            value -> { config.maxBotCount = (int) Math.round(value); });
        serverSection.addItem(maxBotSlider, new ResetButton(0, 0, () -> {
            config.maxBotCount = 0; config.save(); maxBotSlider.setCurrentValue(0);
        }));
        
        ModernCheckbox nonOpCreate = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.translatable("gui.my-bot-mod.general.non_op"), config.allowNonOpControlBot);
        nonOpCreate.setOnChanged(() -> { config.allowNonOpControlBot = nonOpCreate.selected(); config.save(); });
        serverSection.addItem(nonOpCreate, new ResetButton(0, 0, () -> {
            config.allowNonOpControlBot = false; config.save(); nonOpCreate.setSelected(false);
        }));
        
        // Section 3: OP/权限
        SectionCard opSection = addSection(Component.translatable("gui.my-bot-mod.general.section.op").getString());
        
        ModernSlider perPlayer = new ModernSlider(0, 0, 0, DesignTokens.ROW_HEIGHT, 0, 20, config.maxBotsPerPlayer,
            value -> {
                int v = (int) Math.round(value);
                return v == 0 ? Component.translatable("gui.my-bot-mod.general.max_per_player_unlimited")
                              : Component.translatable("gui.my-bot-mod.general.max_per_player", v);
            },
            value -> { config.maxBotsPerPlayer = (int) Math.round(value); });
        opSection.addItem(perPlayer, new ResetButton(0, 0, () -> {
            config.maxBotsPerPlayer = 0; config.save(); perPlayer.setCurrentValue(0);
        }));
        
        ModernCheckbox batonOp = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.translatable("gui.my-bot-mod.general.baton_requires_op"), config.batonRequiresOp);
        batonOp.setOnChanged(() -> { config.batonRequiresOp = batonOp.selected(); config.save(); });
        opSection.addItem(batonOp, new ResetButton(0, 0, () -> {
            config.batonRequiresOp = false; config.save(); batonOp.setSelected(false);
        }));
    }
}
