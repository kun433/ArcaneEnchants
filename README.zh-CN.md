# ArcaneEnchants

[**English**](README.md) · **中文文档**

[下载最新版](https://github.com/kun433/ArcaneEnchants/releases/latest) · [全部发布](https://github.com/kun433/ArcaneEnchants/releases)

**Minecraft 26.3（Leaf / Paper）** 的自定义附魔插件，同一套功能另外提供 **1.21.11** 构建。共 10 个附魔，每个都带独立属性加成，
并且获取难度与该附魔真正做了多少事挂钩：

- **简单附魔**（只有属性加成）能直接从附魔台刷出，也会以正常概率出现在战利品箱里。
- **复杂附魔**（属性 + 独立行为）永远刷不出附魔台，只能通过图书管理员交易，或在战利品箱里以 **0.8%** 的稀有概率出现。

## 附魔一览

| id | 名称 | 层级 | 最高级 | 属性加成 | 行为 | 获取途径 |
| --- | --- | --- | --- | --- | --- | --- |
| `swiftstep` | 轻盈 | 简单 | 3 | `movement_speed` 每级 +4%（脚部） | – | 附魔台、战利品 6% |
| `fortified` | 坚韧 | 简单 | 3 | `armor` 每级 +1（护甲） | – | 附魔台、战利品 6% |
| `keen` | 敏锐 | 简单 | 3 | `attack_damage` 每级 +0.75（主手） | – | 附魔台、战利品 6% |
| `stealthy` | 潜行 | 简单 | 3 | `sneaking_speed` 每级 +15%（腿部） | – | 附魔台、战利品 6% |
| `excavator` | 采掘 | 简单 | 3 | `block_break_speed` 每级 +8%（主手） | – | 附魔台、战利品 6% |
| `eternal` | 永固 | 复杂 | 1 | `knockback_resistance` +0.1（任意槽位） | 耐久永不消耗 | 村民交易、战利品 0.8% |
| `greed` | 贪婪 | 复杂 | 3 | `luck` 每级 +1（主手） | 每级 10% 概率复制一次方块掉落 | 村民交易、战利品 0.8% |
| `bloodpact` | 血契 | 复杂 | 3 | `max_health` 每级 +1（手持） | 造成伤害的 8%/级 回复自身生命（0.5 秒冷却） | 村民交易、战利品 0.8% |
| `warder` | 守望 | 复杂 | 3 | `armor_toughness` 每级 +1（胸甲） | 生命低于 35% 时受击获得抗性提升（20 秒冷却） | 村民交易、战利品 0.8% |
| `resonance` | 回响 | 复杂 | 3 | `attack_speed` 每级 +4%（主手） | 命中点燃目标，每级 2 秒 | 村民交易、战利品 0.8% |

属性加成会写进物品的 `attribute_modifiers` 组件，因此在原版提示里就能看到，并由游戏本体生效。
附魔等级变化时会重新计算，所以用铁砧/砂轮移除附魔不会留下残留属性。

可附魔物品取自原版 `enchantable/*` 物品标签（`#minecraft:swords`、`minecraft:enchantable/armor`、
`minecraft:enchantable/mining`、`minecraft:enchantable/durability` 等），新增的工具材质会自动覆盖。
标签解析失败时退回显式物品 id 列表，并先按服务端物品注册表过滤一遍。

## 分层是怎么实现的

1. 附魔在 **bootstrap 阶段** 注册（`paper-plugin.yml` 的 `bootstrapper`）。附魔注册表在 `onEnable` 之前就已冻结，
   所以只能用 `RegistryEvents.ENCHANTMENT` 在注册表组合时写入。
2. 简单附魔通过 `LifecycleEvents.TAGS` 写进 `#minecraft:in_enchanting_table` 与 `#minecraft:non_treasure`，
   复杂附魔写进 `#minecraft:treasure`。这样「复杂附魔刷不出附魔台」是原版规则直接生效，不需要插件额外拦截。
3. 战利品箱走 `LootGenerateEvent`（简单一次判定 + 复杂一次判定），村民走 `VillagerAcquireTradeEvent`
   （把一格交易替换成附魔书，价格随书等级上浮）。
4. 万一标签那一步失效，简单附魔仍能通过战利品箱和铁砧获得，插件会在启动时告警，`/ae status` 也会显示，
   不会静默失效。

## 安装

先确认服务端版本，再下对应的 jar：

| 服务端 | jar | 运行时要求 |
| --- | --- | --- |
| Leaf / Paper **26.3** | `ArcaneEnchants-1.1.0-26.3.jar` | Java 25 |
| Leaf / Paper **1.21.11** | `ArcaneEnchants-1.1.0-1.21.11.jar` | Java 21 |

1. 从 [发布页](https://github.com/kun433/ArcaneEnchants/releases/latest) 下载对应版本的 jar，或自行构建。
2. 把 jar 放进 `plugins/`。
3. 启动一次服务器，生成 `plugins/ArcaneEnchants/config.yml`。
4. 之后每次启动都会注册附魔，`/ae status` 应当显示 `10/10`。

两个构建的源码与功能完全一致，只有 `paper-plugin.yml` 的 `api-version`（`26.3` / `1.21`）与编译目标不同。
26.3 是 Java 25 平台（原版 26.3 的 `version.json` 即要求 Java 25），jar 不能放进 Java 21 环境；反之 1.21.11 的构建也无法在 26.3 上加载。
本插件是 Paper plugin 格式（`paper-plugin.yml`），不能用于 Spigot 或 Fabric。

## 指令与权限

| 指令 | 说明 |
| --- | --- |
| `/ae list` | 列出全部附魔的层级、最高等级与属性 |
| `/ae info <id>` | 查看单个附魔详情 |
| `/ae give <玩家> <id> [等级]` | 给予附魔书 |
| `/ae apply <id> [等级]` | 给主手物品附魔 |
| `/ae status` | 查看注册、战利品与交易状态 |
| `/ae reload` | 重载配置 |

别名 `/arcaneench`、`/aench`；权限 `arcaneenchants.admin`（默认 op）。

## 配置

`config.yml` 里可以调整：战利品概率、交易价格与概率、各行为开关与冷却、属性同步、以及全部游戏内提示文本
（MiniMessage 格式，中英双语）。

```yaml
loot:
  simple-chance: 0.06     # 每个战利品箱出现简单附魔书的概率
  complex-chance: 0.008   # 每个战利品箱出现复杂附魔书的概率（0.8%）
trades:
  chance: 0.35            # 图书管理员的一格交易被替换成复杂附魔书的概率
  emeralds: 24            # 基础价格，实际价格 = 该值 × 书等级
```

附魔书等级按 `1/等级` 加权抽取：I 级最常见，等级越高越稀有。

## 构建

```powershell
# 26.3：需要 JDK 25（Paper API 26.3 的 class 版本就是 69）
powershell -ExecutionPolicy Bypass -File 26.3\fetch-libs.ps1   # 首次克隆后跑一次，下载编译依赖
powershell -ExecutionPolicy Bypass -File 26.3\build.ps1

# 1.21.11：需要 JDK 21
powershell -ExecutionPolicy Bypass -File 1.21.11\build.ps1
```

`lib\` 只用于编译，不会打进产物，并且已在 `.gitignore` 中。26.3 的 Paper API pom 已把 brigadier 与
`adventure-text-logger-slf4j` 列为 compile 依赖，所以 `26.3\lib\compile\` 里只有 API 与库 jar，**不需要服务端 jar**；
`fetch-libs.ps1` 会把这些 jar 全部下到该目录。1.21.11 仍沿用原来的 `lib\compile\` + `lib\server\` 布局。
路径不同时可用 `ARCANE_JDK`、`ARCANE_LIBS`、`ARCANE_SERVER_LIBS` 覆盖。

## 验证情况

- 26.3：`javac --release 25`，17 个类，零错误、零弃用警告；`tests\run-tests.ps1` 2813 项离线断言 0 失败。
- 1.21.11：`javac --release 21`，17 个类，零错误；同样 2813 项断言 0 失败。
- 断言覆盖「每个附魔都必须有属性」、分层规则（简单层 = 只有属性且可上附魔台，复杂层 = 有行为且不可上附魔台）、
  数值区间、服务端真实存在的标签与槽位名、回退物品清单合法性、属性随等级增长、等级抽取概率
  （和为 1、I 级最常见）、0.8% 默认概率、以及 `paper-plugin.yml` 的 bootstrapper 与 `api-version` 接线。
- 26.3 的物品标签已逐项对照原版 26.3 的 `data/minecraft/tags/item`：`swords`、`enchantable/armor`、
  `enchantable/foot_armor`、`enchantable/leg_armor`、`enchantable/chest_armor`、`enchantable/mining`、
  `enchantable/durability`、`enchantable/melee_weapon` 全部存在；`#minecraft:spears`、铜制装备等 26.x 新增内容
  也已包含在回退物品列表里。附魔侧的 `in_enchanting_table`、`non_treasure`、`treasure` 三个标签同样保留。
- 未做真机进服冒烟测试；附魔注册这条链路只能在真实服务端上最终确认。

## 许可

MIT，见 [LICENSE](LICENSE)。
