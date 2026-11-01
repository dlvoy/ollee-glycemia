# 13: Spec-driven workflow

## Problem
Intent and design decisions lived only in commit messages and the CHANGELOG. There was no place to agree on a change before coding or to track multi-step work, and agents working on the repo had no structured context.

## Goal
Adopt spec-driven development: every non-trivial change gets an intent, a plan and a progress log under `docs/specs/`. The existing history gets documented retroactively.

## Scope
- `docs/specs/README.md` with the workflow, templates and index
- Retroactive specs 00–12 from git history, CHANGELOG and code
- CLAUDE.md section describing the workflow
- CHANGELOG audit against git and tags
- The first forward-looking spec (14)

## Out of scope
- Changing release tooling (spec 14)

## Acceptance criteria
- [x] Each spec folder has `intent.md`, `plan.md`, `progress.md`
- [x] The README index lists every folder
- [x] CLAUDE.md tells agents to follow the workflow
- [x] Every tag v1.0.0–v1.6.0 has an accurate CHANGELOG section and link
