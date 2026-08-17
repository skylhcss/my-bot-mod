package name.modid.menu;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

/**
 * 模组自定义容器菜单注册
 */
public class ModMenus {

    /** 假人背包菜单类型（使用扩展类型以向客户端传递假人 UUID 与手持槽位） */
    public static MenuType<BotInventoryMenu> BOT_INVENTORY;

    /**
     * 注册所有菜单类型（在主初始化中调用，服务端与客户端共用）
     */
    public static void register() {
        BOT_INVENTORY = Registry.register(
            BuiltInRegistries.MENU,
            //? if >=1.21 {
            /*ResourceLocation.fromNamespaceAndPath("my-bot-mod", "bot_inventory"),
            *///?} else {
            new ResourceLocation("my-bot-mod", "bot_inventory"),
            //?}
            //? if >=1.20.5 {
            /*new ExtendedScreenHandlerType<>(BotInventoryMenu::new, BotInventoryMenu.BotInventoryData.CODEC)
            *///?} else {
            new ExtendedScreenHandlerType<>(BotInventoryMenu::new)
            //?}
        );
    }
}
