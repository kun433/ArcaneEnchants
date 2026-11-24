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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 把附魔的属性加成写进物品的 {@code attribute_modifiers} 组件。
 *
 * <p>1.20.5 组件化之后，{@code attribute_modifiers} 组件一旦存在就会<b>整体替换</b>物品的
 * 原版默认属性（钻石剑的攻击伤害、胸甲的护甲值等都来自默认组件）。所以本类写入时必须把
 * 「原版默认修饰符 + 本插件的修饰符」一起写进组件，否则附魔反而会把装备的基础数值抹掉。
 *
 * <p>规则与幂等性：
 * <ul>
 *   <li>只在物品上没有任何第三方自定义属性修饰符时才注入原版默认值——其它插件显式重写过
 *       属性的物品视为由它自己负责完整数值，我们只把本插件的加成叠上去。</li>
 *   <li>组件内容与目标状态一致时直接跳过，周期扫描不会反复重写物品。</li>
 *   <li>移除附魔（砂轮/铁砧）后，如果组件只剩等值于原版默认的内容，就整个撤掉组件，
 *       让物品回落到隐式默认值，不留下会覆盖默认属性的空组件。</li>
 *   <li>修复前（1.1.0）写入的「只有本插件修饰符」的物品会在下一次同步时被自动补上默认值。</li>
 * </ul>
 */
public final class AttributeSync {

    /** 修饰符命名空间，用于识别「我们加的」修饰符。 */
    public static final String NAMESPACE = ArcaneEnchantsBootstrap.NAMESPACE;

    /** 物品类型的原版默认属性修饰符缓存。同步在主线程执行，ConcurrentHashMap 仅为防御。 */
    private static final Map<Material, List<Map.Entry<Attribute, AttributeModifier>>> DEFAULTS =
            new ConcurrentHashMap<>();

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
        try {
            boolean changed = rebuildModifiers(type, meta, present);
            if (changed) {
                stack.setItemMeta(meta);
            }
            return changed;
        } catch (Throwable ignored) {
            // 少数物品没有 attribute_modifiers 组件，忽略即可，不影响附魔本身
            return false;
        }
    }

    /**
     * 重建 meta 上的属性修饰符集合，返回是否发生了变化。
     */
    private static boolean rebuildModifiers(Material type, ItemMeta meta, List<EnchantDef> present) {
        Multimap<Attribute, AttributeModifier> current = meta.getAttributeModifiers();

        // 1. 盘点现有组件：区分「本插件的旧修饰符」与「第三方/默认修饰符」
        List<Map.Entry<Attribute, AttributeModifier>> kept = new ArrayList<>();
        boolean hadOurs = false;
        if (current != null) {
            for (Map.Entry<Attribute, AttributeModifier> entry : current.entries()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                NamespacedKey key = entry.getValue().getKey();
                if (key != null && NAMESPACE.equals(key.getNamespace())) {
                    hadOurs = true;
                } else {
                    kept.add(Map.entry(entry.getKey(), entry.getValue()));
                }
            }
        }

        // 2. 按当前附魔等级算出本插件应当写入的修饰符
        List<Map.Entry<Attribute, AttributeModifier>> ours = oursOf(meta, present);

        // 3. 无附魔的清理路径：从未写过就不动；写过则清理并尽量恢复隐式默认
        if (ours.isEmpty()) {
            if (!hadOurs || current == null) {
                return false;
            }
            for (Map.Entry<Attribute, AttributeModifier> entry : current.entries()) {
                NamespacedKey key = entry.getValue().getKey();
                if (key != null && NAMESPACE.equals(key.getNamespace())) {
                    meta.removeAttributeModifier(entry.getKey(), entry.getValue());
                }
            }
            Multimap<Attribute, AttributeModifier> remaining = meta.getAttributeModifiers();
            if (remaining == null || remaining.isEmpty() || onlyDefaultModifiers(type, remaining)) {
                // 组件只剩（或只剩等值于）原版默认：整个撤掉，回落隐式默认
                meta.setAttributeModifiers(null);
            }
            return true;
        }

        // 4. 写入路径：kept 为空（物品属性还是原版隐式状态）时补上默认值
        List<Map.Entry<Attribute, AttributeModifier>> injected = new ArrayList<>();
        if (kept.isEmpty()) {
            injected.addAll(defaultsOf(type));
        }

        // 5. 与现状一致就不写，避免周期扫描每 5 秒重写一次物品
        List<Map.Entry<Attribute, AttributeModifier>> desired = new ArrayList<>(kept);
        desired.addAll(injected);
        desired.addAll(ours);
        if (sameContent(current, desired)) {
            return false;
        }

        // 6. 应用：删旧的本插件修饰符 → 补默认 → 写新修饰符
        if (current != null) {
            for (Map.Entry<Attribute, AttributeModifier> entry : current.entries()) {
                NamespacedKey key = entry.getValue().getKey();
                if (key != null && NAMESPACE.equals(key.getNamespace())) {
                    meta.removeAttributeModifier(entry.getKey(), entry.getValue());
                }
            }
        }
        for (Map.Entry<Attribute, AttributeModifier> entry : injected) {
            meta.addAttributeModifier(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Attribute, AttributeModifier> entry : ours) {
            // 先删同 key 旧值再写入，保证等级变化时是覆盖而不是追加
            meta.removeAttributeModifier(entry.getKey(), entry.getValue());
            meta.addAttributeModifier(entry.getKey(), entry.getValue());
        }
        return true;
    }

    /** 按当前附魔等级计算本插件要写入的全部修饰符。 */
    private static List<Map.Entry<Attribute, AttributeModifier>> oursOf(
            ItemMeta meta, List<EnchantDef> present) {
        List<Map.Entry<Attribute, AttributeModifier>> out = new ArrayList<>();
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
                out.add(Map.entry(attribute, modifier));
            }
        }
        return out;
    }

    /** 物品的原版默认属性修饰符（带缓存）。查询失败按无默认值处理。 */
    private static List<Map.Entry<Attribute, AttributeModifier>> defaultsOf(Material type) {
        return DEFAULTS.computeIfAbsent(type, material -> {
            List<Map.Entry<Attribute, AttributeModifier>> out = new ArrayList<>();
            try {
                Multimap<Attribute, AttributeModifier> defaults = material.getDefaultAttributeModifiers();
                if (defaults != null) {
                    for (Map.Entry<Attribute, AttributeModifier> entry : defaults.entries()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            out.add(Map.entry(entry.getKey(), entry.getValue()));
                        }
                    }
                }
            } catch (Throwable ignored) {
                // 无默认值可用时退化为只写本插件修饰符
            }
            return List.copyOf(out);
        });
    }

    /** 判断一组修饰符是否与物品的原版默认完全一致（数量与内容都相同）。 */
    private static boolean onlyDefaultModifiers(Material type, Multimap<Attribute, AttributeModifier> actual) {
        return matchesList(actual, defaultsOf(type));
    }

    /** 判断组件现状与目标内容是否完全一致（多重集合比较）。 */
    private static boolean sameContent(Multimap<Attribute, AttributeModifier> actual,
                                       List<Map.Entry<Attribute, AttributeModifier>> desired) {
        return matchesList(actual, desired);
    }

    private static boolean matchesList(Multimap<Attribute, AttributeModifier> actual,
                                       List<Map.Entry<Attribute, AttributeModifier>> expected) {
        if (actual == null || actual.entries().size() != expected.size()) {
            return false;
        }
        List<Map.Entry<Attribute, AttributeModifier>> remaining = new ArrayList<>(actual.entries());
        for (Map.Entry<Attribute, AttributeModifier> entry : expected) {
            boolean matched = false;
            Iterator<Map.Entry<Attribute, AttributeModifier>> iterator = remaining.iterator();
            while (iterator.hasNext()) {
                Map.Entry<Attribute, AttributeModifier> candidate = iterator.next();
                if (candidate == null || candidate.getKey() == null || candidate.getValue() == null) {
                    continue;
                }
                if (sameAttribute(candidate.getKey(), entry.getKey())
                        && candidate.getValue().equals(entry.getValue())) {
                    iterator.remove();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    /**
     * 属性相等判断。同一个属性经注册表与默认值转换两条路径拿到的实例可能不是同一个对象，
     * 统一按 NamespacedKey 比较。
     */
    private static boolean sameAttribute(Attribute left, Attribute right) {
        if (left == right) {
            return true;
        }
        return left != null && right != null && left.getKey().equals(right.getKey());
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
