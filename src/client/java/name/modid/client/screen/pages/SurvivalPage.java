package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ModernSlider;
import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 生存设置页面
 */
public class SurvivalPage extends ConfigPage {
    
    private final ModernConfigScreen parentScreen;
    
    public SurvivalPage(ModernConfigScreen parentScreen, ModConfig config) {
        super(config);
        this.parentScreen = parentScreen;
    }
    
    @Override
    public Component getTitle() {
        return Component.literal("生存设置");
    }
    
    @Override
    protected void initPage() {
        int startY = contentY + 30;
        int currentY = startY;
        
        // === 数量限制 ===
        // 假人最大数量滑块
        ModernSlider maxBotCountSlider = new ModernSlider(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            0,
            50,
            config.maxBotCount,
            value -> {
                int intValue = (int) Math.round(value);
                if (intValue == 0) {
                    return Component.literal("假人最大数量: 无限制");
                }
                return Component.literal(String.format("假人最大数量: %d", intValue));
            },
            value -> config.maxBotCount = (int) Math.round(value)
        );
        addWidget(maxBotCountSlider);
        currentY += ITEM_HEIGHT + GROUP_SPACING + 10;
        
        // === 权限设置 ===
        currentY += 15; // 标题空间
        
        // 允许非 OP 创建假人
        ModernCheckbox nonOpCreateCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            Component.literal("允许非 OP 创建假人"),
            config.allowNonOpCreateBot
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.allowNonOpCreateBot = this.selected();
                config.save();
            }
        };
        addWidget(nonOpCreateCheckbox);
        currentY += ITEM_HEIGHT + GROUP_SPACING;
        
        // === 生存机制 ===
        currentY += 15; // 标题空间
        
        // 假人受到伤害
        ModernCheckbox takeDamageCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            Component.literal("假人受到伤害"),
            config.botTakeDamage
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.botTakeDamage = this.selected();
                config.save();
            }
        };
        addWidget(takeDamageCheckbox);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 假人会饥饿
        ModernCheckbox hungerCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            Component.literal("假人会饥饿"),
            config.botHunger
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.botHunger = this.selected();
                config.save();
            }
        };
        addWidget(hungerCheckbox);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 死亡自动重生
        ModernCheckbox autoRespawnCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            Component.literal("死亡自动重生"),
            config.autoRespawnOnDeath
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.autoRespawnOnDeath = this.selected();
                config.save();
            }
        };
        addWidget(autoRespawnCheckbox);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        currentY += 30; // 说明文本空间
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int startY = contentY + 15;
        
        // 绘制分组标题
        drawGroupTitle(graphics, "数量限制", startY);
        
        // 绘制说明
        drawDescription(graphics, 
            "限制服务器中假人的最大数量（0 = 无限制）",
            startY + 50);
        
        // 绘制权限设置标题
        drawGroupTitle(graphics, "权限设置", startY + 80);
        
        // 绘制权限说明
        drawDescription(graphics, 
            "控制谁可以创建和管理假人",
            startY + 125);
        
        // 绘制生存机制标题
        drawGroupTitle(graphics, "生存机制", startY + 150);
        
        // 绘制生存机制说明
        drawDescription(graphics, 
            "控制假人是否遵循生存模式的规则",
            startY + 245);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
