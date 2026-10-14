# Home Automation

Personal home-automation projects collected in one repository.

## Folders

- **`TCL935/`** — Android TV app for a TCL 65C935. It remaps the Prime Video button to HDMI 1, maps the Media button to a selected TV app (Spotify by default), and includes a live remote-key diagnostic console. See [`TCL935/README.md`](TCL935/README.md).
- **`tab5/`** — ESPHome, AppDaemon, and firmware project for the Tab5 home-control touchscreen and its room/device integrations. See [`tab5/README.md`](tab5/README.md).

## Git workflow

From this repository root:

```bash
make git-status
make git-push
```

`make git-push` stages all repository changes, creates a timestamped commit, and pushes `main` to GitHub.
