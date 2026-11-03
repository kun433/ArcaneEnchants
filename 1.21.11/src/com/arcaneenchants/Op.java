package com.arcaneenchants;

/**
 * 属性修改运算类型。这里刻意用自定义枚举而不是直接引用 Bukkit 的
 * {@code AttributeModifier.Operation}，让定义层保持纯 Java，可以在没有服务端的
 * 环境里做离线断言。
 */
public enum Op {
    /** 直接加减数值，对应 {@code add_value}。 */
    ADD_NUMBER,
    /** 按属性基础值百分比叠加，对应 {@code add_multiplied_base}。 */
    ADD_SCALAR,
    /** 独立乘区，对应 {@code add_multiplied_total}。 */
    MULTIPLY_SCALAR_1
}
