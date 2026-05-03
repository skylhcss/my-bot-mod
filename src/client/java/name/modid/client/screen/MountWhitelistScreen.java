package name.modid.client.screen;

import name.modid.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 骑乘白名单编辑界面
 */
public class MountWhitelistScreen extends Screen {
    
    private final Screen parent;
    private final ModConfig config;
    private final List<String> tempWhitelist;
    private EditBox newEntityBox;
    private int scrollOffset = 0;
    
    public MountWhitelistScreen(Screen parent) {
        super(Component.literal("骑乘白名单"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
        this.tempWhitelist = new ArrayList<>(config.mountWhitelist);
    }
    
    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        
        // 新增实体输入框
        newEntityBox = new EditBox(this.font, centerX - 100, this.height - 80, 150, buttonHeight, Component.literal("实体ID"));
        newEntityBox.setHint(Component.literal("例如: minecraft:pig"));
        newEntityBox.setMaxLength(100);
        this.addRenderableWidget(newEntityBox);
        
        // 添加按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("+"),
                button -> {
                    String entityId = newEntityBox.getValue().trim();
                    if (!entityId.isEmpty() && !tempWhitelist.contains(entityId)) {
                        tempWhitelist.add(entityId);
                        newEntityBox.setValue("");
                    }
                }
            )
            .bounds(centerX + 55, this.height - 80, 45, buttonHeight)
            .build()
        );
        
        // 保存按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("保存"),
                button -> {
                    config.mountWhitelist.clear();
                    config.mountWhitelist.addAll(tempWhitelist);
                    config.save();
                    this.minecraft.setScreen(parent);
                }
            )
            .bounds(centerX - 105, this.height - 30, 100, buttonHeight)
            .build()
        );
        
        // 取消按钮
        this.addRenderableWidget(
            Button.builder(
                Component.literal("取消"),
                button -> this.minecraft.setScreen(parent)
            )
            .bounds(centerX + 5, this.height - 30, 100, buttonHeight)
            .build()
        );
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 说明文本
        graphics.drawCenteredString(
            this.font, 
            Component.literal("§7点击实体ID可以删除"), 
            this.width / 2, 
            40, 
            0xAAAAAA
        );
        
        // 绘制白名单列表
        int startY = 60;
        int lineHeight = 20;
        int maxVisible = (this.height - 150) / lineHeight;
        
        for (int i = scrollOffset; i < Math.min(tempWhitelist.size(), scrollOffset + maxVisible); i++) {
            String entityId = tempWhitelist.get(i);
            int y = startY + (i - scrollOffset) * lineHeight;
            
            // 检查鼠标是否悬停
            boolean hovered = mouseX >= this.width / 2 - 100 && mouseX <= this.width / 2 + 100 &&
                            mouseY >= y && mouseY <= y + lineHeight;
            
            // 绘制背景
            if (hovered) {
                graphics.fill(this.width / 2 - 100, y, this.width / 2 + 100, y + lineHeight, 0x80FF5555);
            } else {
                graphics.fill(this.width / 2 - 100, y, this.width / 2 + 100, y + lineHeight, 0x80000000);
            }
            
            // 绘制文本
            graphics.drawCenteredString(this.font, entityId, this.width / 2, y + 6, hovered ? 0xFF5555 : 0xFFFFFF);
        }
        
        // 滚动提示
        if (tempWhitelist.size() > maxVisible) {
            graphics.drawCenteredString(
                this.font, 
                Component.literal("§7使用鼠标滚轮滚动"), 
                this.width / 2, 
                this.height - 110, 
                0xAAAAAA
            );
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检查是否点击了列表项
        int startY = 60;
        int lineHeight = 20;
        int maxVisible = (this.height - 150) / lineHeight;
        
        for (int i = scrollOffset; i < Math.min(tempWhitelist.size(), scrollOffset + maxVisible); i++) {
            int y = startY + (i - scrollOffset) * lineHeight;
            
            if (mouseX >= this.width / 2 - 100 && mouseX <= this.width / 2 + 100 &&
                mouseY >= y && mouseY <= y + lineHeight) {
                // 删除该项
                tempWhitelist.remove(i);
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int maxVisible = (this.height - 150) / 20;
        int maxScroll = Math.max(0, tempWhitelist.size() - maxVisible);
        
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) scrollDelta));
        
        return true;
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
