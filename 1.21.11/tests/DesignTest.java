import com.arcaneenchants.AttrSpec;
import com.arcaneenchants.Behavior;
import com.arcaneenchants.Defs;
import com.arcaneenchants.EnchantDef;
import com.arcaneenchants.LevelRoll;
import com.arcaneenchants.Tier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 离线断言：只依赖定义层的纯 Java 类，不需要启动服务端。
 *
 * <p>用法：{@code java -cp <编译输出> DesignTest <resources 目录>}
 */
public final class DesignTest {

    /** 1.21.11 中确实存在的可附魔物品标签（已对照服务端 data/minecraft/tags/item 核对）。 */
    private static final Set<String> KNOWN_TAGS = Set.of(
            "minecraft:swords",
            "minecraft:enchantable/armor",
            "minecraft:enchantable/head_armor",
            "minecraft:enchantable/chest_armor",
            "minecraft:enchantable/leg_armor",
            "minecraft:enchantable/foot_armor",
            "minecraft:enchantable/mining",
            "minecraft:enchantable/durability",
            "minecraft:enchantable/melee_weapon",
            "minecraft:enchantable/sharp_weapon",
            "minecraft:enchantable/weapon",
            "minecraft:enchantable/fishing");

    private static final Set<String> KNOWN_SLOTS = Set.of(
            "any", "mainhand", "offhand", "hand", "feet", "legs", "chest", "head", "armor", "body");

    private static final List<String> ATTRIBUTE_IDS = List.of(
            "minecraft:movement_speed", "minecraft:armor", "minecraft:attack_damage",
            "minecraft:sneaking_speed", "minecraft:block_break_speed", "minecraft:knockback_resistance",
            "minecraft:luck", "minecraft:max_health", "minecraft:armor_toughness", "minecraft:attack_speed");

    private static int passed;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        Path resources = Path.of(args.length > 0 ? args[0] : "resources");

        everyEnchantHasAttributes();
        idsAreUniqueAndAscii();
        tiersFollowAcquisitionRules();
        numericRangesAreSane();
        tagsAndSlotsAreKnown();
        fallbackItemsAreSane();
        attributeAmountsGrowWithLevel();
        levelRollIsWeighted();
        configDefaultsMatchDesign(resources);
        pluginDescriptorWiresBootstrapper(resources);

        System.out.println();
        System.out.println("通过 " + passed + " 项断言，失败 " + failures.size() + " 项");
        for (String failure : failures) {
            System.out.println("  FAIL " + failure);
        }
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- 断言

    private static void everyEnchantHasAttributes() {
        for (EnchantDef def : Defs.all()) {
            check(def.attributes() != null && !def.attributes().isEmpty(),
                    def.id() + " 没有属性加成（每个附魔都必须有对应属性）");
            check(!def.attributeLines().isEmpty(), def.id() + " 属性描述为空");
            check(def.summaryZh() != null && !def.summaryZh().isBlank(), def.id() + " 缺少中文说明");
            check(def.summaryEn() != null && !def.summaryEn().isBlank(), def.id() + " 缺少英文说明");
        }
        check(Defs.all().size() >= 5, "附魔数量过少：" + Defs.all().size());
        check(enchantmentsWithBehavior() >= 3, "复杂附魔数量过少：" + enchantmentsWithBehavior());
    }

    private static void idsAreUniqueAndAscii() {
        Set<String> seen = new LinkedHashSet<>();
        for (EnchantDef def : Defs.all()) {
            check(seen.add(def.id()), "附魔 id 重复：" + def.id());
            check(def.id().equals(def.id().toLowerCase(Locale.ROOT)), "id 必须小写：" + def.id());
            check(def.id().matches("[a-z0-9_]{3,32}"), "id 只允许小写字母数字下划线：" + def.id());
            check(def.nameZh() != null && !def.nameZh().isBlank(), def.id() + " 缺少中文名");
            check(def.nameEn() != null && !def.nameEn().isBlank(), def.id() + " 缺少英文名");
            check(Defs.byId(def.id()) == def, "byId 查不到 " + def.id());
            check(Defs.byId(def.id().toUpperCase(Locale.ROOT)) == def, "byId 不区分大小写失败：" + def.id());
        }
    }

    private static void tiersFollowAcquisitionRules() {
        for (EnchantDef def : Defs.byTier(Tier.SIMPLE)) {
            check(def.tableAvailable(), def.id() + " 属于简单层但未标记为可在附魔台获得");
            check(!def.rareOnly(), def.id() + " 属于简单层却被标记为稀有");
            check(def.behavior() == Behavior.NONE,
                    def.id() + " 属于简单层却带有行为 " + def.behavior() + "（简单层应当只有属性）");
            check(def.maxLevel() >= 2, def.id() + " 简单附魔等级过少：" + def.maxLevel());
        }
        for (EnchantDef def : Defs.byTier(Tier.COMPLEX)) {
            check(def.rareOnly(), def.id() + " 属于复杂层但未标记为稀有");
            check(!def.tableAvailable(), def.id() + " 属于复杂层却标记为可在附魔台获得");
            check(def.behavior() != Behavior.NONE,
                    def.id() + " 属于复杂层却没有行为（复杂层必须有属性之外的功能）");
            check(def.weight() <= 2, def.id() + " 复杂附魔的附魔台权重应极低：" + def.weight());
        }
        check(!Defs.byTier(Tier.SIMPLE).isEmpty(), "缺少简单层附魔");
        check(!Defs.byTier(Tier.COMPLEX).isEmpty(), "缺少复杂层附魔");
    }

    private static void numericRangesAreSane() {
        for (EnchantDef def : Defs.all()) {
            check(def.maxLevel() >= 1 && def.maxLevel() <= 5, def.id() + " 等级上限异常：" + def.maxLevel());
            check(def.weight() >= 1 && def.weight() <= 10, def.id() + " 权重异常：" + def.weight());
            check(def.anvilCost() >= 0, def.id() + " 铁砧花费为负");
            check(def.minBase() >= 1, def.id() + " 最低花费基础值过小");
            check(def.minPerLevel() >= 0, def.id() + " 最低花费每级增量为负");
            check(def.maxPerLevel() >= 0, def.id() + " 最高花费每级增量为负");
            check(Defs.byId(def.id()).maxBase() >= def.minBase(), def.id() + " 最高花费小于最低花费");
        }
    }

    private static void tagsAndSlotsAreKnown() {
        for (EnchantDef def : Defs.all()) {
            check(KNOWN_TAGS.contains(def.itemTag()), def.id() + " 使用了 1.21.11 不存在的物品标签：" + def.itemTag());
            check(def.slots() != null && !def.slots().isEmpty(), def.id() + " 没有生效槽位");
            for (String slot : def.slots()) {
                check(KNOWN_SLOTS.contains(slot), def.id() + " 未知槽位组：" + slot);
            }
            for (AttrSpec spec : def.attributes()) {
                check(ATTRIBUTE_IDS.contains(spec.attribute()),
                        def.id() + " 使用了未知属性 id：" + spec.attribute());
                check(KNOWN_SLOTS.contains(spec.slotGroup()),
                        def.id() + " 属性槽位组未知：" + spec.slotGroup());
            }
        }
    }

    private static void fallbackItemsAreSane() {
        for (EnchantDef def : Defs.all()) {
            check(!def.fallbackItems().isEmpty(), def.id() + " 缺少回退物品列表");
            Set<String> unique = new HashSet<>(def.fallbackItems());
            check(unique.size() == def.fallbackItems().size(), def.id() + " 回退物品列表存在重复项");
            for (String item : def.fallbackItems()) {
                check(item.matches("[a-z0-9_]+"), def.id() + " 回退物品 id 非法：" + item);
                check(!item.contains(":") && !item.contains("#"), def.id() + " 回退物品应为不带命名空间的 id：" + item);
            }
        }
    }

    private static void attributeAmountsGrowWithLevel() {
        for (EnchantDef def : Defs.all()) {
            for (AttrSpec spec : def.attributes()) {
                check(spec.perLevel() > 0.0D, def.id() + " 属性 " + spec.attribute() + " 每级数值必须为正");
                double previous = 0.0D;
                for (int level = 1; level <= def.maxLevel(); level++) {
                    double amount = spec.amount(level);
                    check(amount > previous, def.id() + " 属性 " + spec.attribute() + " 未随等级增长");
                    previous = amount;
                }
                check(spec.amount(0) == 0.0D, def.id() + " 0 级不应产生属性数值");
            }
        }
    }

    private static void levelRollIsWeighted() {
        for (EnchantDef def : Defs.all()) {
            double sum = 0.0D;
            for (int level = 1; level <= def.maxLevel(); level++) {
                sum += LevelRoll.probability(def, level);
            }
            check(Math.abs(sum - 1.0D) < 1.0E-9D, def.id() + " 等级概率之和不为 1：" + sum);

            if (def.maxLevel() > 1) {
                check(LevelRoll.probability(def, 1) > LevelRoll.probability(def, 2),
                        def.id() + " 1 级应比 2 级更常见");
            }

            for (int i = 0; i < 200; i++) {
                double random = i / 200.0D;
                int level = LevelRoll.roll(def, random);
                check(level >= 1 && level <= def.maxLevel(), def.id() + " 抽出的等级越界：" + level);
            }
        }
    }

    private static void configDefaultsMatchDesign(Path resources) {
        String config = read(resources.resolve("config.yml"));
        check(config.contains("complex-chance: 0.008"), "config.yml 的复杂附魔战利品概率应为 0.008（0.8%）");
        check(config.contains("simple-chance:"), "config.yml 缺少简单附魔战利品概率");
        check(config.contains("enabled: true"), "config.yml 缺少开关");
        check(config.contains("language: zh_CN"), "config.yml 缺少默认语言");
        check(config.contains("etable") || config.contains("enchanting"), "config.yml 缺少附魔相关说明");
        for (String key : List.of("help-header", "list-entry", "status-loot", "gave", "reloaded")) {
            check(config.contains(key), "config.yml 缺少消息键：" + key);
        }
    }

    private static void pluginDescriptorWiresBootstrapper(Path resources) {
        String yml = read(resources.resolve("paper-plugin.yml"));
        check(yml.contains("main: com.arcaneenchants.ArcaneEnchantsPlugin"), "paper-plugin.yml 的 main 配置不正确");
        check(yml.contains("bootstrapper: com.arcaneenchants.ArcaneEnchantsBootstrap"),
                "paper-plugin.yml 必须配置 bootstrapper（附魔注册只能在 bootstrap 阶段完成）");
        check(yml.contains("api-version: '1.21'"), "paper-plugin.yml 缺少 api-version");
        check(yml.contains("name: ArcaneEnchants"), "paper-plugin.yml 缺少插件名");
    }

    // ---------------------------------------------------------------- 工具

    private static int enchantmentsWithBehavior() {
        int count = 0;
        for (EnchantDef def : Defs.all()) {
            if (def.behavior() != Behavior.NONE) {
                count++;
            }
        }
        return count;
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private static void check(boolean condition, String message) {
        if (condition) {
            passed++;
        } else {
            failures.add(message);
        }
    }

    private DesignTest() {
    }
}
