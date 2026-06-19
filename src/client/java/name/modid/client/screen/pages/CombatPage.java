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
        return Component.literal("战斗");
    }
    
    @Override
    protected void buildPage() {
        // Section 1: 攻击距离
        SectionCard reachSection = addSection("攻击距离");
        
        ModernSlider attackReach = new ModernSlider(0, 0, 0, DesignTokens.ROW_HEIGHT, 1.0, 10.0, config.attackReachDistance,
            value -> Component.literal(String.format("生存模式攻击距离: %.1f 格", value)),
            value -> { config.attackReachDistance = value; });
        reachSection.addItem(attackReach, new ResetButton(0, 0, () -> {
            config.attackReachDistance = 3.0; config.save(); attackReach.setCurrentValue(3.0);
        }));
        
        ModernSlider creativeReach = new ModernSlider(0, 0, 0, DesignTokens.ROW_HEIGHT, 1.0, 15.0, config.creativeAttackReachDistance,
            value -> Component.literal(String.format("创造模式攻击距离: %.1f 格", value)),
            value -> { config.creativeAttackReachDistance = value; });
        reachSection.addItem(creativeReach, new ResetButton(0, 0, () -> {
            config.creativeAttackReachDistance = 5.0; config.save(); creativeReach.setCurrentValue(5.0);
        }));
        
        // Section 2: 杀戮光环
        SectionCard auraSection = addSection("杀戮光环");
        
        ModernCheckbox killAura = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.literal("启用杀戮光环"), config.enableKillAura);
        killAura.setOnChanged(() -> { config.enableKillAura = killAura.selected(); config.save(); });
        auraSection.addItem(killAura, new ResetButton(0, 0, () -> {
            config.enableKillAura = false; config.save(); killAura.setSelected(false);
        }));
        
        ModernSlider auraRange = new ModernSlider(0, 0, 0, DesignTokens.ROW_HEIGHT, 1.0, 10.0, config.killAuraRange,
            value -> Component.literal(String.format("杀戮光环范围: %.1f 格", value)),
            value -> { config.killAuraRange = value; });
        auraSection.addItem(auraRange, new ResetButton(0, 0, () -> {
            config.killAuraRange = 3.0; config.save(); auraRange.setCurrentValue(3.0);
        }));
    }
}
