package name.modid.client.screen.pages;

import name.modid.client.screen.ModernConfigScreen;
import name.modid.client.screen.widget.DesignTokens;
import name.modid.client.screen.widget.ModernCheckbox;
import name.modid.client.screen.widget.ModernSlider;
import name.modid.client.screen.widget.ResetButton;
import name.modid.client.screen.widget.SectionCard;
import name.modid.config.ModConfig;
import net.minecraft.network.chat.Component;

/**
 * 寻路设置页面
 */
public class PathfindingPage extends ConfigPage {

    private final ModernConfigScreen parentScreen;

    public PathfindingPage(ModernConfigScreen parentScreen, ModConfig config) {
        super(config);
        this.parentScreen = parentScreen;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.my-bot-mod.pathfinding.title");
    }

    @Override
    protected void buildPage() {
        // Section: 寻路能力
        SectionCard section = addSection(Component.translatable("gui.my-bot-mod.pathfinding.section.main").getString());

        ModernSlider maxDist = new ModernSlider(0, 0, 0, DesignTokens.ROW_HEIGHT, 32, 1024, config.maxPathfindingDistance,
            value -> Component.translatable("gui.my-bot-mod.pathfinding.max_distance", (int) Math.round(value)),
            value -> { config.maxPathfindingDistance = (int) Math.round(value); });
        section.addItem(maxDist, new ResetButton(0, 0, () -> {
            config.maxPathfindingDistance = 256; config.save(); maxDist.setCurrentValue(256);
        }));

        ModernCheckbox parkour = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.translatable("gui.my-bot-mod.pathfinding.allow_parkour"), config.pathfindingAllowParkour);
        parkour.setOnChanged(() -> { config.pathfindingAllowParkour = parkour.selected(); config.save(); });
        section.addItem(parkour, new ResetButton(0, 0, () -> {
            config.pathfindingAllowParkour = true; config.save(); parkour.setSelected(true);
        }));

        ModernCheckbox swim = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.translatable("gui.my-bot-mod.pathfinding.allow_swim"), config.pathfindingAllowSwim);
        swim.setOnChanged(() -> { config.pathfindingAllowSwim = swim.selected(); config.save(); });
        section.addItem(swim, new ResetButton(0, 0, () -> {
            config.pathfindingAllowSwim = true; config.save(); swim.setSelected(true);
        }));

        ModernCheckbox smooth = new ModernCheckbox(0, 0, 0, DesignTokens.ROW_HEIGHT,
            Component.translatable("gui.my-bot-mod.pathfinding.smooth"), config.pathfindingSmooth);
        smooth.setOnChanged(() -> { config.pathfindingSmooth = smooth.selected(); config.save(); });
        section.addItem(smooth, new ResetButton(0, 0, () -> {
            config.pathfindingSmooth = true; config.save(); smooth.setSelected(true);
        }));
    }
}
