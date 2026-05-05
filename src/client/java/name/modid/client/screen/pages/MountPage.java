package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.MountWhitelistScreen;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ResetButton;
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
        
        // 允许骑乘其他假人
        ModernCheckbox mountBotsCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 40,
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
        
        // 重置按钮
        ResetButton resetMountBots = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.allowMountOtherBots = true;
                config.save();
                mountBotsCheckbox.setSelected(true);
            }
        );
        addWidget(resetMountBots);
        currentY += ITEM_HEIGHT + 30;
        
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
        currentY += ITEM_HEIGHT + 20;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
