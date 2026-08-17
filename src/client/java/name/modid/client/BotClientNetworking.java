package name.modid.client;

import name.modid.net.BotNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 客户端 C2S 发送包装
 * buf 构建仍使用 {@link BotNetworking#c2s()}（带协议版本前缀）；
 * 1.20.5+ Fabric 网络改用 CustomPacketPayload，此处按版本选择传输方式。
 */
public class BotClientNetworking {

    //? if >=1.20.5 {
    /*public static void sendUpdateSetting(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(new BotNetworking.RawPayload(BotNetworking.UPDATE_SETTING_TYPE, buf));
    }
    public static void sendRequestBotList(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(new BotNetworking.RawPayload(BotNetworking.REQUEST_BOT_LIST_TYPE, buf));
    }
    public static void sendBatonAction(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(new BotNetworking.RawPayload(BotNetworking.BATON_ACTION_TYPE, buf));
    }
    public static void sendRequestBehaviorList(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(new BotNetworking.RawPayload(BotNetworking.REQUEST_BEHAVIOR_LIST_TYPE, buf));
    }
    public static void sendBehaviorCommand(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(new BotNetworking.RawPayload(BotNetworking.BEHAVIOR_COMMAND_TYPE, buf));
    }
    public static void sendBehaviorSave(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(new BotNetworking.RawPayload(BotNetworking.BEHAVIOR_SAVE_TYPE, buf));
    }
    public static void sendBehaviorSourceRequest(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(new BotNetworking.RawPayload(BotNetworking.BEHAVIOR_SOURCE_REQUEST_TYPE, buf));
    }
    *///?} else {
    public static void sendUpdateSetting(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(BotNetworking.UPDATE_SETTING, buf);
    }
    public static void sendRequestBotList(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(BotNetworking.REQUEST_BOT_LIST, buf);
    }
    public static void sendBatonAction(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(BotNetworking.BATON_ACTION, buf);
    }
    public static void sendRequestBehaviorList(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(BotNetworking.REQUEST_BEHAVIOR_LIST, buf);
    }
    public static void sendBehaviorCommand(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(BotNetworking.BEHAVIOR_COMMAND, buf);
    }
    public static void sendBehaviorSave(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(BotNetworking.BEHAVIOR_SAVE, buf);
    }
    public static void sendBehaviorSourceRequest(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(BotNetworking.BEHAVIOR_SOURCE_REQUEST, buf);
    }
    //?}
}
