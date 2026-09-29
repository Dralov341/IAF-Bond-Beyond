# Bond Beyond 0.2.1 — verification

## Builds

- Tectonic: **BUILD SUCCESSFUL**, including `compileJava`, the Mixin annotation processor and `reobfJar` (Gradle 8.8, Java 17, Forge 1.20.1-47.3.5).
- Vanilla Ocean: **BUILD SUCCESSFUL**, including the same compile and reobfuscation tasks.
- Dependencies: the project's pinned Ice and Fire file 5633453 and Citadel 2.5.3. The native `shoot`, `hurtMob` and `doSplashDamage` hook signatures were also inspected in the downloaded Ice and Fire JAR and compared with the beta-5 source.
- Both output JARs pass ZIP integrity checks; all 132 class files are identical between editions. Packaged resources match their current source files, and the generated refmap contains the mapped Minecraft overrides.
- The Tectonic JAR requires Tectonic. The Vanilla Ocean JAR does not. Both retain the same mod ID, so install one edition only.

## Automated checks

- Production AI cadence: bite wind-up at tick 6, one hit per animation, 20-tick cooldown, cancellation, goal-restart cooldown preservation, breath hysteresis and shot cadence.
- Production erosion math: stage footprint bounds, random non-uniform coverage, stronger center pressure, and shallow forge spread beneath/beside the 3x3 structure.
- Both ocean profiles: 8,204 geometry/pressure/profile assertions each. Tectonic and Vanilla Ocean depth/pressure settings remain unchanged.
- Growth regression: 150,259 age/scale assertions, including 50,000 simulated initial-age selections across all five stages. These are pure growth-code checks, not live mob spawns.
- All 514 files from the previous release remain. The three existing tame damage formulas, elemental combat rules, growth rules, corpse/skull data and ocean profiles are unchanged.
- JSON/PNG, recipe/model/language references, guide page limits and source/edition parity checks pass.

These executable checks exercise production Java helpers. They do not simulate a full Minecraft world. Gradle's `test` task has no test sources; it is not counted as a gameplay test.

## Runtime limits

Client rendering, live AI behavior and multiplayer gameplay have **not been tested** in Minecraft. The headless attempt reached Forge/ModLauncher and Mixin configuration preparation, but did not establish a complete server/world startup; treat it as **inconclusive**, not a compatibility pass. No EULA was accepted and no existing world was opened. The initial attempt failed while fetching client icon assets; the headless retry excluded that asset-download task. See `server-smoke.log` and `server-smoke-assets.log`.

Before a public release, check wild Stage 1–5 bites/breath/landings; AI-tamed owner-target restrictions; mounted R/bite/G; interrupted commands and target changes; forge start/stop and seabed erosion; `mobGriefing=false`; protected/unloaded terrain; Drowning and counter-armor matchups. Watch for movement/animation behavior under server lag. These are outstanding gameplay checks, not claimed passes.

Detailed reports: `validation.json`, `jar-validation.json`, `combat-checks.log`, `build.log`, `build-vanilla.log`, and the ocean-edition/resource reports in the development checkpoint.
