# Night of False Faces

> Adds A Terrifying Creature..... Ahhhhhh, so scary. :/

A Minecraft Forge **1.19.2** mod by **RedstoneDev** that adds the **Face Thief** — a
skinwalker that stalks, disguises itself as one of your familiar animals, climbs walls,
breaks doors down, and leaves bloody Gut blocks in its wake.

Uses **GeckoLib 3.1.40**.

---

## What's in this project

This is a **source project** — you build the jar yourself with Gradle. (The mod can't be
compiled in the sandbox where I assembled it: no JDK, no internet to fetch the Forge MDK.)

```
night_of_false_faces/
├── build.gradle              ← Gradle build script (ForgeGradle)
├── gradle.properties         ← Versions / mod metadata
├── settings.gradle
├── .github/workflows/build.yml  ← Zero-setup build via GitHub Actions
├── libs/
│   └── geckolib-forge-1.19-3.1.40.jar
└── src/main/
    ├── java/com/redstonedev/nightoffalsefaces/   ← All Java sources
    └── resources/
        ├── META-INF/mods.toml
        ├── pack.mcmeta
        ├── logo.png
        ├── data/...  (loot table)
        └── assets/night_of_false_faces/...  (geo, anims, sounds, textures, models)
```

## How to get the jar

### Option A — GitHub Actions (zero local setup)
1. Create a new GitHub repository (private is fine)
2. Upload the entire contents of this folder
3. Push, or go to the **Actions** tab and click **Run workflow** on "Build Forge Mod"
4. Wait ~3–5 minutes; download `night_of_false_faces-jar.zip` from the run's Artifacts

### Option B — Build locally (JDK 17 + internet)
```bash
cd night_of_false_faces
gradle wrapper --gradle-version 7.5.1   # one-time, if no gradlew yet
./gradlew build
```
Jar lands at `build/libs/night_of_false_faces-1.0.0.jar`.

Drop it (plus `geckolib-forge-1.19-3.1.40.jar`) into your Forge 1.19.2 `mods/` folder.

---

## What the Face Thief actually does

**Spawning**
- Per-dimension limit of one Face Thief at a time (so you never see two at once).
- Surface spawns 14-28 blocks from you on solid ground (`MOTION_BLOCKING` heightmap, so
  it works in modded biomes like Biomes O' Plenty Redwood Forest — no biome filtering).
- Cave spawns 8-16 blocks from you, searching ±8 vertically for an air pocket on solid
  ground (so mineshafts, ravines, and large natural caverns all qualify).
- Underground always uses the night-rate spawn chance regardless of the world clock.

**Per-spawn behavior roll**
Each ambient spawn rolls dice for its initial mode:
- **50% STALKING** — stares from a distance, doesn't move, vanishes the moment you look
  directly at him.
- **35% DISGUISED** — rendered as a random vanilla animal: **Pig / Chicken / Cow / Villager
  / Sheep / Horse**. Just stares. If you get within 3 blocks **or** hit him, he reverts to
  his true form and goes aggressive.
- **15% WALKING** — wanders innocuously. If you look directly at him, he goes aggressive
  and chases you.

**Aggressive (chase) mode**
- `chase_theme.ogg` loops on the client via a `TickableSoundInstance` with `looping = true`
  (gapless), and stops the moment he disappears.
- Plays `scream.ogg` on the first transition to aggressive (and intermittently while chasing).
- Speed: walks slowly (0.20), chases medium-fast (0.30) — you can sprint-outrun him with effort.
- Climbs walls with `WallClimberNavigation` (same class Spider uses); plays the climb
  animation at medium speed when blocked horizontally.
- Breaks doors slowly via `BreakDoorGoal` — the vanilla door break sound plays through MC's
  own mechanic.

**Disguise rendering swap**
- `FaceThiefRenderer` overrides `render()`. When the entity's `DATA_DISGUISE` is non-NONE,
  it caches a fake instance of the disguise's `EntityType` (`PIG.create(level)` etc.,
  never added to the world), syncs that fake's position/yaw to the Face Thief, and
  delegates to MC's renderer for that animal type. So the player sees a Pig, but the
  underlying entity is still the Face Thief — damage, collision, and AI all route to him.

**Ambient sound pool (plays randomly while he's loaded)**
- `ambient.ogg` — soft breathing, every 20-50s
- `distance_moan.ogg` — at the player's position, every 60-180s
- `helpme.ogg` — at a random direction 8-16 blocks from the player (sounds like a different
  voice calling for help), every 80-200s
- `mimicking_helpmeecho.ogg` — at the player's position, every 120-300s
- `hurt.ogg` — when he's attacked
- `scream.ogg` — on going aggressive + intermittently while aggressive
- `skinwalker_aftermath.ogg` — when an aggressive Face Thief vanishes (plays only once,
  not on stalk / disguise despawn)

**Gut block**
- A flat slab of meat that occasionally appears on top of a random block near the player
  (~once per 2 hours of play per player). Has a small slab voxel shape matching the model
  geometry — you can walk over it.
- Only drops the gut item if mined with a **stone-or-better pickaxe** (enforced via the
  loot table).
- Block model is the user's `gut.geo.json` converted to vanilla model format (one cube,
  6×0.5×6, with all six faces UV-mapped). Item form uses a flat `item/generated` model so
  it has a normal 2D inventory icon.

## Tweaking

Most knobs live in `FaceThiefEntity.java` and `ForgeEvents.java`.

| What | Where | Default |
|---|---|---|
| Health | `createAttributes()` | 60 |
| Attack damage | `createAttributes()` | 8 |
| Chase speed | `SPEED_CHASE` | 0.30 |
| Per-spawn behavior roll | `tryAmbientSpawn` | 50% stalk / 35% disguise / 15% walk |
| Surface spawn chance | `tryAmbientSpawn` | 1-in-2400 per 5s (day) / 1-in-300 (night/cave) |
| Gut block drop chance | `tryDropGutBlock` | 1-in-1500 per 5s per player |
| Lifetime cap | `aliveTicks >= 24000` | 20 minutes |

---

## Caveat (same as always)

Source isn't test-compiled in the sandbox where it was assembled. The Forge 1.19.2 + GeckoLib
3.1.40 APIs used here are the same ones already verified in the "Don't Let Him In" mod, so
this should compile cleanly. If `./gradlew build` (or the GitHub Actions workflow) errors,
paste the log and the fix is usually a one-line patch.
