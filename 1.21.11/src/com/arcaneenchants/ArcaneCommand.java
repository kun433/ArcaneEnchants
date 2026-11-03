package com.arcaneenchants;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * {@code /arcaneench} 管理指令（别名 {@code /ae}）。
 */
public final class ArcaneCommand implements BasicCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ArcaneEnchantsPlugin plugin;

    public ArcaneCommand(ArcaneEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        if (args.length == 0) {
            help(sender);
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> list(sender);
            case "info" -> {
                if (args.length < 2) {
                    send(sender, "info-usage");
                    return;
                }
                info(sender, args[1]);
            }
            case "give" -> {
                if (args.length < 3) {
                    send(sender, "give-usage");
                    return;
                }
                int level = parseLevel(args.length >= 4 ? args[3] : "1");
                give(sender, args[1], args[2], level);
            }
            case "apply" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, "player-only");
                    return;
                }
                if (args.length < 2) {
                    send(sender, "apply-usage");
                    return;
                }
                int level = parseLevel(args.length >= 3 ? args[2] : "1");
                apply(player, args[1], level);
            }
            case "status" -> status(sender);
            case "reload" -> {
                plugin.reload();
                send(sender, "reloaded");
            }
            default -> help(sender);
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length == 0 || args.length == 1) {
            return List.of("list", "info", "give", "apply", "status", "reload");
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (sub) {
                case "info", "apply" -> ids();
                case "give" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                default -> List.of();
            };
        }
        if (args.length == 3 && sub.equals("give")) {
            return ids();
        }
        return List.of();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission("arcaneenchants.admin");
    }

    @Override
    public String permission() {
        return "arcaneenchants.admin";
    }

    // ---------------------------------------------------------------- 子指令实现

    private void help(CommandSender sender) {
        send(sender, "help-header", Defs.all().size(),
                Defs.byTier(Tier.SIMPLE).size(), Defs.byTier(Tier.COMPLEX).size());
        send(sender, "help-list");
        send(sender, "help-info");
        send(sender, "help-give");
        send(sender, "help-apply");
        send(sender, "help-status");
        send(sender, "help-reload");
    }

    private void list(CommandSender sender) {
        send(sender, "list-header");
        for (EnchantDef def : Defs.all()) {
            String tier = def.tableAvailable() ? plugin.msg().raw("tier-simple") : plugin.msg().raw("tier-complex");
            String attributes = String.join("、", def.attributeLines());
            send(sender, "list-entry",
                    def.id(), def.displayName(plugin.language()), tier,
                    def.maxLevel(), attributes);
        }
    }

    private void info(CommandSender sender, String id) {
        EnchantDef def = Defs.byId(id);
        if (def == null) {
            send(sender, "unknown-id", id);
            return;
        }
        send(sender, "info-header", def.displayName(plugin.language()), def.id());
        send(sender, "info-tier", def.tableAvailable() ? plugin.msg().raw("tier-simple") : plugin.msg().raw("tier-complex"));
        send(sender, "info-summary", def.summary(plugin.language()));
        send(sender, "info-levels", def.maxLevel());
        send(sender, "info-attributes", String.join("、", def.attributeLines()));
        send(sender, "info-behavior", def.behavior().name());
        send(sender, "info-items", def.itemTag());
    }

    private void give(CommandSender sender, String playerName, String id, int level) {
        EnchantDef def = Defs.byId(id);
        if (def == null) {
            send(sender, "unknown-id", id);
            return;
        }
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            send(sender, "unknown-player", playerName);
            return;
        }
        ItemStack book = Enchants.book(def, level);
        if (book == null) {
            send(sender, "not-registered", def.id());
            return;
        }
        var leftovers = target.getInventory().addItem(book);
        leftovers.values().forEach(stack -> target.getWorld().dropItemNaturally(target.getLocation(), stack));
        send(sender, "gave", target.getName(), def.displayName(plugin.language()), Math.min(level, def.maxLevel()));
    }

    private void apply(Player player, String id, int level) {
        EnchantDef def = Defs.byId(id);
        if (def == null) {
            send(player, "unknown-id", id);
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            send(player, "no-item");
            return;
        }
        var enchantment = Enchants.get(def.id());
        if (enchantment == null) {
            send(player, "not-registered", def.id());
            return;
        }
        if (!enchantment.canEnchantItem(item)) {
            send(player, "cannot-enchant", def.displayName(plugin.language()));
            return;
        }
        int clamped = Math.max(1, Math.min(def.maxLevel(), level));
        item.addUnsafeEnchantment(enchantment, clamped);
        AttributeSync.sync(item);
        player.getInventory().setItemInMainHand(item);
        send(player, "applied", def.displayName(plugin.language()), clamped);
    }

    private void status(CommandSender sender) {
        Settings settings = plugin.settings();
        send(sender, "status-header");
        send(sender, "status-registered", Enchants.resolvedCount(), Defs.all().size());
        send(sender, "status-loot",
                String.format(Locale.ROOT, "%.1f%%", settings.simpleLootChance * 100.0D),
                String.format(Locale.ROOT, "%.2f%%", settings.complexLootChance * 100.0D));
        send(sender, "status-trade",
                String.format(Locale.ROOT, "%.0f%%", settings.tradeChance * 100.0D), settings.tradeEmeralds);
        if (!Enchants.missing().isEmpty()) {
            send(sender, "status-missing", String.join(", ", Enchants.missing()));
        }
        send(sender, "status-discoverable",
                String.join(", ", Defs.byTier(Tier.SIMPLE).stream()
                        .filter(def -> {
                            var enchantment = Enchants.get(def.id());
                            try {
                                return enchantment != null && enchantment.isDiscoverable();
                            } catch (Throwable ignored) {
                                return false;
                            }
                        })
                        .map(def -> def.displayName(plugin.language())).toList()));
        send(sender, "status-table", String.join(", ", Defs.byTier(Tier.SIMPLE).stream()
                .map(def -> def.displayName(plugin.language())).toList()));
    }

    // ---------------------------------------------------------------- 工具

    private static List<String> ids() {
        List<String> ids = new ArrayList<>(Defs.all().size());
        for (EnchantDef def : Defs.all()) {
            ids.add(def.id());
        }
        return ids;
    }

    private static int parseLevel(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void send(CommandSender sender, String key, Object... args) {
        String text = plugin.msg().format(key, args);
        sender.sendMessage(MINI.deserialize(text));
    }

    /** 供其它类复用的消息发送。 */
    static void send(CommandSender sender, Msg msg, String key, Object... args) {
        sender.sendMessage(MINI.deserialize(msg.format(key, args)));
    }

    /** 组件版本，供调试输出使用。 */
    static Component component(String text) {
        return MINI.deserialize(text);
    }
}
