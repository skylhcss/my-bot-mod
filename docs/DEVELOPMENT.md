# 开发文档

本文档面向想要为 My Bot Mod 贡献代码或了解其内部实现的开发者。

## 目录
- [项目结构](#项目结构)
- [核心架构](#核心架构)
- [构建项目](#构建项目)
- [开发环境](#开发环境)
- [代码规范](#代码规范)
- [测试](#测试)
- [贡献指南](#贡献指南)

---

## 项目结构

```
my-bot-mod/
├── src/
│   ├── main/
│   │   ├── java/name/modid/
│   │   │   ├── bot/                    # 假人核心
│   │   │   │   ├── BotPlayer.java      # 假人实体
│   │   │   │   ├── BotActionController.java  # 动作控制
│   │   │   │   ├── BotManager.java     # 假人管理
│   │   │   │   ├── BotSkinManager.java # 皮肤管理
│   │   │   │   ├── BotPersistenceManager.java  # 驻留系统
│   │   │   │   └── FakeServerGamePacketListenerImpl.java  # 网络连接
│   │   │   ├── command/
│   │   │   │   └── BotCommand.java     # 命令系统
│   │   │   ├── config/
│   │   │   │   └── ModConfig.java      # 配置系统
│   │   │   ├── mixin/
│   │   │   │   └── ServerPlayerMixin.java  # 核心 Mixin
│   │   │   └── MyBotMod.java           # 主类
│   │   └── resources/
│   │       ├── fabric.mod.json         # 模组元数据
│   │       └── my-bot-mod.mixins.json  # Mixin 配置
│   └── client/
│       ├── java/name/modid/client/
│       │   ├── screen/                 # 配置界面
│       │   │   ├── ConfigScreen.java
│       │   │   ├── BotFeaturesConfigScreen.java
│       │   │   ├── KeybindConfigScreen.java
│       │   │   ├── AboutScreen.java
│       │   │   └── MountWhitelistScreen.java
│       │   ├── mixin/
│       │   │   └── PlayerInfoMixin.java  # 皮肤渲染 Mixin
│       │   ├── BotSkinTextureLoader.java  # 皮肤加载
│       │   └── MyBotModClient.java     # 客户端主类
│       └── resources/
│           └── my-bot-mod.client.mixins.json
├── docs/                               # 文档
├── run/                                # 开发运行目录
├── build.gradle                        # Gradle 构建脚本
└── README.md                           # 项目说明
```

---

## 核心架构

### 1. BotPlayer（假人实体）

**职责**：
- 继承自 `ServerPlayer`
- 管理假人生命周期
- 持有 `BotActionController` 实例

**关键方法**：
- `tick()`：每 tick 更新状态
- `die()`：死亡处理
- `hurt()`：伤害处理

**关键字段**：
- `actionController`：动作控制器
- `creatorUUID`：创建者 UUID
- `creatorName`：创建者名字

---

### 2. BotActionController（动作控制器）

**职责**：
- 控制假人的所有动作
- 管理动作状态和移动状态
- 参考 Carpet Mod 的 `EntityPlayerActionPack`

**关键方法**：
- `tick()`：更新动作状态
- `applyMovement()`：应用移动输入
- `performAttack()`：执行攻击
- `performUse()`：执行使用物品

**关键字段**：
- `forward`：前后移动值
- `strafing`：左右移动值
- `attacking`：攻击状态
- `using`：使用状态

---

### 3. BotManager（假人管理器）

**职责**：
- 管理所有假人的创建、删除和查询
- 维护假人注册表

**关键方法**：
- `createBot()`：创建假人
- `removeBot()`：删除假人
- `getBot()`：获取假人
- `getAllBots()`：获取所有假人

**数据结构**：
- `bots`：`Map<String, BotPlayer>` - 按名字索引
- `botsByUUID`：`Map<UUID, BotPlayer>` - 按 UUID 索引

---

### 4. ServerPlayerMixin（核心 Mixin）

**职责**：
- 在 `ServerPlayer.tick()` 的 HEAD 位置注入
- 为假人更新动作状态和应用移动输入

**注入点**：
```java
@Inject(method = "tick", at = @At("HEAD"))
private void onTick(CallbackInfo ci) {
    ServerPlayer player = (ServerPlayer) (Object) this;
    if (player instanceof BotPlayer bot) {
        bot.getActionController().tick();
        bot.getActionController().applyMovement();
    }
}
```

**为什么这样做**：
- Carpet Mod 的做法
- 确保移动输入在物理处理之前被设置
- 每个 tick 都更新，即使值为 0

---

### 5. 移动系统

**核心原理**：
- 使用 `zza` 和 `xxa` 字段控制移动
- 在 `tick()` 开始时应用移动输入
- 让游戏物理引擎处理实际移动

**关键代码**：
```java
public void applyMovement() {
    float vel = sneaking ? 0.3F : 1.0F;
    bot.zza = forward * vel;
    bot.xxa = strafing * vel;
}
```

**为什么有效**：
- 每个 tick 都设置移动输入
- 在物理处理之前设置
- 潜行时速度降低

---

### 6. 攻击系统

**核心原理**：
- 使用射线追踪检测视线前方的目标
- 优先攻击实体，如果没有则挖掘方块

**关键方法**：
```java
private void performAttack() {
    // 1. 执行射线追踪
    var hitResult = bot.pick(reachDistance, 0.0F, false);
    
    // 2. 检查是否击中实体
    var entityHitResult = getEntityHitResult(bot, reachDistance);
    
    // 3. 攻击实体或挖掘方块
    if (entityHitResult != null) {
        bot.attack(entityHitResult.getEntity());
    } else if (hitResult.getType() == HitResult.Type.BLOCK) {
        bot.gameMode.destroyBlock(blockPos);
    }
}
```

---

### 7. 皮肤系统

**三级优先级**：
1. Mojang API
2. PNG 文件
3. Base64 文件

**服务器端**：`BotSkinManager`
- 管理皮肤数据
- 应用皮肤到 GameProfile

**客户端**：`BotSkinTextureLoader`
- 加载 PNG 文件
- 创建纹理

**Mixin**：`PlayerInfoMixin`
- 拦截皮肤渲染
- 应用假人皮肤

---

### 8. 配置系统

**配置类**：`ModConfig`
- JSON 格式
- 自动加载和保存
- 单例模式

**配置界面**：
- `ConfigScreen`：主界面
- `BotFeaturesConfigScreen`：功能配置
- `KeybindConfigScreen`：快捷键
- `AboutScreen`：关于
- `MountWhitelistScreen`：白名单

---

### 9. 驻留系统

**核心类**：`BotPersistenceManager`

**工作流程**：
1. 服务器启动时加载假人
2. 假人每 200 tick 自动保存
3. 服务器关闭时保存所有假人

**数据格式**：
```json
{
  "name": "TestBot",
  "uuid": "...",
  "creatorUUID": "...",
  "dimension": "minecraft:overworld",
  "x": 100.0,
  "y": 64.0,
  "z": 200.0,
  "yaw": 0.0,
  "pitch": 0.0,
  "gameMode": "survival",
  "state": {
    "attacking": false,
    "using": false,
    "sneaking": false,
    "jumping": false,
    "sprinting": false,
    "forward": 0.0,
    "strafing": 0.0,
    "attackInterval": 0,
    "useInterval": 0,
    "health": 20.0,
    "foodLevel": 20,
    "saturation": 20.0
  }
}
```

---

## 构建项目

### 前置要求
- JDK 17+
- Gradle 8.0+

### 构建命令
```bash
# 构建项目
./gradlew build

# 清理构建
./gradlew clean

# 运行客户端
./gradlew runClient

# 运行服务器
./gradlew runServer

# 生成源代码
./gradlew genSources
```

### 输出文件
构建后的 JAR 文件位于：
```
build/libs/my-bot-mod-1.0.0.jar
```

---

## 开发环境

### IDE 推荐
- **IntelliJ IDEA**（推荐）
- Eclipse
- VS Code

### 导入项目

#### IntelliJ IDEA
1. File > Open
2. 选择项目根目录
3. 等待 Gradle 同步完成
4. Run > Edit Configurations
5. 添加 Gradle 任务：`runClient`

#### Eclipse
1. File > Import > Gradle > Existing Gradle Project
2. 选择项目根目录
3. 等待导入完成

---

### 调试

#### 启动调试
```bash
./gradlew runClient --debug-jvm
```

#### 附加调试器
1. 在 IDE 中创建 Remote JVM Debug 配置
2. Host: `localhost`
3. Port: `5005`
4. 启动调试会话

---

## 代码规范

### 命名规范
- **类名**：大驼峰（PascalCase）
- **方法名**：小驼峰（camelCase）
- **变量名**：小驼峰（camelCase）
- **常量名**：全大写下划线（UPPER_SNAKE_CASE）

### 注释规范
- **类注释**：说明类的职责和用途
- **方法注释**：说明方法的功能、参数和返回值
- **复杂逻辑**：添加行内注释
- **使用中文**：所有注释使用简体中文

### 代码风格
- **缩进**：4 个空格
- **行长度**：不超过 120 字符
- **大括号**：K&R 风格
- **空行**：方法之间空一行

### 示例
```java
/**
 * 假人动作控制器
 * 负责控制假人的所有动作，如攻击、使用物品、移动等
 */
public class BotActionController {
    
    private final BotPlayer bot;
    private boolean attacking = false;
    
    /**
     * 构造函数
     * @param bot 假人实体
     */
    public BotActionController(BotPlayer bot) {
        this.bot = bot;
    }
    
    /**
     * 每 tick 更新动作状态
     */
    public void tick() {
        // 处理攻击动作
        if (attacking) {
            performAttack();
        }
    }
}
```

---

## 测试

### 测试系统
项目包含完整的测试系统，覆盖所有主要功能。

### 运行测试
```
/bot test              # 运行所有测试
/bot test movement     # 测试移动功能
/bot test actions      # 测试动作功能
/bot test skin         # 测试皮肤系统
```

### 测试覆盖
- ✅ 名字验证
- ✅ 创建和删除
- ✅ 移动功能
- ✅ 动作控制
- ✅ 皮肤系统
- ✅ 骑乘功能

### 添加测试
在 `BotCommand.java` 中添加新的测试方法：

```java
private static int testNewFeature(ServerCommandSource source) {
    // 测试逻辑
    return 1;
}
```

---

## 贡献指南

### 提交 Issue
1. 检查是否已有相同 Issue
2. 使用 Issue 模板
3. 提供详细信息：
   - Minecraft 版本
   - 模组版本
   - 错误日志
   - 复现步骤

### 提交 Pull Request
1. Fork 项目
2. 创建功能分支：`git checkout -b feature/new-feature`
3. 提交更改：`git commit -m "Add new feature"`
4. 推送分支：`git push origin feature/new-feature`
5. 创建 Pull Request

### PR 要求
- ✅ 代码符合规范
- ✅ 添加必要的注释
- ✅ 通过所有测试
- ✅ 更新相关文档
- ✅ 提供清晰的 PR 描述

---

## 常见开发问题

### Q: 如何添加新命令？
A: 在 `BotCommand.java` 中添加新的命令方法，并在 `register()` 中注册。

### Q: 如何修改假人行为？
A: 修改 `BotActionController.java` 中的相关方法。

### Q: 如何添加新的配置项？
A: 在 `ModConfig.java` 中添加字段，并在配置界面中添加对应的 UI 元素。

### Q: 如何调试 Mixin？
A: 使用 `System.out.println()` 或日志输出，或使用 IDE 的调试功能。

### Q: 如何测试皮肤系统？
A: 在 `run/skins/` 文件夹中添加测试皮肤文件，然后运行游戏。

---

## 相关资源

### 官方文档
- [Fabric Wiki](https://fabricmc.net/wiki/)
- [Minecraft Wiki](https://minecraft.fandom.com/)
- [Mixin Documentation](https://github.com/SpongePowered/Mixin/wiki)

### 参考项目
- [Carpet Mod](https://github.com/gnembon/fabric-carpet)
- [Fabric API](https://github.com/FabricMC/fabric)

### 社区
- [Fabric Discord](https://discord.gg/v6v4pMv)
- [Minecraft Modding Discord](https://discord.gg/minecraft-modding)

---

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](../LICENSE) 文件。
