# TV

A personal Android TV app for IPTV: live channels, movies and series from an M3U playlist, with the TV guide
(XMLTV) and a profile for each person who uses the TV. A playlist is added to a profile by its address, or by an
access code from the author's own playlist server.

## Layout

| Module | What it is |
|---|---|
| `:core` | Plain Kotlin, no Android: playlist and guide parsing, the library of channels, movies and shows, profiles, resume points, versions. Tested on the JVM. |
| `:app` | The Android TV app: Compose for TV screens, the player (Media3), navigation (Navigation 3), storage (DataStore) and the in-app update. |

`scripts/deploy-tv.sh` installs a build on the TV. `THIRD_PARTY_LICENSES.md` lists the bundled fonts.

## Set up

- Android Studio (its bundled JDK and the Android SDK). It writes `sdk.dir` into `local.properties`.
- `local.properties` is not committed. Add the address of the playlist server, which an access code is a part of,
  so that typing a code works:

  ```
  backend.base=http://192.168.1.10:5002
  ```

  Without it a typed code is refused and a full playlist address still works. A release build stops without it.

## Build, test, check

```
./gradlew test                 # all tests (JVM, no device)
./gradlew lintDebug            # Android lint
./gradlew :app:assembleRelease # app/build/outputs/apk/release/app-release.apk
```

## Put it on the TV

Over the network with adb (the TV address is in the script; `TV_ADDR=192.168.1.50:5555` overrides it):

```
scripts/deploy-tv.sh           # release build
scripts/deploy-tv.sh debug     # debug build, prints player errors in logcat (about 5x slower on the TV)
```

## Release

The version is set in one place, `appVersion` in `app/build.gradle.kts`. The build number is derived from it
(`major * 10000 + minor * 100 + patch`, so 0.2.1 is 201) and always rises with it.

1. Raise `appVersion` and merge to `main`.
2. The GitHub workflow runs the tests and lint. If the version has no release yet, it builds the release APK
   and publishes it as release `v<version>` with the file `app-release.apk`.
3. Set once in the repository settings (Settings > Secrets and variables > Actions):
   - variable `BACKEND_BASE`: the public address of the playlist server (a release build stops without it);
   - secret `DEBUG_KEYSTORE_BASE64`: the signing key, from `base64 -i ~/.android/debug.keystore`.

The release is signed with that key, the same one as local builds. An update installs over the app only if it
is signed with the same key, so keep a backup of it; a lost key means uninstalling the app first (which erases
the profiles).

## Install and update

- First time: open `https://github.com/calias2000/TV/releases/latest/download/app-release.apk` on the TV
  (for example with the Downloader app). The TV asks once to allow installs from unknown sources.
- After that the app finds new releases by itself when it starts: a button appears on Home, and Settings has the
  version and a "Check for updates" button. Nothing is installed without pressing it.
