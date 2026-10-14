# TCL Remote Mapper

Personal Android TV app for a TCL 65C935 that remaps remote buttons using an Accessibility service. No root or firmware modification is required.

## Current verified mappings

| Remote button | Destination | Status |
| --- | --- | --- |
| Amazon Prime Video | HDMI 1 | Verified on the target TV |
| Media | Configurable TV launcher app; Spotify selected by default | Verified with Spotify on the target TV |

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

- Prime observed by the Accessibility service as vendor keycode `4099`, scan code `752`.
- Media observed as vendor keycode `4057` (`TCL_USB_MEDIA`) during IR capture.
- The user verified both Prime → HDMI 1 and Media → Spotify in both IR and Bluetooth remote operation.

Input IDs and vendor keycodes are device/firmware-specific. Do not copy them to another TV without re-running diagnostics.

## Install

The ready-to-install debug APK is kept in:

```text
releases/TCL-Remote-Mapper-v0.3.1-debug.apk
```

Connect ADB and install:

```bash
adb connect TV_LAN_IP:5555
adb install -r releases/TCL-Remote-Mapper-v0.3.0-debug.apk
adb shell monkey -p io.github.tclremotemapper 1
```

On the TV, open **TCL Remote Mapper**, select **Enable diagnostics**, and enable the Accessibility service. Return to the app and confirm its status is `ENABLED`.

## Configure mappings

- **Prime mapping:** enable it, then choose **Select input** and select HDMI 1.
- **Media mapping:** enable it, then choose **Select app** and select Spotify (or another installed TV launcher app).
- **Live remote / keyboard console:** leave the app on screen and press a button on any remote. The console records every key Android actually delivers to the service: action, Android keycode, scan code, repeat count, device ID, source, and flags. System-reserved keys withheld by Android cannot be observed by an ordinary APK.

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
