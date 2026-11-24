package com.arcaneenchants;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * 指令与提示文本，取自 config.yml 的 {@code messages.<语言>.*}，方便服主自行改写。
 */
public final class Msg {

    private final FileConfiguration config;
    private final Settings settings;

    public Msg(FileConfiguration config, Settings settings) {
        this.config = config;
        this.settings = settings;
    }

    /** 取一条原始文本（未格式化）。 */
    public String raw(String key) {
        String lang = settings.english() ? "en" : "zh";
        String value = config.getString("messages." + lang + "." + key);
        if (value == null) {
            value = config.getString("messages.zh." + key, key);
        }
        return value;
    }

    /** 取一条带占位符的文本。
     *
     * <p>无参调用直接返回原文：消息里常见的字面 {@code %}（例如「0.8% 稀有战利品」）
     * 不再被 {@link String#format} 当成格式说明符而抛异常。有参时若文本与参数不匹配，
     * 也遇错退回原文，保证指令不会因为一条消息把整个子命令炸掉。
     */
    public String format(String key, Object... args) {
        String text = raw(key);
        if (args == null || args.length == 0) {
            // 无参不走 String.format；为了让「%% 表示一个百分号」的规则在有参/无参两条路径上一致，
            // 这里手动反转义一次
            return text.replace("%%", "%");
        }
        try {
            return String.format(text, args);
        } catch (java.util.IllegalFormatException ignored) {
            return text.replace("%%", "%");
        }
    }
}
