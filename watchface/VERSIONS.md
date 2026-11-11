# Watchface versions

| `FACE_VERSION` | Forge Apps version | Minimum app version | Changes |
|---|---|---|---|
| 3 | 0.3 | 2.0.0 | L button lights the watch for 3 s |
| 2 | 0.2 | 2.0.0 | Handshake: answers `Q` with `H1,gly,2` |
| 1 | 0.1 | 1.7.0 | First face: tag, reading age, status flash |

- `FACE_VERSION` is the constant in `glycemia.js`, sent in the handshake hello `H1,gly,<FACE_VERSION>`.
- Forge Apps version is the major.minor typed when uploading in the Ollee Watch app (Settings > Forge Apps). It stays `0.<FACE_VERSION>` until the first official face, 1.0.
- Minimum app version is the oldest Ollee Glycemia release that feeds every feature of that face.
- Each GitHub release carries the `glycemia.js` of its tag.

Every change to `glycemia.js` adds a row here and bumps `FACE_VERSION`.
