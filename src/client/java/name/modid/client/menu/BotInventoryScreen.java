package name.modid.client.menu;

import name.modid.menu.BotInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/**
 * 假人背包界面（原版风格，程序化绘制，左右分布布局）
 *
 * 布局：
 * - 左列：假人装备（盔甲/副手）+ 3D 模型 + 手持槽位选择板 + 假人 36 格物品栏
 * - 右列：查看者自己的物品栏（用于拿取/放入）
 * 采用宽而矮的左右分布，避免竖直方向溢出屏幕。
 */
public class BotInventoryScreen extends AbstractContainerScreen<BotInventoryMenu> {

    // 原版风格调色板
    private static final int PANEL_BG = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int LABEL_COLOR = 0x404040;
    private static final int SELECTED_BORDER = 0xFF3FE03F;

    // 手持槽位选择板几何（相对 leftPos/topPos）
    private static final int PAD_X = 112;
    private static final int PAD_Y = 22;
    private static final int PAD_CELL = 14;
    private static final int PAD_GAP = 2;

    // 模型框几何（相对 leftPos/topPos）
    private static final int MODEL_X1 = 28;
    private static final int MODEL_Y1 = 18;
    private static final int MODEL_X2 = 82;
    private static final int MODEL_Y2 = 94;

    // 假人快捷栏 Y（用于当前手持格高亮）
    private static final int BOT_HOTBAR_Y = 158;

    public BotInventoryScreen(BotInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 354;
        this.imageHeight = 184;
        // "物品栏"标题位于右列上方
        this.inventoryLabelX = 184;
        this.inventoryLabelY = 54;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 面板背景 + 立体描边
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL_BG);
        graphics.fill(x, y, x + this.imageWidth, y + 1, PANEL_LIGHT);
        graphics.fill(x, y, x + 1, y + this.imageHeight, PANEL_LIGHT);
        graphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, PANEL_DARK);
        graphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, PANEL_DARK);

        // 模型框（凹陷）
        drawRecessedBox(graphics, x + MODEL_X1, y + MODEL_Y1, x + MODEL_X2, y + MODEL_Y2);

        // 所有槽位单元格
        for (Slot slot : this.menu.slots) {
            drawSlotCell(graphics, x + slot.x, y + slot.y);
        }

        // 手持槽位选择板
        drawHeldSlotPad(graphics, x, y, mouseX, mouseY);

        // 高亮假人当前手持的快捷栏格子
        int selected = this.menu.getSelectedSlot();
        if (selected >= 0 && selected <= 8) {
            int hx = x + 8 + selected * 18;
            int hy = y + BOT_HOTBAR_Y;
            drawBorder(graphics, hx - 1, hy - 1, 18, 18, SELECTED_BORDER);
        }

        // 假人模型
        renderBotModel(graphics, mouseX, mouseY);
    }

    /**
     * 渲染假人 3D 模型（跟随鼠标）
     */
    private void renderBotModel(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        Player entity = this.minecraft.level.getPlayerByUUID(this.menu.getBotUuid());
        if (entity == null) {
            return;
        }
        int cx = this.leftPos + (MODEL_X1 + MODEL_X2) / 2;
        int bottom = this.topPos + MODEL_Y2 - 6;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
            graphics, cx, bottom, 26,
            (float) cx - mouseX,
            (float) (this.topPos + (MODEL_Y1 + MODEL_Y2) / 2) - mouseY,
            (LivingEntity) entity);
    }

    /**
     * 绘制手持槽位选择板（3x3，数字 1-9）
     */
    private void drawHeldSlotPad(GuiGraphics graphics, int originX, int originY, int mouseX, int mouseY) {
        int selected = this.menu.getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            int cx = originX + PAD_X + col * (PAD_CELL + PAD_GAP);
            int cy = originY + PAD_Y + row * (PAD_CELL + PAD_GAP);

            boolean hovered = mouseX >= cx && mouseX < cx + PAD_CELL && mouseY >= cy && mouseY < cy + PAD_CELL;
            int bg = (i == selected) ? 0xFF3F7F3F : (hovered ? 0xFF6F6F6F : SLOT_BG);
            graphics.fill(cx, cy, cx + PAD_CELL, cy + PAD_CELL, bg);
            drawBorder(graphics, cx, cy, PAD_CELL, PAD_CELL, i == selected ? SELECTED_BORDER : SLOT_DARK);

            String label = String.valueOf(i + 1);
            int tw = this.font.width(label);
            graphics.drawString(this.font, label,
                cx + (PAD_CELL - tw) / 2, cy + (PAD_CELL - 8) / 2, 0xFFFFFFFF, false);
        }
        // 说明文字
        graphics.drawString(this.font, Component.literal("手持"),
            originX + PAD_X, originY + PAD_Y - 10, LABEL_COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 命中手持槽位选择板 → 设置假人手持槽位
        if (button == 0) {
            for (int i = 0; i < 9; i++) {
                int col = i % 3;
                int row = i / 3;
                int cx = this.leftPos + PAD_X + col * (PAD_CELL + PAD_GAP);
                int cy = this.topPos + PAD_Y + row * (PAD_CELL + PAD_GAP);
                if (mouseX >= cx && mouseX < cx + PAD_CELL && mouseY >= cy && mouseY < cy + PAD_CELL) {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }

    // ========== 绘制辅助 ==========

    /** 绘制原版风格的凹陷槽位单元格，item 位于 (x,y)，单元格外框为 (x-1,y-1)~(x+17,y+17) */
    private void drawSlotCell(GuiGraphics g, int x, int y) {
        int left = x - 1, top = y - 1, right = x + 17, bottom = y + 17;
        g.fill(left, top, right, bottom, SLOT_BG);
        g.fill(left, top, right, top + 1, SLOT_DARK);
        g.fill(left, top, left + 1, bottom, SLOT_DARK);
        g.fill(right - 1, top, right, bottom, PANEL_LIGHT);
        g.fill(left, bottom - 1, right, bottom, PANEL_LIGHT);
    }

    /** 绘制凹陷矩形框（用于模型区域） */
    private void drawRecessedBox(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.fill(x1, y1, x2, y2, SLOT_BG);
        g.fill(x1, y1, x2, y1 + 1, SLOT_DARK);
        g.fill(x1, y1, x1 + 1, y2, SLOT_DARK);
        g.fill(x2 - 1, y1, x2, y2, PANEL_LIGHT);
        g.fill(x1, y2 - 1, x2, y2, PANEL_LIGHT);
    }

    /** 绘制矩形边框 */
    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
