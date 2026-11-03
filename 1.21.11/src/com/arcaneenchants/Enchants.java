package com.arcaneenchants;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 运行期的附魔/属性解析缓存。
 *
 * <p>bootstrap 已经把附魔注册进注册表，这里在 onEnable 时按 id 取回
 * {@link Enchantment} 实例并缓存，避免每个事件都查一次注册表。
 */
public final class Enchants {

    private static final Map<String, Enchantment> ENCHANTMENTS = new LinkedHashMap<>();
    private static final Map<String, Attribute> ATTRIBUTES = new HashMap<>();
    private static final Map<String, AttributeModifier.Operation> OPERATIONS = new HashMap<>();
    private static final Map<String, EquipmentSlotGroup> SLOTS = new HashMap<>();
    private static final List<String> MISSING = new ArrayList<>();

    private Enchants() {
    }

    /** 在 onEnable 时调用，解析全部附魔与属性。 */
    public static void resolve() {
        ENCHANTMENTS.clear();
        ATTRIBUTES.clear();
        MISSING.clear();

        RegistryAccess access = RegistryAccess.registryAccess();
        var enchantmentRegistry = access.getRegistry(RegistryKey.ENCHANTMENT);
        for (EnchantDef def : Defs.all()) {
            NamespacedKey key = new NamespacedKey(ArcaneEnchantsBootstrap.NAMESPACE, def.id());
            Enchantment enchantment = null;
            try {
                enchantment = enchantmentRegistry.get(key);
            } catch (Throwable ignored) {
                // 下面再试一次静态查表
            }
            if (enchantment == null) {
                enchantment = Enchantment.getByKey(key);
            }
            if (enchantment == null) {
                MISSING.add(def.id());
            } else {
                ENCHANTMENTS.put(def.id(), enchantment);
            }
        }

        var attributeRegistry = access.getRegistry(RegistryKey.ATTRIBUTE);
        for (EnchantDef def : Defs.all()) {
            for (AttrSpec spec : def.attributes()) {
                ATTRIBUTES.computeIfAbsent(spec.attribute(), id -> {
                    NamespacedKey key = NamespacedKey.fromString(id);
                    return key == null ? null : attributeRegistry.get(key);
                });
            }
        }

        OPERATIONS.put("ADD_NUMBER", AttributeModifier.Operation.ADD_NUMBER);
        OPERATIONS.put("ADD_SCALAR", AttributeModifier.Operation.ADD_SCALAR);
        OPERATIONS.put("MULTIPLY_SCALAR_1", AttributeModifier.Operation.MULTIPLY_SCALAR_1);

        for (EquipmentSlotGroup group : List.of(
                EquipmentSlotGroup.ANY, EquipmentSlotGroup.MAINHAND, EquipmentSlotGroup.OFFHAND,
                EquipmentSlotGroup.HAND, EquipmentSlotGroup.FEET, EquipmentSlotGroup.LEGS,
                EquipmentSlotGroup.CHEST, EquipmentSlotGroup.HEAD, EquipmentSlotGroup.ARMOR,
                EquipmentSlotGroup.BODY)) {
            SLOTS.put(group.toString().toLowerCase(Locale.ROOT), group);
        }
    }

    /** 取已注册的附魔，未注册返回 null。 */
    public static Enchantment get(String id) {
        return id == null ? null : ENCHANTMENTS.get(id.toLowerCase(Locale.ROOT));
    }

    /** 取属性，解析失败返回 null。 */
    public static Attribute attribute(String id) {
        return id == null ? null : ATTRIBUTES.get(id);
    }

    /** 运算类型映射。 */
    public static AttributeModifier.Operation operation(Op op) {
        return OPERATIONS.getOrDefault(op.name(), AttributeModifier.Operation.ADD_NUMBER);
    }

    /** 槽位组映射，未知名回落到 ANY。 */
    public static EquipmentSlotGroup slot(String name) {
        if (name == null) {
            return EquipmentSlotGroup.ANY;
        }
        return SLOTS.getOrDefault(name.toLowerCase(Locale.ROOT), EquipmentSlotGroup.ANY);
    }

    /** 物品上某个附魔的等级，没有则为 0。 */
    public static int level(ItemMeta meta, String id) {
        Enchantment enchantment = get(id);
        if (meta == null || enchantment == null) {
            return 0;
        }
        try {
            return meta.getEnchantLevel(enchantment);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** 物品栈上某个附魔的等级，没有则为 0。 */
    public static int level(ItemStack stack, String id) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return 0;
        }
        return level(stack.getItemMeta(), id);
    }

    /** 物品上带有的本插件附魔定义。 */
    public static List<EnchantDef> presentOn(ItemMeta meta) {
        List<EnchantDef> present = new ArrayList<>(4);
        if (meta == null) {
            return present;
        }
        for (EnchantDef def : Defs.all()) {
            if (level(meta, def.id()) > 0) {
                present.add(def);
            }
        }
        return present;
    }

    /** 构造一本本插件的附魔书，附魔不存在时返回 null。 */
    public static ItemStack book(EnchantDef def, int level) {
        Enchantment enchantment = get(def.id());
        if (enchantment == null) {
            return null;
        }
        int clamped = Math.max(1, Math.min(def.maxLevel(), level));
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storage) {
            storage.addStoredEnchant(enchantment, clamped, true);
        } else if (meta != null) {
            meta.addEnchant(enchantment, clamped, true);
        }
        if (meta != null) {
            book.setItemMeta(meta);
        }
        return book;
    }

    /** 未能在注册表中找到的附魔 id。 */
    public static List<String> missing() {
        return List.copyOf(MISSING);
    }

    /** 是否全部附魔都注册成功。 */
    public static boolean ready() {
        return MISSING.isEmpty() && ENCHANTMENTS.size() == Defs.all().size();
    }

    /** 已解析的附魔数量。 */
    public static int resolvedCount() {
        return ENCHANTMENTS.size();
    }

    /** 已解析的附魔实例集合，仅用于调试输出。 */
    public static Collection<Enchantment> resolved() {
        return List.copyOf(ENCHANTMENTS.values());
    }
}
