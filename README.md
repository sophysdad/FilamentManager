# Filament Manager

**Manage your OrcaSlicer (and fork) filament profiles locally — no cloud required.**

Android client + lightweight Windows companion service for browsing, editing, copying, and syncing filament profiles across multiple slicer installs (OrcaSlicer, Elegoo Slicer, Anycubic, etc.).

## Features (v2)

- View **user + system** filament profiles from enabled slicers
- Multi-select with bulk copy, sync, export, delete
- Copy/sync between slicers (flattens inheritance, handles printer assignments)
- Create / edit profiles directly in the app
- Export (single or batch ZIP)
- Automatic detection of common slicer folders on Windows
- Companion handles Anycubic numeric user folders + correct `.info` sidecars
- Full offline operation after initial sync

## Architecture (v2)

```
Android App (Kotlin + Compose)
        ↕ HTTP (local network, port 7878)
Windows Companion (Go)  →  reads/writes slicer folders directly
```

The companion is a small, auditable Go binary. The Android app never needs direct file access to the PC.

## Requirements

- Windows PC with OrcaSlicer (or fork) installed
- Android phone (API 26+)
- Same local network (Wi-Fi)

## Quick Start

### 1. Windows Companion

1. Download the latest `companion.exe` from Releases (or build from `companion-service/`).
2. Run it (double-click or from terminal).
3. It will auto-detect slicers and open a small status window / browser UI.
4. Note the IP address and port (default 7878).

### 2. Android App

1. Install the APK.
2. On first launch, enter the companion address (`192.168.x.x:7878`).
3. Profiles load. Use the system profiles toggle, multi-select (long-press), and the top action bar for bulk operations.

## Building from Source

### Android App

```bash
# Open in Android Studio or:
./gradlew assembleDebug
```

### Companion Service (Go)

```bash
cd companion-service
go build -o companion.exe .
```

See `companion-service/build.bat` for Windows convenience.

## Notes

- **v2** is the current Android + companion release (this repo).
- Companion service is intentionally minimal (standard library only).
- System profiles are read-only from the slicer's `system/` folder; user copies are written to the active user folder.
- Brand grouping and collapsed-by-default lists are the default experience.

## Future (v3)

A native Windows desktop application (single executable, **no companion service**) with **identical layout, functionality, and actions**.

- Direct access to local slicer folders (same auto-detection logic).
- Same multi-select, copy/sync, system profiles, brand grouping, etc.
- One .exe for Windows users.

See the `desktop/` directory (Compose for Desktop) — work in progress.

## Releases

- **v2.0** (this branch/tag): Current Android + Companion release.
- v3 (in progress): All-in-one native Windows desktop.

## License

MIT — see [LICENSE](LICENSE).

---

Built by Moosepond Designs.