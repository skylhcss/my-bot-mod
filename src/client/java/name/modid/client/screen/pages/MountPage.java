package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.MountWhitelistScreen;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 骑乘设置页面
 */
public class MountPage extends ConfigPage {
    
    private final ModernConfigScreen parentScreen;
    
    public MountPage(ModernConfigScreen parentScreen, ModConfig config) {
        super(config);
        this.parentScreen = parentScreen;
    }
    
    @Override
    public Component getTitle() {
        return Component.literal("骑乘设置");
    }
    
    @Override
    protected void initPage() {
        int currentY = contentY + 20;
        
        // === 骑乘选项 ===
        // 标题占位（20px）
        currentY += 20;
        
        // 允许骑乘其他假人
        ModernCheckbox mountBotsCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 20,
            ITEM_HEIGHT,
            Component.literal("允许骑乘其他假人"),
            config.allowMountOtherBots
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.allowMountOtherBots = this.selected();
                config.save();
            }
        };
        addWidget(mountBotsCheckbox);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 说明文本占位
        currentY += 15;
        
        currentY += GROUP_SPACING;
        
        // === 白名单管理 ===
        // 标题占位（20px）
        currentY += 20;
        
        // 编辑白名单按钮
        ModernButton whitelistButton = new ModernButton(
            contentX + 10,
            currentY,
            150,
            ITEM_HEIGHT,
            Component.literal("编辑骑乘白名单"),
            button -> minecraft.setScreen(new MountWhitelistScreen(parentScreen))
        );
        addWidget(whitelistButton);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 说明文本占位（30px）
        currentY += 30;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int currentY = contentY + 20;
        
        // 绘制骑乘选项标题
        drawGroupTitle(graphics, "骑乘选项", currentY);
        currentY += 20;
        
        // 跳过复选框位置
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 绘制说明
        int descY = (int)(currentY - scrollOffset);
        if (descY >= contentY && descY <= contentY + contentHeight) {
            graphics.drawString(font, 
                Component.literal("§7控制假人可以骑乘哪些实体"), 
                contentX + 10, 
                descY, 
                0xAAAAAA);
        }
        currentY += 15;
        
        currentY += GROUP_SPACING;
        
        // 绘制白名单管理标题
        drawGroupTitle(graphics, "白名单管理", currentY);
        currentY += 20;
        
        // 跳过按钮位置
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 绘制白名单说明
        int desc2Y = (int)(currentY - scrollOffset);
        if (desc2Y >= contentY && desc2Y <= contentY + contentHeight) {
            graphics.drawString(font, 
                Component.literal("§7只有在白名单中的实体类型才能被假人骑乘"), 
                contentX + 10, 
                desc2Y, 
                0xAAAAAA);
            
            // 显示当前白名单数量
            int whitelistCount = config.mountWhitelist.size();
            graphics.drawString(font, 
                Component.literal("§7当前白名单实体数量: §f" + whitelistCount), 
                contentX + 10, 
                desc2Y + 15, 
                0xAAAAAA);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
