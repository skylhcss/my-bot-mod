package name.modid.client.screen;

import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 假人功能配置界面
 */
public class BotFeaturesConfigScreen extends Screen {
    
    private final Screen parent;
    private final ModConfig config;
    
    // 输入框
    private EditBox attackReachBox;
    private EditBox creativeAttackReachBox;
    private EditBox killAuraRangeBox;
    private EditBox maxBotCountBox;
    
    // 滚动偏移
    private double scrollOffset = 0;
    private static final int CONTENT_HEIGHT = 400; // 内容总高度
    
    public BotFeaturesConfigScreen(Screen parent) {
        super(Component.literal("假人功能配置"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int leftX = 50;
        int rightX = this.width - 200;
        int labelWidth = 180;
        int inputWidth = 150;
        int buttonHeight = 20;
        int startY = 60;
        int spacing = 28;
        int row = 0;
        
        // === 攻击设置 ===
        // 攻击距离
        attackReachBox = new EditBox(this.font, rightX, startY + spacing * row, inputWidth, buttonHeight, Component.literal("攻击距离"));
        attackReachBox.setValue(String.valueOf(config.attackReachDistance));
        attackReachBox.setMaxLength(10);
        this.addRenderableWidget(attackReachBox);
        row++;
        
        // 创造模式攻击距离
        creativeAttackReachBox = new EditBox(this.font, rightX, startY + spacing * row, inputWidth, buttonHeight, Component.literal("创造模式攻击距离"));
        creativeAttackReachBox.setValue(String.valueOf(config.creativeAttackReachDistance));
        creativeAttackReachBox.setMaxLength(10);
        this.addRenderableWidget(creativeAttackReachBox);
        row++;
        
        // 启用杀戮光环
        this.addRenderableWidget(
            new Checkbox(
                leftX, 
                startY + spacing * row, 
                labelWidth, 
                buttonHeight,
                Component.literal("启用杀戮光环"),
                config.enableKillAura,
                false
            ) {
                @Override
                public void onPress() {
                    super.onPress();
                    config.enableKillAura = this.selected();
                }
            }
        );
        row++;
        
        // 杀戮光环范围
        killAuraRangeBox = new EditBox(this.font, rightX, startY + spacing * row, inputWidth, buttonHeight, Component.literal("杀戮光环范围"));
        killAuraRangeBox.setValue(String.valueOf(config.killAuraRange));
        killAuraRangeBox.setMaxLength(10);
        this.addRenderableWidget(killAuraRangeBox);
        row++;
        
        row++; // 空行
        
        // === 骑乘设置 ===
        // 允许骑乘其他假人
        this.addRenderableWidget(
            new Checkbox(
                leftX, 
                startY + spacing * row, 
                labelWidth, 
                buttonHeight,
                Component.literal("允许骑乘其他假人"),
                config.allowMountOtherBots,
                false
            ) {
                @Override
                public void onPress() {
                    super.onPress();
                    config.allowMountOtherBots = this.selected();
                }
            }
        );
        row++;
        
        row++; // 空行
        
        // === 生存设置 ===
        // 假人最大数量
        maxBotCountBox = new EditBox(this.font, rightX, startY + spacing * row, inputWidth, buttonHeight, Component.literal("假人最大数量"));
        maxBotCountBox.setValue(String.valueOf(config.maxBotCount));
        maxBotCountBox.setMaxLength(10);
        this.addRenderableWidget(maxBotCountBox);
        row++;
        
        // 允许非 OP 创建假人
        this.addRenderableWidget(
            new Checkbox(
                leftX, 
                startY + spacing * row, 
                labelWidth, 
                buttonHeight,
                Component.literal("允许非 OP 创建假人"),
                config.allowNonOpCreateBot,
                false
            ) {
                @Override
                public void onPress() {
                    super.onPress();
                    config.allowNonOpCreateBot = this.selected();
                }
            }
        );
        row++;
        
        // 死亡自动重生
        this.addRenderableWidget(
            new Checkbox(
                leftX, 
                startY + spacing * row, 
                labelWidth, 
                buttonHeight,
                Component.literal("死亡自动重生"),
                config.autoRespawnOnDeath,
                false
            ) {
                @Override
                public void onPress() {
                    super.onPress();
                    config.autoRespawnOnDeath = this.selected();
                }
            }
        );
        row++;
        
        // 假人受到伤害
        this.addRenderableWidget(
            new Checkbox(
                leftX, 
                startY + spacing * row, 
                labelWidth, 
                buttonHeight,
                Component.literal("假人受到伤害"),
                config.botTakeDamage,
                false
            ) {
                @Override
                public void onPress() {
                    super.onPress();
                    config.botTakeDamage = this.selected();
                }
            }
        );
        row++;
        
        // 假人会饥饿
        this.addRenderableWidget(
            new Checkbox(
                leftX, 
                startY + spacing * row, 
                labelWidth, 
                buttonHeight,
                Component.literal("假人会饥饿"),
                config.botHunger,
                false
            ) {
                @Override
                public void onPress() {
                    super.onPress();
                    config.botHunger = this.selected();
                }
            }
        );
        row++;
        
        row++; // 空行
        
        // === 驻留设置 ===
        // 假人驻留
        this.addRenderableWidget(
            new Checkbox(
                leftX, 
                startY + spacing * row, 
                labelWidth, 
                buttonHeight,
                Component.literal("假人驻留"),
                config.botPersistence,
                false
            ) {
                @Override
                public void onPress() {
                    super.onPress();
                    config.botPersistence = this.selected();
                }
            }
        );
        row++;
        
        // 保留假人状态
        this.addRenderableWidget(
            new Checkbox(
                leftX, 
                startY + spacing * row, 
                labelWidth, 
                buttonHeight,
                Component.literal("保留假人状态"),
                config.preserveBotState,
                false
            ) {
                @Override
                public void onPress() {
                    super.onPress();
                    config.preserveBotState = this.selected();
                }
            }
        );
        row++;
        
        row++; // 空行
        
        // === 兼容性设置 ===
        // Carpet Mod 兼容模式
        this.addRenderableWidget(
            new Checkbox(
                leftX, 
                startY + spacing * row, 
                labelWidth, 
                buttonHeight,
                Component.literal("Carpet Mod 兼容模式"),
                config.carpetModCompatibility,
                false
            ) {
                @Override
                public void onPress() {
                    super.onPress();
                    config.carpetModCompatibility = this.selected();
                }
            }
        );
        row++;
        
        row++; // 空行
        
        // 骑乘白名单按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("编辑骑乘白名单"),
                button -> this.minecraft.setScreen(new MountWhitelistScreen(this))
            )
            .bounds(this.width / 2 - 100, startY + spacing * row, 200, buttonHeight)
            .build()
        );
        
        // 保存按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("保存"),
                button -> {
                    saveConfig();
                    this.minecraft.setScreen(parent);
                }
            )
            .bounds(this.width / 2 - 105, this.height - 30, 100, buttonHeight)
            .build()
        );
        
        // 取消按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("取消"),
                button -> this.minecraft.setScreen(parent)
            )
            .bounds(this.width / 2 + 5, this.height - 30, 100, buttonHeight)
            .build()
        );
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        try {
            config.attackReachDistance = Double.parseDouble(attackReachBox.getValue());
            config.creativeAttackReachDistance = Double.parseDouble(creativeAttackReachBox.getValue());
            config.killAuraRange = Double.parseDouble(killAuraRangeBox.getValue());
            config.maxBotCount = Integer.parseInt(maxBotCountBox.getValue());
            config.save();
        } catch (NumberFormatException e) {
            // 如果输入无效，不保存
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        // 绘制标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 绘制提示
        graphics.drawCenteredString(this.font, Component.literal("§7提示：禁用杀戮光环时只攻击视线前方"), this.width / 2, 38, 0xAAAAAA);
        
        int leftX = 50;
        int rightX = this.width - 200;
        int startY = 60;
        int spacing = 28;
        
        // === 绘制分类标题和标签 ===
        int row = 0;
        
        // 攻击设置
        graphics.drawString(this.font, "§e§l攻击设置", leftX, startY + spacing * row - 10, 0xFFFF55);
        graphics.drawString(this.font, "攻击距离（格）:", leftX, startY + spacing * row + 6, 0xFFFFFF);
        row++;
        
        graphics.drawString(this.font, "创造模式攻击距离（格）:", leftX, startY + spacing * row + 6, 0xFFFFFF);
        row++;
        
        row++; // 复选框行
        
        graphics.drawString(this.font, "杀戮光环范围（格）:", leftX, startY + spacing * row + 6, 0xFFFFFF);
        row++;
        
        row++; // 空行
        
        // 骑乘设置
        graphics.drawString(this.font, "§e§l骑乘设置", leftX, startY + spacing * row - 10, 0xFFFF55);
        row++;
        
        row++; // 空行
        
        // 生存设置
        graphics.drawString(this.font, "§e§l生存设置", leftX, startY + spacing * row - 10, 0xFFFF55);
        graphics.drawString(this.font, "假人最大数量（0=无限）:", leftX, startY + spacing * row + 6, 0xFFFFFF);
        row++;
        
        row += 4; // 4个复选框
        
        row++; // 空行
        
        // 驻留设置
        graphics.drawString(this.font, "§e§l驻留设置", leftX, startY + spacing * row - 10, 0xFFFF55);
        row += 2; // 2个复选框
        
        row++; // 空行
        
        // 兼容性设置
        graphics.drawString(this.font, "§e§l兼容性设置", leftX, startY + spacing * row - 10, 0xFFFF55);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
