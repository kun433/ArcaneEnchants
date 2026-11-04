package com.arcaneenchants;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 复杂附魔的代码行为。属性加成由 {@link AttributeSync} 负责，这里只处理属性表达不了的效果。
 */
public final class ListenerBehaviors implements Listener {

    private final ArcaneEnchantsPlugin plugin;
    private final Map<UUID, Long> lifestealCooldown = new HashMap<>();
    private final Map<UUID, Long> warderCooldown = new HashMap<>();

    public ListenerBehaviors(ArcaneEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- 永固：无限耐久

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (!plugin.settings().eternalDurability) {
            return;
        }
        if (Enchants.level(event.getItem(), "eternal") > 0) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------- 贪婪：额外掉落

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        if (!plugin.settings().greedExtraDrops) {
            return;
        }
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        int level = Enchants.level(tool, "greed");
        if (level <= 0) {
            return;
        }
        // 每级 10% 概率，上限 90%
        double chance = Math.min(0.9D, 0.1D * level);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ItemStack> duplicates = new ArrayList<>(2);
        for (Item item : event.getItems()) {
            if (item.isDead() || !item.isValid()) {
                continue;
            }
            if (random.nextDouble() < chance) {
                duplicates.add(item.getItemStack().clone());
            }
        }
        if (duplicates.isEmpty()) {
            return;
        }
        var world = event.getBlock().getWorld();
        var location = event.getBlock().getLocation().add(0.5D, 0.4D, 0.5D);
        // 事件期间不要动实体列表，推迟到下一 tick 生成
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (ItemStack stack : duplicates) {
                world.dropItemNaturally(location, stack);
            }
        });
    }

    // ---------------------------------------------------------------- 血契 / 回响：命中时

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        double damage = event.getFinalDamage();

        if (plugin.settings().lifestealEnabled) {
            int level = Enchants.level(weapon, "bloodpact");
            if (level > 0 && damage > 0.0D) {
                long now = System.currentTimeMillis();
                long readyAt = lifestealCooldown.getOrDefault(player.getUniqueId(), 0L);
                if (now >= readyAt) {
                    var maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
                    double max = maxHealth == null ? 20.0D : maxHealth.getValue();
                    double heal = damage * 0.08D * level;
                    if (heal > 0.0D && player.getHealth() < max) {
                        player.setHealth(Math.min(max, player.getHealth() + heal));
                        lifestealCooldown.put(player.getUniqueId(), now + plugin.settings().lifestealCooldownMs);
                    }
                }
            }
        }

        if (plugin.settings().igniteEnabled) {
            int level = Enchants.level(weapon, "resonance");
            if (level > 0) {
                int ticks = Math.min(200, 40 * level);
                if (target.getFireTicks() < ticks) {
                    target.setFireTicks(ticks);
                }
            }
        }
    }

    // ---------------------------------------------------------------- 守望：低血量抗性

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        if (!plugin.settings().warderEnabled) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack chestplate = player.getInventory().getChestplate();
        int level = Enchants.level(chestplate, "warder");
        if (level <= 0) {
            return;
        }
        var maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? 20.0D : maxHealth.getValue();
        if (max <= 0.0D) {
            return;
        }
        double afterHit = (player.getHealth() - event.getFinalDamage()) / max;
        if (afterHit > 0.35D) {
            return;
        }
        long now = System.currentTimeMillis();
        long readyAt = warderCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            return;
        }
        int duration = 40 + 20 * level;
        int amplifier = Math.min(2, level - 1);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, duration, amplifier, false, true, true));
        warderCooldown.put(player.getUniqueId(), now + plugin.settings().warderCooldownSeconds * 1000L);
    }
}
