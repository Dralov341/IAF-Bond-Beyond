# Bond Beyond 0.2.2 — Wander mating / water release / guide, 2026-09-29

Current review: **mate-water-022/CHANGELOG.md, CONFIG-0.2.2.md, VALIDATION.md**.
Three Java and six JSON files changed. All 541 source/resource files (114 Java) retained.
Both parents must Wander to breed. Horn release targets source/flowing water and retains failure safeguards.
The bilingual guide updates existing copies on login/open; still one 99-page, uncraftable gift.
Current test/build evidence is in mate-water-022/. Client visuals and full modpack remain untested.
No new config keys; protocol 9 and maximum scale 11 remain.

Everything below describes older revisions.

---

# Bond Beyond 0.2.2 — Follow focus and feature audit, 2026-09-28

Current review: **follow-audit-022/CHANGELOG.md, CONFIG-0.2.2.md, VALIDATION.md**.
Source: project/ (Tectonic), variants/vanilla-ocean/ (same Java).
541 source/resource files, 114 Java. Three Java files changed; all resources retained byte-for-byte.

Follow keeps its head/body facing the owner while idle, approaching or detouring.
Owner combat retains priority; Wander-only water seeking is preserved.
Egg placement and hatching retain the egg when another mod vetoes spawning.
Current build and test evidence is in follow-audit-022/. See VALIDATION.md for coverage and limits.
No new config keys; protocol 9 and maximum scale 11 remain.

Everything below describes older revisions.

---

# Bond Beyond 0.2.2 — native horn / water-command correction, 2026-09-28

Current review: **horn-water-022/CHANGELOG.md, CONFIG-0.2.2.md, VALIDATION.md**.
Source: project/ (Tectonic), variants/vanilla-ocean/ (same Java).
541 source/resource files, 114 Java; exactly four files changed from the preceding horn release.

Native IAF Dragon Horn sprites, palette-only recolor with exact pixel/alpha preservation.
Pale Sea Serpent Bone empty horn; dark sea-blue native fill. No redrawn replacement.
Tamed autonomous water-seeking is Wander-only. Stay/Follow cancel the active water goal/path immediately.
Wild water-seeking, owner combat, forge, storage/health/data and all other systems are retained.

Both editions build/reobf successfully; all 15 required headless Forge GameTests passed. Current build/test evidence is in horn-water-022/. Client visuals and full modpack untested.
Protocol remains 9; no new config keys or reset.

Everything below describes older revisions.

---

# Bond Beyond 0.2.2 — current Sea Serpent Horn revision, 2026-09-27

Current review: **horn-022/CHANGELOG.md, CONFIG-0.2.2.md, VALIDATION.md**.
Source: project/ (Tectonic), variants/vanilla-ocean/ (same Java).
541 source/resource files, 114 Java. Protocol 9, max scale 11.

Pale empty horn / navy occupied horn. Four Sea Serpent Bones + one Fang.
Owned living unridden capture; block-top release. Complete entity save, exact
health restored after IAF's native load-time heal; invalid/blocked/vetoed release
retains data. Temporary forge/attack state is cleared. 13/13 headless GameTests,
including real unridden wild/tame projectile erosion and the seven stable combat
regressions. Both editions build/reobf. Client visuals/full modpack untested.

Everything below describes older revisions.

---

# Bond Beyond 0.2.2 — current owner-combat revision, 2026-09-27

Current review: **owner-combat-022/CHANGELOG.md, CONFIG-0.2.2.md, VALIDATION.md**.
Current source: project/ (Tectonic). Generate Vanilla Ocean with prepare_variant.py.
534 source/resource files, 113 Java. Protocol 9, max scale 11.

Direct Forge owner-combat events; all three idle commands defend/assist; land
breath; conspecific enemies allowed, same-owner pets protected; no bite/breath
gap; ranged aim holding; ground breach for owned Stage 3+ when valid. Native
idle/water-seeking AI yields during combat. The preceding forge fix is retained.

Seven actual headless Forge GameTests pass, with posted player attack events,
real entities/projectiles and damage. Physical input, client visuals/FPS and
full modpack gameplay are not tested. Both editions build/reobf; production
helpers and preservation audits pass. Test code is excluded from release JARs.

Everything below describes older revisions and may contain superseded behavior.

---

# Bond Beyond 0.2.2 — current forge / owner-combat hotfix, 2026-09-27

Current review: **ai-forge-022/CHANGELOG.md, CONFIG-0.2.2.md, VALIDATION.md**.
Source: project/ (Tectonic), variants/vanilla-ocean/. 533 source/resource files,
112 Java files. Both editions share Java and differ in four resources.

Escort yields to combat; new owner attack/defense memory chooses fresh threats
and preserves a valid fight. Home-return and forge reservations yield to owner
combat. Forge recruitment permits turning before exact physical emission and
accepts the assembled input rim. Wild breath can reach shore targets.

Mouth spray, separate particles, shared damage/erosion, max scale 11, camera,
growth/remains and Extras priority-1100 fix are retained. Protocol stays 9.
Both editions build/reobf and production checks pass. Live gameplay NOT TESTED.
Run review/ai-forge-022/run_checks.py after the Forge build, then validate_release.py
and verify_jars.py. The following sections are historical.

---

# Bond Beyond 0.2.2 — current spray / SpartanFire Extras correction

Current review: **spray-022/CHANGELOG.md, CONFIG-0.2.2.md, VALIDATION.md**.
Source: project/ (Tectonic); regenerate Vanilla Ocean with
review/ocean-editions/prepare_variant.py. 531 source/resource files, 110 Java.

Shared animated-jaw muzzle; 4–12 smaller mixed bubbles every 2 ticks;
moderate Stage/scale-dependent spread; independent cosmetic water particles
with terrain collision. Stage 5 max scale remains 11. Damage, erosion,
growth, wild AI, corpses/skulls and camera systems retained.
AlchemySwordLightningMixin priority 1100 resolves the reproduced Extras
priority-1000 overwrite injection failure. Client/server protocol is now 9.

Both editions build/reobf. Production geometry/config/budget and actual
Mixin transformation checks pass. No live gameplay, visual/FPS benchmark
or full modpack startup claim. The sections below describe older revisions.

---

# Bond Beyond 0.2.2 — continuous water stream rework, 2026-09-26

Current review: **breath-022/CHANGELOG.md, CONFIG-0.2.2.md, VALIDATION.md**.
Current Tectonic source: project/. Generate Vanilla Ocean with
review/ocean-editions/prepare_variant.py. Both editions share the same Java.

The revised 0.2.2 breath uses 3 differently sized bubbles every 2 ticks by
default, with water trails and impact spray. All sources share emission,
damage/knockback and terrain budgets. Age-based stream damage now increases
through Ancient Stage 5, ending at 125 days and maximum scale **11**. Young
wild breach damage now follows Stage-1/2 ages. Existing Stage-3+ slams, bite
attributes, elements, status effects, growth curve, remains and camera aim
are retained. Protocol 8 requires the reworked build on client and server.

Automated checks exercise the real production budget/curve helpers and actual
Forge config/Minecraft packet code. Read current VALIDATION.md for build status
and limitations. Live-world gameplay and visual performance are NOT TESTED.
The earlier 0.2.2 and 0.2.1 sections below are historical, especially their
statements about unchanged breath damage and protocol 7.

---

# Bond Beyond 0.2.2 — riding, water AOE and configuration, 2026-09-25

Current source is project/ (Tectonic), with vanilla generated by
review/ocean-editions/prepare_variant.py. All 516 delivered 0.2.1 source/resource
files remain. The release adds 4 shared Java files (100 Java, 520 total).

The rear riding camera is raised according to scale and gains a local FOV
bonus/reticle. Validated camera offsets cross the version-7 rider-input packet;
the server computes the aim ray and converges attacks from the animated mouth.
The bite box now handles a sideways camera-converged aim without degenerating.
Water bubbles have shared impact AOE and a configurable visible/collision
diameter. Existing damage formulas, elements and Drowning remain unchanged.

Forge SERVER config owns breath, aging, ocean multipliers, incubation and
pressure. CLIENT config owns camera/FOV/reticle. A shared fixed-point clock
persists fractional progress and migrates old egg fractions; original spawn
ages, growth curve and saved remains code are unchanged. Warm ocean natural
growth defaults to 1.3x; incubation rates retain the previous defaults.

Read riding-022/CHANGELOG.md, CONFIG-0.2.2.md and VALIDATION.md. Both JARs compile
and reobfuscate; production geometry/config/packet/math checks run headlessly.
Live Minecraft gameplay/rendering has NOT been tested. Earlier sections below
are historical and do not validate this release.

---

# Bond Beyond 0.2.1 — wild combat and natural erosion, 2026-09-25

This release continues the delivered 0.2.0-beta.2 source (version 17, September
24). All 514 source/resource files from that archive are preserved. There are
now 516 source/resource files, including 96 Java files. Two new classes implement
the shared AI combat goal and its per-entity attack cadence.

Wild/AI-tamed/mounted/forge breath uses one projectile factory with a centered
muzzle. Wild AI no longer constructs IAF's old bubble. The legacy bubble mixin
remains a compatibility fallback. Native bite and splash damage entry points
are intercepted; the AI chooses bite/breath with hysteresis, observes a real
20-tick bite cooldown and consumes the bite once at animation tick 6. AI surface
breaches use the existing tame slam cooldown. Water and land impacts share
stage-scaled damage and cannot double-hit the same landing.

The tame bite, breath and slam formulas are byte-for-byte unchanged. Existing
Drowning, elemental matchups and counter-armor effects are unchanged. Water
breath carries water effects; bites/slams retain their physical damage type.

Erosion keeps the existing stage footprint but samples uneven patches with a
jittered pressure center and radial falloff. Only exposed eligible soil is
selected, except the immediate soil supporting an active forge. Successful
forge hits add a shallow spreading footprint beneath/around its foundation.
The union is processed once per block, preserving dirt → gravel → sand and
Forge/mobGriefing protection. Forge bricks and block entities do not erode.

Both editions retain their original pressure/depth profiles and dependency
separation. Read combat-021/CHANGELOG.md and combat-021/VALIDATION.md for this
release. Earlier notes below are historical; their old test/build statements
do not validate version 0.2.1.

---

# Bond Beyond 0.2.0-beta.2 — erosion and ocean editions, 2026-09-24

Based on version 13 of the unfinished Combat-Polish source ZIP. All 468 original
files are retained. Source now contains 514 files: 94 Java, 282 JSON and 131 PNG.
This is source code, not a release-tested JAR.

## Historical erosion and ocean-edition update — September 24

Block impacts erode the entire IAF beta-5 sustained-breath footprint instead
of a single block. Stages 1–3 use a 3x3x3 cube (27 candidates); stage 4 uses
a rounded 5x5x5 region (81); stage 5 uses a rounded 7x7x7 region (179).
These are the native dragon terrain extents/boundary, not the charged-shot
explosion or entity damage radius. All eligible soil blocks advance once per
impact: dirt-family -> gravel -> sand. Unlike IAF's random block selection,
each eligible block within this water-breath footprint is processed.
Entity damage, drowning duration and projectile flight reach are unchanged.
Wild/tamed/mounted breath already shares this impact path. Mob-griefing and
per-block Forge vetoes remain; block entities, unloaded chunks and positions
outside build height are skipped. At most 12 debris events play per impact.

Both editions share identical Java and assets except the packaged profile,
Tectonic dependency metadata and three guide pages per language. The resource
bond_beyond_ocean.properties selects the edition. The included build.gradle
adds -tectonic or -vanilla-ocean to the JAR/mod version and loads Tectonic in
runClient only for the Tectonic edition. Mod ID stays the same; install one
edition per instance and use the same edition on clients and server.

| Egg rule | Tectonic (existing balance) | Vanilla Ocean (new balance) |
|---|---|---|
| Continuous water blocks above egg | 15 minimum | 8 minimum |
| Safe depth above egg | up to 20 | up to 32 |
| Break chance per extra block, per check | 0.1% | 0.05% |
| Maximum break chance per check | 15% | 5% |
| Check interval while incubating | 100 ticks | 100 ticks |

Vanilla settings are a chosen gameplay balance, not a claim that every vanilla
ocean has a fixed depth. Ocean biome, immersion, biome temperature multipliers,
incubation time, saved progress, ownership and hatching behavior stay the same.
The actual continuous water column is measured, not a fixed Y coordinate.

Validation for this correction: both editions run 8,204 assertions each against
the production footprint/pressure/profile code. The Tectonic pressure formula
matches the former formula at every tested depth -1..500. Edition-specific
resource, metadata, book limits and exact Java equality are checked. Java 17
syntax: 94 files. Available Minecraft bytecode confirms the BlockPos iterator
and build-height API signatures. Full Forge build/client gameplay remain
unverified; this environment still lacks the complete build tool/dependency
cache. See ocean-editions/ and HUONG-DAN-HAI-BAN.md in each source ZIP.

## Historical sword and guide correction — September 22

The two Sea Serpent swords now inherit IAF's dragonbone sword and fire-blood
sword models, including their native display transforms. Their 32x32 sprites
preserve every original alpha pixel and neutral outline/grip pixel. Bone colors
are pale and less yellow; the fire-colored pixels become cyan water colors.
The custom sea_serpent_blade cuboid model is no longer referenced or shipped.
Combat stats, effects and both sword recipes are unchanged by this correction.

The manual is one prewritten first-login gift, with the established bilingual
content, welcome and contents links (99 physical pages). The old recipe and
unlock are disabled by Forge's root conditions/forge:false rule; keeping these
disabled files makes an overlay update disable the previous recipe as well.
No vanilla recipe is overridden. Book generation 2 prevents vanilla copying.
On login, owned tagged guide books retain their contents and receive that
no-copy setting. The persisted gift receipt survives death, so relogging and
respawning do not give extra books. The legacy item ID stays for commands and
old saves, outside the creative list, and opens the same full guide.

Sword palette references were edited with ImageGen. ExportSwordPalettes.java
uses their colors only, mapped onto the exact native IAF pixels; generated
geometry was not imported. Native RecipeManager, ConditionalAdvancement and
BookCloningRecipe bytecode confirm condition keys and generation behavior.
See sword-book/native-contracts.txt and previews/06 comparison in the bundle.

## Preview and lightning audit update — September 22

The latest request was for actual asset previews and a review of the older
lightning/shield requirement. The five lightning Dragonsteel tools already
redirect the native sky-bolt ability, but that replacement had dropped the
native 1.0 knockback. It now retains knockback on the server when the IAF
lightning-ability setting is enabled. This is the only production-code change
in that earlier preview update; base damage, arc falloff, recipes and models stayed as in
the preceding source snapshot.

Static review covers the two native weapon sky-bolt creation paths, all five
Dragonsteel tool targets, lightning blood sword cancellation/durability,
shield block hook and predicate, server target selection, packet direction,
dimension guard and the client IAF arc renderer. See previews/AUDIT.md.
Current syntax and resource checks pass again; client/Mixin runtime remains
unrun. No Minecraft screenshot or successful full-build claim is made.

Preview renderers in previews/ produce forge comparisons, four tower shields,
two serpent swords, Sea Steel armor on all three dragons, serpent armor with
fin on/off, an illustrative arc PNG/GIF and all 66 active recipes on 11 sheets.
An additional comparison shows the native IAF and recolored sword sprites.
The shields are original Bond Beyond geometry, not imported Spartan assets.
Vanilla rim/grip/gem materials use labeled flat preview swatches because the
provided source contains no vanilla textures. Other model/texture views use
the actual project and supplied IAF assets. Recipe slot labels come directly
from JSON. Lightning is a code-based illustration, not a running-client capture.

## Implemented

- Four baked 3D tower shields: Sea Serpent Steel, Fire, Ice and Lightning
  Dragonsteel. A successful block retaliates with drowning (6s), burning (5s),
  freezing (5s), or arcs. Visible hostile attacker within 10 blocks; cooldown
  10 ticks; no recursive shield retaliation. Matching ingot repair; durability 2500.
- Sea Serpent Bone Sword (1660 durability, 8 total base damage) and Blood Sword
  (2000, 9.5). Blood sword applies the existing six-second Drowning effect.
- Wild, AI-tamed and mounted breath share a hit path. Drowning lasts 2/4/6/8/10s
  at stages 1–5. Original base breath formula is byte-for-byte unchanged.
  Water vs fire dragons x1.6, ice dragons x1.5, lightning dragons x1.0.
  Lightning dragons still receive normal Drowning.
- Serpents receive ice/freeze x1.3, lightning x1.6, fire/physical bites x1.
  The actual damage type determines the element, not the attacker's species.
- Matching armor removes the weakness bonus (x1), retaining normal damage and
  effects. Serpents equip one complete armor item; dragons need all four matching
  head/neck/body/tail pieces. Mixed or incomplete sets do not count.
  Native fire/ice blood-sword bonus hits (13.5) and lightning sword bonus hits
  (9.5 vs fire/ice dragons) also respect matching counter armor. No unrelated
  fire-vs-ice breath multiplier is invented where IAF beta-5 has none.
- Four Sea Serpent Steel dragon armor pieces work on all three dragon species,
  +10 per piece through IAF's equipment system. Species-fitted native ice-steel
  UV assets receive a teal tint. Native dragonsteel crafting patterns use
  Sea Serpent Steel ingots.
- Lightning blood sword, lightning Dragonsteel tools and shield share arcs:
  no vanilla sky-bolt entity, maximum 10 targets in a box extending 10 blocks
  along each axis around the wielder (not a radius-10 sphere),
  half damage per hop. Team/owner/PvP checks, visibility, ordinary hurt immunity
  and IAF ability config remain.
- Forge: tamed unridden stage-3+ serpent in Wander/Staying, in range and with a
  visible input, holds still while fueling. Navigation, native movement/jumping,
  travel and home return are locked. Escort/riding immediately releases it;
  lost work/core/visibility expires the ten-tick lease.
- Core and directly adjacent input open the forge GUI. Bricks open only for a
  complete submerged forge. Unfinished layers return PASS so building continues.
  There is no separate output block; output is a GUI slot.
- New wild spawn: derive stage from the already chosen scale, randomize a day
  inside that stage's existing age range, then derive matching scale. Preset
  maximum-size serpents stay at maximum. Hatchlings still start at zero.
  Saved age never rerolls. Old wild saves without age infer it from exact saved
  scale without resizing on load. Normal growth continues.
- Corpse -> skeleton -> skull item -> placed skull -> picked-up skull preserves
  exact death scale, stage and age. Legacy skulls lacking real scale use a valid
  stage-size fallback; new skulls never reset to small display size.
- Brick recesses/cracks/grille bars and egg lower/rear surfaces are darker.
  All nine serpent body-armor materials have deeper recesses and raised edges.
  Forty PNGs have changed pixels; dimensions, alpha, model geometry, saddle
  leather and black nostrils are preserved. Separate fin atlases cover roots/rays,
  leaving membranes exposed; body and fin armor both disappear on unequip.
- Existing bubble erosion confirmed: dirt-family -> gravel -> sand, one step per
  impact, wild and tamed, respecting mob-griefing and protection events.
- EN/VI names, tooltips, recipes/unlocks and guide updated. New guides always
  include the welcome, with 99 physical pages and working contents links.

## Crafting and install

Bone sword: two serpent bones above a stick. Blood sword: bone sword + blood;
bottle returned. Shield: ISI / IBI / _I_; I is matching steel ingot, S vanilla
shield, B serpent bone (five ingots). Existing Sea Steel serpent armor uses five
steel blocks + saddle; it was retained and retextured, not registered twice.

Copy the selected ZIP into the existing project root containing gradlew.bat,
including its src and supplied Gradle files. Keep the existing Gradle wrapper.
Each archive includes the profile-aware build file and instructions. Run:
- .\gradlew.bat build --console=plain --max-workers=2
- .\gradlew.bat runClient

## Validation actually completed for this update

- Java 17 parser: 94 production Java files pass syntax checking.
- Executed actual production growth curve/sampler: 150,259 assertions, 50,000
  spawns. Every valid day represented in each stage; age/size agree; saved-scale
  round trips pass, including scale 11.
- 282 JSON files parse. New recipes/unlocks/translations/model geometry and local
  textures validate; book page/line limits pass.
- All 131 PNGs decode; atlas alpha/dimensions and accepted entity model geometry are unchanged.
  Actual production meshes/textures rendered offline and visually reviewed for
  armor, eggs, bricks and new item geometry. These are not Minecraft screenshots.
- Verified IAF beta-5 source contracts and available genuine Forge/Minecraft
  signatures for new API calls. This is not a whole-project type check.
- Full Gradle compile BLOCKED: required Minecraft/Forge/IAF/Citadel dependencies
  missing from cache and inaccessible here. Shield-only compilation also stopped
  at missing Brigadier. Neither proves successful compilation.
- Client, dedicated-server integration and GameTests NOT RUN. Successful logs in
  old checkpoint history concern older versions only.

Runtime checks still needed: Mixin application/remapping; shield hand poses and
blocking; elemental/counter-armor combinations; forge lock/release and incomplete
layer clicking; spawn/save/reload growth; Ancient corpse through skull pickup and
reload; armor on/off fin layers; both erosion steps with griefing enabled/disabled.

## Reproduce the checkpoint

Current source-snapshot.zip, review folder and original v13 comparison source
are included. Previous checkpoint work is preserved under work/. Extract the
snapshot into project/. Java 17 plus Python/Pillow/NumPy are needed.

Run java review/JavaChecks.java project/src/main/java. Compile production
SeaSerpentGrowth.java and review/GrowthChecks.java via review/Compile.java, then
execute GrowthChecks. Run python3 review/validate.py for resource checks.
review/textures holds coordinate-aware asset generators and offline renderers.
Run java review/sword-book/ExportSwordPalettes.java to reproduce sword recolors.
Run the preview recipes.py, gallery.py weapons, sword_book.py, then package_preview.py
after asset changes. The other preview images remain valid for this correction.

ImageGen built-in edit produced a Sea Steel material reference, but shifted UV
islands, so it was rejected as a production atlas. Reference/prompt are included.
Existing geometry-aware asset source implements its dark-recess/raised-edge
direction while preserving exact UV/alpha registration.
