package name.modid.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 假人玩家类
 * 继承自 ServerPlayer，代表一个由命令控制的机器人玩家
 * 
 * 实现参考 Carpet Mod 的 EntityPlayerMPFake
 */
public class BotPlayer extends ServerPlayer {
    
    /**
     * 假人的动作控制器
     */
    private final BotActionController actionController;
    
    /**
     * 创建假人的玩家UUID
     */
    private final java.util.UUID creatorUUID;
    
    /**
     * 创建假人的玩家名字
     */
    private final String creatorName;

    /**
     * 构造函数
     * @param server 服务器实例
     * @param level 世界
     * @param profile 游戏档案（包含假人名字和UUID）
     * @param connection 网络连接
     * @param creator 创建假人的玩家
     */
    public BotPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, Connection connection, ServerPlayer creator) {
        super(server, level, profile);
        this.connection = new FakeServerGamePacketListenerImpl(server, connection, this);
        this.creatorUUID = creator.getUUID();
        this.creatorName = creator.getName().getString();
        this.actionController = new BotActionController(this);
    }

    /**
     * 获取动作控制器
     */
    public BotActionController getActionController() {
        return actionController;
    }

    /**
     * 获取创建者UUID
     */
    public java.util.UUID getCreatorUUID() {
        return creatorUUID;
    }
    
    /**
     * 获取创建者名字
     */
    public String getCreatorName() {
        return creatorName;
    }

    /**
     * 每tick更新假人状态
     * 参考 Carpet Mod 的 EntityPlayerMPFake.tick()
     */
    @Override
    public void tick() {
        // 每 10 tick 重置位置
        // 这是 Carpet Mod 的做法，确保假人的位置同步
        if (this.level().getServer().getTickCount() % 10 == 0) {
            this.connection.resetPosition();
        }
        
        // 调用父类 tick
        // 注意：移动输入的应用在 tick() 开始时通过 ServerPlayerMixin 完成
        super.tick();
        
        // 在 super.tick() 之后调用 doTick()
        // 这会处理其他玩家相关的逻辑
        this.doTick();
    }
    
    /**
     * 假人死亡时的处理
     * 从管理器中移除假人
     */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        super.die(damageSource);
        // 从管理器中移除假人
        BotManager.removeBot(this.getName().getString());
    }

    /**
     * 判断是否为假人
     */
    public static boolean isBot(Player player) {
        return player instanceof BotPlayer;
    }

    /**
     * 设置假人的位置和旋转
     */
    public void setPositionAndRotation(Vec3 pos, float yaw, float pitch) {
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setYHeadRot(yaw);
    }
}
