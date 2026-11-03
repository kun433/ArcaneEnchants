# ArcaneEnchants

Custom enchantments for **Minecraft 1.21.11 on Leaf / Paper**. Ten enchantments, each with its own attribute bonuses,
and acquisition tied to how much the enchantment actually does:

- **Simple** enchantments (attribute only) show up in the enchanting table and in loot chests at a normal rate.
- **Complex** enchantments (attribute plus a real behaviour) never appear in the enchanting table; you get them from
  librarian trades or from rare loot chests at **0.8%**.

## Enchantments

| id | Name | Tier | Max | Attribute bonus | Behaviour | How to get |
| --- | --- | --- | --- | --- | --- | --- |
| `swiftstep` | Swiftstep | simple | 3 | `movement_speed` +4% per level (feet) | – | Enchanting table, loot 6% |
| `fortified` | Fortified | simple | 3 | `armor` +1 per level (armor) | – | Enchanting table, loot 6% |
| `keen` | Keen | simple | 3 | `attack_damage` +0.75 per level (main hand) | – | Enchanting table, loot 6% |
| `stealthy` | Stealthy | simple | 3 | `sneaking_speed` +15% per level (legs) | – | Enchanting table, loot 6% |
| `excavator` | Excavator | simple | 3 | `block_break_speed` +8% per level (main hand) | – | Enchanting table, loot 6% |
| `eternal` | Eternal | complex | 1 | `knockback_resistance` +0.1 (any slot) | Durability never decreases | Librarian trade, loot 0.8% |
| `greed` | Greed | complex | 3 | `luck` +1 per level (main hand) | 10% per level chance to duplicate a block drop | Librarian trade, loot 0.8% |
| `bloodpact` | Bloodpact | complex | 3 | `max_health` +1 per level (hand) | Heal 8% per level of damage dealt (0.5s cooldown) | Librarian trade, loot 0.8% |
| `warder` | Warder | complex | 3 | `armor_toughness` +1 per level (chest) | Below 35% health, getting hit grants Resistance (20s cooldown) | Librarian trade, loot 0.8% |
| `resonance` | Resonance | complex | 3 | `attack_speed` +4% per level (main hand) | Ignites the target on hit, 2s per level | Librarian trade, loot 0.8% |

Attribute bonuses are written into the item's `attribute_modifiers` component, so they show up in the vanilla tooltip
and are applied by the game itself. They are recalculated whenever the item's enchantment levels change, so removing an
enchantment in an anvil or grindstone leaves no leftovers.

Applicable items come from the vanilla `enchantable/*` item tags (`#minecraft:swords`, `minecraft:enchantable/armor`,
`minecraft:enchantable/mining`, `minecraft:enchantable/durability`, and so on), so new tool tiers are picked up
automatically. If a tag cannot be resolved, the plugin falls back to an explicit item id list, filtered against the
server's item registry.

## How the acquisition tiers are implemented

1. Enchantments are registered during the **bootstrap phase** (`bootstrapper` in `paper-plugin.yml`), because the
   enchantment registry is frozen before `onEnable`. `RegistryEvents.ENCHANTMENT` is the registry event used here.
2. Simple enchantments are added to `#minecraft:in_enchanting_table` and `#minecraft:non_treasure` through
   `LifecycleEvents.TAGS`; complex ones are added to `#minecraft:treasure`.
   Vanilla then refuses to offer complex enchantments at the table without any extra logic from this plugin.
3. Loot chests are handled by `LootGenerateEvent` (simple roll plus complex roll), and librarians by
   `VillagerAcquireTradeEvent` (a trade slot is replaced with a book, priced per book level).
4. If the tag step ever fails, simple enchantments are still obtainable from loot chests and anvils, and the plugin
   prints a warning at startup plus `/ae status` output instead of failing silently.

## Install

1. Drop `ArcaneEnchants-1.0.0-1.21.11.jar` into `plugins/`.
2. Start the server once; `plugins/ArcaneEnchants/config.yml` is generated.
3. Enchantments are registered at server start. `/ae status` should report `10/10` registered.

Leaf and Paper 1.21.11 only. This is a Paper plugin (`paper-plugin.yml`), so it does not work on Spigot or Fabric.

## Commands and permissions

| Command | Description |
| --- | --- |
| `/ae list` | List every enchantment with tier, max level and attributes |
| `/ae info <id>` | Details for one enchantment |
| `/ae give <player> <id> [level]` | Give an enchanted book |
| `/ae apply <id> [level]` | Enchant the item in your main hand |
| `/ae status` | Registration, loot and trade status |
| `/ae reload` | Reload config |

Aliases: `/arcaneench`, `/aench`. Permission: `arcaneenchants.admin` (default op).

## Configuration

Everything tunable lives in `config.yml`: loot chances, trade price and odds, per-behaviour switches and cooldowns,
attribute syncing, and all in-game messages (MiniMessage format, Chinese and English).

```yaml
loot:
  simple-chance: 0.06     # simple enchantment books per loot container
  complex-chance: 0.008   # complex enchantment books per loot container (0.8%)
trades:
  chance: 0.35            # chance a librarian trade slot becomes a complex book
  emeralds: 24            # base price, multiplied by book level
```

Book levels are drawn with a `1/level` weighting: level I is the most common, higher levels are rarer.

## Build

```powershell
# JDK 21 required; libs are compile-only and never bundled
powershell -ExecutionPolicy Bypass -File 1.21.11\build.ps1
```

`lib\compile\` needs the Paper API and Adventure jars for 1.21.11; `lib\server\` needs the Paper server jar
(compile-only, for Brigadier and Adventure logger types). Both directories are gitignored.
Override with `ARCANE_JDK`, `ARCANE_LIBS` and `ARCANE_SERVER_LIBS` if your paths differ.

## Validation

- `javac --release 21`: 17 classes, no errors.
- `tests\run-tests.ps1`: 2813 offline assertions, 0 failures. These cover "every enchantment has attributes",
  tier rules (simple = attributes only and table-available, complex = behaviour plus table-excluded),
  numeric ranges, tag and slot names valid for 1.21.11, fallback item list sanity, attribute growth per level,
  level-roll probability (sums to 1, level I most common), the 0.8% config default, and the `paper-plugin.yml`
  bootstrapper wiring.
- No live in-game smoke test was run; the enchantment registration path can only be confirmed on a running server.

## License

MIT, see [LICENSE](LICENSE).
