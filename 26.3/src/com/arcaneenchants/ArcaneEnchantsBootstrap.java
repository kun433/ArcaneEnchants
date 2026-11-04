package com.arcaneenchants;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * bootstrap 阶段完成两件事：
 * <ol>
 *   <li>把所有附魔注册进服务端附魔注册表（{@link RegistryEvents#ENCHANTMENT}）。</li>
 *   <li>把简单层写进 {@code #minecraft:in_enchanting_table} / {@code #minecraft:non_treasure}，
 *       把复杂层写进 {@code #minecraft:treasure}，让「功能少的上附魔台、功能多的上不了」
 *       由原版规则直接生效。</li>
 * </ol>
 * 附魔注册必须在 bootstrap 阶段完成：注册表在插件 onEnable 之前就已经冻结，
 * 所以本类通过 paper-plugin.yml 的 {@code bootstrapper} 字段挂载。
 */
public final class ArcaneEnchantsBootstrap implements PluginBootstrap {

    /** 附魔命名空间，同时是配置文件目录之外的唯一前缀。 */
    public static final String NAMESPACE = "arcaneenchants";

    /**
     * bootstrap 阶段拿不到 Bukkit 的 Logger（ComponentLogger 需要额外依赖），
     * 这里记下注册过程，交给运行期插件在 onEnable 统一打印。
     */
    private static final List<String> NOTES = new ArrayList<>();

    /** bootstrap 阶段留下的信息，供运行期打印。 */
    public static List<String> notes() {
        synchronized (NOTES) {
            return List.copyOf(NOTES);
        }
    }

    private static void note(String message) {
        synchronized (NOTES) {
            NOTES.add(message);
        }
    }

    @Override
    public void bootstrap(BootstrapContext context) {
        final String language = readLanguage(context.getDataDirectory());

        context.getLifecycleManager().registerEventHandler(
                RegistryEvents.ENCHANTMENT.compose().newHandler(registerAll(language)));

        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT).newHandler(tagsRegistered()));

        note("开始注册 " + Defs.all().size() + " 个附魔（显示语言：" + language + "）");
    }

    /** 逐个登记附魔定义。 */
    private static LifecycleEventHandler<RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>>
    registerAll(String language) {
        return event -> {
            int registered = 0;
            for (EnchantDef def : Defs.all()) {
                RegistryKeySet<ItemType> supported = supportedItems(event, def);
                if (supported == null) {
                    note("跳过附魔 " + def.id() + "：拿不到可附魔物品集合");
                    continue;
                }
                List<EquipmentSlotGroup> slots = slotsOf(def);
                event.registry().register(
                        RegistryKey.ENCHANTMENT.typedKey(NAMESPACE + ":" + def.id()),
                        builder -> builder
                                .description(Component
                                        .translatable("enchantment." + NAMESPACE + "." + def.id())
                                        .fallback(def.displayName(language)))
                                .supportedItems(supported)
                                .weight(def.weight())
                                .maxLevel(def.maxLevel())
                                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost
                                        .of(def.minBase(), def.minPerLevel()))
                                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost
                                        .of(def.maxBase(), def.maxPerLevel()))
                                .anvilCost(def.anvilCost())
                                .activeSlots(slots));
                registered++;
            }
            note("已注册 " + registered + " 个附魔");
        };
    }

    /** 标签落地后把分层规则补进原版标签。 */
    private static LifecycleEventHandler<io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent<io.papermc.paper.tag.PostFlattenTagRegistrar<Enchantment>>>
    tagsRegistered() {
        return event -> {
            List<TypedKey<Enchantment>> simple = keysOf(Tier.SIMPLE);
            List<TypedKey<Enchantment>> complex = keysOf(Tier.COMPLEX);
            var registrar = event.registrar();
            registrar.addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, simple);
            registrar.addToTag(EnchantmentTagKeys.NON_TREASURE, simple);
            registrar.addToTag(EnchantmentTagKeys.TREASURE, complex);
            note("获取途径已配置：" + simple.size() + " 个简单附魔可上附魔台，"
                    + complex.size() + " 个复杂附魔只通过交易与稀有战利品获得");
        };
    }

    /** 某一层的附魔注册键。 */
    private static List<TypedKey<Enchantment>> keysOf(Tier tier) {
        List<TypedKey<Enchantment>> keys = new ArrayList<>();
        for (EnchantDef def : Defs.byTier(tier)) {
            keys.add(RegistryKey.ENCHANTMENT.typedKey(NAMESPACE + ":" + def.id()));
        }
        return keys;
    }

    /**
     * 解析可附魔物品集合：优先用原版 {@code enchantable/*} 标签，
     * 标签取不到时退回定义表里的显式物品 id 列表。
     */
    private static RegistryKeySet<ItemType> supportedItems(
            RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> event,
            EnchantDef def) {
        try {
            TagKey<ItemType> tagKey = RegistryKey.ITEM.tagKey(def.itemTag());
            Tag<ItemType> tag = event.getOrCreateTag(tagKey);
            if (tag != null && !tag.values().isEmpty()) {
                return tag;
            }
        } catch (Throwable ignored) {
            // 落到显式列表
        }
        if (def.fallbackItems().isEmpty()) {
            return null;
        }
        List<TypedKey<ItemType>> keys = new ArrayList<>(def.fallbackItems().size());
        for (String id : def.fallbackItems()) {
            keys.add(RegistryKey.ITEM.typedKey("minecraft:" + id));
        }
        // 显式列表必须全部是服务端真实存在的物品，否则注册表冻结时会直接报错，
        // 所以先按物品注册表过滤一遍；拿不到注册表就宁可跳过该附魔。
        try {
            var itemRegistry = io.papermc.paper.registry.RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ITEM);
            keys.removeIf(key -> itemRegistry.get(key.key()) == null);
        } catch (Throwable ignored) {
            return null;
        }
        if (keys.isEmpty()) {
            return null;
        }
        note("附魔 " + def.id() + " 使用显式物品列表（" + keys.size() + " 项）");
        return RegistrySet.keySet(RegistryKey.ITEM, keys);
    }

    private static List<EquipmentSlotGroup> slotsOf(EnchantDef def) {
        List<EquipmentSlotGroup> out = new ArrayList<>(def.slots().size());
        for (String name : def.slots()) {
            EquipmentSlotGroup group = EquipmentSlotGroup.getByName(name);
            out.add(group == null ? EquipmentSlotGroup.ANY : group);
        }
        return out.isEmpty() ? List.of(EquipmentSlotGroup.ANY) : out;
    }

    /**
     * bootstrap 阶段不能用 Bukkit 的配置类，这里只读 config.yml 的 language 一行。
     */
    private static String readLanguage(Path dataDirectory) {
        Path config = dataDirectory.resolve("config.yml");
        if (!Files.isRegularFile(config)) {
            return "zh_CN";
        }
        try {
            for (String line : Files.readAllLines(config, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("language:")) {
                    String value = trimmed.substring("language:".length()).trim();
                    if (value.endsWith("\"") && value.length() > 1) {
                        value = value.substring(0, value.length() - 1);
                    }
                    if (value.startsWith("\"")) {
                        value = value.substring(1);
                    }
                    if (value.endsWith("'")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    if (value.startsWith("'")) {
                        value = value.substring(1);
                    }
                    return value.isEmpty() ? "zh_CN" : value;
                }
            }
        } catch (IOException ignored) {
            // 读不动就用默认值
        }
        return "zh_CN";
    }
}
