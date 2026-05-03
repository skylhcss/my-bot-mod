package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ModernSlider;
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
        
        // === 攻击距离设置 ===
        // 标题占位（20px）
        currentY += 20;
        
        // 攻击距离滑块（需要额外空间显示文本）
        currentY += 15; // 文本空间
        ModernSlider attackReachSlider = new ModernSlider(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            1.0,
            10.0,
            config.attackReachDistance,
            value -> Component.literal(String.format("攻击距离: %.1f 格", value)),
            value -> config.attackReachDistance = value
        );
        addWidget(attackReachSlider);
        currentY += ITEM_HEIGHT + ITEM_SPACING + 10;
        
        // 创造模式攻击距离滑块
        currentY += 15; // 文本空间
        ModernSlider creativeAttackReachSlider = new ModernSlider(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            1.0,
            15.0,
            config.creativeAttackReachDistance,
            value -> Component.literal(String.format("创造模式攻击距离: %.1f 格", value)),
            value -> config.creativeAttackReachDistance = value
        );
        addWidget(creativeAttackReachSlider);
        currentY += ITEM_HEIGHT + ITEM_SPACING + 10;
        
        // 说明文本占位
        currentY += 15;
        
        currentY += GROUP_SPACING;
        
        // === 杀戮光环设置 ===
        // 标题占位（20px）
        currentY += 20;
        
        // 启用杀戮光环
        ModernCheckbox killAuraCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 20,
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
        currentY += ITEM_HEIGHT + ITEM_SPACING + 10;
        
        // 杀戮光环范围滑块
        currentY += 15; // 文本空间
        ModernSlider killAuraRangeSlider = new ModernSlider(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            1.0,
            10.0,
            config.killAuraRange,
            value -> Component.literal(String.format("杀戮光环范围: %.1f 格", value)),
            value -> config.killAuraRange = value
        );
        addWidget(killAuraRangeSlider);
        currentY += ITEM_HEIGHT + ITEM_SPACING + 10;
        
        // 说明文本占位（30px）
        currentY += 30;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int currentY = contentY + 20;
        
        // 绘制攻击距离标题
        drawGroupTitle(graphics, "攻击距离", currentY);
        currentY += 20;
        
        // 跳过滑块位置（包括文本空间）
        currentY += 15 + ITEM_HEIGHT + ITEM_SPACING + 10;
        currentY += 15 + ITEM_HEIGHT + ITEM_SPACING + 10;
        
        // 绘制说明
        int descY = (int)(currentY - scrollOffset);
        if (descY >= contentY && descY <= contentY + contentHeight) {
            graphics.drawString(font, 
                Component.literal("§7设置假人的攻击距离（格数）"), 
                contentX + 10, 
                descY, 
                0xAAAAAA);
        }
        currentY += 15;
        
        currentY += GROUP_SPACING;
        
        // 绘制杀戮光环标题
        drawGroupTitle(graphics, "杀戮光环", currentY);
        currentY += 20;
        
        // 跳过复选框和滑块位置
        currentY += ITEM_HEIGHT + ITEM_SPACING + 10;
        currentY += 15 + ITEM_HEIGHT + ITEM_SPACING + 10;
        
        // 绘制杀戮光环说明
        int desc2Y = (int)(currentY - scrollOffset);
        if (desc2Y >= contentY && desc2Y <= contentY + contentHeight) {
            graphics.drawString(font, 
                Component.literal("§7启用后，假人会攻击范围内的所有实体"), 
                contentX + 10, 
                desc2Y, 
                0xAAAAAA);
            
            graphics.drawString(font, 
                Component.literal("§7禁用时，假人只攻击视线前方的目标"), 
                contentX + 10, 
                desc2Y + 15, 
                0xAAAAAA);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
