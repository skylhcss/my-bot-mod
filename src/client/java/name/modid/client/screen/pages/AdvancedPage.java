package name.modid.client.screen.pages;

import name.modid.client.screen.AboutScreen;
import name.modid.client.screen.KeybindConfigScreen;
import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 高级设置页面
 */
public class AdvancedPage extends ConfigPage {
    
    private final ModernConfigScreen parentScreen;
    
    public AdvancedPage(ModernConfigScreen parentScreen, ModConfig config) {
        super(config);
        this.parentScreen = parentScreen;
    }
    
    @Override
    public Component getTitle() {
        return Component.literal("高级设置");
    }
    
    @Override
    protected void initPage() {
        int currentY = contentY + 20;
        
        // === 驻留设置 ===
        // 标题占位（20px）
        currentY += 20;
        
        // 假人驻留
        ModernCheckbox persistenceCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            Component.literal("假人驻留"),
            config.botPersistence
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.botPersistence = this.selected();
                config.save();
            }
        };
        addWidget(persistenceCheckbox);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 保留假人状态
        ModernCheckbox preserveStateCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            Component.literal("保留假人状态"),
            config.preserveBotState
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.preserveBotState = this.selected();
                config.save();
            }
        };
        addWidget(preserveStateCheckbox);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 说明文本占位（30px）
        currentY += 30;
        
        // 警告信息占位（如果启用驻留）
        if (config.botPersistence) {
            currentY += 30;
        }
        
        currentY += GROUP_SPACING;
        
        // === 其他设置 ===
        // 标题占位（20px）
        currentY += 20;
        
        // 快捷键配置按钮
        ModernButton keybindButton = new ModernButton(
            contentX + 10,
            currentY,
            150,
            ITEM_HEIGHT,
            Component.literal("快捷键配置"),
            button -> minecraft.setScreen(new KeybindConfigScreen(parentScreen))
        );
        addWidget(keybindButton);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 关于 & 帮助按钮
        ModernButton aboutButton = new ModernButton(
            contentX + 10,
            currentY,
            150,
            ITEM_HEIGHT,
            Component.literal("关于 & 帮助"),
            button -> minecraft.setScreen(new AboutScreen(parentScreen))
        );
        addWidget(aboutButton);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int currentY = contentY + 20;
        
        // 绘制驻留设置标题
        drawGroupTitle(graphics, "驻留设置", currentY);
        currentY += 20;
        
        // 跳过两个复选框位置
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 绘制说明
        int descY = (int)(currentY - scrollOffset);
        if (descY >= contentY && descY <= contentY + contentHeight) {
            graphics.drawString(font, 
                Component.literal("§7假人驻留：退出世界后假人依然存在"), 
                contentX + 10, 
                descY, 
                0xAAAAAA);
            
            graphics.drawString(font, 
                Component.literal("§7保留状态：重新加载时保留假人的动作状态"), 
                contentX + 10, 
                descY + 15, 
                0xAAAAAA);
        }
        currentY += 30;
        
        // 绘制警告信息
        if (config.botPersistence) {
            int warningY = (int)(currentY - scrollOffset);
            if (warningY >= contentY && warningY <= contentY + contentHeight) {
                graphics.drawString(font, 
                    Component.literal("§e⚠ 驻留功能已启用"), 
                    contentX + 10, 
                    warningY, 
                    0xFFFF55);
                
                graphics.drawString(font, 
                    Component.literal("§7假人数据将保存到世界文件夹"), 
                    contentX + 10, 
                    warningY + 15, 
                    0xAAAAAA);
            }
            currentY += 30;
        }
        
        currentY += GROUP_SPACING;
        
        // 绘制其他设置标题
        drawGroupTitle(graphics, "其他设置", currentY);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
