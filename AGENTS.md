# AGENTS.md — Agent 操作边界声明

## 项目简介

My Bot Mod 是一个 Minecraft Fabric 模组（1.20.1），用于在服务端生成和管理假人（Bot）。

- **技术栈**: Fabric 1.20.1, Java 17, Gradle + Fabric Loom
- **构建命令**: `./gradlew build`

## 核心文件边界

以下目录中的文件为项目核心逻辑，修改时必须格外谨慎：

- `src/main/java/name/modid/bot/` — 假人核心系统（BotPlayer、BotManager、BotActionController、BotPathfinder、BotPersistenceManager、BotSkinManager、BotSettings、FakeServerGamePacketListenerImpl）

## 操作约束

1. **修改核心文件后必须确保编译通过** — 执行 `./gradlew build` 验证。
2. **禁止删除或重命名 Mixin 类** — 包括：
   - `src/main/java/name/modid/mixin/ServerPlayerMixin.java`
   - `src/client/java/name/modid/client/mixin/ClientPacketListenerMixin.java`
   - `src/client/java/name/modid/client/mixin/MouseHandlerMixin.java`
   - `src/client/java/name/modid/client/mixin/PlayerInfoMixin.java`
3. **不得跨维度寻路** — 假人寻路不可跨维度。
4. **不得随意修改 Mixin 注入点** — 变更注入点可能导致运行时崩溃。
