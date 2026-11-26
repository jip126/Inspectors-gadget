# Inspector's Gadget

Android app for home inspectors:

1. **Pre-inspection agreement.** Prefilled from the company profile, sent to the client, and tracked as not sent, sent or signed. You can attach a signed copy, or the client can sign on the device.
2. **General home inspection.** Covers each system. You can take photos, mark them up in high-contrast colors, and dictate notes.
3. **4-Point inspection.** Reuses the General photos and answers.
4. **Wind mitigation.** Follows OIR-B1-1802 Rev. 01/12. Answers are suggested from the General findings.

There is a company profile (with an optional logo) and a roster of inspectors (license type and number, issuing body, expiry, saved signature). The selected inspector's details go on every report. Reports export as PDFs, and photos are compressed so the files fit in an email.

## Build

The app is plain Java (Java 8 bytecode) and uses only Android framework APIs. It has no Gradle or androidx dependencies. Settings: `minSdk 26`, `targetSdk 34`, package `com.inspectorsgadget.app`.

`./build.sh` runs aapt → javac → dx → zipalign → apksigner. It expects these files:

- `sdk/android35.jar`: the compile classpath, i.e. an API 35 `android.jar`. Override it with `ANDROID_JAR=...`.
- `sdk/android23-res.jar`: an `android.jar` for aapt's framework resources. Override it with `RES_JAR=...`.
- `aapt`, `dalvik-exchange` (dx), `zipalign` and `apksigner` on `PATH`.

Signing uses `demo.keystore`, which is not in the repo. Set `KEYSTORE=...` to sign with your own key. If no key is found, `build.sh` generates a new demo key. An APK signed with a different key can't install over an existing install, so keep using one key.

## Tests

`test/run.sh` runs Robolectric 4.18.1 tests at SDK 35 against `build/classes`. Two things are needed first:

- The jars listed in `test/pom.xml`, placed in `test/lib/` (this includes androidx.test stubs built from `test/stubs/`).
- Robolectric's `android-all-instrumented` jar for SDK 35, placed in `test/androidall/`.

The tests also save report page previews and screenshots to `test/out/`.
