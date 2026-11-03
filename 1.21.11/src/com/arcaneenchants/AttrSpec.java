package com.arcaneenchants;

/**
 * 一条属性加成：每级提供的数值、运算方式，以及生效的装备槽位。
 *
 * @param attribute 属性 id，例如 {@code minecraft:movement_speed}
 * @param perLevel  每级数值（第 n 级合计为 perLevel * n）
 * @param operation 运算方式
 * @param slotGroup 生效槽位组，例如 {@code any}、{@code hand}、{@code armor}、{@code feet}
 */
public record AttrSpec(String attribute, double perLevel, Op operation, String slotGroup) {

    /** 第 level 级时该属性的总数值。等级从 1 开始。 */
    public double amount(int level) {
        if (level <= 0) {
            return 0.0D;
        }
        return perLevel * level;
    }

    /** 生成用于展示的简短描述，例如 {@code movement_speed +4%/级}。 */
    public String describe() {
        String shortName = attribute.startsWith("minecraft:") ? attribute.substring("minecraft:".length()) : attribute;
        if (operation == Op.ADD_NUMBER) {
            return shortName + " +" + trim(perLevel) + "/级";
        }
        return shortName + " +" + trim(perLevel * 100.0D) + "%/级";
    }

    private static String trim(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.valueOf(Math.round(value * 1000.0D) / 1000.0D);
    }
}
