# ArcaneEnchants

**English** · [**中文文档**](README.zh-CN.md)

[Download](https://github.com/kun433/ArcaneEnchants/releases/latest) · [All releases](https://github.com/kun433/ArcaneEnchants/releases)

Custom enchantments for **Minecraft 26.3 on Paper-family servers (Paper / Purpur / Leaf)**. Ten enchantments, all with attribute bonuses; five of them also add a real behaviour on top.

## How acquisition works: two tiers

The stronger the effect, the harder it is to get:

| | Simple (5) | Complex (5) |
| --- | --- | --- |
| What it gives | Attribute bonus only | Attribute bonus + a behaviour |
| Enchanting table | ✅ Can appear | ❌ Never appears |
| Loot chests | 6% chance per chest | 0.8% chance per chest (very rare) |
| Librarian trades | ❌ | ✅ The only reliable source |
| Anvil combining / grindstone removal | Vanilla rules | Vanilla rules |

- **Simple** enchantments behave like a few extra vanilla-style enchants: the enchanting table offers them, and chests often contain their books.
- **Complex** enchantments come from librarians: whenever a librarian reaches **level 3 (expert) or above** and gains a new trade, there is a **35%** chance one slot is replaced with a complex book. Price = **24 emeralds × book level** (level 1: 24 / level 2: 48 / level 3: 72).
- Book levels from chests and trades are random: **level I is the most common, higher levels are rarer**. By default each container holds at most 1 book, and fishing loot is excluded.

## The enchantments

### Simple (available in the enchanting table)

#### Swiftstep

**Run faster.**

- Applies to: any boots; max level 3
- Attribute: movement speed **+4%** per level (+12% at max)
- How to get: enchanting table / loot chests 6%

#### Fortified

**Take hits better.**

- Applies to: any armor piece; max level 3
- Attribute: armor **+1** per level (+3 at max)
- How to get: enchanting table / loot chests 6%

#### Keen

**Hit harder.**

- Applies to: swords; max level 3
- Attribute: attack damage **+0.75** per level (+2.25 at max)
- How to get: enchanting table / loot chests 6%

#### Stealthy

**Sneak (hold Shift) noticeably faster.**

- Applies to: leggings; max level 3
- Attribute: sneaking speed **+15%** per level (+45% at max)
- How to get: enchanting table / loot chests 6%

#### Excavator

**Break blocks faster.**

- Applies to: pickaxes / shovels / axes / hoes / shears; max level 3
- Attribute: block break speed **+8%** per level (+24% at max)
- How to get: enchanting table / loot chests 6%

### Complex (librarian trades or rare loot only)

#### Eternal

**Gear never wears out.**

- Applies to: everything with durability (armor, tools, weapons, bows, elytra, shields, …); level 1 only
- Attribute: knockback resistance **+10%**
- Behaviour: durability never decreases — effectively unbreakable
- How to get: librarian trade (24 emeralds) / loot chests 0.8%

#### Greed

**Chance to double your mined drops.**

- Applies to: pickaxes / shovels / axes / hoes / shears; max level 3
- Attribute: luck **+1** per level (+3 at max, affects some vanilla loot rolls)
- Behaviour: each drop from a mined block has a **10%/level** chance to be duplicated (30% at max, hard cap 90%)
- How to get: librarian trade (24 / 48 / 72 emeralds; prices above 64 are split across both trade slots) / loot chests 0.8%

#### Bloodpact

**Lifesteal plus extra max health.**

- Applies to: swords; max level 3
- Attribute: max health **+1** per level (+3 at max)
- Behaviour: after damaging a mob, heal for **8%/level** of the damage dealt (0.5s cooldown; 24% at max — hitting for 10 damage heals 2.4)
- How to get: librarian trade (24 / 48 / 72 emeralds; prices above 64 are split across both trade slots) / loot chests 0.8%

#### Warder

**Auto-Resistance when you are about to die.**

- Applies to: chestplates; max level 3
- Attribute: armor toughness **+1** per level (+3 at max)
- Behaviour: when a hit would drop your health to **35% or below**, you instantly gain a short Resistance buff — level 1: Resistance I 3s / level 2: Resistance II 4s / level 3: Resistance III 5s; 20s cooldown
- How to get: librarian trade (24 / 48 / 72 emeralds; prices above 64 are split across both trade slots) / loot chests 0.8%

#### Resonance

**Everything you hit catches fire.**

- Applies to: melee weapons (swords, axes — follows the vanilla `enchantable/melee_weapon` tag); max level 3
- Attribute: attack speed **+4%** per level (+12% at max)
- Behaviour: hits ignite the target for 2s per level (6s at max)
- How to get: librarian trade (24 / 48 / 72 emeralds; prices above 64 are split across both trade slots) / loot chests 0.8%

## How attributes are applied

Attribute bonuses are written into the item's `attribute_modifiers` component, so they show up in the vanilla tooltip
(for example "+12% movement speed") and are applied by the game itself — no per-tick temporary attributes from the plugin.
They are recalculated whenever the item's enchantment levels change, so removing an enchantment in an anvil or grindstone
leaves no leftovers.

Since the 1.20.5 data-component rework, an explicit `attribute_modifiers` component **replaces** the item's default
attribute modifiers. The sync therefore writes the vanilla default modifiers (from
`Material#getDefaultAttributeModifiers`) back into the component together with the plugin's own bonuses — otherwise
an enchanted diamond sword would lose its native attack damage. Items that already carry third-party custom
modifiers are left untouched except for the plugin's own entries. When an enchantment is removed and the component
would only contain the vanilla defaults, the component is dropped entirely so the item falls back to its implicit
defaults. Items written by v1.1.0 (defaults stripped) are repaired automatically on the next sync.

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

The maintained target is **Paper-family 26.3**: `ArcaneEnchants-1.1.1-26.3.jar`, which requires **Java 25**.

1. Download `ArcaneEnchants-1.1.1-26.3.jar` from the [releases page](https://github.com/kun433/ArcaneEnchants/releases/latest), or build it yourself.
2. Drop the jar into `plugins/`.
3. Start the server once; `plugins/ArcaneEnchants/config.yml` is generated.
4. Enchantments are registered at server start. `/ae status` should report `10/10` registered.

The 1.21.11 build stays at **1.0.0** (`ArcaneEnchants-1.0.0-1.21.11.jar`, Java 21). It is no longer updated and only
remains on the [v1.0.0 release](https://github.com/kun433/ArcaneEnchants/releases/tag/v1.0.0); the `1.21.11/` directory is
kept in the repository as a historical record and does not receive new features. The two jars are not interchangeable:
the 26.3 build only loads on 26.3+, and the 1.21.11 build will not load on 26.3.
This is a Paper plugin (`paper-plugin.yml`), so it does not work on Spigot or Fabric.

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
attribute syncing, and all in-game messages (MiniMessage format, Chinese and English). The per-enchantment numbers
(attributes per level, trigger conditions) are defined in code.

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
# 26.3: JDK 25 required (Paper API 26.3 is compiled to class file version 69)
powershell -ExecutionPolicy Bypass -File 26.3\fetch-libs.ps1   # once after cloning, pulls the compile dependencies
powershell -ExecutionPolicy Bypass -File 26.3\build.ps1
```

`lib\` is compile-only, never bundled, and gitignored. The Paper API 26.3 pom already lists brigadier and
`adventure-text-logger-slf4j` as compile dependencies, so `26.3\lib\compile\` holds only API and library jars —
**no server jar is needed** — and `fetch-libs.ps1` downloads all of them.
Override with `ARCANE_JDK` and `ARCANE_LIBS` if your paths differ.

The `1.21.11/` directory still carries the 1.0.0 build script (JDK 21 plus the `lib/server` layout) as a historical record;
it is not updated with new features.

## Validation

- 26.3: `javac --release 25`, 17 classes, no errors and no deprecation warnings; `tests\run-tests.ps1` passes
  2813 offline assertions with 0 failures.
- 1.21.11 (the 1.0.0 build, unchanged): `javac --release 21` clean, and the same 2813 assertions pass with 0 failures.
- Assertions cover "every enchantment has attributes", tier rules (simple = attributes only and table-available,
  complex = behaviour plus table-excluded), numeric ranges, tag and slot names that really exist on the server,
  fallback item list sanity, attribute growth per level, level-roll probability (sums to 1, level I most common),
  the 0.8% config default, and the `paper-plugin.yml` bootstrapper plus `api-version` wiring.
- The 26.3 item tags were checked one by one against vanilla 26.3 `data/minecraft/tags/item`: `swords`,
  `enchantable/armor`, `enchantable/foot_armor`, `enchantable/leg_armor`, `enchantable/chest_armor`,
  `enchantable/mining`, `enchantable/durability` and `enchantable/melee_weapon` all exist, and 26.x additions such as
  `#minecraft:spears` and copper gear are already covered by the fallback item lists. The `in_enchanting_table`,
  `non_treasure` and `treasure` enchantment tags are also unchanged.
- No live in-game smoke test was run; the enchantment registration path can only be confirmed on a running server.

## License

MIT, see [LICENSE](LICENSE).
