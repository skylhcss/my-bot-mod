package name.modid.client.screen.pages;

import name.modid.client.screen.AboutScreen;
import name.modid.client.screen.KeybindConfigScreen;
import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ResetButton;
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
        
        // 假人驻留
        ModernCheckbox persistenceCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 40,
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
        
        // 重置按钮
        ResetButton resetPersistence = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.botPersistence = false;
                config.save();
                persistenceCheckbox.setSelected(false);
            }
        );
        addWidget(resetPersistence);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
        
        // 保留假人状态
        ModernCheckbox preserveStateCheckbox = new ModernCheckbox(
            contentX + 10,
            currentY,
            contentWidth - 40,
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
        
        // 重置按钮
        ResetButton resetPreserveState = new ResetButton(
            contentX + contentWidth - 26,
            currentY,
            () -> {
                config.preserveBotState = false;
                config.save();
                preserveStateCheckbox.setSelected(false);
            }
        );
        addWidget(resetPreserveState);
        currentY += ITEM_HEIGHT + 30;
        
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
        
        // 关于按钮
        ModernButton aboutButton = new ModernButton(
            contentX + 10,
            currentY,
            150,
            ITEM_HEIGHT,
            Component.literal("关于"),
            button -> minecraft.setScreen(new AboutScreen(parentScreen))
        );
        addWidget(aboutButton);
        currentY += ITEM_HEIGHT + ITEM_SPACING;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
