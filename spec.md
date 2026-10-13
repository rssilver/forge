# Forge MTG — Repository Specification & System Map

> **Audience:** this file is a machine/agent-facing map of the codebase so features and files can be found quickly and accurately. It pairs with `changelog.md` (what changed, when) and `agents.md` (how to use these two files).
> Last updated: 2026-10-12 21:00 -04:00 (commit `91335c221e4`, branch base `master-origin`).

## Why one file, not a folder
The repo has ~58k tracked files but the **Java source is highly concentrated** (~1.1k source files across 11 Maven modules; ~55k of the total are card art / game assets under `res/`). A single well-indexed `spec.md` stays in sync trivially and is fully searchable with `grep`/`Read`. Splitting would only fragment the index. Keep it as one file unless a section grows past ~1,500 lines.

---

## Table of Contents
1. [Project overview](#1-project-overview)
2. [Repo topology & branch model](#2-repo-topology--branch-model)
3. [Module map](#3-module-map)
4. [Java package index (searchable core)](#4-java-package-index-searchable-core)
5. [Resource tree (`res/` and data locations)](#5-resource-tree-res-and-data-locations)
6. [Build, run & launch](#6-build-run--launch)
7. [Card data format & scripting API](#7-card-data-format--scripting-api)
8. [Behavioral Execution Model](#8-behavioral-execution-model)
9. [Config, generated files & `.gitignore` semantics](#9-config-generated-files--gitignore-semantics)
10. [Testing](#10-testing)
11. [Feature → file lookup table](#11-feature--file-lookup-table)
12. [Conventions](#12-conventions)
13. [Glossary](#13-glossary)

---

## 1. Project overview
- **What:** Forge — open-source **rules engine + client** for Magic: The Gathering, play against AI or online. Independent of Wizards of the Coast (GPL-3). Inception year 2007.
- **Language / runtime:** Java, **release target Java 17**. Encoding UTF-8.
- **Build:** Maven multi-module (`pom.xml` is a parent `pom`, packaging `pom`). Version scheme `versionCode` = `maj.min.rev` (currently `2.0.16`), snapshot suffix `-SNAPSHOT`.
- **Official site / upstream:** https://www.cardforge.org/ ; upstream repo `https://github.com/Card-Forge/forge.git`.
- **Desktop entry point:** `forge-gui-desktop/src/main/java/forge/view/Main.java`.

---

## 2. Repo topology & branch model
### Remotes
| Remote | URL | Role |
|---|---|---|
| `origin` | `https://github.com/rssilver/forge.git` | **This clone's primary fork** (`rssilver/forge`). |
| `upstream` | `https://github.com/Card-Forge/forge.git` | Canonical upstream. |

### Branch model (as of this snapshot)
- **`master-origin`** = local working baseline / the branch docs are derived from. Commit `91335c221e4` (`2026-10-12 20:31 -04:00`). This is the "existing code" base for changelog purposes.
- **`origin/master`** = `0fa862eb1c` (`2026-10-08`) — rssilver fork mainline; equals its merge-base with upstream (fork point).
- **`upstream/master`** = `3a025afacf8` (`2026-10-12 08:17 +08:00`) — Card-Forge mainline.
- Relationship: `origin/master` is the fork's base; `master-origin` sits **65 commits ahead of `origin/master`** (48 non-merge work commits + merges) and **2 ahead of `upstream/master`**. The 65-commit delta from `origin/master..master-origin` is exactly the set documented in `changelog.md`.
- Local feature branches present: `new/battle-simulator-ultima`, `perf/ai-creature-evaluation-caching`, plus many `remotes/upstream/*` PR branches.

> **How to find "what changed since base":** `git log --no-merges origin/master..master-origin --format='%h|%ci|%s'`. This is the authoritative changelog source; keep it in sync with `changelog.md`.

---

## 3. Module map
`pom.xml <modules>` (order matters for build):

| Module | Tracked files | Purpose / contents |
|---|---|---|
| `forge-core` | 154 | Core card model, data structures, tokens, utilities. Package root `forge.*`. |
| `forge-game` | 825 | **Rules engine**: game state, entities, effects, triggers, stack, trackable. `forge.game`, `forge.trackable`. |
| `forge-ai` | 193 | AI decision-making (`forge.ai`). |
| `forge-gui` | **56,076** | Shared GUI + **all game assets**. ~55,554 are under `res/` (art/sound/data). Java source is small. |
| `forge-gui-desktop` | 564 | Desktop Swing client; **main entry point** (`forge.view.Main`). |
| `forge-gui-mobile` | 389 | Mobile GUI shared code. |
| `forge-gui-android` | 109 | Android port (partial resources ignored in git). Entry `forge.app.{Launcher,Main}`. |
| `forge-gui-ios` | 82 | iOS port (early stage). Entry `forge.ios.Main`. |
| `forge-gui-mobile-dev` | 28 | Mobile dev harness (`forge.app.{GameLauncher,Main}`). |
| `forge-lda` | 24 | Local data assistant / helpers. |
| `adventure-editor` | 49 | Adventure-mode map/quest editor. Entry `forge.adventure.Main`. |
| `forge-installer` | 7 | Installer module. |

Build output of interest: `forge-gui-desktop/target/forge-gui-desktop-*-jar-with-dependencies.jar` (the runnable fat JAR).

### Module dependency graph (runtime chain)
Declared via each module's `<dependencies>` in its `pom.xml`. The build/runtime order is a strict chain; lower modules must be installed before higher ones:

```
forge-core  ──┬──► forge-game   ──► forge-ai    ──► forge-gui       ──┬──► forge-gui-desktop
(guava,      │     (depends on   │  (commons- │    (netty, jetty,   │     [Swing client; audio=jlayer/mp3]
 commons)    │      forge-core)  │    math3)  │    jupnp, lz4=     ├──► forge-gui-mobile
             │                   │            │    networking)      │      [libGDX]
             └───────────────► adventure-editor                       │     [mobile GUI code]
                              (depends on forge-gui + forge-gui-mobile)┘
```
- **forge-core** — leaf; depends only on guava / commons-lang3-text / tinylog. No `forge.*` deps.
- **forge-game** → forge-core (+ jgrapht, sentry). Pure engine, no UI/networking.
- **forge-ai** → forge-core + forge-game (commons-math3). AI evaluation/decision logic in `forge.ai`.
- **forge-gui** → forge-core + forge-game + forge-ai (+ netty/jetty/jupnp/lz4/xstream/gson). This is where **networking/netplay code lives** (`forge-net` classes, deckscreen network controllers) and XML/deck serialization.
- **forge-gui-desktop** → all four core modules (+ miglayout-swing, jlayer/mp3 audio, launch4j exe wrapper). Desktop Swing UI + main entry `forge.view.Main`.
- **forge-gui-mobile / forge-gui-android / -ios / -mobile-dev** → forge-core+game+ai+gui (+ libGDX on mobile).
- **adventure-editor** → forge-gui + forge-gui-mobile (+ Forge editor for Adventure maps/quests).

> Consequence: to change rules, edit `forge-game` (and its `forge-core` model as needed) and rebuild from the root; `forge-gui*` re-link automatically. Networking changes go in `forge-gui` (not desktop), since both desktop and mobile share it.

### §3.1 Fork-authored vs upstream-inherited code
This fork (`rssilver/forge`) is based on `Card-Forge/forge`. **Most source here is inherited, not authored locally** — our commits mostly *modify inherited files* and add *data*. Verify provenance per file with `git show #SHA` / blame (path-presence-in-upstream can't distinguish content origin). Three tiers:

**A. Genuinely new to this fork** (files absent from `upstream/master`):
- Root launcher/config: `run.bat`, `run.ps1`, `CLAUDE.md`, `.gitignore` (`.cortex/` entry).
- New draft/limited mode entry: `forge-gui/src/main/java/forge/gamemodes/limited/LimitedGameMode.java` (Commander Draft capability).

**B. Fork-authored logic layered onto inherited files** (exist upstream too, but we materially extended them):
- **Undo feature** — `forge-game/src/main/java/forge/game/undo/{UndoManager,GameMemento}.java` (+ desktop toolbox `FUndoManager`). Developed here; also merged upstream.
- **Quest-mode enhancements** — logic in `forge-gui/src/main/java/forge/gamemodes/quest/*` (Attractions & Contraptions support, sticker support) and quest data under `res/quest/`. The entire `gamemodes/quest/*` base is inherited; our additions sit on top.
- **Deck-statistics reorg** — `forge-gui/src/main/java/forge/deck/DeckStatistics.java` + mobile deck screens (`FDeckStatsScreen`, `FDeckStatsView`). The feature arrived via upstream PR #12221; we only relocated it locally (`d1b6a00`).

**C. Upstream-inherited core** (inherited from Card-Forge/forge; unchanged or lightly touched — read before assuming it's ours):
- `forge-core` model; `forge-game` engine subsystems (§4.1); the entire `gamemodes/quest/*` base and most of `gamemodes/limited/`; GUI rendering (`forge-gui/.../gui`); editions + base card-data files.

**Data-only additions (no new code):** `res/draft/rankings/`, CMR Rankings file, `res/cardsfolder/upcoming/`, `res/tokenscripts/`.

> **Bottom line:** treat the engine, Quest mode, deck statistics and undo as *upstream-owned* unless `git show #SHA` shows a fork-authored commit. Our genuinely new code is small (launcher scripts + `LimitedGameMode.java`).

---

## 4. Java package index (searchable core)
Primary source roots are `<module>/src/main/java/forge/<pkg>`.

| Module | Packages under `.../forge/` | What lives here |
|---|---|---|
| `forge-core/src/main/java/forge` | `card`(33), `deck`(26), `item`(23), `token`(2), `util`(63) | Card **definition model** (what a card *is*), deck builder/parsing, item base classes, token generation, shared utils. Foundation for everything above it. |
| `forge-game/src/main/java/forge` | `game`(822 across subpkgs), `trackable`, `util` | **Game engine / rules**. See §4.1 below for the `game/*` taxonomy. `trackable` = change tracking / memento (undo). |
| `forge-ai/src/main/java/forge` | `ai` | AI evaluation & decision logic. |
| `forge-gui/src/main/java/forge` | `deck`, `download`, `error`, `gamemodes`, `gui`, `haptic`, `interfaces`, `itemmanager`, `localinstance`, `model`, `player`, `sound`, `util` | GUI model & controllers: card display, game modes (Quest/Adventure/Draft/etc.), player UI, sound/haptic, interfaces. Networking code also lives here (see graph). |

### §4.1 Game engine internals — `forge-game/src/main/java/forge/game/*`
This is the rules engine; most rule/combat changes land here. Subpackage file counts (`.java`):

| Subpackage | # | Responsibility |
|---|---|---|
| `trigger` | 143 | Triggered abilities / when-ever/when conditions. Largest subsystem. |
| `ability` | 217 | All ability types (activatable, triggered, static). Root of card behavior. |
| `staticability` | 67 | Static effects / continuous effects. |
| `event` | 65 | Game event pipeline (`GameEventForwarder`, batching). |
| `cost` | 53 | Costs (mana, tap, discard, etc.). |
| `replacement` | 47 | Damage/replacement & replacement effects. |
| `card` | 46 | Card/game-entity state & behavior. |
| `keyword` | 38 | Keywords (flying, first strike, …). |
| `player` | 34 | Player actions/state. |
| `spellability` | 24 | Instant/sorcery & spell abilities. |
| `zone` | 8 | Zones (library, hand, battlefield, graveyard, command, exile…). |
| `phase` | 7 | Turn/step phase structure. |
| `mulligan` | 7 | Mulligan rules. |
| `mana` | 6 | Mana system / adding mana. |
| `extrahands` | 1 | Additional-hand logic. |
| `undo` | 2 | **Undo/restore** — `GameMemento`, `UndoManager`. |

> Rule-change cheat sheet: combat → `combat` (small, ~10 files), triggers → `trigger`, abilities/costs/statics → `ability`/`cost`/`staticability`/`replacement`, keywords → `keyword`, game-state/events → `event`/`zone`.

### §4.2 Networking & netplay
Not a single package — spread across:
- **`forge-gui`** (shared): networking libs + client/server abstractions (`netty`, `jupnp`). The core netplay protocol lives here, not in desktop.
- **`forge-gui-desktop/src/main/java/forge/screens/deckeditor/controllers/`**: `CEditorNetworkDraft`, `NetworkDraftLog`.
- **`forge-gui-desktop/src/test/java/forge/net/`**: `HeadlessNetworkClient`, `NetworkPlayIntegrationTest` — useful reference for how netplay is driven/tested.

> Note: the package list above is for `forge-gui/src/main/java/forge`. `gamemodes` holds single-player/online game mode implementations; `gui` holds rendering/combat UI.

---

## 5. Resource tree (`res/` and data locations)
All assets live under `forge-gui/res/`. **Card text/data files** are in `res/cardsfolder/` (~34,088 files). This is the largest data directory — card definitions, not art.

| Subdir | Files | Contents |
|---|---|---|
| `cardsfolder` | 34,088 | **Card data files** (per-card text/data). Primary place to add/inspect cards. |
| `quest` | 9,564 | Quest-mode world/assets. |
| `adventure` | 5,707 | Adventure-mode assets (maps, enemies, rewards — Tiled projects). |
| `formats` | 1,341 | Format definitions (Standard, Modern, Commander, etc.). |
| `editions` | 685 | Set/edition data. |
| `genetica...` (genetica sets) | 786 | Genetica set assets. |
| `conquest` | 1,434 | Conquest-mode assets. |
| `puzzle` | 375 | Puzzle mode assets. |
| `tokenscr...` (token cards) | 857 | Token card images/data. |
| `skins` | 112 | UI skins. |
| `setlookup` | 66 | Set lookup data. |
| `draft` | 288 | Draft assets. |
| `blockdata`, `cube`, `languages`(18), `fonts`, `sound`(39), `music`(9), `defaults`(17), `licenses`, `lists`(35), `sealed`, `tutorial` | — | Supporting data: localization (18 langs), sound, music, fonts, license lists. |
| `ai`(4), `conquest` | — | AI/skin assets. |

---

## 6. Build, run & launch
- **Build:** Maven. Parent `pom.xml`; build all with `mvn clean package -DskipTests`. Requires Java 17+.
- **Run (Windows):** `run.bat [rebuild]` / `run.ps1 [-Rebuild]`.
  - Without the rebuild flag it launches the existing fat JAR directly.
  - With `rebuild` it runs `mvn clean package -DskipTests`, then syncs resources, then launches.
  - **Resource sync step:** `xcopy /E /I /Y /Q forge-gui\res res` copies assets to repo root so the running app finds skins/etc. (a stale root `res/` can shadow real assets — always re-sync).
  - Launch: `java -Xmx4g -jar <forge-gui-desktop-*-jar-with-dependencies.jar>`.
- **Per-platform entry points:** desktop `forge.view.Main`; android `forge.app.Launcher/Main`; ios `forge.ios.Main`; mobile-dev `forge.app.GameLauncher/Main`.

### Verify your changes (authoritative, from `.github/workflows/test-build.yaml`)
- **Run the test suite** (TestNG): `mvn -U -B clean test`. Tests are GUI-heavy and run under a virtual framebuffer — CI installs `xvfb` for this. For one module: `mvn -pl forge-game test`.
- **Package / fat JAR:** `mvn clean package -DskipTests` (what `run.bat rebuild` does).
- **Checkstyle:** enforced via the `.checkstyle` + `checkstyle.xml` config file — it is **NOT** wired into Maven as a plugin (`maven-checkstyle-plugin` absent from all poms), so it's checked by an external tool / your editor's Checkstyle extension, not by `mvn`. Run that check before committing to avoid CI/rejection failures.

---

## 7. Card data format & scripting API
### 7.1 Per-card files — `res/cardsfolder/*.txt` (34,085 .txt files)
One file per card; filename is a slugified name (`forge-gui/res/cardsfolder/<letter>/aclazotz_deepest_betrayal_..._txt`). Lines are `Key:Value`. Common fields:

| Field | Meaning | Example |
|---|---|---|
| `Name` | Card name | `Name:Aclazotz, Deepest Betrayal` |
| `ManaCost` | Mana cost (or `no cost`) | `ManaCost:3 B B` |
| `Types` | Card type line | `Types:Legendary Creature Bat God` |
| `PT` | Power/Toughness (creatures) | `PT:4/4` |
| `K` | Keyword(s), comma-separated | `K:Flying\nK:Lifelink` |
| `A` | Ability (activatable/static): `A:AB$ <type> \| ...` | `A:AB$ Mana \| Cost$ T \| Produced$ B` |
| `T` | Trigger/mode entry; each starts a mode + `TriggerDescription` | `T:Mode$ Attacks \| ValidCard$ Card.Self \| Execute$ TrigDiscard …` |
| `SVar:` | Sub-ability / variable definitions backing `T` modes (`DB$`, `Mode$`, `Defined$`, etc.) | `SVar:TrigDiscard:DB$ Discard \| Mode$ TgtChoose …` |
| `DeckHas` / `DeckHints` | Deck-level flags/hints (optimization) | `DeckHas:Ability$Discard\|LifeGain\|Token` |
| `Oracle` | Plain-text oracle text (rendered to UI; `\n` = newline) | `Oracle:Flying, lifelink\nWhenever …` |
| `AlternateMode` / `ALTERNATE` | Double-faced/transform cards: a second block after the `ALTERNATE` marker | `AlternateMode:DoubleFaced` then `Name:... Oracle:...` |

**Scripting primitives** used inside `A`/`T`/`SVar`: `DB$` (ability definition type — e.g. `Discard`, `Draw`, `Mana`, `Token`, `ChangeZone`, `Effect`), `Mode$`, `ValidCard$`, `Execute$`, `TriggerZones$`, `Cost$`, `Transformed$`. These map to implementations in the matching `forge-game/.../*` subpackages (see §4.1).

**Authoritative scripting docs:** `docs/Card-scripting-API/` — `AbilityFactory.md`, `Costs.md`, `Triggers.md`, `Restrictions.md`, `Statics.md`, `Targeting.md`, `Card-scripting-API.md`. Also `docs/File-Formats.md`, `docs/Creating-a-custom-Card.md`, `docs/Creating-a-custom-Set.md`.

### 7.2 Edition / set files — `res/editions/*.txt` (685 files)
INI-style, grouped by set:
```ini
[metadata]
Code=P15A                      # internal code
Date=2008-04-01
Name=15th Anniversary Cards
Type=Promo
ScryfallCode=P15A             # maps to the Scryfall API

[cards]
1 R Char @Dave Dorman         # <position> <rarity> <nickname> @<artist>
2 R Kamahl, Pit Fighter @Kev Walker
```
Rarities: `C / R / M / S / ...`. Nicknames in the `[cards]` list are what cards reference across editions; the actual card text lives in `res/cardsfolder/`.

### 7.3 Other data dirs
- **Net deck archives** (online decklist database): updated by commits like "Update net deck archive lists" — files under `forge-gui/res/...` referenced by the online browser.
- **Formats**: `res/formats/` (Standard, Modern, Commander, …).
- **Skins / languages (`res/languages`, 18)**: UI theme + i18n property bundles.

---

## 8. Behavioral Execution Model
> High-level map of *how a game runs*. Ground truth for MTG rules is the **Comprehensive Rules** (Wizards of the Coast); Forge-specific execution lives in these classes — read them before assuming edge-case behavior. This section points at the right class; it does not reproduce every rule.

Forge drives a game as a **turn/phase loop**: `Game.java` orchestrates, `PhaseHandler` advances phases/steps (`PhaseType`), and between steps the loop checks state-based actions and processes queued spell/ability commands plus triggered abilities.

### 8.1 Turn & phase structure
`forge-game/src/main/java/forge/game/phase/PhaseType.java` is authoritative — it lists every step in order and groups them via `PHASE_GROUPS`:
- **Start:** Untap → Upkeep → Draw.
- **Pre-combat main:** Main 1.
- **Combat:** Begin Combat → Declare Attackers → Declare Blockers → First Strike Damage → Combat Damage → End Combat.
- **Post-combat main:** Main 2.
- **End:** End of Turn → Cleanup.
`PhaseHandler.java` walks these; `Untap`, `ExtraTurn`, `ExtraPhase` model special phase effects.

### 8.2 Stack, costs & resolution
- Costs are paid via `Game.costPaymentStack` (`CostPaymentStack`).
- Spells/abilities resolve on the game stack; when they resolve, their effects fire and create new events.
- **Triggered abilities** live in `forge-game/src/main/java/forge/game/trigger/*` (see §4.1). Events are modeled in `event/Event.java`. The ordering of simultaneous/multiple triggers is resolved there — the most subtle area; read that code plus `docs/Card-scripting-API/` for exact semantics rather than assuming.

### 8.3 Combat
`forge-game/src/main/java/forge/game/combat/*` implements the combat sub-steps from §8.1 (attackers, blockers, damage assignment).

### 8.4 State-based actions & lethal damage
The loop checks state-based actions each time priority would pass; `event/*` plus the SBAs in `Game.java` model when permanents are destroyed and how lethal damage is tracked.

### 8.5 Undo / memento (fork area)
`game/undo/{GameMemento,UndoManager}.java` together with `Game.copyLastState()` snapshot state to support undo (§3.1).

**Drill-down pointers:** rules/combat/timing → `forge-game`; scripting primitives → `docs/Card-scripting-API/`; card behavior definitions → `res/cardsfolder/*.txt`, which reference the classes above.

---

## 9. Config, generated files & `.gitignore` semantics
Key ignored patterns (so you don't treat them as source):
- Build/temp: `target`, `test-output`, `bin`, `gen`, `*.log`, flattened/release poms.
- IDE: `*.idea`, `*.iml`, `.settings`, `.classpath`, `.project`, `.vscode/*`, `nbactions.xml`.
- Project-local config: `forge.profile.properties`, `/jgv.txt`.
- Mobile assets (partially ignored): `forge-gui-android/res/*/*` except launchers/icons/layout; `forge-gui-ios/resources/**` kept; mobile-dev `res`/`testAssets`.
- **Generated** (tracked but auto-produced): `forge-gui/release-files/CHANGES.txt`, `forge-gui/tools/AllCards.json`, `PerSetTrackingResults`, edition tracking results.
- Card-data temp: `forge-gui/res/cardsfolder/*.bat`; quest world `.directory`.
- Tiled adventure projects/sessions, python temporaries (`__pycache__`, `*.pyc`), Claude/Cortex config (`.claude`, `.cortex/`).

---

## 10. Testing
- Standard Maven test layout: `<module>/src/test/java`. Primary tree under `forge-gui/src/test`.
- No custom test runner config at repo root; rely on Maven Surefire (`mvn test`). Build scripts use `-DskipTests` for speed.
- Checkstyle enforced via `.checkstyle` + `checkstyle.xml` (see Conventions).

---

## 11. Feature → file lookup table
Use this to jump straight to the right area. When searching, grep by class name or package root.

| Feature / concern | Where to look |
|---|---|
| Card model & data | `forge-core/src/main/java/forge/card`, `res/cardsfolder` |
| Deck building/browsing | `forge-gui/src/main/java/forge/deck`, `forge-core/.../deck` |
| Rules engine (stack, effects, triggers, combat, lethal damage) | `forge-game/src/main/java/forge/game` |
| Change tracking / undo & memento | `forge-game/.../trackable`, `GameMemento`, `UndoManager` |
| AI evaluation | `forge-ai/src/main/java/forge/ai` |
| Desktop UI / combat rendering / card zoom / match screen | `forge-gui/src/main/java/forge/gui`, `forge-gui-desktop/.../forge/view` |
| Game modes (Quest, Adventure, Draft, Commander, Puzzle, Conquest) | `forge-gui/src/main/java/forge/gamemodes` + `res/{quest,adventure,draft,puzzle,conquest}` |
| Netplay / networking / network logging | search `Network`, `netplay`; forge-gui gui/network classes |
| Card images (art) | `forge-gui/res/cardsfolder`, `pics*` (gitignored generated pics) |
| Editions / sets / rankings | `res/editions`, `res/setlookup`; ranking files e.g. CMR Rankings |
| Sound / music / coins / dice 3D / haptics | `res/sound`, `res/music`, `forge-gui/.../sound`, `haptic`, `Coin3D`, `Dice3D` |
| Adventure editor | `adventure-editor/src/main/java/forge/adventure` |
| Localization (18 languages) | `res/languages` |
| Skins / UI themes | `res/skins` |
| Commander / EDHREC integration | `forge-gui` GUI + MCP tools in `agents.md` |

### Search shortcuts
- Find a class/file: `git ls-files \| Select-String <Name>` or the `Glob`/`Grep` tools.
- Find where a feature changed recently: `git log --no-merges origin/master..master-origin --format='%h|%ci|%s'` then `git show <hash>`.
- Card text search across data files: `rg -i "<cardname>" forge-gui/res/cardsfolder`.

---

## 12. Conventions
- **Java 17**, UTF-8, Maven standard layout (`src/main/java`, `src/test/java`).
- Code style enforced by **Checkstyle** (`.checkstyle`, `checkstyle.xml`) — run the build's checkstyle goal before committing to avoid rejections.
- Package root is `forge.*`; module → package mirrors the table in §4.
- Commit messages reference upstream PR numbers (`(#NNNNN`) when backporting; local work uses descriptive subjects.

---

## 13. Glossary
- **master-origin** — local baseline branch (this snapshot's base) used to derive docs and the changelog delta.
- **fork base / existing code** — `origin/master` (`0fa862eb1c`); commits on top of it = the changelog set.
- **Quest mode** — single-player campaign mode (supports Attractions, Contraptions, Stickers, Dungeons).
- **Adventure mode** — overworld/exploration single-player mode with maps, enemies, rewards.
- **MDD / MDC / PMEI / SLD / TRC / TRK / CMR / YSOS** — MTG set/edition abbreviations touched by edition-update commits.
- **GameMemento / UndoManager** — save-state + undo infrastructure for the game engine.

---
*Regenerate/refresh this map after major structural changes: `git ls-files | wc` per module (§3), and re-list package roots (§4). Cross-reference `changelog.md` for what moved.*
