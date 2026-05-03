package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.ModernCheckbox;
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
        
        // === 基础设置 ===
        // 标题占位（20px）
        currentY += 20;
        
        // 启用假人功能
        ModernCheckbox enableBotCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 20,
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
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 提示文本占位（如果禁用则显示）
        if (!config.enableBotFeature) {
            currentY += 15;
        }
        
        currentY += GROUP_SPACING;
        
        // === 兼容性设置 ===
        // 标题占位（20px）
        currentY += 20;
        
        // Carpet Mod 兼容模式
        ModernCheckbox carpetCompatCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 20,
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
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 说明文本占位（15px）
        currentY += 15;
        
        currentY += GROUP_SPACING;
        
        // === 模组信息 ===
        // 标题占位（20px）
        currentY += 20;
        // 信息文本占位（30px）
        currentY += 30;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int currentY = contentY + 20;
        
        // 绘制基础设置标题
        drawGroupTitle(graphics, "基础设置", currentY);
        currentY += 20;
        
        // 跳过复选框位置
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 绘制提示信息
        if (!config.enableBotFeature) {
            int tipY = (int)(currentY - scrollOffset);
            if (tipY >= contentY && tipY <= contentY + contentHeight) {
                graphics.drawString(
                    font,
                    Component.literal("§c假人功能已禁用"),
                    contentX + 10,
                    tipY,
                    0xFF5555
                );
            }
            currentY += 15;
        }
        
        currentY += GROUP_SPACING;
        
        // 绘制兼容性设置标题
        drawGroupTitle(graphics, "兼容性设置", currentY);
        currentY += 20;
        
        // 跳过复选框位置
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 绘制说明
        int descY = (int)(currentY - scrollOffset);
        if (descY >= contentY && descY <= contentY + contentHeight) {
            graphics.drawString(font, 
                Component.literal("§7启用后，检测到 Carpet Mod 时将自动禁用本模组的假人功能"), 
                contentX + 10, 
                descY, 
                0xAAAAAA);
        }
        currentY += 15;
        
        currentY += GROUP_SPACING;
        
        // 绘制模组信息标题
        drawGroupTitle(graphics, "模组信息", currentY);
        currentY += 20;
        
        // 绘制模组信息
        int infoY = (int)(currentY - scrollOffset);
        if (infoY >= contentY && infoY <= contentY + contentHeight) {
            graphics.drawString(font, 
                Component.literal("§7版本: §f" + config.modVersion), 
                contentX + 10, 
                infoY, 
                0xAAAAAA);
            
            graphics.drawString(font, 
                Component.literal("§7作者: §f" + config.author), 
                contentX + 10, 
                infoY + 15, 
                0xAAAAAA);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
