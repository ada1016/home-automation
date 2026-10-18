# Mac mini SSD migration checklist

**Purpose:** rebuild the Mac from a clean macOS installation after the SSD replacement, then restore only the data and settings that are needed. This is a checklist and application inventory, not a copy of any secret, licence key, or recovery phrase.

**Snapshot date:** 2026-10-18 (Asia/Taipei)

## Before opening the Mac

- [ ] Time Machine shows a successful *Latest backup* after the final manual backup.
- [ ] The Time Machine destination is the Synology `TimeMachines` SMB share.
- [ ] The portable SSD contains the verified `Mac-Documents-Downloads-2026-10-17` copy.
- [ ] The portable SSD contains the separately copied `Windows 11.pvm`, if this VM is being retained.
- [ ] The NAS holds the private Home Assistant configuration listed below.
- [ ] Keep the old SSD untouched until the new Mac is fully working and the data has been checked.

## Private data to restore manually

Keep these files in an access-controlled, encrypted NAS location. Do **not** commit them to Git or paste them into this document.

```text
home-automation-private/
├── esphome/secrets.yaml
└── appdaemon/secrets.yaml
```

Restore them only after cloning this repository:

```text
tab5/esphome/secrets.yaml
tab5/appdaemon/secrets.yaml
```

Also retain separately:

- [ ] 1Password Emergency Kit, Secret Key, account password, and any two-factor recovery method.
- [ ] Brave Sync code and exported bookmarks HTML.
- [ ] Brave Wallet recovery phrase and any separately imported wallet keys, if Brave Wallet is used. Never place these in ordinary cloud/NAS files.
- [ ] ExpressVPN sign-in / activation details, plus screenshots of any split-tunnel, protocol, or dedicated-IP settings.
- [ ] Synology credentials for the normal account and the restricted Time Machine account.

## Clean rebuild order

- [ ] Install the new SSD and install current macOS from scratch.
- [ ] Create a new local user account.
- [ ] Install macOS updates and enable FileVault.
- [ ] Sign in to 1Password first; restore only through its normal account recovery flow.
- [ ] Install Brave, join the existing Sync chain, then import the bookmark HTML as a fallback check.
- [ ] Install Synology Drive Client and sign in. Confirm all expected folders have synchronized.
- [ ] Install ExpressVPN and reapply any recorded custom settings.
- [ ] Restore selected Documents/Downloads from the portable SSD or NAS. Do not use Time Machine Migration Assistant to copy the whole prior system if the goal is a clean setup.
- [ ] Configure Time Machine again after the new Mac is stable. Use the NAS `TimeMachines` SMB destination and a fresh encrypted backup.
- [ ] Keep the old SSD and the old Time Machine backup until all items below have been verified.

## Home automation / Tab5 rebuild

- [ ] Clone the private `home-automation` repository.
- [ ] Restore the two secret files from the encrypted NAS location to the paths above.
- [ ] Install Python 3.12 and create the local environment:

  ```bash
  python3.12 -m venv .venv
  .venv/bin/pip install -r tools/build-requirements.lock
  ```

- [ ] Run the project’s offline tests before touching physical devices.
- [ ] Confirm the ESPHome dashboard/device connection, then deploy only when deliberately intended.
- [ ] Do not restore `.venv/`, `.esphome/`, `__pycache__/`, raw logs, or firmware binaries from an old system; they are generated and excluded from Git.

## Application snapshot

This is the `/Applications` inventory captured on 2026-10-18. It is a reminder of what was installed, **not** a recommendation to reinstall every item. Prefer fresh downloads from the vendor, App Store, or a trusted package manager.

| Application | Restore decision / note |
| --- | --- |
| 1Password 7 | Reinstall current 1Password; sign in using the Emergency Kit and Secret Key. |
| Android Studio | Reinstall only if Android development is still needed. |
| BaiduNetdisk_mac | Reinstall only if still used; sign in normally. |
| Bit Slicer | Optional. Download only from its trusted publisher. |
| Blackmagic Disk Speed Test | Optional App Store utility. |
| Brave Browser | Reinstall; restore with Brave Sync and bookmark export. |
| ChatGPT | Reinstall and sign in. |
| CheatEngine752 | Optional; install only from a trusted source if still needed. |
| Display Pilot 2 | Reinstall if the display requires it. |
| ESPHome Device Builder | Reinstall if still preferred; project source and secrets are restored separately. |
| ExpressVPN | Reinstall and reapply recorded settings. |
| IDA Professional 9.1 | Reinstall only from the legitimate licensed installer. |
| Logi Options / logioptionsplus | Reinstall the current Logitech software only if the peripherals need it. |
| Microsoft Edge | Optional; reinstall and sign in if used. |
| Mythic Survivors | Optional game. |
| Notion | Optional; reinstall and sign in if used. |
| OSCAR | Optional; reinstall from the trusted source if needed. |
| Parallels Desktop | Reinstall only a legitimate, current Parallels release. Test the separately copied `.pvm` before relying on it. |
| Parallels Desktop Activation Tool SIP ON | **Do not restore or reinstall.** |
| PictureView | Optional. |
| Pixea | Optional. |
| PlayCover | Optional; reinstall only if still needed. |
| PowerShell | Optional developer tool. |
| Sideloadly | Optional; reinstall only if still needed. |
| Synology Drive Client | Reinstall and sign in; allow the needed folders to sync. |
| Synology Image Assistant | Optional companion utility. |
| Telegram | Optional; reinstall and sign in. |
| TradingView | Reinstall and sign in. Pine scripts should be recovered from saved source/Git where applicable. |
| Unzip - RAR ZIP 7Z Unarchiver | Optional archive utility. |
| Visual Studio Code | Reinstall; re-add only required extensions and settings. |
| Xcode | Reinstall only if needed; do not copy old DerivedData or simulator caches. |
| iMovie | Optional App Store app. |
| iTerm | Optional terminal app. |

## Intentionally not restoring

These were removed or deliberately excluded from the clean system and should not be copied from an old backup:

- Docker Desktop and its containers/images.
- MuMu Android emulator and its data.
- BlueStacks data.
- Adobe Creative Cloud components.
- M5Burner.
- Any cracked, activation-bypass, or unknown installer/tool.

## Final verification before retiring the old SSD

- [ ] 1Password vaults, account access, and recovery materials work.
- [ ] Brave bookmarks, passwords, extensions, and (if applicable) Wallet recovery are confirmed.
- [ ] Synology Drive has completed synchronization.
- [ ] Home automation source is present; secrets are restored locally but remain untracked by Git.
- [ ] Tab5 builds/tests pass and devices are reachable.
- [ ] The Parallels VM opens in a legitimate Parallels installation, if retained.
- [ ] Documents, Downloads, Photos, and other irreplaceable files are checked from the NAS/portable copy.
- [ ] A new Time Machine backup has completed on the rebuilt Mac.
- [ ] Only after all checks pass: decide whether to erase, retain, or archive the old SSD.
