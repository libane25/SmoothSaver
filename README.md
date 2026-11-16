# SmoothSaver 1.3.2

LSPosed module for the Pixel 10a (`stallion`) system framework.

The framework check used the phone's own `services.jar` from `google/stallion/stallion:17/CP3A.261005.005/16271449`. Android 17, SDK 37, Tensor G4.

## Why Settings search crashed

1.1.0 runs in `system_server` and does three unsafe things:

- It replaces every `onChange`, `observe`, `register`, and `startObserving` on a guessed observer. The real class is `DisplayModeDirector$SettingsObserver`, whose `onChange(boolean, Uri, int)` also updates peak refresh rate, minimum refresh rate, and content frame rate.
- It hooks every `DisplayModeDirector` method whose name contains `vote` and `update` or `set`, then returns null when the stack contains `BatterySaver`, `LowPower`, or `PowerSave`. A null return from a primitive method crashes the caller. Settings search reaches those paths.
- Otherwise it hooks `Settings.Global.getInt` and forces `low_power` to 0 whenever `DisplayManagerService` is on the stack.

## What the stallion framework does

`SettingsObserver.updateLowPowerModeSettingLocked()` reads `Settings.Global` `low_power`. When Battery Saver is on, it calls:

`VotesStorage.updateGlobalVote(20, Vote.forRenderFrameRates(0f, 60f))`

`Vote.priorityToString` maps 20 to `PRIORITY_LOW_POWER_MODE_RENDER_RATE` and 19 to `PRIORITY_LOW_POWER_MODE_MODES`. The same method then calls `BrightnessObserver.onLowPowerModeEnabledLocked` and `updateLowPowerModeAllowedModesLocked`. The modes path uses `updateVote` at priority 19.

## What 1.3.0 hooks

One method: `VotesStorage.updateGlobalVote(int, Vote)`, which returns void.

The hook sets the vote argument to null only when the priority is 20 and the caller is `SettingsObserver.updateLowPowerModeSettingLocked`. The original method still runs. Priority 19, the brightness callback, thermal votes, and `Settings.Global.getInt` are not hooked. The module does not write the Battery Saver setting and does not force 120 Hz or 90 Hz.

Before the hook is installed, the running framework must report `Vote.priorityToString(20)` as `PRIORITY_LOW_POWER_MODE_RENDER_RATE`, the method signature must match, and the device codename must be `stallion`. If any check fails, the hook is not installed.

Startup logs use the tag `SmoothSaver`.

## Install

1. Install `SmoothSaver-1.3.2.apk`.
2. Enable SmoothSaver in Vector.
3. Scope: System Framework only. On this Vector build that scope is stored as `system`. A scope of `android` does not inject the module into `system_server`. Vector still reports the process package name as `android` once it is loaded. Remove Settings if it is listed.
4. Reboot.

On the connected phone the working command was:

```bash
/data/adb/lspd/cli scope set com.smoothsaver.xposed system/0
```

`scope set` replaces the whole scope. Use `scope add` to append.

## Checked on the phone

After the scope was `system` and the phone rebooted, `system_server` logged `Installed hook: VotesStorage.updateGlobalVote(int, Vote)`. With Battery Saver on, `low_power` stayed `1`, `mLowPowerModeEnabled` was true, and the priority 20 render vote was absent. The user peak vote stayed 0–120 Hz. With Battery Saver off, `low_power` was `0` and the same 0–120 Hz peak vote remained. No `PRIORITY_LOW_POWER_MODE_RENDER_RATE` vote was present in either state. `system_server` did not restart. Settings and the search process stayed running. The lock screen was up, so the search field was not typed into.

## Recovery

1. Disable SmoothSaver in Vector and reboot.
2. If the phone will not boot normally, boot Android safe mode by holding Volume Down during the boot animation. Safe mode does not load Vector modules. Disable SmoothSaver, then reboot.
3. From a root shell: `pm uninstall com.smoothsaver.xposed`, then reboot.

## Build

```bash
cd xposed
./gradlew assembleRelease testDebugUnitTest
```

The APK is `xposed/app/build/outputs/apk/release/app-release.apk`.
