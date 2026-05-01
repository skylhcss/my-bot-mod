# 假人皮肤文件夹说明

注意：这个文件夹中的文件仅用于开发参考。

在实际运行时，Minecraft 会从 `run/temporary` 文件夹加载皮肤文件。

## 支持的皮肤格式

1. **PNG 文件**（推荐）
   - 64x64 或 64x32 像素
   - 标准 Minecraft 皮肤格式
   - 文件名任意（例如：bot1.png, steve.png）

2. **Base64 编码的 .txt 文件**
   - 包含 Mojang 皮肤 API 返回的 Base64 编码数据
   - 文件名任意（例如：skin1.txt）
   - 不要命名为 README.txt

## 皮肤优先级

当创建假人时，系统会按以下顺序尝试获取皮肤：

1. **Mojang API**：如果假人名字是正版玩家名，尝试获取其皮肤
2. **PNG 文件**：从 run/temporary 文件夹随机选择一个 PNG 文件
3. **Base64 文件**：从 run/temporary 文件夹随机选择一个 .txt 文件
4. **默认皮肤**：如果以上都失败，使用 Minecraft 默认皮肤（Steve/Alex）

## 如何添加皮肤

1. 将 PNG 皮肤文件复制到 `run/temporary` 文件夹
2. 重启游戏或使用 `/bot reload` 命令（如果实现了）
3. 创建新的假人时会自动应用随机皮肤
