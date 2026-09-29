# Bond Beyond 0.2.2 — configuration (egg balance and optional Jade)

Forge creates commented TOML files automatically. Launch once; enter a world to create the server file. Close that world before editing, then reopen it. The server owns gameplay settings and sends them to clients; camera preferences belong to each player.

| File | Location | Controls |
|---|---|---|
| `iceandfire_bond_beyond-client.toml` | `.minecraft/config/` | Camera distance, height, FOV, rear-view reticle and water particles |
| `iceandfire_bond_beyond-server.toml` | Single player: `.minecraft/saves/<world>/serverconfig/`; dedicated server: `<world>/serverconfig/` | Breath AOE, bubble size, erosion, growth, wild egg drops, incubation and pressure |

For a modpack, put the server TOML in `defaultconfigs/` to seed new worlds. Existing worlds use their own `serverconfig` file. Edit the existing keys; do not append duplicate TOML sections.

## Warmer water and faster growth

This is **post-hatch aging**, separate from egg incubation. Wild and owned serpents use the same growth settings. Ocean modifiers apply while the serpent is in water in an ocean recognized by biome tags or IAF's serpent biome list. Other locations use `base_speed` alone. Sickly Dragon Meal still locks growth. Food, hunger, initial wild spawn ages and the newborn bonding timer are unchanged.

```toml
[growth]
base_speed = 1.0
ocean_temperature_affects_growth = true

[growth.ocean_multipliers]
frozen_ocean = 0.75
deep_frozen_ocean = 0.8
cold_ocean = 0.85
deep_cold_ocean = 0.9
ocean = 1.0
deep_ocean = 1.05
lukewarm_ocean = 1.15
deep_lukewarm_ocean = 1.2
warm_ocean = 1.3
```

For example, change `growth.ocean_multipliers.warm_ocean` to `2.0` for twice the ordinary growth rate in a warm ocean. `base_speed = 2.0` multiplies all of those rates again. Set `ocean_temperature_affects_growth = false` and `base_speed = 1.0` for the old uniform aging speed, or set `base_speed = 0.0` to pause natural aging. Fractional progress survives saving/reloading. Age, Stage and size still follow the existing growth curve and cap.

`eggs.incubation_speed` and the **separate** `[eggs.ocean_multipliers]` section control hatching. Their default biome multipliers are the same numbers above and preserve the 0.2.1 incubation rates. Modded oceans with Forge's cold/hot biome tags use the cold/warm entries; others use `ocean`.

## Continuous water breath

```toml
[breath]
area_damage = true
area_radius_multiplier = 1.0
bubble_size_multiplier = 1.35
spray_bubbles_per_pulse = 12
spray_half_angle_degrees = 12.0
damage_multiplier = 1.0
knockback_strength = 0.12
erosion_enabled = true
erosion_radius_multiplier = 0.75
erosion_blocks_per_second = 20
```

Wild, AI-tamed, ridden and forge breath use one shared mouth-aligned spray. Every 2 ticks it emits **4–12 small, mixed-size bubbles**, depending on Stage and scale. The maximum at Stage 5 / scale 11 is 120 bubbles/second at 20 TPS. Both the client model and server muzzle use the same neck/head/jaw transforms; bubbles begin between the open jaws.

The spray fills irregular inner and outer directions rather than forming a straight line or a perfect cone. Its half-angle grows from **4.8° to 12°** by default. A precisely aimed central droplet powers the forge and carries erosion. All droplets use the same size distribution, biased toward smaller sizes. At scale 11/default multiplier, diameters range approximately **0.27–1.14 blocks**. Speed is 2.4 blocks/tick with satellite variation; bubbles expire at 144 blocks of travel or 80 ticks.

A separate visual pulse emits **12–40 water particles** before client density/distance/vanilla particle reductions; the default density produces about 8–26 per pulse nearby with vanilla particles set to All. These particles start at the mouth, spread slightly beyond the bubbles, vary in speed and size, and each has its own client-side box for collision with terrain. Contact produces a splash. They are short-lived cosmetic particles: **no damage, Drowning, knockback, erosion, forge power, or server projectile entity**.

Existing configs automatically receive the new `spray_bubbles_per_pulse` and `spray_half_angle_degrees` keys. These replace the old narrow-stream keys `bubbles_per_pulse` / `spread_degrees`. Keep your existing camera, growth, damage and erosion preferences; deleting the whole config is unnecessary.

### Damage and knockback

Damage is budgeted per target, per serpent, every **10 ticks**, across all droplets, multipart hitboxes and attack routes. Normal Minecraft hurt cooldowns still apply. Changing `spray_bubbles_per_pulse` (maximum 4–16) increases stream density and coverage, not the maximum sustained damage to a particular target. `damage_multiplier` (0–10) adjusts the entire stream.

With IAF's default `dragonAttackDamageFire = 2` and multiplier 1:

| Age in days | Stage | Raw damage / 10-tick window | Nominal direct DPS at 20 TPS |
|---:|---:|---:|---:|
| 0 | 1 | 1 | 2 |
| 25 | 2 | 2 | 4 |
| 50 | 3 | 3.5 | 7 |
| 75 | 4 | 6 | 12 |
| 100 | 5 | 9 | 18 |
| 125 | 5, scale 11 | 11 | 22 |

The curve interpolates with fractional age between these points. Stage 5 keeps growing in strength through 125 days. These are base direct-damage budgets, before armor, elemental multipliers, other damage immunity, accuracy, and the separate Drowning status effect. They are not guaranteed real-world DPS. Changing IAF's base scales the curve proportionally. Wild bites retain age/scale-based attack attributes; young wild breach damage now grows through Stage 1–2, and established Stage 3–5 slam damage is preserved.

`knockback_strength` defaults to 0.12 (range 0–0.4). A successful breath hit adds a gentle push along the stream using vanilla knockback resistance; all droplets share that hit's push. Zero disables this extra push. Water retains Drowning for 2/4/6/8/10 seconds at Stages 1–5, and existing elemental multipliers/armor counters.

Default central-impact AOE radii: **1.5 / 2.25 / 3 / 3.75 / 4.5 blocks** for Stages 1–5. Satellite splashes are smaller. `area_radius_multiplier` ranges 0–3, with final radius capped at 12. `area_damage = false` or radius 0 keeps direct hits only; forging still works. AOE requires line of sight from the collision point. Owner, pet, team and PvP protection remain active.

`bubble_size_multiplier` (0.5–2) controls both visual diameter and physical collision for newly emitted bubbles. The new base size varies per droplet, so 1.0 no longer means the old 0.2.1 single-bubble size. Larger droplets may hit nearby terrain earlier. `spray_half_angle_degrees` controls the Stage-5 maximum (3–20°); younger/smaller serpents use a proportionally smaller angle.

### Erosion

Only centered bubbles carry erosion. Extra droplets, spread, bubble size and AOE radius do not multiply the erosion budget. They can still change which surface a shot reaches.

- At the default cap, Stages 1–5 can change at most **2 / 4 / 6 / 8 / 10 blocks per 10 ticks**, under continuous impacts. That is 4/8/12/16/20 changes per second at 20 TPS.
- `erosion_blocks_per_second` sets the Stage-5 ceiling (0–100). Other stages receive proportional budgets, rounded down per half-second. Very low values can round young-stage budgets to zero.
- A specific world/block position can change once per **40 ticks**, shared across all serpents. Dirt does not turn immediately into sand within one burst: dirt → gravel → sand remains two separate transitions.
- `erosion_radius_multiplier` (0.25–1) defaults to 0.75. Impact radii are 0.75/0.75/0.75/1.5/2.25 blocks for Stages 1–5, with block-center inclusion allowing half a block at the edge. Ancient default erosion no longer reaches the third block directly outward from the impact.
- A powered forge adds a shallow bed around its foundation, within the same change budget. It can affect eligible soil directly beneath the foundation. More bubbles do not speed up crafting.
- Erosion remains irregular and surface-first, respects `mobGriefing` and Forge protection vetoes, skips block entities, and never deliberately loads edge chunks. Forge bricks and the forge itself do not erode.
- Set `erosion_enabled = false` or rate 0 to disable soil changes while preserving combat and forge power.

### Water particles (client)

```toml
[water_effects]
particle_density = 0.65
```

Range 0–1, in the **client** TOML. Lower this to reduce independent water droplets, trails and impact spray; 0 disables these cosmetic particles. Bubbles, collision, damage, knockback and erosion remain server-controlled. Minecraft's own particle settings also apply. Higher bubble counts can increase entity/network/rendering work; these defaults have not been benchmarked in live gameplay.

## Camera and aiming

```toml
[riding_camera]
distance_per_scale = 1.8
far_distance_per_scale = 3.0
height_per_scale = 1.1
fov_bonus = 12.0
show_aim_reticle = true
```

F7 (IAF's rebindable dragon-view key) cycles first person, rear, far rear, front, far front. F5 remains usable. Raised third-person cameras are limited by terrain; rear views get a center reticle. The server reconstructs the reticle ray using your current rider position/look and bounded camera offsets, then fires from the actual mouth. A wall in front of the mouth can still block a target visible from above it.

Front views are for looking at your mount: attacks still follow the rider's forward look and do not aim backward toward the camera. First-person aiming stays eye-based. Camera configuration does not change other mounts or saved global FOV. Set `fov_bonus = 0.0` to disable the extra FOV; set `height_per_scale = 0.0` for an unraised camera.

## Egg depth and pressure

| Setting under `[eggs]` | Default | Meaning |
|---|---:|---|
| `incubation_speed` | 1.0 | Global incubation multiplier; 0 pauses incubation |
| `hatch_time_ticks` | 0 | 0 inherits IAF's `dragonEggTime`; positive values override only serpent eggs |
| `minimum_water_depth` | -1 | -1 uses this edition's minimum depth |
| `pressure_safe_depth` | -1 | -1 uses this edition's safe depth |
| `pressure_chance_multiplier` | 1.0 | 0 disables pressure breaking; 1 preserves edition balance |

Tectonic defaults: 15 water blocks above the egg; pressure-safe through 20. Vanilla Ocean defaults: 8 and 32. Pressure is checked every 100 ticks, with the existing edition-specific probabilities. A paused egg in an otherwise valid location can still break from pressure unless pressure is disabled. The `-1` defaults follow the installed edition when moving an existing world between editions.

## Ghi chú tiếng Việt

- Chỉ **Wander** tự tìm về nước; **Stay và Follow/Escort** tắt việc tự tìm nước và dừng đường đi tìm nước cũ ngay khi đổi lệnh. Đi theo chủ hoặc chiến đấu vẫn hoạt động.

- Stage 5 tối đa **scale 11**, tuổi tối đa 125 ngày; damage phun vẫn tăng từ 100 đến 125 ngày.
- Nhiều bong bóng không nhân damage mỗi mục tiêu hay ngân sách xói mòn. Chỉnh độ dày ở `spray_bubbles_per_pulse`, damage ở `damage_multiplier`, hạt nước ở `water_effects.particle_density`.
- Chỉnh **tốc độ lớn của con đã nở** tại `growth`; chỉnh **tốc độ ấp trứng** tại `eggs`. Hai mục độc lập.
- Muốn Warm Ocean lớn nhanh x2: sửa `warm_ocean = 2.0` trong `[growth.ocean_multipliers]` của file **server**.
- Chỉnh camera/FOV ở file **client**. Camera nâng theo scale; tâm ngắm phía sau dùng cho cả cắn lẫn phun.
- Thay JAR cũ bằng đúng **một** edition 0.2.2. Client và server đều phải dùng bản rework này: protocol 9, không nối được với bản 0.2.2 cũ dùng protocol 7/8.
- Mô tả và con số ở đây là hành vi trong code. Hình ảnh camera, chiến đấu thực tế và tương thích camera mod khác vẫn cần kiểm tra trong game.

## SpartanFire Extras compatibility

The alchemy-sword mixin now uses priority 1100 so its armor-counter redirect can be applied to the method overwritten by Extras at priority 1000. The redirect remains required; missing effects are not silently ignored. Fire/ice continue through the merged Extras implementation. Lightning alchemy swords keep Bond Beyond’s arc replacement, so the old sky strike and a second Extras lightning chain do not stack. SpartanFire’s separate weapon traits are untouched.

## Forge and companion commands (owner-combat revision)

No new keys or config reset are needed. In Wander, Staying and Escort, an unridden owned serpent responds to the owner's attack/defense events on land or in water, including other Sea Serpents. Its owner and same-owner pets are protected. Nearby mouth contact selects bite; farther visible targets select spray. Stage 3+ can use breach/slam where clearance, range and cooldown permit. Player PvP rules remain in effect. Mounted attacks remain rider-controlled.

Automatic water-seeking is enabled only in **Wander** for owned serpents. **Staying and Follow/Escort** disable it and immediately cancel any active water-seeking path when selected. They can still enter water to follow an owner or fight; this restriction applies specifically to autonomous return-to-water behavior. Wild serpents retain their native water-seeking policy.

After combat, the previous command resumes. Idle Wander or Staying can fuel a complete submerged forge with exposed input, iron/blood and output space, provided the serpent is tamed, Stage 3+ and unridden. Escort does not automatically fuel. Owner combat interrupts forging in either fueling command.


## Sea Serpent Horn

Both states use the original IAF Dragon Horn sprites with a palette-only change: pale cool Sea Serpent Bone when empty, dark sea-blue native fill when occupied.

No new config is required. Craft with four Sea Serpent Bones and one Sea Serpent Fang in the native Dragon Horn shape. Use it in the main hand on your own living, unridden serpent to store it; use the top of a block in open space to release. Stored age is paused. Exact age/scale, ownership, health and equipment survive storage, including Ancient scale 11.

Unridden wild and tame AI breath was verified to erode terrain with actual server projectile collisions. The central droplet must hit a block; hitting only a mob or spraying into open water does not independently change the seabed. Keep `breath.erosion_enabled = true` and `/gamerule mobGriefing true` for terrain erosion. Only eligible soils change (dirt → gravel → sand); stone and forge blocks remain intact. Existing per-serpent and per-soil budgets still apply.

## Follow focus correction

Follow/Escort keeps the head and body facing the owner, including while waiting nearby or taking a ground detour. Existing owner combat takes priority and Follow resumes afterward. Egg placement/hatching canceled by another mod keeps the egg for retry. No new settings or config reset are required.

## Wander mating and water release

Both parents must be in Wander before activating breeding with a Heart of the Sea. Stay/Follow refuses breeding without consuming the Heart. Filled horns can also release when aiming at source or flowing water; a blocked or canceled release retains the stored serpent. Existing guides update on login or opening. No config reset or new keys are required.


## Wild female egg drops (latest 0.2.2 revision)

Edit these existing keys in the world's **server** TOML. Values are probabilities from 0.0 (disabled) to 1.0 (guaranteed). Restart/reopen the world after editing.

```toml
[eggs]
wild_female_stage_3_drop_chance = 0.25
wild_female_stage_4_drop_chance = 0.5
wild_female_stage_5_drop_chance = 1.0
```

Merge the three keys into your existing `[eggs]` section; do not add a second `[eggs]` header. Existing incubation/pressure keys remain independent.

Only wild, unowned, non-hatched females at Stage 3 or higher are eligible. No separate Ancient flag is required. A successful roll drops exactly one egg matching the mother's color at death. Males and Stage 1–2 never drop hunting eggs. Hatched/owned companions use breeding instead. `doMobLoot=false` suppresses these eggs. Harvesting or reloading a corpse does not roll for another egg.

## Optional Jade sex tooltip

Jade is not required to start or play Bond Beyond. With Jade on the client and server, the addon sends only the existing sex value for the targeted supported IAF/Bond Beyond creature. The tooltip reads **Male/Female** (or **Đực/Cái** in Vietnamese). There is no label for missing or unrecognized sex data, and viewing never assigns or rerolls sex.

Native dragon `Gender`, Cockatrice `Hen` (true = female), and Bond Beyond's `BondBeyondMale` fields are supported. Recognized boolean Male/IsMale and Female/IsFemale fields on other IAF/addon mobs are also read when present. The adapter handles native multipart parents. Other mod namespaces are left alone.

Toggle **Creature sex** in Jade's Bond Beyond plugin settings. No new required dependency or network protocol change is introduced. Developers can run a Jade-free development environment using `gradlew runClient -PwithoutJade`; the API is compile-only, so the first build still downloads the declared Jade artifact.


## Lightning compatibility and mating color verification (2026-09-29)

Breeding already chooses one parent's color with equal probability. No new random color is introduced; same-color parents keep that color. Wild egg loot still uses only the mother's color and is independent of breeding.

Lightning melee hits now use a common Forge damage hook, with the original IAF callbacks deduplicated. The first visual arc survives a lethal initial hit. Additional eligible targets chain with 100% probability, within 12 blocks of the impact and at most 12 blocks per hop, capped at 10 targets. Half damage per hop, normal damage immunity, ally/pet/PvP protection and the IAF ability toggle remain. The older code also had no random chain failure; improved targeting/recognition is the actual reliability correction.

Recognition supports native lightning tiers and lightning-blood swords, plus equipment whose registry path contains a lightning, thunder or electric token. It never uses a custom display name. Include tags support additional item IDs without a code change; exclude tags override all automatic recognition. Projectile weapons retain their launch item for later hits. Nonstandard addon projectile launchers may require a dedicated adapter.

To explicitly support a tool, add a datapack file at:
`data/iceandfire_bond_beyond/tags/items/lightning_weapons.json`

```json
{"replace": false, "values": [{"id": "your_addon:your_tool", "required": false}]}
```

To opt an item out, use the same format at:
`data/iceandfire_bond_beyond/tags/items/lightning_weapons_excluded.json`

Reload datapacks with `/reload`. No additional mod dependency is required. The IAF `dragonWeaponLightningAbility` option must be enabled. Arcs require a valid hit; misses/canceled damage do not proc, and friendly-fire protections remain. Matching IAF/Spartan weapon-attributed, loot-protected sky bolts are replaced only when their accepted hit already has a pending arc. Natural lightning and unrelated bolts are not globally disabled.
