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
- **Key Providers**: xDrip, Nightscout, Synthetic data

## Important Notes

### Timestamps & History
- All readings use millisecond timestamps
- `GlycemiaReading` has a `timestamp` field (defaults to current time if not set)
- `GlycemiaHistoryStore` maintains 24-hour rolling history
- Back-filled readings should be placed at their measurement time, not arrival time

### Broadcast Handlers
Three xDrip broadcast types are supported:
1. `"com.eveningoutpost.dexdrip.BROADCAST"` - xDrip native format
2. `"org.nightscout.android.broadcast"` - Nightscout format
3. `"com.eveningoutpost.dexdrip.ExternalStatusChange"` - Compatible JSON format

### Common Pitfalls to Avoid
- ❌ Don't use `System.currentTimeMillis()` for broadcast timestamps - extract from broadcast
- ❌ Don't treat all received readings as "current" - filter by timestamp
- ❌ Don't add comments that reference issues/PRs - those belong in commit messages
- ❌ Don't over-engineer solutions - keep it simple and focused

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
2. **Check timestamps** - they should be milliseconds (Long)
3. **Verify broadcasts** - use the logging in XdripReceiver to see actual fields
4. **Test filtering logic** - old readings shouldn't override current ones
5. **Update tests** - any new behavior needs test coverage

## VSCode & Gradle Configuration

### Java Version
- **System**: Homebrew OpenJDK 21 at `/opt/homebrew/opt/openjdk@21`
- **VSCode Java Extension**: Configured to use Homebrew Java (see `.vscode/settings.json`)
- **Gradle Tasks**: Auto-set JAVA_HOME before execution (see `.vscode/tasks.json`)

### Why This Matters
After VSCode restart, the Java extension may try to use its bundled JRE which lacks `jlink`. The configuration prevents this automatically — **no manual `./gradlew --stop` needed**.

See `docs/GRADLE_JAVA_FIX.md` for detailed explanation.

## Communication Style
- Be concise and direct
- Show what changed and why
- Use file_path:line_number format for code references
- Don't narrate internal deliberation
