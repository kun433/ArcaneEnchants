package com.arcaneenchants;

/**
 * 附魔书等级抽取。等级分布按 1/level 加权：1 级最常见，等级越高越稀有。
 *
 * <p>纯函数、不依赖 Bukkit，便于离线断言。
 */
public final class LevelRoll {

    private LevelRoll() {
    }

    /**
     * 抽取一个等级。
     *
     * @param def       附魔定义
     * @param random01  [0, 1) 区间的随机数
     * @return 1 .. def.maxLevel()
     */
    public static int roll(EnchantDef def, double random01) {
        int max = Math.max(1, def.maxLevel());
        if (max == 1) {
            return 1;
        }
        double total = totalWeight(max);
        double remaining = clamp01(random01) * total;
        for (int level = 1; level <= max; level++) {
            remaining -= weight(level);
            if (remaining <= 0.0D) {
                return level;
            }
        }
        return max;
    }

    /** 抽出某一级的概率。 */
    public static double probability(EnchantDef def, int level) {
        int max = Math.max(1, def.maxLevel());
        if (level < 1 || level > max) {
            return 0.0D;
        }
        return weight(level) / totalWeight(max);
    }

    private static double weight(int level) {
        return 1.0D / level;
    }

    private static double totalWeight(int max) {
        double total = 0.0D;
        for (int level = 1; level <= max; level++) {
            total += weight(level);
        }
        return total;
    }

    private static double clamp01(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        return Math.min(value, 0.9999999D);
    }
}
