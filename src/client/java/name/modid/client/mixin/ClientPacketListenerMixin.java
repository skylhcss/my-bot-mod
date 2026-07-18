package name.modid.client.mixin;

import name.modid.client.BotSkinTextureLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

/**
 * 客户端 Mixin：当玩家信息被移除时，释放其 PNG 皮肤动态纹理
 * 修复假人删除后 GPU 纹理泄漏（假人 UUID 每次召唤都是随机的，若不释放会持续累积）
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handlePlayerInfoRemove", at = @At("HEAD"))
    private void myBotMod$onPlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
        // 复制一份 UUID 列表，调度到客户端主线程释放纹理（避免在网络线程操作 GPU 资源）
        List<UUID> ids = List.copyOf(packet.profileIds());
        Minecraft.getInstance().execute(() -> {
            for (UUID uuid : ids) {
                BotSkinTextureLoader.removeCache(uuid);
            }
        });
    }
}
