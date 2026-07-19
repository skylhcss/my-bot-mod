# 配置指南

## 配置文件

位置：`.minecraft/config/my-bot-mod.json`（JSON，可手动编辑；改后用 `/botmod config reload` 重载）。若文件损坏会自动回退默认配置。

```json
{
  "enableBotFeature": true,
  "configMenuKey": "key.keyboard.b",
  "attackReachDistance": 3.0,
  "creativeAttackReachDistance": 5.0,
  "enableKillAura": false,
  "killAuraRange": 3.0,
  "mountWhitelist": ["minecraft:pig", "minecraft:horse", "minecraft:boat"],
  "allowMountOtherBots": false,
  "maxBotCount": 0,
  "allowNonOpControlBot": false,
  "autoRespawnOnDeath": false,
  "botTakeDamage": true,
  "botHunger": true,
  "allowBotAutoJump": true,
  "allowBatonTeleportNonCreative": false,
  "botPersistence": false,
  "preserveBotState": false,
  "carpetModCompatibility": true
}
```

## 打开配置

- **全局配置界面**：按 **B** 键（可在 游戏设置 → 控制 → 按键绑定 中改键）。界面分类卡片可折叠，"假人"标签页列出所有假人。
- **每假人设置面板**：**右键假人**打开——左侧为操作（背包/末影箱/游戏模式/手持槽位/停止/传送/删除），右侧为**个人配置**。

## 配置项

| 配置项 | 类型 | 默认 | 说明 |
|--------|------|------|------|
| `enableBotFeature` | bool | true | 总开关，关闭后无法创建假人 |
| `attackReachDistance` | 数字 | 3.0 | 生存模式攻击/破坏距离 |
| `creativeAttackReachDistance` | 数字 | 5.0 | 创造模式攻击/破坏距离 |
| `enableKillAura` | bool | false | 开启后攻击范围内所有实体（否则仅视线目标） |
| `killAuraRange` | 数字 | 3.0 | 杀戮光环范围 |
| `mountWhitelist` | 字符串数组 | 常见坐骑 | 可骑乘的实体类型 ID |
| `allowMountOtherBots` | bool | false | 是否允许假人骑乘其他假人 |
| `maxBotCount` | 整数 | 0 | 假人数量上限（0=无限） |
| `allowNonOpControlBot` | bool | false | 允许非 OP 玩家**创建并控制**假人（v1.3.1 由 `allowNonOpCreateBot` 更名，旧配置自动迁移） |
| `autoRespawnOnDeath` | bool | false | 假人死亡后自动重生（回重生点或创建者处，含跨维度） |
| `botTakeDamage` | bool | true | 假人是否受伤害 |
| `botHunger` | bool | true | 假人是否会饥饿 |
| `allowBotAutoJump` | bool | true | 移动时自动跳过 1 格高障碍 |
| `allowBatonTeleportNonCreative` | bool | false | 指挥棒传送模式是否允许非创造模式使用（v1.3.1 新增） |
| `botPersistence` | bool | false | 退出世界后假人是否保留 |
| `preserveBotState` | bool | false | 驻留时是否保留退出前的动作/状态 |
| `carpetModCompatibility` | bool | true | 检测到 Carpet Mod 时自动禁用本模组假人功能以避免冲突 |

配置修改后**立即生效**。数值范围：距离 0-10，`maxBotCount` ≥ 0。

## 假人个人配置（三态）

在假人设置面板右侧，可为**单个假人**单独设置：受到伤害、会饥饿、死亡自动重生、自动跳跃、杀戮光环。每项为三态——**继承**（用全局）/ **强制开** / **强制关**，**优先于全局配置**，并随假人驻留保存。

## 假人驻留

启用 `botPersistence` 后，假人数据以 NBT 存于 `world/data/my_bot_mod_bots.dat`（SavedData，每世界独立，避免同名存档冲突），在首位玩家加入时加载，并用区块加载票据保持假人所在区块常驻（跨维度，定期刷新）。保存内容含位置/维度/游戏模式/物品栏/末影箱/手持槽位/经验/药水效果等。开启 `preserveBotState` 还会保留退出前的动作与移动状态。

## 推荐配置

**单人**：`botPersistence`/`preserveBotState`/`autoRespawnOnDeath` = true，`botTakeDamage`/`botHunger` = false。
**多人服务器**：`maxBotCount` = 10，`allowNonOpControlBot` = false，其余保持默认。

---

## 相关文档
[命令参考](COMMANDS.md) · [皮肤系统](SKINS.md) · [常见问题](FAQ.md)
