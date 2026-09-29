# Ice And Fire: Bond Beyond — 0.2.1

Applies to both the Tectonic and Vanilla Ocean editions.

- Wild and AI-controlled tamed Sea Serpents now fire the fixed Bond Beyond bubble directly. A fallback still converts old/native IAF bubbles before their broken tick logic runs.
- Reworked AI pursuit and melee: clear-water swimming, throttled path searches around obstacles, mouth-based contact checks, a real 20-tick bite cooldown, and one hit on animation tick 6. Bites cannot switch victims during their wind-up.
- Added stable switching between melee and breath, with separate approach/retreat thresholds. Surface attacks retain a breach and landing slam, using the existing stage-based slam cooldown.
- Unified damage routing: wild bites read the same attack attribute as tame bites; bubbles use the existing stage/progress breath formula; water splash and land slam use the existing stage/progress slam formula. A landing cannot apply the same area impact twice.
- Wild bubbles use the same Drowning duration, elemental multipliers, counter-armor rules and collision handling as tame/mounted bubbles. Existing physical attacks remain physical; Drowning belongs to water breath.
- Erosion now forms uneven patches with stronger pressure near the impact, a jittered center and softer edges. It favors exposed soil instead of uniformly transforming an entire block volume.
- Forge breath uses the same projectile factory and centered muzzle. Successful impacts also erode the seabed below and around the forge, while leaving forge blocks intact.
- Overlapping impact/forge areas transform each block at most once per projectile: dirt-family blocks → gravel → sand. Mob griefing, Forge block vetoes, block-entity protection, loaded-chunk boundaries and the debris cap still apply.
- Updated both languages of the field guide and bumped both edition versions to 0.2.1.

The Tectonic and Vanilla Ocean editions retain their respective 0.2.0 egg depth/pressure settings. Install only one edition and use the same edition on server and clients.

Target: Minecraft 1.20.1, Forge, Java 17, original Ice and Fire 2.1.13-beta-5 and Citadel. Tectonic is required only by the Tectonic edition.

See `VALIDATION.md` for build and runtime verification status. These notes describe source changes; they are not evidence of an in-game test.
