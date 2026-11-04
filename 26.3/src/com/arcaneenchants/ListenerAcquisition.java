package com.arcaneenchants;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 附魔的获得途径：
 * <ul>
 *   <li>战利品箱：简单附魔按 {@code loot.simple-chance}（默认 6%）出现，
 *       复杂附魔按 {@code loot.complex-chance}（默认 0.8%）出现。</li>
 *   <li>村民：图书管理员升级时按 {@code trades.chance} 把一格交易换成复杂附魔书，
 *       价格随书等级上浮。</li>
 * </ul>
 * 简单附魔同时还在 {@code #minecraft:in_enchanting_table} 标签里，可以直接刷附魔台。
 */
public final class ListenerAcquisition implements Listener {

    private final ArcaneEnchantsPlugin plugin;

    public ListenerAcquisition(ArcaneEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- 战利品箱

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        Settings settings = plugin.settings();
        if (!settings.lootEnabled || event.isPlugin()) {
            return;
        }
        LootTable table = event.getLootTable();
        if (table == null || table.getKey() == null) {
            return;
        }
        if (!allowedTable(table.getKey().getKey(), settings)) {
            return;
        }

        List<ItemStack> loot = new ArrayList<>(event.getLoot());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int added = 0;

        if (random.nextDouble() < settings.simpleLootChance) {
            ItemStack book = rollBook(Tier.SIMPLE, random);
            if (book != null) {
                loot.add(book);
                added++;
            }
        }

        if (added < settings.maxBooksPerContainer && random.nextDouble() < settings.complexLootChance) {
            ItemStack book = rollBook(Tier.COMPLEX, random);
            if (book != null) {
                loot.add(book);
                added++;
            }
        }

        if (added > 0) {
            event.setLoot(loot);
        }
    }

    private boolean allowedTable(String name, Settings settings) {
        if (name.isEmpty()) {
            return false;
        }
        if (name.startsWith("entities/") || name.contains("entity")) {
            return false;
        }
        if (name.contains("fishing") && !settings.allowFishingLoot) {
            return false;
        }
        for (String blocked : settings.lootTableBlacklist) {
            if (blocked != null && !blocked.isBlank() && name.contains(blocked.trim())) {
                return false;
            }
        }
        return true;
    }

    private ItemStack rollBook(Tier tier, ThreadLocalRandom random) {
        List<EnchantDef> pool = Defs.byTier(tier);
        if (pool.isEmpty()) {
            return null;
        }
        EnchantDef def = pool.get(random.nextInt(pool.size()));
        return Enchants.book(def, LevelRoll.roll(def, random.nextDouble()));
    }

    // ---------------------------------------------------------------- 村民交易

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVillagerTrade(VillagerAcquireTradeEvent event) {
        Settings settings = plugin.settings();
        if (!settings.tradesEnabled) {
            return;
        }
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (settings.requireLibrarian
                && (villager.getProfession() == null
                || !villager.getProfession().equals(Villager.Profession.LIBRARIAN))) {
            return;
        }
        if (villager.getVillagerLevel() < settings.tradeMinVillagerLevel) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= settings.tradeChance) {
            return;
        }

        List<EnchantDef> pool = Defs.byTier(Tier.COMPLEX);
        if (pool.isEmpty()) {
            return;
        }
        EnchantDef def = pool.get(random.nextInt(pool.size()));
        int level = LevelRoll.roll(def, random.nextDouble());
        ItemStack book = Enchants.book(def, level);
        if (book == null) {
            return;
        }

        MerchantRecipe recipe = new MerchantRecipe(book, settings.tradeMaxUses);
        recipe.addIngredient(new ItemStack(Material.EMERALD, Math.max(1, settings.tradeEmeralds * level)));
        recipe.setVillagerExperience(10);
        recipe.setExperienceReward(true);
        event.setRecipe(recipe);
    }
}
