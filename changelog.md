# Changelog — Commits added on top of the fork base (`origin/master`)

> **Scope:** every commit in `origin/master..master-origin` (i.e. work added on top of the existing/fork code). This is the authoritative "what changed and when" record for this branch. Base (existing code) = `origin/master` `0fa862eb1c`. Tip / docs base = `master-origin` `91335c221e4`.
> **Regenerate to audit:** `git log --no-merges origin/master..master-origin --format='%h|%ci|%s'` (non-merge) and `git log --merges origin/master..master-origin --format='%h|%ci|%s'` (merges).
> Last audited: 2026-10-12 21:00 -04:00.

---

## How to read this file (lookup system)
- Entries are grouped by **category** (so a topic like *undo*, *netplay*, or *editions* is found in one place), and within each category by **descending date**.
- Each entry carries three anchors for lookup:
  - **Full timestamp** `[YYYY-MM-DD HH:mm zzz]` — the committer time (authoritative, from git).
  - **Short SHA** `` `#SHORT` `` — to jump straight to the commit (`git show #SHORT`).
  - **PR reference** `(refs #NNNN)` when the message backports an upstream PR.
- Categories: `Features`, `Rules Engine & Game Logic`, `UI / Rendering / Controls`, `Networking`, `AI`, `Card Data, Editions & Sets`, `Quest & Adventure Mode`, `Build / Launch / Infrastructure`, `Maintenance & Cleanup`, `Reverts`.

---

## Summary (this delta: 57 non-merge + 8 merge commits)
| # | Category | Commits |
|---|---|---|
| A | Features & New Modes | 6 |
| B | Rules Engine & Game Logic | 14 |
| C | UI / Rendering / Controls | 10 |
| D | Networking | 3 |
| E | AI | 1 |
| F | Card Data, Editions & Sets | 11 |
| G | Quest & Adventure Mode | 5 |
| H | Build / Launch / Infrastructure | 3 |
| I | Maintenance & Cleanup | 3 |
| J | Reverts | 1 |

---

## A. Features & New Modes
- **[2026-10-11 22:23:37 +0800]** `#e6f4f67` — Add UndoManager, fix Undo (refs #12226)
- **[2026-10-12 05:24:33 +0800]** `#78b4334` — Update UndoManager *(undo infra follow-up)*
- **[2026-10-12 04:44:53 +0800]** `#a9a9280` — Update GameMemento *(undo/infra follow-up)*
- **[2026-10-11 10:54:42 +0800]** `#328d23a` — Add deck statistics (refs #12221)
- **[2026-10-08 20:54:21 +0800]** `#4621576` — Add FlipOntoBattlefieldAnimation (refs #12183)
- **[2026-10-05 21:22:52 -0400]** `#e54d206` — Add the capability to do Commander Draft

## B. Rules Engine & Game Logic
- **[2026-10-11 09:14:10 +0800]** `#9392ce3` — minor optimization *(perf; area ambiguous, engine)*
- **[2026-10-11 12:30:43 +0800]** `#25ee7cf` — add blocked gate check for castle
- **[2026-10-10 07:53:21 +0000]** `#5568558` — Simultaneous entry: what sees a permanent enter is decided after everything entering with it has (refs #12188)
- **[2026-10-10 14:17:09 +0000]** `#ca64cc9` — Eureka, Hypergenesis: end the process when no card was actually put onto the battlefield (refs #12204)
- **[2026-10-10 16:31:19 +0000]** `#c2ba0f0` — fix(cards): AddType$ comma lists and the g_1_1_elf token type (refs #12212)
- **[2026-10-09 20:17:55 +0000]** `#a2ba8ce` — Rework GameEventForwarder to batch events at a game loop checkpoint (refs #12189)
- **[2026-10-09 11:35:01 +0800]** `#3b2abcd` — fix soft lock (refs #12191)
- **[2026-10-09 15:05:51 +0800]** `#3607237` — increase hit chance for FlipOntoBattlefieldEffect
- **[2026-10-09 14:20:38 +0800]** `#29f8daa` — fix logic for two hit cards
- **[2026-10-09 09:01:12 +0800]** `#9039334` — Fix lethalDamage update
- **[2026-10-09 08:19:17 +0200]** `#4d8fbea` — Fix seven card-script param bugs found porting to Crucible (refs #12193)
- **[2026-10-08 14:38:05 +0000]** `#1aeded0` — Player must attack: don't count on an attack that has a cost (refs #12178)
- **[2026-10-08 12:44:40 +0000]** `#bf97db8` — Simultaneous entry: don't let permanents entering together change how each other enters (refs #12171)
- **[2026-10-08 13:25:32 +0200]** `#df629c8` — Booster slots draw from MyRandom, so a seeded pack is reproducible (refs #12184)

> Note: two commits with generic subjects (`minor optimization`, and the event-forwarder rework) are placed by best judgment; see `agents.md` for how to confirm exact file scope via `git show`.

## C. UI / Rendering / Controls
- **[2026-10-11 01:55:57 +0800]** `#5222f05` — Update CardZoom layout, MatchScreen layout and CommandZone (refs #12199)
- **[2026-10-11 13:44:07 +0800]** `#d81b4b1` — update deckstats icon
- **[2026-10-11 15:06:48 +0800]** `#3c5d9b7` — fix clipping
- **[2026-10-10 12:18:53 +0800]** `#e9407d1` — make CoinFlipOverlay auto close
- **[2026-10-10 12:27:12 -0400]** `#f653bf2` — Set default section to Commander
- **[2026-10-10 09:43:01 +0000]** `#74af3ac` — Show the waiting prompt after blockers are declared (refs #12203)
- **[2026-10-09 13:59:21 +0800]** `#3326f1a` — fix clashing of boss dialog and coinflip (refs #12192)
- **[2026-10-09 15:13:27 +0800]** `#f08703d` — fix logic for desktop swing since its not possible with the corner hit and don't have animation.
- **[2026-10-08 22:15:40 +0800]** `#d57b0cd` — update Coin3D, CoinFlipOverlay, Dice3D params
- **[2026-10-12 08:17:10 +0800]** `#3a025af` — Fix animation when using undo *(undo follow-up)*

## D. Networking
- **[2026-10-11 08:41:21 +0000]** `#42f2849` — Network logging changes for readability and size (refs #12224)
- **[2026-10-11 07:46:39 +0000]** `#8848e87` — Compose the "Waiting for" prompt client-side (refs #12205)
- **[2026-10-08 13:02:32 +0000]** `#9fb0151` — Fix netplay host bugs (refs #12181)

## E. AI
- **[2026-10-08 11:49:40 +0000]** `#e9fa54a` — AI: pay for a mana ability with a mana cost that is activated on its own (refs #12179)

## F. Card Data, Editions & Sets
- **[2026-10-11 07:19:52 +0000]** `#c23dc20` — YSOS: Summitfest Closing Ceremony (refs #12217)
- **[2026-10-10 11:29:35 +0100]** `#f5700f9` — Edition updates: MDC, MDD, PMEI
- **[2026-10-10 08:17:31 +0000]** `#82e0dea` — The Darkhold (MDD) and other MDD spoilers (refs #12202)
- **[2026-10-10 10:29:20 +0800]** `#9627a0c` — Fix specialized downloading base image *(asset-download fix)*
- **[2026-10-09 11:42:07 +0100]** `#096d6ae` — Edition updates: SLD, TRC, TRK
- **[2026-10-09 12:38:10 +0200]** `#df19539` — Update net deck archive lists
- **[2026-10-09 12:10:31 +0200]** `#84cfd43` — Update net deck archive lists *(second archive-list update)*
- **[2026-10-09 08:18:25 +0200]** `#2ede475` — TRK/TRC cards from New York Comic Con (refs #12187)
- **[2026-10-08 13:25:46 +0200]** `#55496c7` — Fix typos in Reality Fracture edition names (refs #12185)
- **[2026-10-05 21:22:56 -0400]** `#2679f5f` — Set free commander in CardEdition
- **[2026-10-05 21:22:56 -0400]** `#e20954a` — Add CMR Rankings file

## G. Quest & Adventure Mode
- **[2026-10-09 10:02:32 +0200]** `#bb00615` — Fix stale boss dialogue portraits between Adventure duels (refs #12176)
- **[2026-10-08 07:53:21 +0000]** `#3146cbd` — Old Border Shandalar: Fix Svyelunite Temple quest offers and rewards (refs #12177)
- **[2026-10-02 20:52:26 +0200]** `#e1db01e` — add sticker support to quest mode and cleanup
- **[2026-10-02 23:10:54 +0200]** `#c0557e5` — prevent funny sets from creating drafts in quest mode, since they crash the game *(reverted — see J)*
- **[2026-09-19 10:20:11 +0200]** `#3da46bb` — Update QuestMode to support Attractions and Contraptions

## H. Build / Launch / Infrastructure
- **[2026-10-12 22:58:38 -0400]** `#bb5daf9ded6` — Update agents.md with concrete pre-commit changelog procedure (write entries with a `#<sha>` placeholder before committing → fill real SHA/timestamp from `git log -1 --format='%h|%ci'` after commit → single `--amend`, no separate fixup commit)  *(developer documentation)*
- **[2026-10-12 22:41:37 -0400]** `#4988dbec9c0` — Add developer documentation (spec.md, changelog.md, agents.md): repo map with module dependency graph + §3.1 fork-vs-upstream tiers, §4 game-engine taxonomy / §7 card & edition formats, §8 Behavioral Execution Model; establish pre-commit changelog update + Removed-header convention  *(developer documentation)*
- **[2026-10-12 20:31:46 -0400]** `#91335c2` — Add URL deck deduplication and fix run.bat/run.ps1 rebuild flow
- **[2026-10-12 05:06:31 -0400]** `#8d67c78` — Add Windows launcher scripts (run.bat/run.ps1) and ignore .cortex/
- **[2026-10-11 11:01:53 +0800]** `#d1b6a00` — move DeckStatistics *(relocation/refactor)*

## I. Maintenance & Cleanup
- **[2026-10-10 16:36:39 +0000]** `#a69da2b` — Some cleanup (refs #12213)
- **[2026-10-09 09:17:22 +0800]** `#2fa7795` — add comment
- **[2026-10-08 12:42:03 +0000]** `#2342d00` — Some cleanup (refs #12186)

## J. Reverts
- **[2026-10-02 22:05:05 +0200]** `#348dbb2` — Revert "prevent funny sets from creating drafts in quest mode, since they crash the game"

---

## Merge commits (backports pulled into this branch)
| Full timestamp | SHA | Subject |
|---|---|---|
| 2026-10-11 20:16:15 -0400 | `#4d2d22c` | Merge pull request #10779 from Card-Forge/commander-draft |
| 2026-10-10 11:30:34 +0100 | `#c4f997f` | Merge pull request #12208 from paulsnoops/edition-updates |
| 2026-10-09 11:44:08 +0100 | `#c26a2e9` | Merge pull request #12197 from paulsnoops/edition-updates |
| 2026-10-09 11:43:18 +0100 | `#86c637a` | Merge pull request #12196 from churrufli/update-deck-archive-lists-min5 |
| 2026-10-09 11:13:31 +0100 | `#95492da` | Merge pull request #12195 from churrufli/master |
| 2026-10-08 11:59:45 -0400 | `#7e3b22b` | Merge pull request #12105 from Rensen3/quest_atttractions |
| 2026-10-02 12:09:00 +0200 | `#e58aec7` | Merge branch 'master-fork' into quest_atttractions |
| 2026-09-30 13:32:10 +0200 | `#d863380` | Merge branch 'master-fork' into quest_atttractions |

---

## Parent-repo / upstream-origin changes (incorporated, not authored here)
This fork (`rssilver/forge`) is based on the parent repo **Card-Forge/forge** (`upstream/master` = `3a025afacf8`, 2026-10-12). A large part of this branch's delta over the fork base (`origin/master`) was pulled from, or mirrors, upstream — it is not original work created in this fork. Two forms:

1. **Merge commits (8)** — pull upstream PRs/branches into the fork; unambiguously upstream-origin:
   - `#4d2d22c` Merge PR #10779 from Card-Forge/commander-draft *(Commander Draft originated upstream)*
   - `#c4f997f`, `#c26a2e9` Merge edition-update PRs (paulsnoops)
   - `#86c637a`, `#95492da` Merge deck-archive-list + churrufli/master updates
   - `#7e3b22b` Merge PR #12105 from Rensen3/quest_atttractions *(Quest Attractions/Contraptions)*
   - `#e58aec7`, `#d863380` Re-sync merges of 'master-fork' into quest_atttractions
2. **Backport commits** — 27 of the 57 non-merge commits reference an upstream PR (`refs #NNNN`), i.e. they mirror a Card-Forge/forge PR rather than add fork-only logic.

**To see what is genuinely fork-authored** (added here that upstream did not have):
```powershell
git log --no-merges upstream/master..master-origin --format='%h|%ci|%s'
```
Likely local-only additions include the Quest Attractions/Contraptions support, quest-mode sticker support, the "prevent funny sets from crashing quest drafts" fix (later reverted), CMR Rankings file, set-free-commander / default-section-Commander edition tweaks, UndoManager + undo, deck statistics, and — clearly ours at HEAD — the Windows launcher scripts (`run.bat`/`run.ps1`) and URL deck deduplication.

---

## How to keep this file current (update procedure)
See `agents.md` §"Changelog update workflow". In short, after new work lands on top of the base:
1. Run the two audit commands above; diff against this file.
2. Append new non-merge entries under the correct category (descending date), using the entry template below.
3. Add merges to the merge table.
4. Update the Summary counts and the "Last audited" line.

**Entry template:**
```
- **[YYYY-MM-DD HH:mm zzz]** `#SHORT` — <subject>  (refs #NNNN)   [+ one-line scope note if obvious]
```
