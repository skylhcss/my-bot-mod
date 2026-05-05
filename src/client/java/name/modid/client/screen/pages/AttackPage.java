package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ModernSlider;
import name.modid.client.screen.widget.ResetButton;
import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 攻击设置页面
 */
public class AttackPage extends ConfigPage {
    
    private final ModernConfigScreen parentScreen;
    
    public AttackPage(ModernConfigScreen parentScreen, ModConfig config) {
        super(config);
        this.parentScreen = parentScreen;
    }
    
    @Override
    public Component getTitle() {
        return Component.literal("攻击设置");
    }
    
    @Override
    protected void initPage() {
        int currentY = contentY + 20;
        
        // 生存模式攻击距离滑块
        ModernSlider attackReachSlider = new ModernSlider(
            contentX + 10,
            currentY,
            contentWidth - 40,
            ITEM_HEIGHT,
            1.0,
            10.0,
            config.attackReachDistance,
            value -> Component.literal(String.format("生存模式攻击距离: %.1f 格", value)),
            value -> {
                config.attackReachDistance = value;
                config.save();
            }
        );
        addWidget(attackReachSlider);
        
        // 重置按钮
        ResetButton resetAttackReach = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.attackReachDistance = 4.5;
                config.save();
                attackReachSlider.setCurrentValue(4.5);
            }
        );
        addWidget(resetAttackReach);
        currentY += ITEM_HEIGHT + 30;
        
        // 创造模式攻击距离滑块
        ModernSlider creativeAttackReachSlider = new ModernSlider(
            contentX + 10,
            currentY,
            contentWidth - 40,
            ITEM_HEIGHT,
            1.0,
            15.0,
            config.creativeAttackReachDistance,
            value -> Component.literal(String.format("创造模式攻击距离: %.1f 格", value)),
            value -> {
                config.creativeAttackReachDistance = value;
                config.save();
            }
        );
        addWidget(creativeAttackReachSlider);
        
        // 重置按钮
        ResetButton resetCreativeAttackReach = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.creativeAttackReachDistance = 6.0;
                config.save();
                creativeAttackReachSlider.setCurrentValue(6.0);
            }
        );
        addWidget(resetCreativeAttackReach);
        currentY += ITEM_HEIGHT + 30;
        
        // 启用杀戮光环
        ModernCheckbox killAuraCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 40,
            ITEM_HEIGHT,
            Component.literal("启用杀戮光环"),
            config.enableKillAura
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.enableKillAura = this.selected();
                config.save();
            }
        };
        addWidget(killAuraCheckbox);
        
        // 重置按钮
        ResetButton resetKillAura = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.enableKillAura = false;
                config.save();
                killAuraCheckbox.setSelected(false);
            }
        );
        addWidget(resetKillAura);
        currentY += ITEM_HEIGHT + 20;
        
        // 杀戮光环范围滑块
        ModernSlider killAuraRangeSlider = new ModernSlider(
            contentX + 10,
            currentY,
            contentWidth - 40,
            ITEM_HEIGHT,
            1.0,
            10.0,
            config.killAuraRange,
            value -> Component.literal(String.format("杀戮光环范围: %.1f 格", value)),
            value -> {
                config.killAuraRange = value;
                config.save();
            }
        );
        addWidget(killAuraRangeSlider);
        
        // 重置按钮
        ResetButton resetKillAuraRange = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.killAuraRange = 4.0;
                config.save();
                killAuraRangeSlider.setCurrentValue(4.0);
            }
        );
        addWidget(resetKillAuraRange);
        currentY += ITEM_HEIGHT + 30;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
