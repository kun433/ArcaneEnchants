package com.arcaneenchants;

/**
 * 附魔的代码行为。属性加成由 {@link AttrSpec} 描述，写在物品的 attribute_modifiers 上；
 * 这里只记录属性无法表达的部分。
 *
 * <p>分层约定：SIMPLE 层只提供属性加成（{@link #NONE}），
 * COMPLEX 层在属性之外再挂一个行为，符合「功能多 -> 获取更难」的设计。
 */
public enum Behavior {
    /** 无额外行为，纯属性加成。 */
    NONE,
    /** 无限耐久：装备该附魔的物品不再消耗耐久。 */
    ETERNAL,
    /** 贪婪：挖掘方块时有概率额外掉落一份战利品。 */
    GREED,
    /** 血契：造成伤害时按比例回复自身生命。 */
    LIFESTEAL,
    /** 守望：生命过低时受击自动获得抗性提升。 */
    WARDER,
    /** 回响：命中目标时将其点燃。 */
    IGNITE
}
