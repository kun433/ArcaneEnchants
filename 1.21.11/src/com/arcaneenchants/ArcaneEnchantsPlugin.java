package com.arcaneenchants;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ArcaneEnchants 主类。
 *
 * <p>附魔本身在 bootstrap 阶段（{@link ArcaneEnchantsBootstrap}）注册完毕，
 * 这里负责运行期行为：属性同步、附魔行为、获取途径、管理指令与配置重载。
 */
public final class ArcaneEnchantsPlugin extends JavaPlugin {

    private Settings settings;
    private Msg msg;
    private BukkitTask syncTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = Settings.load(getConfig());
        this.msg = new Msg(getConfig(), settings);

        Enchants.resolve();

        for (String note : ArcaneEnchantsBootstrap.notes()) {
            getLogger().info(note);
        }

        if (Enchants.missing().isEmpty()) {
            getLogger().info("已解析 " + Enchants.resolvedCount() + " 个附魔");
        } else if (Enchants.resolvedCount() == 0) {
            getLogger().severe("服务端注册表里找不到任何本插件附魔：请确认服务端为 Leaf/Paper 1.21.11，"
                    + "并且 paper-plugin.yml 的 bootstrapper 已随插件加载。插件将保持静默。");
            return;
        } else {
            getLogger().warning("以下附魔未能在注册表中找到（相关功能自动跳过）：" + String.join(", ", Enchants.missing()));
        }

        warnIfTagsMissing();
        registerPermission();

        getServer().getPluginManager().registerEvents(new ListenerBehaviors(this), this);
        getServer().getPluginManager().registerEvents(new ListenerAcquisition(this), this);
        getServer().getPluginManager().registerEvents(new ListenerSync(this), this);

        registerCommand();
        startSyncTask();

        logSummary();
    }

    @Override
    public void onDisable() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
    }

    /** 重载配置（{@code /ae reload}）。 */
    public void reload() {
        reloadConfig();
        this.settings = Settings.load(getConfig());
        this.msg = new Msg(getConfig(), settings);
        if (syncTask != null) {
            syncTask.cancel();
        }
        startSyncTask();
        getLogger().info("配置已重载");
    }

    public Settings settings() {
        return settings;
    }

    public Msg msg() {
        return msg;
    }

    /** 附魔显示语言。 */
    public String language() {
        return settings == null ? "zh_CN" : settings.language;
    }

    /**
     * 自检：简单附魔应当出现在原版「可发现」标签里（附魔台 / 随机战利品 / 交易 / 刷怪装备）。
     * 标签注入没生效时打印告警，玩家仍可通过战利品箱与铁砧获得，不会静默失效。
     */
    private void warnIfTagsMissing() {
        List<String> missing = new ArrayList<>();
        for (EnchantDef def : Defs.byTier(Tier.SIMPLE)) {
            var enchantment = Enchants.get(def.id());
            if (enchantment == null) {
                continue;
            }
            try {
                if (!enchantment.isDiscoverable()) {
                    missing.add(def.id());
                }
            } catch (Throwable ignored) {
                // 单个附魔自检失败不影响其它附魔
            }
        }
        if (!missing.isEmpty()) {
            getLogger().warning("以下简单附魔未进入原版附魔台标签（仍可通过战利品箱与铁砧获得）："
                    + String.join(", ", missing));
        }
    }

    private void registerPermission() {
        var permissions = getServer().getPluginManager();
        if (permissions.getPermission("arcaneenchants.admin") == null) {
            permissions.addPermission(new Permission(
                    "arcaneenchants.admin", "允许使用 /arcaneench 管理指令", PermissionDefault.OP));
        }
    }

    private void registerCommand() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        "arcaneench",
                        "ArcaneEnchants 管理指令",
                        List.of("ae", "aench"),
                        new ArcaneCommand(this)));
    }

    private void startSyncTask() {
        if (!settings.attributesEnabled) {
            return;
        }
        int interval = settings.syncIntervalTicks;
        syncTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (!settings.attributesEnabled) {
                return;
            }
            for (var player : getServer().getOnlinePlayers()) {
                AttributeSync.syncInventory(player.getInventory());
            }
        }, interval, interval);
    }

    private void logSummary() {
        List<String> simple = Defs.byTier(Tier.SIMPLE).stream().map(EnchantDef::id).toList();
        List<String> complex = Defs.byTier(Tier.COMPLEX).stream().map(EnchantDef::id).toList();
        getLogger().info("简单附魔（附魔台 + 战利品箱 "
                + percent(settings.simpleLootChance) + "）：" + String.join(", ", simple));
        getLogger().info("复杂附魔（村民交易 + 稀有战利品 "
                + percent(settings.complexLootChance) + "）：" + String.join(", ", complex));
        getLogger().info("指令 /arcaneench（别名 /ae），权限 arcaneenchants.admin");
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0D);
    }
}
