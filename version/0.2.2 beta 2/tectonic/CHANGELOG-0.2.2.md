# Latest 0.2.2 revision — lightning tools and breeding colors

- Confirmed breeding inherits either parent color (50/50), never an unrelated variant.
- Common lightning weapon detection across native IAF and addon equipment, with include/exclude datapack tags.
- Guaranteed primary arc on an eligible hit, including a killing blow; no random chain failure.
- Impact-centered 12-block chain search, ten-target cap, half damage per hop.
- Deduplicated native callbacks and generic hits; lightning cannot recursively proc itself.
- Ranged hit packets reach the shooter and nearby observers; launch weapon is retained.
- Targeted replacement of matching marked addon weapon sky bolts.
- Field guide lightning page refreshed in English and Vietnamese.

# Bond Beyond 0.2.2 — egg hunting and optional Jade sex tooltips

Applies to Tectonic and Vanilla Ocean. 29 September 2026.

- Wild female Sea Serpents can drop one matching-color egg from Stage 3 onward. Defaults: Stage 3 = 25%, Stage 4 = 50%, Stage 5 = 100%. No separate Ancient gate.
- Added three server config chances under eggs, each from 0 (disabled) to 1 (guaranteed). Males, Stage 1–2, hatched/owned serpents are excluded from hunting drops; breeding is separate.
- Egg drops respect doMobLoot. The roll happens on the normal LivingDropsEvent death path; corpse harvests and corpse reloads never reroll eggs.
- Optional Jade integration reads stored/native sex for IAF and Bond Beyond creatures, including all dragons, Cockatrices and Sea Serpents. Missing or unrecognized data yields no tooltip line. Lookup does not assign or reroll sex.
- Only the resulting boolean crosses Jade's server-data channel. Native multipart targets resolve to their parent. No Jade classes are bundled or loaded by the core mod.
- Jade's API is compile-only; development runs include Jade unless launched with -PwithoutJade. Jade remains absent from mandatory dependency metadata.
- Added English/Vietnamese Jade labels. Updated the guide's egg/taming/incubation pages and its in-place update revision; kept 99 physical pages and correct edition depth values.
- Updated public description and configuration manual. Existing horn WATER-click fix, owner AI, forge, combat, growth and edition differences remain.

See VALIDATION.md for verification and remaining limits.

---

# Bond Beyond 0.2.2 — direct WATER-block horn release

29 September 2026. Applies to Tectonic and Vanilla Ocean.

- A filled Sea Serpent Horn now handles a direct click on a WATER block before doing a second camera ray. It releases into that exact water cell, on any clicked face, including flowing water.
- Normal fluid-aware aiming and solid top-face placement remain available. Full NBT, health, owner, UUID, collision and spawn-veto protections remain in the shared release path.
- Three new regression tests use the actual server block-use route with Survival FakePlayers. They failed on the previous source and pass after this fix. The prior 36 GameTests also pass (39 total).
- Source ZIPs now include the native Gradle 8.8 wrapper and README-BUILD.md. No dependencies, profiles, protocol, guide pages or other gameplay changed in this correction.

# Bond Beyond 0.2.2 — Wander mating, water release and guide update

29 September 2026. Applies to Tectonic and Vanilla Ocean.

- Both Sea Serpent parents must now be in Wander to mate. Stay or Follow on either parent prevents breeding without consuming a Heart or starting the breeding cooldown. Existing sex, owner, Stage, health, riding and cooldown requirements remain.
- Filled Sea Serpent Horns can release onto water, including flowing water, using a fluid-aware targeting ray. When the ordinary block interaction targets the seabed behind water, the nearer water cell is used instead. Existing top-of-block release remains available.
- Water release shares the existing full-NBT restoration, health/age/scale preservation, duplicate-UUID, collision and spawn-veto safeguards. A failed release keeps the serpent in its horn; solid cover cannot be bypassed by the water ray.
- Updated Vietnamese and English guide pages, contents links, breeding messages and horn tooltip. The guide now explains Wander-only mating, water release, Follow focus and Wander-only return to water. Mount-armor information was consolidated to make room for the horn instructions while keeping the single bilingual book within 99 pages.
- Existing tagged guide books update in place on login or opening, including books retrieved from storage. Custom names and unrelated item tags remain. The one-time gift receipt, uncraftable recipe and no-copy behavior remain.
- No new config keys, dependencies or protocol changes. Maximum scale remains 11, protocol 9. Install only one edition and update clients/server together.

See VALIDATION.md for test coverage and remaining client-test limits.
