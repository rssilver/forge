# agents.md — How to use this repo's documentation

This file tells the agent how to read and maintain the two companion docs:
- **`spec.md`** — static map of *what the codebase is* (modules, dependency graph, packages, resources, build, where things live). Deepened this session: §3 module dependency chain, §3.1 fork-authored vs upstream-inherited code, §4.1 `forge-game/game/*` engine taxonomy with file counts, §4.2 netplay location, §7 concrete card/edition data formats, §8 Behavioral Execution Model (turn phases, stack/resolution, combat, SBAs).
- **`changelog.md`** — dynamic record of *what changed and when* (timestamps + SHAs). Split into two parts: the main body = work committed in this fork; a bottom **"Parent-repo / upstream-origin changes"** section = changes pulled from or mirroring the parent repo `Card-Forge/forge`.

Read `spec.md` first to understand structure; read `changelog.md` to understand recent/authoritative changes. Both are single root-level files by design (see `spec.md` §"Why one file").

---

## 1. When to open which file
| Situation | Read |
|---|---|
| Starting any task / need repo orientation | `spec.md` (§3 dependency graph, §4 package index, §4.1 game engine taxonomy, §8 execution flow, §7 formats, §11 feature lookup) |
| Working on a known area (undo, netplay, editions, quest…) | `changelog.md` category for that area → short SHA → `git show #SHORT` |
| "What changed since the base / recently?" | `changelog.md` (top of each category = most recent) |
| Is this change ours or from upstream? | `changelog.md` bottom "Parent-repo / upstream-origin changes" section; verify with `git log --no-merges upstream/master..master-origin` |
| Confirming a file/class exists before editing | `spec.md` §4/§5, then `Glob`/`Grep`, or `git ls-files` |
| Understanding how a mechanic behaves at runtime | `spec.md` §8 (Behavioral Execution Model) → engine classes in `forge-game` |
| Verifying something may be out of date | §2 below (staleness check) |

---

## 2. Timestamp / commit-number tracking system & staleness checks
Every durable fact in these files is anchored to **one of two sources of truth**, never a bare description:

1. **Full committer timestamp** `[YYYY-MM-DD HH:mm zzz]` — the git committer date (authoritative; wall-clock "now" differs per committer, so we always use the git value).
2. **Short SHA** `` `#SHORT` `` (7–10 chars) — used for `git show #SHORT`, blame, and diffing.

### Staleness / freshness check (run before trusting or editing these docs)
```powershell
# Non-merge work added on top of the fork base (authoritative changelog source):
git log --no-merges origin/master..master-origin --format='%h|%ci|%s'
# Merge commits brought into this branch:
git log --merges  origin/master..master-origin --format='%h|%ci|%s'
# Genuinely fork-authored work (added here that upstream did NOT have):
git log --no-merges upstream/master..master-origin --format='%h|%ci|%s'
```
Compare the output against `changelog.md`:
- **New commits not in the file** → append them (see §3).
- **Dates/SHAs that don't match** → the file is stale; regenerate affected sections.
- The **"Last audited"** line at the top of `changelog.md` and the **"Last updated"** line in `spec.md` record when each was last verified against git — treat a date older than your current session as "needs re-audit".

> **Fork vs upstream:** this fork (`rssilver/forge`) is based on parent repo `Card-Forge/forge` (`upstream/master`). Roughly half of the delta over the fork base is upstream-origin — 8 merge commits pull PRs in, and ~27 of 57 non-merge commits reference an upstream PR (`refs #NNNN`, i.e. backports). So **do not assume every changelog entry here was authored in this fork.** The main body = work committed in this fork; the bottom **"Parent-repo / upstream-origin changes"** section lists the merges + backports that mirror `Card-Forge/forge`. Use `git log --no-merges upstream/master..master-origin` to see what is genuinely fork-only.
>
> Base references: existing/fork code = `origin/master` (`0fa862eb1c`); docs/changelog tip = `master-origin` (`91335c221e4`). If either ref moves, re-run the commands above.

---

## 3. Changelog update workflow (how to save new changes)
After work lands on top of the base (i.e. commits appear in `origin/master..<current tip>`):

1. Re-run the audit commands in §2; identify entries not already in `changelog.md`, and run `git log --no-merges upstream/master..master-origin` to know which are genuinely fork-authored vs backports.
2. For each new commit, add one line under the **correct category** (A–J in `changelog.md`), newest-first:
   ```markdown
   - **[YYYY-MM-DD HH:mm zzz]** `#SHORT` — <subject>  (refs #NNNN)   [+ optional one-line scope note]
   ```
3. Add any new merge commits to the **Merge commits** table (reverse-chronological).
4. Update the **Summary** counts and the **"Last audited"** line.
5. If a change alters structure/modules/packages/resources/build, update the matching section of `spec.md`.
6. If a commit is upstream-origin (a merge, or a backport referencing an upstream PR `refs #NNNN` that mirrors `Card-Forge/forge`), note it in the bottom **"Parent-repo / upstream-origin changes"** section so readers know it isn't fork-authored work.

#### Pre-commit & removal changelog rule (enforced from this session)
- **Describe *before* you commit.** Whenever you are about to commit work on top of the fork base, add the corresponding `changelog.md` entry(ies) **first** so history and docs stay in sync. Every entry MUST carry its committer timestamp `[YYYY-MM-DD HH:mm zzz]` + short SHA (see template above); if a commit touches several areas use one category but list each `#SHORT`. When committing the doc files themselves, add an entry under **Build / Launch / Infrastructure** noting "(developer documentation)".
- **Removing/reverting a documented commit.** If a commit already in `changelog.md` is later removed or reverted, annotate its existing entry with a **Removed header** — do NOT delete the original line (keeps history readable). Format:
  ```markdown
  - **[original committer time]** `#ORIGSHA` — <subject>  (refs #NNNN)
    > **Removed** `[removal-timestamp]` by `#REMOVALSHA` → reverts `#ORIGSHA` (<one-line why>)
  ```
  The original entry keeps its own committer timestamp; the Removed header carries a **separate** removal timestamp and the SHA of the commit that removed it. Place the Removed header immediately under the entry's bullet.

#### Concrete procedure (placeholder → real SHA → single amend)
Because an entry must name a commit's SHA but that SHA only exists *after* committing, use this exact sequence so you never leave `#<sha>` placeholders or stray "fixup" commits in history:
1. Write each changelog entry with a `#<sha>` placeholder and the expected committer timestamp (use `[YYYY-MM-DD HH:mm zzz]` to fill).
2. Commit the work; git assigns the real SHA + committer time. Read it back: `git log -1 --format='%h|%ci'`.
3. Replace the `#<sha>` placeholder with the real short SHA and set the timestamp from `%ci`.
4. Amend once (`git commit --amend`) to fold the corrected changelog into the single commit — do not make a separate "fixup" commit.

Notes:
- After the amend, HEAD's SHA differs from the pre-amend SHA the entry references; that is intentional and fine. The entry points at a real commit that genuinely added the files (amend changes only commit metadata, not tree content), and a changelog is a historical record — referencing the actual authoring commit is correct even if it isn't `git rev-parse HEAD`.
- Do NOT chase making the changelog contain its own final SHA in one pass: a file inside a commit cannot hold that commit's hash, since the hash is computed from tree contents including that text. One amend is sufficient; stop there.

### Category selection rules
Group by **primary effect area**, not by file path: user-facing capability → `Features`; engine/rules/stack/combat/randomness → `Rules Engine`; rendering/UI/controls → `UI`; netplay/networking → `Networking`; AI logic → `AI`; card/set/edition/archive data & image downloading → `Card Data, Editions & Sets`; Quest/Adventure modes → `Quest & Adventure`; build/launch/refactor/relocation → `Build / Launch / Infrastructure`; generic cleanup/comments → `Maintenance & Cleanup`; explicit `Revert` → `Reverts`.
If a commit spans two areas, pick the primary one and add a short `(cross-ref …)` note.

---

## 4. Spec.md update workflow (structural changes only)
Refresh these sections when they drift from reality:
- **§3 Module map + dependency graph** — recompute per-module file counts: `git ls-files "<module>" | Measure-Object -Line`; re-check each module's `<dependencies>` in its `pom.xml` if the build/runtime chain changes.
- **§3.1 Fork-authored vs upstream-inherited code** — refresh when big changes land; new files absent from `upstream/master` are ours, inherited engine/quest/deckstats/update areas are upstream (verify with `git show #SHA`).
- **§4 Package index + §4.1 game taxonomy** — re-list package roots and, for `forge-game/game/*`, recount subpackage `.java` files (ability/trigger/cost/replacement/keyword/combat/phase/etc.) since these counts drive the rule-change cheat sheet.
- **§4.2 Netplay location** — if networking code moves modules or new entry points appear, update where netplay classes live (`forge-gui` core vs `forge-gui-desktop` screens/tests).
- **§5 Resource tree** — re-list `forge-gui/res` subdirs and counts.
- **§7 Card data / §6 Build** — only if format/build instructions change (e.g. new card-file fields, edition INI structure, or the run script flow).

---

## 5. Search workflow for features & files (fast path)
1. Identify the area via `spec.md` §11 (feature → location table); for runtime behavior, see §8 (Behavioral Execution Model).
2. Confirm exact paths with `Glob` (by pattern) or `Grep`/`rg` (by content/class name).
3. To find where something recently changed: `git log --no-merges origin/master..master-origin --format='%h|%s' | Select-String "<keyword>"`.
4. Card-text search: `rg -i "<cardname>" forge-gui/res/cardsfolder`.
5. Confirm the entry point for the platform you're touching (desktop = `forge-gui-desktop/src/main/java/forge/view/Main.java`; android = `forge-gui-android/src/forge/app/Main.java`; ios = `forge-gui-ios/src/forge/ios/Main.java`).

---

## 6. Available MTG data tools (MCP)
Useful for card/deck/format lookups that don't require reading the code:
- Card details/rulings/price/image, format legality (`mtg_commander_check_commander_legality`), banned list.
- Deck fetch + validation from Archidekt / Moxfield (`get_archidekt_deck`, `get_moxfield_deck`, `validate_deck`).
- EDHREC recommendations/combos; SearchRules / glossary for rule text.
These complement — but do not replace — reading the actual engine in `forge-game`/`forge-core`.

---

## 7. Gotchas (learned from this repo)
- **PowerShell 5.1**: no `&&`, no `wc`. Use `;`/`if ($?) {}` and `(Measure-Object -Line).Lines`.
- **`res/` is huge (~55k files)** but mostly art/data under `forge-gui/res/cardsfolder`; Java source is small and concentrated — don't eyeball file counts to judge code size.
- **Resource sync at runtime**: launcher copies `forge-gui\res` → repo-root `res/`; a stale root `res/` shadows real assets.
- **Checkstyle** (`.checkstyle`, `checkstyle.xml`) is enforced via an external tool / IDE extension, NOT a Maven plugin (`maven-checkstyle-plugin` is absent from all poms) — verify style in your editor/CI before committing; there's no `mvn checkstyle:check`.
- **Tests use TestNG**; CI runs `mvn -U -B clean test` under Xvfb (virtual framebuffer) because tests are GUI-heavy. Per module: `mvn -pl <module> test`. Package a fat JAR with `mvn clean package -DskipTests`.
- Generated files exist but are tracked (`forge-gui/release-files/CHANGES.txt`, `forge-gui/tools/AllCards.json`); don't hand-edit them.

---
*These three docs (spec / changelog / agents) form one self-contained system: spec = map, changelog = history with timestamps+SHAs, agents = how to read and update both. Keep the audit commands (§2) as the single source of truth.*
