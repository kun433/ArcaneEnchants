package com.arcaneenchants;

import java.util.List;

/**
 * 一个自建附魔的完整定义。运行期由 bootstrap 阶段注册进服务端附魔注册表。
 *
 * <p>本类不引用任何 Bukkit 类型，定义表因此可以在没有服务端的 JVM 里直接做断言测试。
 *
 * @param id            附魔 id，注册为 {@code arcaneenchants:<id>}
 * @param nameZh        中文名
 * @param nameEn        英文名
 * @param tier          强度分层，决定获取途径
 * @param behavior      代码行为，SIMPLE 层固定为 {@link Behavior#NONE}
 * @param maxLevel      最大等级
 * @param weight        附魔台权重（越大越常见），仅 SIMPLE 层实际生效
 * @param anvilCost     铁砧花费
 * @param minBase       最低花费基础值
 * @param minPerLevel   最低花费每级增量
 * @param maxBase       最高花费基础值
 * @param maxPerLevel   最高花费每级增量
 * @param itemTag       可附魔物品的标签 id，例如 {@code minecraft:enchantable/sword}
 * @param slots         生效槽位组名称
 * @param attributes    属性加成
 * @param fallbackItems 标签取不到时退回的显式物品 id 列表
 * @param summaryZh     中文功能说明
 * @param summaryEn     英文功能说明
 */
public record EnchantDef(
        String id,
        String nameZh,
        String nameEn,
        Tier tier,
        Behavior behavior,
        int maxLevel,
        int weight,
        int anvilCost,
        int minBase,
        int minPerLevel,
        int maxBase,
        int maxPerLevel,
        String itemTag,
        List<String> slots,
        List<AttrSpec> attributes,
        List<String> fallbackItems,
        String summaryZh,
        String summaryEn
) {

    /** 是否是简单附魔（可以在附魔台刷出）。 */
    public boolean tableAvailable() {
        return tier == Tier.SIMPLE;
    }

    /** 是否是复杂附魔（只能靠交易或稀有战利品）。 */
    public boolean rareOnly() {
        return tier == Tier.COMPLEX;
    }

    /** 按语言取显示名。 */
    public String displayName(String language) {
        return language != null && language.toLowerCase(java.util.Locale.ROOT).startsWith("en") ? nameEn : nameZh;
    }

    /** 按语言取功能说明。 */
    public String summary(String language) {
        return language != null && language.toLowerCase(java.util.Locale.ROOT).startsWith("en") ? summaryEn : summaryZh;
    }

    /** 属性加成的可读描述，用于指令输出与文档。 */
    public List<String> attributeLines() {
        return attributes.stream().map(AttrSpec::describe).toList();
    }
}
