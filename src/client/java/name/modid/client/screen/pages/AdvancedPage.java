package name.modid.client.screen.pages;

import name.modid.client.screen.AboutScreen;
import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.DesignTokens;
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
        return Component.translatable("gui.my-bot-mod.advanced.title");
    }
    
    @Override
    protected void buildPage() {
        // Section 1: 数据持久化
        SectionCard persistenceSection = addSection(Component.translatable("gui.my-bot-mod.advanced.section.persistence").getString());
        
        ModernCheckbox persistence = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.translatable("gui.my-bot-mod.advanced.persistence"), config.botPersistence);
        persistence.setOnChanged(() -> { config.botPersistence = persistence.selected(); config.save(); });
        persistenceSection.addItem(persistence, new ResetButton(0, 0, () -> {
            config.botPersistence = false; config.save(); persistence.setSelected(false);
        }));
        
        ModernCheckbox preserveState = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.translatable("gui.my-bot-mod.advanced.preserve_state"), config.preserveBotState);
        preserveState.setOnChanged(() -> { config.preserveBotState = preserveState.selected(); config.save(); });
        persistenceSection.addItem(preserveState, new ResetButton(0, 0, () -> {
            config.preserveBotState = false; config.save(); preserveState.setSelected(false);
        }));
        
        // Section 2: 指挥棒
        SectionCard batonSection = addSection(Component.translatable("gui.my-bot-mod.advanced.section.baton").getString());
        
        ModernCheckbox batonTeleport = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.translatable("gui.my-bot-mod.advanced.baton_teleport"), config.allowBatonTeleportNonCreative);
        batonTeleport.setOnChanged(() -> { config.allowBatonTeleportNonCreative = batonTeleport.selected(); config.save(); });
        batonSection.addItem(batonTeleport, new ResetButton(0, 0, () -> {
            config.allowBatonTeleportNonCreative = false; config.save(); batonTeleport.setSelected(false);
        }));
        
        // Section 3: 外观与防护
        SectionCard lookSection = addSection(Component.translatable("gui.my-bot-mod.advanced.section.look").getString());
        
        ModernCheckbox glowing = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.translatable("gui.my-bot-mod.advanced.glowing"), config.botGlowing);
        glowing.setOnChanged(() -> { config.botGlowing = glowing.selected(); config.save(); });
        lookSection.addItem(glowing, new ResetButton(0, 0, () -> {
            config.botGlowing = false; config.save(); glowing.setSelected(false);
        }));
        
        ModernCheckbox fireImmune = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.translatable("gui.my-bot-mod.advanced.fire_immune"), config.botFireImmune);
        fireImmune.setOnChanged(() -> { config.botFireImmune = fireImmune.selected(); config.save(); });
        lookSection.addItem(fireImmune, new ResetButton(0, 0, () -> {
            config.botFireImmune = false; config.save(); fireImmune.setSelected(false);
        }));
        
        // Section 4: 其他
        SectionCard otherSection = addSection(Component.translatable("gui.my-bot-mod.advanced.section.other").getString());
        
        ModernButton aboutBtn = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.translatable("gui.my-bot-mod.about.title"),
            button -> minecraft.setScreen(new AboutScreen(parentScreen)));
        otherSection.addItem(aboutBtn);
    }
}
