package com.arcaneenchants;

import com.google.common.collect.Multimap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 把附魔的属性加成写进物品的 {@code attribute_modifiers} 组件。
 *
 * <p>这样属性会显示在物品提示里、由原版直接生效，不需要每 tick 给玩家挂临时属性。
 * 写入是幂等的：先清掉本插件命名空间下的旧修饰符，再按当前附魔等级补上，
 * 因此等级变化、附魔被铁砧/砂轮移除、物品被复制都不会留下残留。
 */
public final class AttributeSync {

    /** 修饰符命名空间，用于识别「我们加的」修饰符。 */
    public static final String NAMESPACE = ArcaneEnchantsBootstrap.NAMESPACE;

    private AttributeSync() {
    }

    /**
     * 同步单件物品的属性修饰符。
     *
     * @return 是否改动了物品
     */
    public static boolean sync(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        Material type = stack.getType();
        if (type == Material.ENCHANTED_BOOK || type == Material.BOOK || type == Material.WRITTEN_BOOK) {
            return false;
        }

        ItemMeta meta;
        try {
            meta = stack.getItemMeta();
        } catch (Throwable ignored) {
            return false;
        }
        if (meta == null) {
            return false;
        }

        List<EnchantDef> present = Enchants.presentOn(meta);
        boolean changed = false;

        try {
            Multimap<Attribute, AttributeModifier> current = meta.getAttributeModifiers();
            if (current != null && !current.isEmpty()) {
                for (Map.Entry<Attribute, AttributeModifier> entry : new ArrayList<>(current.entries())) {
                    NamespacedKey key = entry.getValue().getKey();
                    if (key != null && NAMESPACE.equals(key.getNamespace())) {
                        meta.removeAttributeModifier(entry.getKey(), entry.getValue());
                        changed = true;
                    }
                }
            }

            for (EnchantDef def : present) {
                int level = Enchants.level(meta, def.id());
                if (level <= 0) {
                    continue;
                }
                for (AttrSpec spec : def.attributes()) {
                    Attribute attribute = Enchants.attribute(spec.attribute());
                    if (attribute == null) {
                        continue;
                    }
                    double amount = spec.amount(level);
                    if (amount == 0.0D) {
                        continue;
                    }
                    NamespacedKey key = new NamespacedKey(
                            NAMESPACE, def.id() + "_" + attribute.getKey().getKey());
                    AttributeModifier modifier = new AttributeModifier(
                            key, amount, Enchants.operation(spec.operation()), Enchants.slot(spec.slotGroup()));
                    meta.addAttributeModifier(attribute, modifier);
                    changed = true;
                }
            }

            if (changed) {
                stack.setItemMeta(meta);
            }
        } catch (Throwable ignored) {
            // 少数物品没有 attribute_modifiers 组件，忽略即可，不影响附魔本身
            return false;
        }
        return changed;
    }

    /** 扫描背包（含护甲与副手）并同步。 */
    public static void syncInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                continue;
            }
            if (sync(item)) {
                inventory.setItem(slot, item);
            }
        }
    }
}
