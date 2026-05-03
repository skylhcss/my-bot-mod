# 皮肤系统

本文档详细说明 My Bot Mod 的皮肤系统。

## 目录
- [皮肤优先级](#皮肤优先级)
- [添加自定义皮肤](#添加自定义皮肤)
- [皮肤文件格式](#皮肤文件格式)
- [皮肤文件夹位置](#皮肤文件夹位置)
- [故障排除](#故障排除)

---

## 皮肤优先级

假人皮肤按以下优先级加载：

### 1. Mojang API（最高优先级）
如果假人名字是正版 Minecraft 玩家名，系统会尝试从 Mojang API 获取该玩家的皮肤。

**示例**：
```
/bot Notch spawn      # 会尝试获取 Notch 的皮肤
/bot Steve spawn      # 会尝试获取 Steve 的皮肤
```

**优点**：
- 自动获取正版玩家皮肤
- 无需手动配置

**缺点**：
- 需要网络连接
- 可能失败（玩家不存在、网络问题）

---

### 2. PNG 文件（第二优先级）
如果 Mojang API 获取失败，系统会从 `skins/` 文件夹随机选择一个 PNG 文件。

**优点**：
- 离线可用
- 支持自定义皮肤
- 随机选择增加多样性

**缺点**：
- 需要手动添加文件

---

### 3. Base64 编码文件（第三优先级）
如果没有 PNG 文件，系统会从 `skins/` 文件夹的 .txt 文件加载 Base64 编码的皮肤数据。

**优点**：
- 可以保存完整的皮肤数据（包括签名）
- 适合从 Mojang API 导出的数据

**缺点**：
- 需要手动获取 Base64 数据
- 不如 PNG 文件直观

---

### 4. 默认皮肤（最低优先级）
如果以上都失败，使用 Minecraft 默认皮肤（Steve 或 Alex）。

---

## 添加自定义皮肤

### 方法 1：使用 PNG 文件（推荐）

#### 步骤 1：准备皮肤文件
- 格式：PNG
- 尺寸：64x64 或 64x32 像素
- 标准 Minecraft 皮肤格式

#### 步骤 2：放入皮肤文件夹
将 PNG 文件放入 `skins/` 文件夹。

**文件夹位置**：
- 开发环境：`run/skins/`
- 生产环境：`.minecraft/skins/`

#### 步骤 3：重启游戏或重新加载
- 重启游戏
- 或使用 `/bot reload` 命令（如果可用）

#### 示例
```
skins/
├── bot1.png
├── bot2.png
├── bot3.png
└── steve.png
```

---

### 方法 2：使用 Base64 编码

#### 步骤 1：获取 Base64 数据
从 Mojang API 获取皮肤数据：
```
https://sessionserver.mojang.com/session/minecraft/profile/<UUID>?unsigned=false
```

#### 步骤 2：提取 textures 值
从返回的 JSON 中提取 `properties[0].value` 字段。

#### 步骤 3：保存为 .txt 文件
将 Base64 字符串保存为 .txt 文件（不要命名为 README.txt）。

#### 步骤 4：放入皮肤文件夹
将 .txt 文件放入 `skins/` 文件夹。

#### 示例
```
skins/
├── skin1.txt
├── skin2.txt
└── README.txt
```

**skin1.txt 内容示例**：
```
ewogICJ0aW1lc3RhbXAiIDogMTYxMjM0NTY3ODkwMCwKICAicHJvZmlsZUlkIiA6ICI...
```

---

## 皮肤文件格式

### PNG 文件格式

#### 标准格式（64x64）
```
+--------+--------+--------+--------+
|  头部  |  头部  |  头部  |  头部  |
|  顶部  |  底部  |  右侧  |  左侧  |
+--------+--------+--------+--------+
|  身体  |  身体  |  身体  |  身体  |
|  前面  |  后面  |  右侧  |  左侧  |
+--------+--------+--------+--------+
|  右臂  |  右臂  |  右臂  |  右臂  |
|  前面  |  后面  |  右侧  |  左侧  |
+--------+--------+--------+--------+
|  左臂  |  左臂  |  左臂  |  左臂  |
|  前面  |  后面  |  右侧  |  左侧  |
+--------+--------+--------+--------+
```

#### 旧格式（64x32）
只包含头部、身体和四肢的基本纹理。

---

### Base64 文件格式

Base64 编码的 JSON 数据，包含：
- `timestamp`：时间戳
- `profileId`：玩家 UUID
- `profileName`：玩家名字
- `textures`：纹理 URL

**解码后的 JSON 示例**：
```json
{
  "timestamp": 1612345678900,
  "profileId": "069a79f444e94726a5befca90e38aaf5",
  "profileName": "Notch",
  "textures": {
    "SKIN": {
      "url": "http://textures.minecraft.net/texture/..."
    }
  }
}
```

---

## 皮肤文件夹位置

### 自动检测
系统会按以下顺序检测皮肤文件夹：

1. `./skins`
2. `./run/skins`
3. `run/skins`（相对路径）
4. `skins`（相对路径）

### 开发环境
```
项目根目录/run/skins/
```

### 生产环境
```
.minecraft/skins/
```

### 查看日志
启动游戏时，日志会显示：
```
[INFO] (my-bot-mod) 游戏目录: /path/to/minecraft
[INFO] (my-bot-mod) 检查皮肤文件夹: /path/to/minecraft/skins
[INFO] (my-bot-mod) 找到皮肤文件夹: /path/to/minecraft/skins
[INFO] (my-bot-mod) 成功加载 3 个皮肤文件（PNG: 3, Base64: 0）
```

---

## 故障排除

### 问题 1：皮肤没有加载

**症状**：
- 假人使用默认皮肤
- 日志显示"没有找到任何皮肤文件"

**解决方案**：
1. 检查皮肤文件夹位置
2. 查看日志中的路径
3. 确保 PNG 文件格式正确
4. 重启游戏

---

### 问题 2：PNG 文件无法识别

**症状**：
- PNG 文件存在但未加载
- 日志显示 0 个 PNG 文件

**解决方案**：
1. 确保文件扩展名是 `.png`（小写）
2. 确保文件不是损坏的
3. 确保文件尺寸是 64x64 或 64x32
4. 检查文件权限

---

### 问题 3：Base64 文件无法加载

**症状**：
- .txt 文件存在但未加载
- 日志显示加载失败

**解决方案**：
1. 确保文件不是 `README.txt`
2. 确保 Base64 字符串完整
3. 确保没有多余的空格或换行
4. 使用在线工具验证 Base64 格式

---

### 问题 4：皮肤显示不正确

**症状**：
- 皮肤加载了但显示错误
- 纹理错位或缺失

**解决方案**：
1. 确保使用标准 Minecraft 皮肤格式
2. 检查 PNG 文件尺寸
3. 使用皮肤编辑器验证
4. 尝试其他皮肤文件

---

### 问题 5：Mojang API 获取失败

**症状**：
- 日志显示"从 Mojang API 获取失败"
- 使用了备用皮肤

**原因**：
- 网络连接问题
- 玩家名不存在
- Mojang 服务器维护

**解决方案**：
- 检查网络连接
- 确认玩家名正确
- 等待 Mojang 服务器恢复
- 使用 PNG 文件作为备用

---

## 皮肤工具推荐

### 皮肤编辑器
- **Minecraft Skin Editor**：在线编辑器
- **Nova Skin**：功能强大的在线工具
- **Skinseed**：移动端应用

### 皮肤下载网站
- **NameMC**：查看和下载玩家皮肤
- **MinecraftSkins**：皮肤库
- **Planet Minecraft**：社区皮肤

### 皮肤转换工具
- **Base64 Encoder/Decoder**：在线转换工具
- **Skin Converter**：格式转换

---

## 高级用法

### 为特定假人指定皮肤

虽然系统是随机选择皮肤，但你可以：

1. **使用正版玩家名**：
   ```
   /bot Notch spawn    # 使用 Notch 的皮肤
   ```

2. **只放一个 PNG 文件**：
   系统会始终使用该文件

3. **使用文件名匹配**（未来功能）：
   ```
   /bot Steve spawn    # 自动查找 steve.png
   ```

---

### 批量添加皮肤

1. 下载多个皮肤文件
2. 重命名为有意义的名字
3. 批量复制到 `skins/` 文件夹
4. 重启游戏

**示例**：
```
skins/
├── steve.png
├── alex.png
├── zombie.png
├── skeleton.png
├── creeper.png
└── enderman.png
```

---

### 导出 Mojang 皮肤

使用以下 API 获取皮肤数据：

1. **获取 UUID**：
   ```
   https://api.mojang.com/users/profiles/minecraft/<玩家名>
   ```

2. **获取皮肤数据**：
   ```
   https://sessionserver.mojang.com/session/minecraft/profile/<UUID>?unsigned=false
   ```

3. **提取 Base64**：
   从 `properties[0].value` 字段提取

4. **保存为 .txt**：
   保存到 `skins/` 文件夹

---

## 相关文档

- [命令参考](COMMANDS.md) - 所有命令的详细说明
- [配置指南](CONFIG.md) - 配置系统详细说明
- [常见问题](FAQ.md) - 常见问题解答
