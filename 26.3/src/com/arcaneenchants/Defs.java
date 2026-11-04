package com.arcaneenchants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全部附魔的定义表。
 *
 * <p>设计原则（与获取途径绑定）：
 * <ul>
 *   <li>SIMPLE 层只给一条属性加成，功能单一，加入 {@code #minecraft:in_enchanting_table}
 *       标签，可正常从附魔台刷出，同时以较高概率出现在战利品箱。</li>
 *   <li>COMPLEX 层在属性之外再多挂一条代码行为，功能复合，不进附魔台，
 *       只能通过图书管理员交易或 0.8% 概率的稀有战利品获得。</li>
 * </ul>
 */
public final class Defs {

    /** 1.21.11 的工具/装备材质前缀，用于在标签不可用时拼出显式物品 id。 */
    private static final List<String> MATERIALS = List.of(
            "wooden", "stone", "copper", "iron", "golden", "diamond", "netherite");

    private static final List<String> ARMOR_MATERIALS = List.of(
            "leather", "copper", "chainmail", "iron", "golden", "diamond", "netherite");

    private static final List<String> SWORDS = toolSet("sword");
    private static final List<String> BOOTS = armorSet("boots");
    private static final List<String> LEGGINGS = armorSet("leggings");
    private static final List<String> FULL_ARMOR = fullArmor();

    private static final List<String> MINING_TOOLS = concat(
            toolSet("pickaxe"), toolSet("shovel"), toolSet("axe"), toolSet("hoe"), List.of("shears"));

    private static final List<String> SPEARS = toolSet("spear");

    private static final List<String> MELEE_WEAPONS = concat(SWORDS, SPEARS);

    private static final List<String> DURABILITY_ITEMS = durabilityItems();

    /** 无序表实体，key 为附魔 id。 */
    private static final Map<String, EnchantDef> BY_ID = new LinkedHashMap<>();

    /** 全部附魔，顺序即为指令与文档中的展示顺序。 */
    public static final List<EnchantDef> ALL;

    static {
        List<EnchantDef> list = new ArrayList<>();

        // ---------- 简单层：属性加法，附魔台可得 ----------
        list.add(new EnchantDef(
                "swiftstep", "轻盈", "Swiftstep", Tier.SIMPLE, Behavior.NONE,
                3, 6, 2, 10, 10, 25, 10,
                "minecraft:enchantable/foot_armor", List.of("feet"),
                List.of(new AttrSpec("minecraft:movement_speed", 0.04D, Op.ADD_SCALAR, "feet")),
                BOOTS,
                "移动速度每级 +4%。",
                "Movement speed +4% per level."));

        list.add(new EnchantDef(
                "fortified", "坚韧", "Fortified", Tier.SIMPLE, Behavior.NONE,
                3, 5, 3, 12, 8, 30, 8,
                "minecraft:enchantable/armor", List.of("armor"),
                List.of(new AttrSpec("minecraft:armor", 1.0D, Op.ADD_NUMBER, "armor")),
                FULL_ARMOR,
                "盔甲值每级 +1。",
                "Armor +1 per level."));

        list.add(new EnchantDef(
                "keen", "敏锐", "Keen", Tier.SIMPLE, Behavior.NONE,
                3, 5, 3, 12, 10, 32, 10,
                "minecraft:swords", List.of("mainhand"),
                List.of(new AttrSpec("minecraft:attack_damage", 0.75D, Op.ADD_NUMBER, "mainhand")),
                SWORDS,
                "近战攻击伤害每级 +0.75。",
                "Attack damage +0.75 per level."));

        list.add(new EnchantDef(
                "stealthy", "潜行", "Stealthy", Tier.SIMPLE, Behavior.NONE,
                3, 3, 4, 15, 12, 40, 12,
                "minecraft:enchantable/leg_armor", List.of("legs"),
                List.of(new AttrSpec("minecraft:sneaking_speed", 0.15D, Op.ADD_SCALAR, "legs")),
                LEGGINGS,
                "潜行移动速度每级 +15%。",
                "Sneaking speed +15% per level."));

        list.add(new EnchantDef(
                "excavator", "采掘", "Excavator", Tier.SIMPLE, Behavior.NONE,
                3, 4, 3, 12, 10, 32, 10,
                "minecraft:enchantable/mining", List.of("mainhand"),
                List.of(new AttrSpec("minecraft:block_break_speed", 0.08D, Op.ADD_SCALAR, "mainhand")),
                MINING_TOOLS,
                "方块破坏速度每级 +8%（对所有方块生效）。",
                "Block break speed +8% per level (all blocks)."));

        // ---------- 复杂层：属性 + 行为，不进附魔台 ----------
        list.add(new EnchantDef(
                "eternal", "永固", "Eternal", Tier.COMPLEX, Behavior.ETERNAL,
                1, 1, 8, 25, 25, 75, 25,
                "minecraft:enchantable/durability", List.of("any"),
                List.of(new AttrSpec("minecraft:knockback_resistance", 0.1D, Op.ADD_NUMBER, "any")),
                DURABILITY_ITEMS,
                "耐久永不消耗，并额外获得 10% 击退抗性。",
                "Durability never decreases; also grants +10% knockback resistance."));

        list.add(new EnchantDef(
                "greed", "贪婪", "Greed", Tier.COMPLEX, Behavior.GREED,
                3, 1, 6, 20, 12, 60, 12,
                "minecraft:enchantable/mining", List.of("mainhand"),
                List.of(new AttrSpec("minecraft:luck", 1.0D, Op.ADD_NUMBER, "mainhand")),
                MINING_TOOLS,
                "挖掘方块时每级 10% 概率额外掉落一份，并每级 +1 幸运。",
                "10% chance per level to duplicate a block drop; +1 luck per level."));

        list.add(new EnchantDef(
                "bloodpact", "血契", "Bloodpact", Tier.COMPLEX, Behavior.LIFESTEAL,
                3, 1, 8, 20, 15, 55, 15,
                "minecraft:swords", List.of("hand"),
                List.of(new AttrSpec("minecraft:max_health", 1.0D, Op.ADD_NUMBER, "hand")),
                SWORDS,
                "造成伤害的 8%/级 回复自身生命，并每级 +1 最大生命。",
                "Heal for 8%/level of damage dealt; +1 max health per level."));

        list.add(new EnchantDef(
                "warder", "守望", "Warder", Tier.COMPLEX, Behavior.WARDER,
                3, 1, 8, 22, 15, 60, 15,
                "minecraft:enchantable/chest_armor", List.of("chest"),
                List.of(new AttrSpec("minecraft:armor_toughness", 1.0D, Op.ADD_NUMBER, "chest")),
                armorSet("chestplate"),
                "生命低于 35% 时受击自动获得短暂抗性提升（有冷却），并每级 +1 盔甲韧性。",
                "Gain brief Resistance when hit below 35% health (cooldown); +1 armor toughness per level."));

        list.add(new EnchantDef(
                "resonance", "回响", "Resonance", Tier.COMPLEX, Behavior.IGNITE,
                3, 1, 8, 22, 14, 58, 14,
                "minecraft:enchantable/melee_weapon", List.of("mainhand"),
                List.of(new AttrSpec("minecraft:attack_speed", 0.04D, Op.ADD_SCALAR, "mainhand")),
                MELEE_WEAPONS,
                "命中目标时将其点燃（每级 2 秒），并每级 +4% 攻击速度。",
                "Ignite the target on hit (2s per level); +4% attack speed per level."));

        ALL = List.copyOf(list);
        for (EnchantDef def : ALL) {
            BY_ID.put(def.id(), def);
        }
    }

    private Defs() {
    }

    /** 全部附魔。 */
    public static List<EnchantDef> all() {
        return ALL;
    }

    /** 按 id 查定义，未知名返回 null。 */
    public static EnchantDef byId(String id) {
        return id == null ? null : BY_ID.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    /** 指定层级的全部附魔。 */
    public static List<EnchantDef> byTier(Tier tier) {
        return ALL.stream().filter(def -> def.tier() == tier).toList();
    }

    private static List<String> toolSet(String suffix) {
        List<String> out = new ArrayList<>(MATERIALS.size());
        for (String material : MATERIALS) {
            out.add(material + "_" + suffix);
        }
        return List.copyOf(out);
    }

    private static List<String> armorSet(String suffix) {
        List<String> out = new ArrayList<>(ARMOR_MATERIALS.size());
        for (String material : ARMOR_MATERIALS) {
            out.add(material + "_" + suffix);
        }
        return List.copyOf(out);
    }

    private static List<String> fullArmor() {
        List<String> out = new ArrayList<>();
        for (String material : ARMOR_MATERIALS) {
            out.add(material + "_helmet");
            out.add(material + "_chestplate");
            out.add(material + "_leggings");
            out.add(material + "_boots");
        }
        out.add("turtle_helmet");
        return List.copyOf(out);
    }

    private static List<String> durabilityItems() {
        // 去重：mining 集合里已经包含 shears，额外清单里也会出现
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>(FULL_ARMOR);
        out.addAll(SWORDS);
        out.addAll(MINING_TOOLS);
        out.addAll(SPEARS);
        out.addAll(List.of(
                "bow", "crossbow", "trident", "shield", "elytra", "fishing_rod",
                "flint_and_steel", "shears", "carrot_on_a_stick", "warped_fungus_on_a_stick",
                "brush", "mace"));
        return List.copyOf(out);
    }

    @SafeVarargs
    private static List<String> concat(List<String>... parts) {
        List<String> out = new ArrayList<>();
        for (List<String> part : parts) {
            out.addAll(part);
        }
        return List.copyOf(out);
    }
}
