package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ModernSlider;
import name.modid.client.screen.widget.ResetButton;
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
        int currentY = contentY + 20;
        
        // 假人最大数量滑块
        ModernSlider maxBotCountSlider = new ModernSlider(
            contentX + 10,
            currentY,
            contentWidth - 40,
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
            value -> {
                config.maxBotCount = (int) Math.round(value);
                config.save();
            }
        );
        addWidget(maxBotCountSlider);
        
        // 重置按钮
        ResetButton resetMaxBotCount = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.maxBotCount = 0;
                config.save();
                maxBotCountSlider.setCurrentValue(0);
            }
        );
        addWidget(resetMaxBotCount);
        currentY += ITEM_HEIGHT + 30;
        
        // 允许非 OP 创建假人
        ModernCheckbox nonOpCreateCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 40,
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
        
        // 重置按钮
        ResetButton resetNonOpCreate = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.allowNonOpCreateBot = false;
                config.save();
                nonOpCreateCheckbox.setSelected(false);
            }
        );
        addWidget(resetNonOpCreate);
        currentY += ITEM_HEIGHT + 20;
        
        // 假人受到伤害
        ModernCheckbox takeDamageCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 40,
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
        
        // 重置按钮
        ResetButton resetTakeDamage = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.botTakeDamage = true;
                config.save();
                takeDamageCheckbox.setSelected(true);
            }
        );
        addWidget(resetTakeDamage);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 假人会饥饿
        ModernCheckbox hungerCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 40,
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
        
        // 重置按钮
        ResetButton resetHunger = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.botHunger = true;
                config.save();
                hungerCheckbox.setSelected(true);
            }
        );
        addWidget(resetHunger);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 死亡自动重生
        ModernCheckbox autoRespawnCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 40,
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
        
        // 重置按钮
        ResetButton resetAutoRespawn = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.autoRespawnOnDeath = false;
                config.save();
                autoRespawnCheckbox.setSelected(false);
            }
        );
        addWidget(resetAutoRespawn);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
