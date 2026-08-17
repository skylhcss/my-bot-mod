package name.modid.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if <1.20.5 {
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
//?}

import java.util.List;

/**
 * 指挥棒物品
 * 实际交互（选人、切换模式、下令寻路/传送）由客户端输入回调与网络数据包处理，
 * 物品本身仅提供 tooltip 与不可堆叠属性。
 */
public class CommandBatonItem extends Item {

    public CommandBatonItem(Properties properties) {
        super(properties);
    }

    //? if >=1.20.5 {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.my-bot-mod.command_baton.line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.my-bot-mod.command_baton.line2").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
    *///?} else {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.my-bot-mod.command_baton.line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.my-bot-mod.command_baton.line2").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
    //?}
}
