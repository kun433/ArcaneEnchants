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

    /** 取一条带占位符的文本。 */
    public String format(String key, Object... args) {
        return String.format(raw(key), args);
    }
}
