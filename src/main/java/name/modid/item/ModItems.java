package name.modid.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * 模组物品注册
 */
public class ModItems {

    /** 指挥棒：手持控制假人寻路/传送，不可堆叠 */
    public static final Item COMMAND_BATON =
        new CommandBatonItem(new Item.Properties().stacksTo(1));

    /**
     * 注册所有物品（在主初始化中调用，服务端与客户端共用）
     */
    public static void register() {
        Registry.register(
            BuiltInRegistries.ITEM,
            //? if >=1.21 {
            /*ResourceLocation.fromNamespaceAndPath("my-bot-mod", "command_baton"),
            *///?} else {
            new ResourceLocation("my-bot-mod", "command_baton"),
            //?}
            COMMAND_BATON
        );

        // 加入"工具与实用物品"创造物品栏
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(entries -> entries.accept(COMMAND_BATON));
    }
}
