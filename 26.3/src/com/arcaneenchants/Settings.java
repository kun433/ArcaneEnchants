package com.arcaneenchants;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * config.yml 的强类型视图。所有开关与概率都集中在这里，改完执行 {@code /ae reload} 即可生效。
 */
public final class Settings {

    /** 附魔显示语言，zh_CN 或 en_US（只在启动时读取，改语言需重启）。 */
    public String language = "zh_CN";

    // ---------- 战利品 ----------
    public boolean lootEnabled = true;
    public double simpleLootChance = 0.06D;
    public double complexLootChance = 0.008D;
    public int maxBooksPerContainer = 1;
    public boolean allowFishingLoot = false;
    public List<String> lootTableBlacklist = new ArrayList<>();

    // ---------- 村民交易 ----------
    public boolean tradesEnabled = true;
    public double tradeChance = 0.35D;
    public int tradeEmeralds = 24;
    public int tradeMaxUses = 3;
    public int tradeMinVillagerLevel = 3;
    public boolean requireLibrarian = true;

    // ---------- 属性 ----------
    public boolean attributesEnabled = true;
    public int syncIntervalTicks = 100;

    // ---------- 行为 ----------
    public boolean eternalDurability = true;
    public boolean greedExtraDrops = true;
    public boolean lifestealEnabled = true;
    public boolean warderEnabled = true;
    public boolean igniteEnabled = true;
    public int lifestealCooldownMs = 500;
    public int warderCooldownSeconds = 20;

    public static Settings load(FileConfiguration config) {
        Settings s = new Settings();
        s.language = config.getString("language", "zh_CN");

        s.lootEnabled = config.getBoolean("loot.enabled", true);
        s.simpleLootChance = clamp(config.getDouble("loot.simple-chance", 0.06D), 0.0D, 1.0D);
        s.complexLootChance = clamp(config.getDouble("loot.complex-chance", 0.008D), 0.0D, 1.0D);
        s.maxBooksPerContainer = Math.max(1, config.getInt("loot.max-books-per-container", 1));
        s.allowFishingLoot = config.getBoolean("loot.allow-fishing", false);
        s.lootTableBlacklist = new ArrayList<>(config.getStringList("loot.table-blacklist"));

        s.tradesEnabled = config.getBoolean("trades.enabled", true);
        s.tradeChance = clamp(config.getDouble("trades.chance", 0.35D), 0.0D, 1.0D);
        s.tradeEmeralds = Math.max(1, config.getInt("trades.emeralds", 24));
        s.tradeMaxUses = Math.max(1, config.getInt("trades.max-uses", 3));
        s.tradeMinVillagerLevel = Math.max(1, Math.min(5, config.getInt("trades.min-villager-level", 3)));
        s.requireLibrarian = config.getBoolean("trades.require-librarian", true);

        s.attributesEnabled = config.getBoolean("attributes.enabled", true);
        s.syncIntervalTicks = Math.max(20, config.getInt("attributes.sync-interval-ticks", 100));

        s.eternalDurability = config.getBoolean("behaviors.eternal-durability", true);
        s.greedExtraDrops = config.getBoolean("behaviors.greed-extra-drops", true);
        s.lifestealEnabled = config.getBoolean("behaviors.bloodpact-lifesteal", true);
        s.warderEnabled = config.getBoolean("behaviors.warder-low-health", true);
        s.igniteEnabled = config.getBoolean("behaviors.resonance-ignite", true);
        s.lifestealCooldownMs = Math.max(0, config.getInt("behaviors.bloodpact-cooldown-ms", 500));
        s.warderCooldownSeconds = Math.max(1, config.getInt("behaviors.warder-cooldown-seconds", 20));

        return s;
    }

    /** 当前语言是否英文。 */
    public boolean english() {
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("en");
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
