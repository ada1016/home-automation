# TCL Remote Mapper

Personal Android TV app for a TCL 65C935 that remaps remote buttons using an Accessibility service. No root or firmware modification is required.

## Current verified mappings and built-in button identities

| Remote button | Destination | Status |
| --- | --- | --- |
| Amazon Prime Video | HDMI 1 | Verified on the target TV |
| Media | Configurable TV launcher app; Spotify selected by default | Verified with Spotify on the target TV |
| TCL Channel | KKYS TV (`com.kkys.tv1`) | Verified key; configured destination |
| Netflix | Choose any installed app or TV input | Verified key; destination not yet selected |
| Guard | Choose any installed app or TV input | Verified key; destination not yet selected |
| YouTube | Choose any installed app or TV input | Verified key; destination not yet selected |

The app consumes the matching down/up event pair. It triggers only on the first key-down event, so key-up, repeats, and long presses do not cause duplicate launches.

## Target-TV results

- TV: TCL 65C935, Google TV
- User-supplied firmware: `V8-T615T03-LF1V613`
- ADB-reported platform: Android 11, API 30, build `AR2101`
- Remote observed during testing: TCL RC902V_M1
- Verified HDMI 1 ID on this TV:

  ```text
  com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744128
  ```

- All six labelled remote keys were observed through the Accessibility service on the target TV:

  | Button | Android keycode | Scan code |
  | --- | ---: | ---: |
  | Netflix | `4062` | `242` |
  | Prime Video | `4099` | `752` |
  | Guard | `4140` | `748` |
  | Media | `4057` | `236` |
  | YouTube | `4065` | `245` |
  | TCL Channel | `4020` | `227` |

- Prime → HDMI 1 and Media → Spotify were verified in both IR and Bluetooth remote operation.
- TCL Channel’s configured KKYS TV destination has a valid Android TV launcher activity. The v0.4.4+ app includes its verified key identity.

These are non-standard vendor keycodes (all are above Android’s public keycode range), and scan codes are raw device-specific values. Android’s API explicitly says scan codes are unreliable and can vary by device. Do not copy these values to another TCL TV without re-running diagnostics. See [Android KeyEvent documentation](https://developer.android.com/reference/android/view/KeyEvent).

## Install

The current ready-to-install debug APK is:

```text
releases/TCL-Remote-Mapper-v0.4.11.apk
```

Connect ADB and install:

```bash
adb connect TV_LAN_IP:5555
adb install -r releases/TCL-Remote-Mapper-v0.4.11.apk
adb shell monkey -p io.github.tclremotemapper 1
```

On the TV, open **TCL Remote Mapper**, enable the Accessibility service, and confirm its status is `ENABLED`. The service’s **Settings** entry in TV Settings → Accessibility opens the app directly.

## Configure mappings

- Every labelled button has a built-in identity on this exact remote. Select **Configure**, choose an installed app or a TV input, and the app enables that mapping automatically.
- **Prime mapping:** select HDMI 1.
- **Media mapping:** select Spotify (or another installed TV launcher app).
- **TCL Channel mapping:** KKYS TV is already selected.
- **Netflix, Guard, and YouTube:** choose their desired destinations. They remain disabled until a destination is selected, so an unset slot never blocks its original OEM button behavior.
- **Learn this physical button** remains available to adapt a slot if a future firmware/remote sends a different key.
- **Log every key (diagnostic mode):** off by default. With it off, the mapper does not persist or broadcast every remote/D-pad event, reducing background work. Mapping launches, failures, and configuration changes continue to be recorded. Turn it on only while diagnosing a button; the console then records every key Android delivers to the service: action, Android keycode, scan code, repeat count, device ID, source, and flags. System-reserved keys withheld by Android cannot be observed by an ordinary APK.

## Android TV Home tile

v0.4.6 provides a dedicated 320×180 Android TV banner with a high-contrast remote-control graphic. Google TV uses this banner for the rectangular app tile shown on the Home/Your apps screen; the previous banner was a blank background.

## Remote-style configuration screen

v0.4.8 removes the in-app Accessibility shortcut and presents the six configuration cards in the physical remote order: Netflix | Prime Video, Guard | Media, YouTube | TCL Channel. Each card shows a source-button icon, an arrow, and the selected destination's installed-app icon or HDMI icon. The optional engineering controls (Refresh, Clear log, and Diagnose mode) sit below the grid, above the event console.

## Save and load configuration

The Engineering tools section includes **Save config** and **Load config**.

- **Save config** writes one file directly to `Download/TCL_Remapp/tcl-remote-mapper-config.json`; it never opens an app or folder picker.
- Later saves replace that one file. **Load config** imports and validates that same file before replacing the six mappings.
- The feature uses Android's scoped Downloads storage and does not require broad storage permission.

## ADB diagnostics

Enable Developer options and network/USB debugging in the TV settings. For a network-connected TV:

```bash
adb connect TV_LAN_IP:5555
adb devices -l
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getevent -lt
```

Use the included baseline script if needed:

```bash
chmod +x scripts/collect-tv-info.sh
./scripts/collect-tv-info.sh TV_LAN_IP:5555
```

`getevent` exposes low-level Linux input, while the on-screen console shows only what an ordinary APK can receive. They are intentionally not treated as equivalent.

## Startup reliability

On this TCL firmware, the service needed the following per-app AppOps allowance after installation:

```bash
adb shell cmd appops set io.github.tclremotemapper APP_AUTO_START allow
```

The user must still enable the Accessibility service in TV settings. After a firmware update or if mapping stops after restart, open the app and confirm the service status; toggle the service off/on if necessary.

## Build from source

Requirements: Android SDK Platform 35, Build-Tools, and JDK 17 or newer.

```bash
./gradlew :app:assembleDebug
```

The build output is `app/build/outputs/apk/debug/app-debug.apk`. Copy a tested build into `releases/` before committing a release.

## Rollback

1. In the app, disable either mapping or both.
2. In TV Accessibility settings, disable **TCL Remote Mapper**.
3. Reverse the only ADB setting applied for startup reliability:

   ```bash
   adb shell cmd appops set io.github.tclremotemapper APP_AUTO_START ignore
   ```

4. Optionally uninstall the app:

   ```bash
   adb uninstall io.github.tclremotemapper
   ```

The app does not root the TV, flash firmware, alter key-layout files, or disable system/streaming packages. Prime Video was removed separately by the user before this app was completed; this project does not manage its installation or restoration.
