package name.modid.client.screen.pages;

import name.modid.client.screen.AboutScreen;
import name.modid.client.screen.KeybindConfigScreen;
import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ResetButton;
import name.modid.client.screen.widget.SectionCard;
import name.modid.config.ModConfig;
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
        return Component.literal("高级");
    }
    
    @Override
    protected void buildPage() {
        // Section 1: 数据持久化
        SectionCard persistenceSection = addSection("数据持久化");
        
        ModernCheckbox persistence = new ModernCheckbox(0, 0, 0, 22, Component.literal("假人驻留"), config.botPersistence);
        persistence.setOnChanged(() -> { config.botPersistence = persistence.selected(); config.save(); });
        persistenceSection.addItem(persistence, new ResetButton(0, 0, () -> {
            config.botPersistence = false; config.save(); persistence.setSelected(false);
        }));
        
        ModernCheckbox preserveState = new ModernCheckbox(0, 0, 0, 22, Component.literal("保留假人状态"), config.preserveBotState);
        preserveState.setOnChanged(() -> { config.preserveBotState = preserveState.selected(); config.save(); });
        persistenceSection.addItem(preserveState, new ResetButton(0, 0, () -> {
            config.preserveBotState = false; config.save(); preserveState.setSelected(false);
        }));
        
        // Section 2: 其他
        SectionCard otherSection = addSection("其他");
        
        ModernButton keybindBtn = new ModernButton(0, 0, 0, 22,
            Component.literal("快捷键配置"),
            button -> minecraft.setScreen(new KeybindConfigScreen(parentScreen)));
        otherSection.addItem(keybindBtn);
        
        ModernButton aboutBtn = new ModernButton(0, 0, 0, 22,
            Component.literal("关于"),
            button -> minecraft.setScreen(new AboutScreen(parentScreen)));
        otherSection.addItem(aboutBtn);
    }
}
