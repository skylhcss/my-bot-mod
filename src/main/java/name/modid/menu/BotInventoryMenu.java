package name.modid.menu;

//? if <1.21.2 {
import com.mojang.datafixers.util.Pair;
//?}
import name.modid.bot.BotPlayer;
//? if <1.20.5 {
import net.minecraft.network.FriendlyByteBuf;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 假人背包容器菜单
 *
 * 直接以假人的 {@link Inventory}（41 格：0-35 主物品栏/快捷栏、36-39 盔甲、40 副手）为后端，
 * 玩家在界面中的改动会实时写入假人物品栏。同时展示查看者自己的物品栏用于拿取/放入。
 *
 * 槽位布局（菜单 slot 索引）：
 *   [0-4]   假人装备：头(39) 胸(38) 腿(37) 脚(36) 副手(40)
 *   [5-31]  假人主存储 27 格（物品栏索引 9-35）
 *   [32-40] 假人快捷栏 9 格（物品栏索引 0-8）
 *   [41-67] 查看者主存储 27 格
 *   [68-76] 查看者快捷栏 9 格
 */
public class BotInventoryMenu extends AbstractContainerMenu {

    /** 假人物品栏容器大小（36 主 + 4 盔甲 + 1 副手） */
    public static final int BOT_INV_SIZE = 41;

    //? if >=1.20.5 {
    /*// 1.20.5+ 扩展菜单改用数据对象 + StreamCodec（取代原始 FriendlyByteBuf 工厂）
    public record BotInventoryData(UUID uuid, int selected) {
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, BotInventoryData> CODEC =
            net.minecraft.network.codec.StreamCodec.of(
                (buf, data) -> { buf.writeUUID(data.uuid()); buf.writeVarInt(data.selected()); },
                buf -> new BotInventoryData(buf.readUUID(), buf.readVarInt())
            );
    }
    *///?}

    private final Container botInventory;
    /** 服务端为真实假人实体，客户端为 null */
    private final BotPlayer bot;
    /** 当前手持槽位（0-8），通过 DataSlot 从服务端同步到客户端 */
    private final DataSlot selectedSlot;
    /** 假人 UUID（供客户端渲染假人模型） */
    private final UUID botUuid;

    //? if >=1.20.5 {
    /*// 1.20.5+ 客户端构造：由 ExtendedScreenHandlerType 以数据对象调用
    public BotInventoryMenu(int containerId, Inventory playerInventory, BotInventoryData data) {
        this(containerId, playerInventory, new SimpleContainer(BOT_INV_SIZE), null,
            data.uuid(), data.selected());
    }
    *///?} else {
    /**
     * 客户端构造：由 ExtendedScreenHandlerType 调用，从缓冲区读取假人 UUID 与手持槽位
     */
    public BotInventoryMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainer(BOT_INV_SIZE), null,
            buf.readUUID(), buf.readVarInt());
    }
    //?}

    /**
     * 服务端构造：以假人真实物品栏为后端
     */
    public BotInventoryMenu(int containerId, Inventory playerInventory, Container botInventory, BotPlayer bot) {
        this(containerId, playerInventory, botInventory, bot,
            bot.getUUID(), bot.getInventory().selected);
    }

    private BotInventoryMenu(int containerId, Inventory playerInventory, Container botInventory,
                             BotPlayer bot, UUID botUuid, int initialSelected) {
        super(ModMenus.BOT_INVENTORY, containerId);
        checkContainerSize(botInventory, BOT_INV_SIZE);
        this.botInventory = botInventory;
        this.bot = bot;
        this.botUuid = botUuid;
        botInventory.startOpen(playerInventory.player);

        // 手持槽位数据同步（服务端读取真实值，客户端为独立同步槽）
        if (bot != null) {
            final BotPlayer finalBot = bot;
            this.selectedSlot = this.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return finalBot.getInventory().selected;
                }

                @Override
                public void set(int value) {
                    // 服务端权威，忽略客户端写入
                }
            });
        } else {
            DataSlot standalone = DataSlot.standalone();
            standalone.set(initialSelected);
            this.selectedSlot = this.addDataSlot(standalone);
        }

        // ===== 假人装备：头 胸 腿 脚 副手（左上，竖排） =====
        this.addSlot(armorSlot(botInventory, 39, 8, 20, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET));
        this.addSlot(armorSlot(botInventory, 38, 8, 38, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE));
        this.addSlot(armorSlot(botInventory, 37, 8, 56, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS));
        this.addSlot(armorSlot(botInventory, 36, 8, 74, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS));
        this.addSlot(armorSlot(botInventory, 40, 88, 74, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));

        // ===== 假人主存储 27 格（物品栏索引 9-35，左列下方） =====
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(botInventory, 9 + row * 9 + col, 8 + col * 18, 100 + row * 18));
            }
        }
        // ===== 假人快捷栏 9 格（物品栏索引 0-8） =====
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(botInventory, col, 8 + col * 18, 158));
        }

        // ===== 查看者主存储 27 格（右列） =====
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 9 + row * 9 + col, 184 + col * 18, 66 + row * 18));
            }
        }
        // ===== 查看者快捷栏 9 格（右列） =====
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 184 + col * 18, 124));
        }
    }

    /**
     * 创建带空槽图标的装备槽（不限制物品类型，方便灵活配置）
     */
    private static Slot armorSlot(Container container, int index, int x, int y, ResourceLocation emptyIcon) {
        return new Slot(container, index, x, y) {
            //? if <1.21.2 {
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, emptyIcon);
            }
            //?}
        };
    }

    /** 获取假人 UUID（供客户端渲染模型） */
    public UUID getBotUuid() {
        return botUuid;
    }

    /** 获取当前手持槽位（0-8） */
    public int getSelectedSlot() {
        return selectedSlot.get();
    }

    /**
     * 点击菜单按钮：id 为 0-8 时设置假人手持槽位
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (bot != null && id >= 0 && id <= 8) {
            bot.getInventory().selected = id;
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    /**
     * Shift 点击转移：假人槽 <-> 查看者物品栏
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            int playerStart = 41;   // 查看者物品栏起始 slot 索引
            int playerEnd = 77;     // 结束（不含）
            int botMainStart = 5;   // 假人主物品栏起始（跳过装备槽）
            int botMainEnd = 41;    // 结束（不含）

            if (index < playerStart) {
                // 从假人槽移动到查看者物品栏
                if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从查看者物品栏移动到假人主物品栏
                if (!this.moveItemStackTo(stack, botMainStart, botMainEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        // 客户端（bot 为 null）恒有效；服务端校验：假人存在 + 同维度 + 距离限制，
        // 防止跨维度/无限距离操作假人背包
        if (bot == null) {
            return true;
        }
        if (bot.isRemoved() || player.level() != bot.level()) {
            return false;
        }
        return player.distanceToSqr(bot) <= 64.0D * 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.botInventory.stopOpen(player);
        // 关闭背包时保存假人数据（saveBot 内部按 botPersistence 判断），减少改动丢失窗口
        if (bot != null) {
            name.modid.bot.BotPersistenceManager.saveBot(bot);
        }
    }
}
