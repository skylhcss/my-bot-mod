# 皮肤系统

假人皮肤按优先级回退加载：

1. **Mojang API** — 若假人名字是正版玩家名，获取其皮肤（需联网，可能失败）。
2. **PNG 文件** — 从 `skins/` 文件夹随机选一个 PNG（离线可用，推荐）。
3. **Base64 .txt** — 从 `skins/` 文件夹加载 Base64 编码皮肤数据（含签名，适合从 Mojang 导出）。
4. **默认皮肤** — 以上都失败时使用 Steve/Alex。

## 皮肤文件夹

自动检测顺序：`./skins` → `./run/skins` → `run/skins` → `skins`。
- 开发环境：`项目根目录/run/skins/`
- 生产环境：`.minecraft/skins/`

启动日志会打印检测到的文件夹与加载数量。

## 添加 PNG 皮肤（推荐）

1. 准备标准 Minecraft 皮肤 PNG（**64x64** 或 64x32）。
2. 放入 `skins/` 文件夹（可放多个，随机选用）。
3. 重启游戏。之后创建的假人会随机使用。

```
skins/
├── bot1.png
├── bot2.png
└── steve.png
```

> 注意：尺寸必须为 64x64 或 64x32，扩展名小写 `.png`；不合规的文件会被跳过（客户端会负缓存，避免每帧重试）。

## 添加 Base64 皮肤

1. 取玩家 UUID：原 `https://api.mojang.com/users/profiles/minecraft/<玩家名>` 端点已被 Mojang 退役，可改用第三方镜像（如 `https://playerdb.co/api/player/minecraft/<玩家名>`）或从游戏内获取。
2. 取皮肤数据：`https://sessionserver.mojang.com/session/minecraft/profile/<UUID>?unsigned=false`
3. 提取 `properties[0].value`（Base64 字符串）。
4. 存为 `.txt` 放入 `skins/`（**不要**命名为 `README.txt`）。

## 为特定假人指定皮肤

- 用**正版玩家名**创建：`/bot Notch spawn` 使用 Notch 的皮肤。
- 只放**一个** PNG：所有假人都用它。

## 故障排除

- **皮肤没加载 / 显示默认**：检查文件夹位置与日志路径，确认 PNG 尺寸/扩展名正确，重启游戏。
- **Base64 未加载**：确认不是 `README.txt`、字符串完整无多余空白。
- **Mojang 获取失败**：检查网络与玩家名是否存在；会自动回退到 PNG/默认皮肤。注意：模组内置的名字→UUID 查询端点（api.mojang.com）已被 Mojang 退役，正版皮肤获取可能因此失败，建议直接用 PNG 方式。

---

## 相关文档
[命令参考](COMMANDS.md) · [配置指南](CONFIG.md) · [常见问题](FAQ.md)
