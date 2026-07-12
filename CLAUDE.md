# Claude Code Project Configuration

## Commit Policy
**Do NOT auto-commit changes.** Always ask the user before creating any commits. The user will decide when to commit and what to include.

## Build & Test Policy
**Do NOT run tests or builds automatically.** Instead:
1. Tell the user what tests/builds are recommended
2. Provide the exact commands to run
3. Only execute if the user explicitly asks
4. Wait for user feedback before proceeding based on test results

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