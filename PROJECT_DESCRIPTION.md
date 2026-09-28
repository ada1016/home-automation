# Tab5 Home Assistant Dashboard — Project Handoff

## Purpose

This repository is the source for a battery-powered **M5Stack Tab5** wall/tablet dashboard used to control and monitor a Home Assistant installation. Its goal is a reliable, responsive, all-room touch UI that keeps interaction logic on the tablet while using Home Assistant as the source of truth for device state and service execution.

The UI is intentionally designed for four rooms plus a house-wide page:

- **Living Room (客廳)**: one AC and one light.
- **Parents (爹娘)**: one AC and one window light.
- **Darren (宸)**: one AC, head/foot/desk lights, and a room light group.
- **Amber (芹)**: one AC, door/foot/desk lights, and a room light group.
- **House (全部)**: all-AC controls, all-light controls, and explicit ALL OFF.

It also retains a gas-meter submission UI and a Xiaomi Home recovery action.

## Current source revision

The current source revision is **`TAB5_V4.2.2_RECOVERY_TOUCH`**.

It builds on the earlier all-room release and adds conservative idle-power improvements:

- ESPHome log output is limited to warnings/errors.
- Full state reconciliation in AppDaemon runs every 5 minutes instead of every 30 seconds.
- Real HA state changes are still forwarded immediately.
- The periodic UI feedback repaint is reduced from 250 ms to 1 second; commands and incoming state packets still request immediate feedback.
- The connection resync script is `single` rather than continually restarting its own delay.
- Persistent AC defaults (temperature, Auto/Quiet fan, and Cool/Dry/Fan Only mode) are applied when an AC is turned on from either a room or House page.
- The settings panel prioritizes AC defaults, gives Device Recovery a larger middle section, and uses compact theme choices at the bottom.

The checked-in `firmware/manifest.json` documents the earlier all-room binary, not a binary for this source revision. Firmware binaries and raw logs are intentionally not committed.

## System architecture

```text
Tab5 touchscreen + ESPHome/LVGL
  |  ESPHome API subscriptions (normalized state sensors)
  v
Home Assistant
  |  esphome.tab5_v4_command event
  v
AppDaemon: tab5_bridge_v4.py
  |  HA service calls + normalized state publishing
  +----------------------------------------------> Home Assistant entities
```

### Responsibilities

**ESPHome (`esphome/`)** owns the device-local experience:

- Display, touch, battery measurement, display timeout, and hardware power-off control.
- Pages, themes, room layout, group membership, widget rendering, and visual feedback.
- Local state cache, freshness handling, debouncing, pending command tracking, and collision prevention.
- Parsing normalized packets received through Home Assistant text sensors.

**AppDaemon (`appdaemon/tab5_bridge_v4.py`)** is deliberately thin:

- Maps logical names such as `ac_living` to site-specific HA entity IDs.
- Listens for `esphome.tab5_v4_command` events from the tablet.
- Calls generic HA climate/light/input services.
- Converts entity state into compact protocol-v3 packets and writes them to `sensor.tab5_v4_*` entities.
- Performs a 5-minute reconciliation snapshot, while state listeners publish changes immediately.

**Home Assistant** remains authoritative. The tablet never assumes an action succeeded just because it was tapped; it waits for fresh state confirmation.

## User-visible capabilities

- Per-room AC controls: target temperature, automatic mode, quiet mode, and explicit power on/off.
- Per-light power, brightness, and color-temperature controls.
- Room and whole-house group actions with per-device readiness and confirmation status.
- Explicit, domain-correct ALL OFF for both lights and climate devices.
- Temperature/humidity and AC humidity display where corresponding HA entities are configured.
- Four local visual themes.
- Battery voltage/current/percentage display; percentage is a simple voltage-derived estimate, not a calibrated fuel gauge.
- Display dims after 20 seconds, turns off and pauses LVGL after 60 seconds, and wakes on touch.
- Gas-meter PIN/submission flow and manual Xiaomi integration reload request.

## Reliability behavior that must be preserved

- Each device has at most one pending intent; overlapping individual/group actions cannot conflict.
- Group actions snapshot only currently ready members. Missing, unavailable, or stale members are skipped and are never replayed later.
- Group feedback distinguishes confirmed, skipped, pending, timed-out, and mixed states.
- AC changes use per-device debounce; navigation must not redirect a queued action to another room.
- Slider dragging locks overlapping controls and stale drag locks expire.
- Commands use explicit on/off values rather than toggles where correctness matters.
- On boot, `CONFIG_ESP_MAIN_TASK_STACK_SIZE: "8192"` is required to avoid the observed ESP32-P4 NVS startup assertion. Do not remove it without hardware validation.

## Repository map

| Path | Role |
| --- | --- |
| `esphome/tab5-v4-lab.yaml` | Main ESPHome entry point, hardware setup, LVGL root configuration, battery/display behavior. |
| `esphome/ui_state.h` | Local device/group state machine and protocol handling. |
| `esphome/ui_feedback.h` | LVGL feedback/rendering helpers. |
| `esphome/ui/` | Modular rooms, pages, widgets, actions, navigation, themes, and interaction scheduler. |
| `esphome/bridge/v4_subscriptions.yaml` | HA text-sensor subscriptions for normalized bridge packets. |
| `esphome/assets/`, `esphome/fonts/` | UI images and fonts required at compile time. |
| `appdaemon/tab5_bridge_v4.py` | HA entity adapter and command/state transport. |
| `appdaemon/apps.yaml` | Template AppDaemon entry with this installation's entity mappings; merge, do not replace an existing apps file. |
| `tests/` | Offline bridge and C++ state/group behavior tests. |
| `docs/` | Installation, behavior, and historical validation notes. |
| `firmware/manifest.json` | Metadata for the prior all-room binary only. |

## Deployment model and local configuration

This repository stores source only. It must be deployed in two independent parts:

1. **ESPHome firmware:** copy the complete `esphome/` source tree to the ESPHome configuration directory, preserving that installation's `secrets.yaml`; then build/upload `tab5-v4-lab.yaml`.
2. **AppDaemon bridge:** copy `appdaemon/tab5_bridge_v4.py` into the AppDaemon apps directory and merge the `tab5_v4_bridge` entry from `appdaemon/apps.yaml`. Reload/restart AppDaemon for Python changes to take effect.

The complete ESPHome tree matters: `ui_state.h`, `ui_feedback.h`, `ui/`, `bridge/`, `assets/`, and `fonts/` are compile-time dependencies. The YAML filename is arbitrary; the `esphome.name` value controls the ESPHome/Home Assistant identity. Retaining `tab5-v4-lab` preserves the current V4 identity.

Never commit or overwrite local `esphome/secrets.yaml`, generated firmware binaries, build caches, or raw logs. They are ignored by Git.

The operator, not an AI agent, performs firmware upload, device restart, shutdown, and hardware power actions. Code changes should always be given a visible source revision/build marker and validated before handoff.

## Build and test

From this directory:

```bash
python3.12 -m venv .venv
.venv/bin/pip install -r tools/build-requirements.lock
.venv/bin/python -m unittest discover -s tests -p 'test_*.py'
c++ -std=c++17 tests/test_state.cpp -o /tmp/tab5-test-state && /tmp/tab5-test-state
c++ -std=c++17 tests/test_groups.cpp -o /tmp/tab5-test-groups && /tmp/tab5-test-groups
.venv/bin/esphome config esphome/tab5-v4-lab.yaml
```

Use the local ESPHome dashboard or the documented commands in `README.md` for an operator-initiated firmware build/upload. A successful configuration check does not replace real-device validation.

## Current validation status

- Earlier all-room firmware was built, OTA-uploaded, boot-validated, and confirmed to receive HA state.
- Offline Python bridge tests and ESPHome configuration validation pass for the idle-power source change.
- Full physical acceptance of every room, group edge case, gesture, and gas flow remains manual.
- The hardware has reported battery voltage/current through INA226 (for example, approximately `8.12 V` and `0.284 A` during an observed session). Current is the preferred metric for idle-drain comparison; voltage-derived percentage is load-sensitive.

## Known limitations and next development priorities

1. **Battery life:** display-off does not deep-sleep the ESP32-P4, hosted Wi-Fi, or HA API connection. Measure current after the screen has been off for at least 90 seconds, then compare overnight averages before pursuing more aggressive power modes. Deep sleep or Wi-Fi power-off would trade away immediate touch/HA responsiveness and needs a deliberate UX design.
2. **Battery percentage:** it currently maps voltage linearly from 6.0–8.4 V, so it is approximate and can appear to drop/recover under load. Calibrated discharge data would improve it.
3. **Version evidence:** logger level `WARN` intentionally suppresses the INFO boot marker. If runtime proof of a build is required, introduce a warning-level version marker or a dedicated version sensor in a future revision.
4. **Documentation drift:** some historical documents and the firmware manifest still identify `TAB5_V4_ALL_ROOMS`; use this file for current-source intent and treat those as prior-release records.
5. **Entity mapping portability:** `appdaemon/apps.yaml` contains this home's concrete entity IDs. A new installation must replace them with its own HA entity IDs while retaining the logical keys expected by the UI.

## First steps for a new AI contributor

1. Read this file, then `README.md`, `docs/ALL_ROOMS.md`, and `docs/VALIDATION.md`.
2. Treat `esphome/tab5-v4-lab.yaml` plus all of `esphome/ui/`, `ui_state.h`, and `ui_feedback.h` as one integrated firmware unit.
3. Preserve protocol version `3` and logical device keys unless both ESPHome subscriptions and the AppDaemon bridge are updated together.
4. Run offline tests and `esphome config` after changes.
5. Do not deploy or operate the physical Tab5. Present the source revision, changed files, validation results, and operator deployment steps for human approval.
