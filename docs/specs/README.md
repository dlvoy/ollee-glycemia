# Specs

This project uses spec-driven development. Every non-trivial change (a feature, a larger fix, a tooling or process change) gets its own spec folder here before any code is written.

## Layout

```
docs/specs/
├── README.md                 # this file: workflow and index
└── NN-kebab-case-name/
    ├── intent.md             # what and why
    ├── plan.md               # how
    └── progress.md           # execution tracking
```

- `NN` is a two-digit number. Use the next free one, in increasing order, and never reuse a number, even for an abandoned spec.
- The name says what the intent is (`11-nightscout-provider`), not what the code touched.

## Workflow

1. **Intent**: create the folder and write `intent.md`. Agree on it before planning.
2. **Plan**: write `plan.md`. Agree on it before coding.
3. **Execute**: keep `progress.md` current while working: tick the checklist, add dated log lines with commit hashes, and record the release once tagged.
4. **Close**: set the status to `done` (or `abandoned`, with the reason) and update the index below.

If the plan changes during the work, edit `plan.md` and note the change in the `progress.md` log. Don't silently rewrite history.

## File templates

### intent.md
```markdown
# NN: Title

## Problem
What is missing or wrong, and for whom.

## Goal
The outcome, in one or two sentences.

## Scope
- What this spec covers

## Out of scope
- What it deliberately leaves out

## Acceptance criteria
- [ ] Checkable statements that define "done"
```

### plan.md
```markdown
# NN: Title (plan)

## Approach
The chosen design and why.

## Files
- `path/to/File.kt`: role in the change

## Steps
1. Ordered implementation steps

## Tests
- Unit tests (Robolectric) and manual checks
```

### progress.md
```markdown
# NN: Title (progress)

**Status:** planned | in-progress | done | abandoned
**Release:** vX.Y.Z (or "Unreleased")

## Checklist
- [ ] Mirrors the plan steps

## Log
- YYYY-MM-DD: what happened (`commit-hash`)
```

## Retroactive specs

Specs 00–12 were written after the fact (2026-11-01) from the git history, the CHANGELOG and the code at v1.6.0. Commit hashes refer to the current `master` history.

Note: tags `v1.0.0`–`v1.3.0` point at commits from before a history rewrite (author emails were switched to the GitHub noreply address), so they are not ancestors of `master`. Each affected progress.md lists the matching commit on `master`.

## Index

| # | Spec | Status | Release |
|---|---|---|---|
| 00 | [bgollee-prototype](00-bgollee-prototype/intent.md) | done | pre-1.0 |
| 01 | [delta-broadcast-compat-branding](01-delta-broadcast-compat-branding/intent.md) | done | pre-1.0 |
| 02 | [pluggable-glycemia-providers](02-pluggable-glycemia-providers/intent.md) | done | v1.0.0 |
| 03 | [synthetic-sgv-generation](03-synthetic-sgv-generation/intent.md) | done | v1.0.0 |
| 04 | [history-graph-ble-permissions](04-history-graph-ble-permissions/intent.md) | done | v1.0.0 |
| 05 | [multi-watch-pairing-ui](05-multi-watch-pairing-ui/intent.md) | done | v1.0.0, v1.1.0 |
| 06 | [release-and-versioning-pipeline](06-release-and-versioning-pipeline/intent.md) | done | v1.0.0–v1.4.0 |
| 07 | [watch-activity-states-night-pause](07-watch-activity-states-night-pause/intent.md) | done | v1.1.0, v1.1.1 |
| 08 | [xdrip-timestamps-no-data](08-xdrip-timestamps-no-data/intent.md) | done | v1.2.0 |
| 09 | [settings-ux-and-splash](09-settings-ux-and-splash/intent.md) | done | v1.3.0 |
| 10 | [debug-mode-backup-restore](10-debug-mode-backup-restore/intent.md) | done | v1.4.0 |
| 11 | [nightscout-provider](11-nightscout-provider/intent.md) | done | v1.5.0 |
| 12 | [disclaimer-and-docs](12-disclaimer-and-docs/intent.md) | done | v1.6.0 |
| 13 | [spec-driven-workflow](13-spec-driven-workflow/intent.md) | done | Unreleased |
| 14 | [unify-commit-skill](14-unify-commit-skill/intent.md) | in-progress | – |
| 15 | [forge-glycemia-watchface](15-forge-glycemia-watchface/intent.md) | in-progress | – |
