package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.MountWhitelistScreen;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernButton;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ResetButton;
import name.modid.client.screen.widget.SectionCard;
import name.modid.config.ModConfig;
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
        return Component.literal("生存");
    }
    
    @Override
    protected void buildPage() {
        // Section 1: 假人属性
        SectionCard attrSection = addSection("假人属性");
        
        ModernCheckbox takeDamage = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.literal("假人受到伤害"), config.botTakeDamage);
        takeDamage.setOnChanged(() -> { config.botTakeDamage = takeDamage.selected(); config.save(); });
        attrSection.addItem(takeDamage, new ResetButton(0, 0, () -> {
            config.botTakeDamage = true; config.save(); takeDamage.setSelected(true);
        }));
        
        ModernCheckbox hunger = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.literal("假人会饥饿"), config.botHunger);
        hunger.setOnChanged(() -> { config.botHunger = hunger.selected(); config.save(); });
        attrSection.addItem(hunger, new ResetButton(0, 0, () -> {
            config.botHunger = true; config.save(); hunger.setSelected(true);
        }));
        
        ModernCheckbox autoRespawn = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.literal("死亡自动重生"), config.autoRespawnOnDeath);
        autoRespawn.setOnChanged(() -> { config.autoRespawnOnDeath = autoRespawn.selected(); config.save(); });
        attrSection.addItem(autoRespawn, new ResetButton(0, 0, () -> {
            config.autoRespawnOnDeath = false; config.save(); autoRespawn.setSelected(false);
        }));
        
        // Section 2: 骑乘设置
        SectionCard mountSection = addSection("骑乘设置");
        
        ModernCheckbox mountBots = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT, Component.literal("允许骑乘其他假人"), config.allowMountOtherBots);
        mountBots.setOnChanged(() -> { config.allowMountOtherBots = mountBots.selected(); config.save(); });
        mountSection.addItem(mountBots, new ResetButton(0, 0, () -> {
            config.allowMountOtherBots = false; config.save(); mountBots.setSelected(false);
        }));
        
        ModernButton whitelistBtn = new ModernButton(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.literal("编辑骑乘白名单"),
            button -> minecraft.setScreen(new MountWhitelistScreen(parentScreen)));
        mountSection.addItem(whitelistBtn);
    }
}
