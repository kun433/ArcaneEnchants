# ArcaneEnchants

[**English**](README.md) · **中文文档**

[下载最新版](https://github.com/kun433/ArcaneEnchants/releases/latest) · [全部发布](https://github.com/kun433/ArcaneEnchants/releases)

**Minecraft 26.3（Paper 系服务端：Paper / Purpur / Leaf）** 的自定义附魔插件。共 10 个附魔，全部带属性加成，其中 5 个还带独立的行为效果。

## 获取规则：附魔分两档

效果越强，越难获得：

| | 简单附魔（5 个） | 复杂附魔（5 个） |
| --- | --- | --- |
| 效果构成 | 只有属性加成 | 属性加成 + 行为效果 |
| 附魔台 | ✅ 能刷出 | ❌ 永远刷不出 |
| 战利品箱 | 6% 概率出书 | 0.8% 概率出书（很稀有） |
| 图书管理员交易 | ❌ | ✅ 唯一的稳定来源 |
| 铁砧合书 / 砂轮去除 | 同原版规则 | 同原版规则 |

- **简单附魔**相当于新增了几种原版风格的小附魔：附魔台就能刷出，开箱子也常有。
- **复杂附魔**要认准图书管理员：村民达到 **3 级（专家）及以上**、每次获得新交易时，有 **35%** 概率把其中一格换成复杂附魔书，价格 = **24 绿宝石 × 书的等级**（1 级 24 / 2 级 48 / 3 级 72）。
- 战利品箱和交易给的书等级随机：**1 级最常见，等级越高越稀有**。每个箱子默认最多 1 本，钓鱼默认不出。

## 附魔详解

### 简单附魔（附魔台可得）

#### 轻盈（swiftstep）

**跑得更快。**

- 可附魔：任意靴子；最高 3 级
- 属性：移动速度每级 **+4%**（满级 +12%）
- 获取：附魔台 / 战利品箱 6%

#### 坚韧（fortified）

**更耐打。**

- 可附魔：任意盔甲；最高 3 级
- 属性：盔甲值每级 **+1**（满级 +3）
- 获取：附魔台 / 战利品箱 6%

#### 敏锐（keen）

**砍得更疼。**

- 可附魔：剑；最高 3 级
- 属性：攻击伤害每级 **+0.75**（满级 +2.25）
- 获取：附魔台 / 战利品箱 6%

#### 潜行（stealthy）

**按住 Shift 潜行时走得更快。**

- 可附魔：护腿；最高 3 级
- 属性：潜行速度每级 **+15%**（满级 +45%）
- 获取：附魔台 / 战利品箱 6%

#### 采掘（excavator）

**挖方块更快。**

- 可附魔：镐 / 铲 / 斧 / 锄 / 剪刀；最高 3 级
- 属性：挖掘速度每级 **+8%**（满级 +24%）
- 获取：附魔台 / 战利品箱 6%

### 复杂附魔（只能交易或稀有战利品）

#### 永固（eternal）

**装备永不磨损。**

- 可附魔：一切有耐久的物品（盔甲、工具、武器、弓、鞘翅、盾牌等）；只有 1 级
- 属性：击退抗性 **+10%**
- 行为：耐久值永远不掉，等于无限耐久
- 获取：图书管理员交易（24 绿宝石）/ 战利品箱 0.8%

#### 贪婪（greed）

**挖方块有机会多掉一份。**

- 可附魔：镐 / 铲 / 斧 / 锄 / 剪刀；最高 3 级
- 属性：幸运每级 **+1**（满级 +3，影响部分原版战利品判定）
- 行为：挖掉方块后，每个掉落物有 **10%/级** 概率额外复制一份（满级 30%，封顶 90%）
- 获取：图书管理员交易（24 / 48 / 72 绿宝石，超过 64 的价格会拆到两个交易格）/ 战利品箱 0.8%

#### 血契（bloodpact）

**攻击吸血，血量上限更高。**

- 可附魔：剑；最高 3 级
- 属性：最大生命值每级 **+1**（满级 +3）
- 行为：对生物造成伤害后，按伤害的 **8%/级** 回复自己（0.5 秒冷却；满级 24%，打 10 点伤害回 2.4）
- 获取：图书管理员交易（24 / 48 / 72 绿宝石，超过 64 的价格会拆到两个交易格）/ 战利品箱 0.8%

#### 守望（warder）

**快被打死时自动给一层抗性保命。**

- 可附魔：胸甲；最高 3 级
- 属性：盔甲韧性每级 **+1**（满级 +3）
- 行为：当一次受击会把你的生命值打到 **35% 及以下**时，立即获得短暂的「抗性提升」——1 级抗性 I 3 秒 / 2 级抗性 II 4 秒 / 3 级抗性 III 5 秒；触发冷却 20 秒
- 获取：图书管理员交易（24 / 48 / 72 绿宝石，超过 64 的价格会拆到两个交易格）/ 战利品箱 0.8%

#### 回响（resonance）

**砍谁谁着火。**

- 可附魔：近战武器（剑、斧，以原版 `enchantable/melee_weapon` 标签为准）；最高 3 级
- 属性：攻击速度每级 **+4%**（满级 +12%）
- 行为：命中生物时将其点燃，每级 2 秒（满级 6 秒）
- 获取：图书管理员交易（24 / 48 / 72 绿宝石，超过 64 的价格会拆到两个交易格）/ 战利品箱 0.8%

## 属性是怎么生效的

属性加成写进物品的 `attribute_modifiers` 组件，因此物品提示框里能直接看到（例如「+12% 移动速度」），由游戏本体生效，不靠插件逐 tick 挂临时属性。附魔等级变化时会自动重算，所以用铁砧/砂轮移除附魔不会留下残留属性。

1.20.5 组件化之后，显式写入 `attribute_modifiers` 组件会**替换**物品的原版默认属性。因此同步时会把
原版默认修饰符（`Material#getDefaultAttributeModifiers`）连同本插件的加成一起写进组件，否则附魔过的
钻石剑会丢掉原版攻击伤害。已带有其它插件自定义修饰符的物品只叠加本插件的部分；移除附魔后如果
组件内容只剩下原版默认值，则整个撤掉组件，让物品回落到隐式默认。v1.1.0 时期写坏的物品
（默认属性被抹掉）会在下一次同步时自动修复。

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

当前维护目标是 **Paper 系 26.3**，对应 jar 为 `ArcaneEnchants-1.1.1-26.3.jar`，运行时要求 **Java 25**。

1. 从 [发布页](https://github.com/kun433/ArcaneEnchants/releases/latest) 下载 `ArcaneEnchants-1.1.1-26.3.jar`，或自行构建。
2. 把 jar 放进 `plugins/`。
3. 启动一次服务器，生成 `plugins/ArcaneEnchants/config.yml`。
4. 之后每次启动都会注册附魔，`/ae status` 应当显示 `10/10`。

1.21.11 的构建停在 **1.0.0**（`ArcaneEnchants-1.0.0-1.21.11.jar`，Java 21），已不再更新，
只在 [v1.0.0 发布](https://github.com/kun433/ArcaneEnchants/releases/tag/v1.0.0) 页留存；仓库里的 `1.21.11/` 目录作为历史记录保留，
不跟随新功能。两个 jar 不能互换：26.3 的构建只能进 26.3+，1.21.11 的构建也进不了 26.3。
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
（MiniMessage 格式，中英双语）。各附魔本身的数值（每级属性、触发条件）定义在代码里。

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
```

`lib\` 只用于编译，不会打进产物，并且已在 `.gitignore` 中。26.3 的 Paper API pom 已把 brigadier 与
`adventure-text-logger-slf4j` 列为 compile 依赖，所以 `26.3\lib\compile\` 里只有 API 与库 jar，**不需要服务端 jar**；
`fetch-libs.ps1` 会把这些 jar 全部下到该目录。
路径不同时可用 `ARCANE_JDK`、`ARCANE_LIBS` 覆盖。

`1.21.11/` 目录仍保留 1.0.0 的构建脚本（JDK 21 + `lib/server` 布局），仅作为历史记录，不随新功能更新。

## 验证情况

- 26.3：`javac --release 25`，17 个类，零错误、零弃用警告；`tests\run-tests.ps1` 2813 项离线断言 0 失败。
- 1.21.11（1.0.0 那代，未改动）：`javac --release 21` 零错误，同样 2813 项断言 0 失败。
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
