package name.modid.client.screen.widget;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 配置界面搜索用的双语索引。
 * 从 jar 内的 en_us.json / zh_cn.json 一次性加载全部翻译，
 * 使搜索无论游戏语言为何都能同时匹配中文与英文（以及翻译键本身）。
 */
public final class SearchLang {

    private SearchLang() {}

    private static Map<String, String> en;
    private static Map<String, String> zh;

    private static void ensureLoaded() {
        if (en != null) {
            return;
        }
        en = load("/assets/my-bot-mod/lang/en_us.json");
        zh = load("/assets/my-bot-mod/lang/zh_cn.json");
    }

    private static Map<String, String> load(String path) {
        Map<String, String> map = new HashMap<>();
        try (InputStream in = SearchLang.class.getResourceAsStream(path)) {
            if (in != null) {
                JsonObject obj = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> e : obj.entrySet()) {
                    map.put(e.getKey(), e.getValue().getAsString().toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception ignored) {
            // 加载失败时退化为仅按当前语言标签搜索
        }
        return map;
    }

    /** 提取一个 Component 的翻译键（若为 translatable），否则返回 null */
    public static String keyOf(Component c) {
        if (c != null && c.getContents() instanceof TranslatableContents tc) {
            return tc.getKey();
        }
        return null;
    }

    /** 一段文本（当前语言）是否包含查询串 */
    public static boolean textContains(String text, String queryLower) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(queryLower);
    }

    /** 某控件的标签是否匹配查询（当前语言标签 + 翻译键 + 中英双语译文） */
    public static boolean widgetMatches(AbstractWidget w, String queryLower) {
        if (w == null) {
            return false;
        }
        Component msg = w.getMessage();
        if (msg != null && textContains(msg.getString(), queryLower)) {
            return true;
        }
        return keyMatches(keyOf(msg), queryLower);
    }

    /** 某翻译键是否匹配查询（键名本身 + 中英双语译文） */
    public static boolean keyMatches(String key, String queryLower) {
        if (key == null) {
            return false;
        }
        if (key.toLowerCase(Locale.ROOT).contains(queryLower)) {
            return true;
        }
        ensureLoaded();
        String e = en.get(key);
        if (e != null && e.contains(queryLower)) {
            return true;
        }
        String z = zh.get(key);
        return z != null && z.contains(queryLower);
    }
}
