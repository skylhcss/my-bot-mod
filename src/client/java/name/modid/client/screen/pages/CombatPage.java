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
 * 战斗设置页面
 */
public class CombatPage extends ConfigPage {
    
    private final ModernConfigScreen parentScreen;
    
    public CombatPage(ModernConfigScreen parentScreen, ModConfig config) {
        super(config);
        this.parentScreen = parentScreen;
    }
    
    @Override
    public Component getTitle() {
        return Component.translatable("gui.my-bot-mod.combat.title");
    }
    
    @Override
    protected void buildPage() {
        // Section 1: 攻击距离
        SectionCard reachSection = addSection(Component.translatable("gui.my-bot-mod.combat.section.reach").getString());
        
        ModernSlider attackReach = new ModernSlider(0, 0, 0, DesignTokens.ROW_HEIGHT, 1.0, 10.0, config.attackReachDistance,
            value -> Component.translatable("gui.my-bot-mod.combat.survival_reach", String.format("%.1f", value)),
            value -> { config.attackReachDistance = value; });
        reachSection.addItem(attackReach, new ResetButton(0, 0, () -> {
            config.attackReachDistance = 3.0; config.save(); attackReach.setCurrentValue(3.0);
        }));
        
        ModernSlider creativeReach = new ModernSlider(0, 0, 0, DesignTokens.ROW_HEIGHT, 1.0, 15.0, config.creativeAttackReachDistance,
            value -> Component.translatable("gui.my-bot-mod.combat.creative_reach", String.format("%.1f", value)),
            value -> { config.creativeAttackReachDistance = value; });
        reachSection.addItem(creativeReach, new ResetButton(0, 0, () -> {
            config.creativeAttackReachDistance = 5.0; config.save(); creativeReach.setCurrentValue(5.0);
        }));
        
        // Section 2: 杀戮光环
        SectionCard auraSection = addSection(Component.translatable("gui.my-bot-mod.combat.section.aura").getString());
        
        ModernCheckbox killAura = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.translatable("gui.my-bot-mod.combat.enable_aura"), config.enableKillAura);
        killAura.setOnChanged(() -> { config.enableKillAura = killAura.selected(); config.save(); });
        auraSection.addItem(killAura, new ResetButton(0, 0, () -> {
            config.enableKillAura = false; config.save(); killAura.setSelected(false);
        }));
        
        ModernSlider auraRange = new ModernSlider(0, 0, 0, DesignTokens.ROW_HEIGHT, 1.0, 10.0, config.killAuraRange,
            value -> Component.translatable("gui.my-bot-mod.combat.aura_range", String.format("%.1f", value)),
            value -> { config.killAuraRange = value; });
        auraSection.addItem(auraRange, new ResetButton(0, 0, () -> {
            config.killAuraRange = 3.0; config.save(); auraRange.setCurrentValue(3.0);
        }));
    }
}
