# Claude Code Project Configuration

## Commit Policy
**Do NOT auto-commit changes.** Always ask the user before creating any commits. The user will decide when to commit and what to include.

## Spec-Driven Development
Every non-trivial change starts as a spec in `docs/specs/` (see `docs/specs/README.md`):
1. Create `docs/specs/NN-kebab-name/` (next free two-digit number, never reused)
2. Write `intent.md` (what and why), then `plan.md` (how), and get them agreed before coding
3. Update `progress.md` during the work (checklist, dated log, commits, release)
4. Keep the index table in `docs/specs/README.md` current

## Project Context
- **Type**: Android app (Kotlin)
- **Target**: Glucose monitoring for smartwatches
- **Data Source**: xDrip (real CGM data)
- **Key Providers**: xDrip, Nightscout - TODO in future, Synthetic data

## Important Notes

### File Organization
```
app/src/main/java/pl/cukrzycowy/ollee/glycemia/
├── BleService.kt                    # Service managing BLE + readings
├── XdripProvider.kt                 # xDrip broadcast handler
├── GlycemiaReading.kt               # Data model
├── GlycemiaHistoryStore.kt          # 24h rolling history
├── GlycemiaHistoryEntry.kt          # History entry model
└── ui/screens/MainScreen.kt         # Main UI
```

### Testing
- Unit tests use Robolectric
- No integration tests needed unless explicitly requested
- Test file location: `app/src/test/java/com/arthur/bgollee/`

## Before Making Changes

1. **Read existing code** to understand current implementation

## VSCode & Gradle Configuration

### Java Version
- **System**: Homebrew OpenJDK 21 at `/opt/homebrew/opt/openjdk@21`
- **VSCode Java Extension**: Configured to use Homebrew Java (see `.vscode/settings.json`)
- **Gradle Tasks**: Auto-set JAVA_HOME before execution (see `.vscode/tasks.json`)

## Communication Style
- Be concise and direct
- Show what changed and why - very shortly
- Use file_path:line_number format for code references
- Don't narrate internal deliberation

DO NOT AUTO-EXTEND THIS FILE UNLESS ASKED - if you feel the need - suggest with SHORT summary what you want to put it at end