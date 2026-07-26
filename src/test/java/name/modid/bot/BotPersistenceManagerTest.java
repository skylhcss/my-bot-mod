package name.modid.bot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BotPersistenceManager 的 NBT 序列化/反序列化一致性测试。
 * 验证 save → CompoundTag → load 循环后数据不丢失、不变形。
 */
class BotPersistenceManagerTest {

    // ==================== 辅助方法 ====================

    /**
     * 构造一个完整的假人 CompoundTag（模拟 saveBot 产出的数据结构）
     */
    private CompoundTag createBotTag(String name, UUID uuid, UUID creatorUUID,
                                     String creatorName, String dimension,
                                     double x, double y, double z,
                                     float yaw, float pitch, String gameMode,
                                     int selectedSlot) {
        CompoundTag data = new CompoundTag();
        data.putString("Name", name);
        data.putUUID("UUID", uuid);
        data.putUUID("CreatorUUID", creatorUUID);
        data.putString("CreatorName", creatorName);
        data.putString("Dimension", dimension);
        data.putDouble("X", x);
        data.putDouble("Y", y);
        data.putDouble("Z", z);
        data.putFloat("Yaw", yaw);
        data.putFloat("Pitch", pitch);
        data.putString("GameMode", gameMode);
        data.putInt("SelectedSlot", selectedSlot);

        // 空物品栏 + 空末影箱
        data.put("Inventory", new ListTag());
        data.put("EnderItems", new ListTag());

        // 默认配置
        CompoundTag settingsTag = new CompoundTag();
        data.put("Settings", settingsTag);

        return data;
    }

    /**
     * 构造包含状态信息的假人 CompoundTag
     */
    private CompoundTag createBotTagWithState(String name) {
        UUID uuid = UUID.randomUUID();
        UUID creatorUUID = UUID.randomUUID();
        CompoundTag data = createBotTag(name, uuid, creatorUUID, "Player1",
                "minecraft:overworld", 100.5, 64.0, -200.3, 90.0f, -15.0f,
                "survival", 3);

        // 添加状态
        CompoundTag state = new CompoundTag();
        state.putBoolean("Attacking", true);
        state.putBoolean("Using", false);
        state.putBoolean("Sneaking", true);
        state.putBoolean("Jumping", false);
        state.putBoolean("Sprinting", true);
        state.putFloat("Forward", 1.0f);
        state.putFloat("Strafing", -0.5f);
        state.putInt("AttackInterval", 20);
        state.putInt("UseInterval", 0);
        state.putFloat("Health", 18.5f);
        state.putInt("FoodLevel", 15);
        state.putFloat("Saturation", 5.0f);
        state.putFloat("Exhaustion", 1.2f);
        state.putInt("XpLevel", 30);
        state.putFloat("XpProgress", 0.75f);
        state.put("Effects", new ListTag());
        data.put("State", state);

        return data;
    }

    /**
     * 执行完整的 save → load 循环
     */
    private BotPersistenceManager saveAndLoad(BotPersistenceManager original) {
        CompoundTag saved = original.save(new CompoundTag());
        return BotPersistenceManager.load(saved);
    }

    // ==================== 测试用例 ====================

    @Test
    @DisplayName("空数据 save/load 循环应返回空的管理器")
    void testEmptySaveLoadCycle() {
        BotPersistenceManager manager = new BotPersistenceManager();

        BotPersistenceManager loaded = saveAndLoad(manager);

        // save 产出的 tag 应包含空 Bots 列表
        CompoundTag tag = manager.save(new CompoundTag());
        assertTrue(tag.contains("Bots"));
        ListTag bots = tag.getList("Bots", 10);
        assertEquals(0, bots.size());
    }

    @Test
    @DisplayName("单个假人 save/load 循环应保持所有基本字段一致")
    void testSingleBotSaveLoadRoundTrip() {
        BotPersistenceManager manager = new BotPersistenceManager();

        UUID uuid = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        UUID creatorUUID = UUID.fromString("abcdefab-abcd-abcd-abcd-abcdefabcdef");
        CompoundTag botTag = createBotTag("TestBot", uuid, creatorUUID, "Creator1",
                "minecraft:the_nether", -50.25, 32.0, 100.75,
                -45.0f, 30.0f, "creative", 7);

        // 模拟 saveBot 行为：放入 botsData
        CompoundTag saveTag = new CompoundTag();
        ListTag botsList = new ListTag();
        botsList.add(botTag.copy());
        saveTag.put("Bots", botsList);

        // load
        BotPersistenceManager loaded = BotPersistenceManager.load(saveTag);

        // 再次 save 验证一致性
        CompoundTag reSaved = loaded.save(new CompoundTag());
        ListTag reList = reSaved.getList("Bots", 10);

        assertEquals(1, reList.size());
        CompoundTag restored = reList.getCompound(0);

        assertEquals("TestBot", restored.getString("Name"));
        assertEquals(uuid, restored.getUUID("UUID"));
        assertEquals(creatorUUID, restored.getUUID("CreatorUUID"));
        assertEquals("Creator1", restored.getString("CreatorName"));
        assertEquals("minecraft:the_nether", restored.getString("Dimension"));
        assertEquals(-50.25, restored.getDouble("X"), 1e-10);
        assertEquals(32.0, restored.getDouble("Y"), 1e-10);
        assertEquals(100.75, restored.getDouble("Z"), 1e-10);
        assertEquals(-45.0f, restored.getFloat("Yaw"), 1e-5f);
        assertEquals(30.0f, restored.getFloat("Pitch"), 1e-5f);
        assertEquals("creative", restored.getString("GameMode"));
        assertEquals(7, restored.getInt("SelectedSlot"));
    }

    @Test
    @DisplayName("多个假人 save/load 循环应保持数量和数据一致")
    void testMultipleBotsSaveLoadRoundTrip() {
        CompoundTag saveTag = new CompoundTag();
        ListTag botsList = new ListTag();

        // 添加 3 个不同假人
        for (int i = 1; i <= 3; i++) {
            CompoundTag bot = createBotTag("Bot" + i,
                    UUID.randomUUID(), UUID.randomUUID(), "Player" + i,
                    "minecraft:overworld", i * 100.0, 64.0, i * -50.0,
                    i * 30.0f, 0.0f, "survival", i - 1);
            botsList.add(bot);
        }
        saveTag.put("Bots", botsList);

        // load → save 循环
        BotPersistenceManager loaded = BotPersistenceManager.load(saveTag);
        CompoundTag reSaved = loaded.save(new CompoundTag());
        ListTag reList = reSaved.getList("Bots", 10);

        assertEquals(3, reList.size());

        // 验证每个假人名称存在（顺序可能不同，用名称集合验证）
        java.util.Set<String> names = new java.util.HashSet<>();
        for (int i = 0; i < reList.size(); i++) {
            names.add(reList.getCompound(i).getString("Name"));
        }
        assertTrue(names.contains("Bot1"));
        assertTrue(names.contains("Bot2"));
        assertTrue(names.contains("Bot3"));
    }

    @Test
    @DisplayName("load 应将假人名按小写存储（大小写不敏感查找）")
    void testNameCaseInsensitiveStorage() {
        CompoundTag saveTag = new CompoundTag();
        ListTag botsList = new ListTag();

        CompoundTag bot = createBotTag("MixedCaseBot",
                UUID.randomUUID(), UUID.randomUUID(), "Player",
                "minecraft:overworld", 0, 64, 0, 0, 0, "survival", 0);
        botsList.add(bot);
        saveTag.put("Bots", botsList);

        BotPersistenceManager loaded = BotPersistenceManager.load(saveTag);

        // save 后数据仍应完整（Name 字段保持原始大小写）
        CompoundTag reSaved = loaded.save(new CompoundTag());
        ListTag reList = reSaved.getList("Bots", 10);
        assertEquals(1, reList.size());
        assertEquals("MixedCaseBot", reList.getCompound(0).getString("Name"));
    }

    @Test
    @DisplayName("包含状态数据的假人 save/load 循环应保持状态一致")
    void testBotWithStateSaveLoadRoundTrip() {
        CompoundTag botTag = createBotTagWithState("StatefulBot");

        CompoundTag saveTag = new CompoundTag();
        ListTag botsList = new ListTag();
        botsList.add(botTag.copy());
        saveTag.put("Bots", botsList);

        BotPersistenceManager loaded = BotPersistenceManager.load(saveTag);
        CompoundTag reSaved = loaded.save(new CompoundTag());
        ListTag reList = reSaved.getList("Bots", 10);

        assertEquals(1, reList.size());
        CompoundTag restored = reList.getCompound(0);
        assertTrue(restored.contains("State"));

        CompoundTag state = restored.getCompound("State");
        assertTrue(state.getBoolean("Attacking"));
        assertFalse(state.getBoolean("Using"));
        assertTrue(state.getBoolean("Sneaking"));
        assertFalse(state.getBoolean("Jumping"));
        assertTrue(state.getBoolean("Sprinting"));
        assertEquals(1.0f, state.getFloat("Forward"), 1e-5f);
        assertEquals(-0.5f, state.getFloat("Strafing"), 1e-5f);
        assertEquals(20, state.getInt("AttackInterval"));
        assertEquals(0, state.getInt("UseInterval"));
        assertEquals(18.5f, state.getFloat("Health"), 1e-5f);
        assertEquals(15, state.getInt("FoodLevel"));
        assertEquals(5.0f, state.getFloat("Saturation"), 1e-5f);
        assertEquals(1.2f, state.getFloat("Exhaustion"), 1e-3f);
        assertEquals(30, state.getInt("XpLevel"));
        assertEquals(0.75f, state.getFloat("XpProgress"), 1e-5f);
    }

    @Test
    @DisplayName("空 Name 的 CompoundTag 应被 load 忽略")
    void testEmptyNameIsIgnoredOnLoad() {
        CompoundTag saveTag = new CompoundTag();
        ListTag botsList = new ListTag();

        // 一个有效的假人
        CompoundTag validBot = createBotTag("ValidBot",
                UUID.randomUUID(), UUID.randomUUID(), "Player",
                "minecraft:overworld", 0, 64, 0, 0, 0, "survival", 0);
        botsList.add(validBot);

        // 一个 Name 为空的无效数据
        CompoundTag invalidBot = new CompoundTag();
        invalidBot.putString("Name", "");
        botsList.add(invalidBot);

        saveTag.put("Bots", botsList);

        BotPersistenceManager loaded = BotPersistenceManager.load(saveTag);
        CompoundTag reSaved = loaded.save(new CompoundTag());
        ListTag reList = reSaved.getList("Bots", 10);

        // 只有有效假人被保留
        assertEquals(1, reList.size());
        assertEquals("ValidBot", reList.getCompound(0).getString("Name"));
    }

    @Test
    @DisplayName("不含 Bots 键的 tag 应安全加载为空")
    void testLoadWithoutBotsKey() {
        CompoundTag emptyTag = new CompoundTag();

        BotPersistenceManager loaded = BotPersistenceManager.load(emptyTag);
        CompoundTag reSaved = loaded.save(new CompoundTag());
        ListTag reList = reSaved.getList("Bots", 10);

        assertEquals(0, reList.size());
    }

    @Test
    @DisplayName("save/load 循环应保持物品栏和末影箱 NBT 结构完整")
    void testInventoryAndEnderChestPreservation() {
        CompoundTag botTag = createBotTag("InvBot",
                UUID.randomUUID(), UUID.randomUUID(), "Player",
                "minecraft:overworld", 0, 64, 0, 0, 0, "survival", 4);

        // 模拟物品栏数据
        ListTag inventory = new ListTag();
        CompoundTag item1 = new CompoundTag();
        item1.putByte("Slot", (byte) 0);
        item1.putString("id", "minecraft:diamond_sword");
        item1.putByte("Count", (byte) 1);
        inventory.add(item1);
        botTag.put("Inventory", inventory);

        // 模拟末影箱数据
        ListTag enderItems = new ListTag();
        CompoundTag item2 = new CompoundTag();
        item2.putByte("Slot", (byte) 5);
        item2.putString("id", "minecraft:ender_pearl");
        item2.putByte("Count", (byte) 16);
        enderItems.add(item2);
        botTag.put("EnderItems", enderItems);

        CompoundTag saveTag = new CompoundTag();
        ListTag botsList = new ListTag();
        botsList.add(botTag.copy());
        saveTag.put("Bots", botsList);

        BotPersistenceManager loaded = BotPersistenceManager.load(saveTag);
        CompoundTag reSaved = loaded.save(new CompoundTag());
        CompoundTag restored = reSaved.getList("Bots", 10).getCompound(0);

        // 物品栏
        ListTag restoredInv = restored.getList("Inventory", 10);
        assertEquals(1, restoredInv.size());
        assertEquals("minecraft:diamond_sword", restoredInv.getCompound(0).getString("id"));
        assertEquals((byte) 1, restoredInv.getCompound(0).getByte("Count"));

        // 末影箱
        ListTag restoredEnder = restored.getList("EnderItems", 10);
        assertEquals(1, restoredEnder.size());
        assertEquals("minecraft:ender_pearl", restoredEnder.getCompound(0).getString("id"));
        assertEquals((byte) 16, restoredEnder.getCompound(0).getByte("Count"));
        assertEquals((byte) 5, restoredEnder.getCompound(0).getByte("Slot"));
    }

    @Test
    @DisplayName("同名假人（不同大小写）save 后 load 应合并为一条记录")
    void testDuplicateNameMergesOnLoad() {
        CompoundTag saveTag = new CompoundTag();
        ListTag botsList = new ListTag();

        // 两个同名（大小写不同）假人
        CompoundTag bot1 = createBotTag("TestBot",
                UUID.randomUUID(), UUID.randomUUID(), "P1",
                "minecraft:overworld", 10, 64, 10, 0, 0, "survival", 0);
        CompoundTag bot2 = createBotTag("TESTBOT",
                UUID.randomUUID(), UUID.randomUUID(), "P2",
                "minecraft:the_nether", 20, 32, 20, 0, 0, "creative", 1);
        botsList.add(bot1);
        botsList.add(bot2);
        saveTag.put("Bots", botsList);

        BotPersistenceManager loaded = BotPersistenceManager.load(saveTag);
        CompoundTag reSaved = loaded.save(new CompoundTag());
        ListTag reList = reSaved.getList("Bots", 10);

        // 后者覆盖前者（Map key 为小写），只保留 1 条
        assertEquals(1, reList.size());
        // 最终保留的是后写入的 TESTBOT
        assertEquals("TESTBOT", reList.getCompound(0).getString("Name"));
    }
}
