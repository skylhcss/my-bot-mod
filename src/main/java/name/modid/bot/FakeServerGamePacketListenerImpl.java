package name.modid.bot;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * 假的网络包监听器
 * 用于假人玩家，不处理任何网络包
 */
public class FakeServerGamePacketListenerImpl extends ServerGamePacketListenerImpl {

    //? if >=1.20.2 {
    /*public FakeServerGamePacketListenerImpl(MinecraftServer server, Connection connection, ServerPlayer player,
            net.minecraft.server.network.CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
    }
    *///?} else {
    public FakeServerGamePacketListenerImpl(MinecraftServer server, Connection connection, ServerPlayer player) {
        super(server, connection, player);
    }
    //?}

    /**
     * 假人无真实网络连接：覆盖父类 tick，跳过 keepalive/超时检测，避免被误判超时而断连。
     */
    @Override
    public void tick() {
        // no-op
    }

    /**
     * 假人无客户端，服务端下行包无需真正发送。覆盖 send 直接丢弃，
     * 避免底层 Connection 因永不连接而把所有出站包堆入 pendingActions 队列造成内存泄漏。
     */
    @Override
    public void send(net.minecraft.network.protocol.Packet<?> packet) {
        // no-op：丢弃所有下行包
    }

    @Override
    public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) {
        // no-op：丢弃所有下行包（含发送回调）
    }

    // 覆盖所有方法，使其不执行任何操作
    @Override public void handlePlayerInput(ServerboundPlayerInputPacket packet) {}
    @Override public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) {}
    @Override public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {}
    @Override public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) {}
    @Override public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) {}
    @Override public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) {}
    @Override public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) {}
    @Override public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) {}
    @Override public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) {}
    //? if <1.21.4 {
    @Override public void handlePickItem(ServerboundPickItemPacket packet) {}
    //?}
    @Override public void handleRenameItem(ServerboundRenameItemPacket packet) {}
    @Override public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) {}
    @Override public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) {}
    @Override public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) {}
    @Override public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) {}
    @Override public void handleSelectTrade(ServerboundSelectTradePacket packet) {}
    @Override public void handleEditBook(ServerboundEditBookPacket packet) {}
    //? if <1.20.5 {
    @Override public void handleEntityTagQuery(ServerboundEntityTagQuery packet) {}
    @Override public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQuery packet) {}
    //?}
    @Override public void handleMovePlayer(ServerboundMovePlayerPacket packet) {}
    @Override public void handlePlayerAction(ServerboundPlayerActionPacket packet) {}
    @Override public void handleUseItemOn(ServerboundUseItemOnPacket packet) {}
    @Override public void handleUseItem(ServerboundUseItemPacket packet) {}
    @Override public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) {}
    @Override public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) {}
    @Override public void handleClientCommand(ServerboundClientCommandPacket packet) {}
    @Override public void handleContainerClose(ServerboundContainerClosePacket packet) {}
    @Override public void handleContainerClick(ServerboundContainerClickPacket packet) {}
    @Override public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) {}
    @Override public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) {}
    @Override public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) {}
    @Override public void handleChat(ServerboundChatPacket packet) {}
    @Override public void handleChatCommand(ServerboundChatCommandPacket packet) {}
    @Override public void handleChatAck(ServerboundChatAckPacket packet) {}
    @Override public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket packet) {}
    @Override public void handleAnimate(ServerboundSwingPacket packet) {}
    @Override public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) {}
    @Override public void handleInteract(ServerboundInteractPacket packet) {}
    @Override public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {}
    @Override public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) {}
    @Override public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) {}
    @Override public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {}
    @Override public void handleSignUpdate(ServerboundSignUpdatePacket packet) {}
    // 以下包在 1.20.2+ 移至 common 协议包（由 ServerCommonPacketListenerImpl 声明）
    //? if >=1.20.2 {
    /*@Override public void handleKeepAlive(net.minecraft.network.protocol.common.ServerboundKeepAlivePacket packet) {}
    @Override public void handlePong(net.minecraft.network.protocol.common.ServerboundPongPacket packet) {}
    @Override public void handleCustomPayload(net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket packet) {}
    @Override public void handleClientInformation(net.minecraft.network.protocol.common.ServerboundClientInformationPacket packet) {}
    @Override public void handleResourcePackResponse(net.minecraft.network.protocol.common.ServerboundResourcePackPacket packet) {}
    *///?} else {
    @Override public void handleKeepAlive(ServerboundKeepAlivePacket packet) {}
    @Override public void handlePong(ServerboundPongPacket packet) {}
    @Override public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {}
    @Override public void handleClientInformation(ServerboundClientInformationPacket packet) {}
    @Override public void handleResourcePackResponse(ServerboundResourcePackPacket packet) {}
    //?}
}
