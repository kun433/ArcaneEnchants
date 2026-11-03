package com.arcaneenchants;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

/**
 * 属性同步的触发点：玩家进服、切换手上物品时各同步一次，
 * 另外由 {@link ArcaneEnchantsPlugin} 周期性兜底扫描背包。
 */
public final class ListenerSync implements Listener {

    private final ArcaneEnchantsPlugin plugin;

    public ListenerSync(ArcaneEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.settings().attributesEnabled) {
            return;
        }
        // 等背包数据完全就绪后再扫
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> AttributeSync.syncInventory(event.getPlayer().getInventory()), 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeld(PlayerItemHeldEvent event) {
        if (!plugin.settings().attributesEnabled) {
            return;
        }
        var inventory = event.getPlayer().getInventory();
        var item = inventory.getItem(event.getNewSlot());
        if (AttributeSync.sync(item)) {
            inventory.setItem(event.getNewSlot(), item);
        }
    }
}
