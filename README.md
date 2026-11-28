# Inspector's Gadget

Android app for home inspectors:

1. **Pre-inspection agreement.** Prefilled from the company profile, sent to the client, and tracked as not sent, sent or signed. You can attach a signed copy, or the client can sign on the device.
2. **General home inspection.** Covers each system. You can take photos, mark them up in high-contrast colors, and dictate notes.
3. **4-Point inspection.** Reuses the General photos and answers.
4. **Wind mitigation.** Follows OIR-B1-1802 Rev. 01/12. Answers are suggested from the General findings.

5. **E-signature.** Sends the agreement to the client for remote signing through BoldSign or DocuSign. The signed PDF and status come back into the inspection automatically.
6. **State forms.** `StateForms` catalogs 47 official state and national forms (from the 2026-11-27 research), each tied to a state, an inspection type and the licenses that may issue it. The property state and the inspector's license state and licenses decide which forms apply and flag ones the inspector can't issue. Field mapping and form layouts are not built yet.
7. **Subscriptions.** One free trial inspection, then a monthly or annual subscription. Demo inspections can be explored but not exported.

There is a company profile (with an optional logo) and a roster of inspectors (license type and number, issuing body, expiry, saved signature). The selected inspector's details go on every report. Reports export as PDFs, and photos are compressed so the files fit in an email.

## Online services

Everything online goes through the relay in [`backend/`](backend/README.md), which holds the e-signature and Google Play credentials. With `Config.RELAY_URL` empty (the default) the app runs in demo mode: e-signature requests are simulated on the device (the client "signs" after 20 seconds) and purchases are simulated with no charge.

| Piece | App | Backend |
| --- | --- | --- |
| E-signature | `ESign` (provider-agnostic), `RelayClient` | `src/esign/boldsign.js`, `src/esign/docusign.js` |
| Trial and subscription | `Account`, `Billing`, `PaywallActivity` | `/v1/subscriptions/play`, Play notifications |
| Settings | `Config` | `wrangler.toml` and secrets |

## Build

The app is plain Java (Java 8 bytecode) and uses only Android framework APIs. It has no Gradle or androidx dependencies. Settings: `minSdk 26`, `targetSdk 34`, package `com.inspectorsgadget.app`.

`./build.sh` runs aapt → javac → dx → zipalign → apksigner. It expects these files:

- `sdk/android35.jar`: the compile classpath, i.e. an API 35 `android.jar`. Override it with `ANDROID_JAR=...`.
- `sdk/android23-res.jar`: an `android.jar` for aapt's framework resources. Override it with `RES_JAR=...`.
- `aapt`, `dalvik-exchange` (dx), `zipalign` and `apksigner` on `PATH`.

Signing uses `demo.keystore`, which is not in the repo. Set `KEYSTORE=...` to sign with your own key. If no key is found, `build.sh` generates a new demo key. An APK signed with a different key can't install over an existing install, so keep using one key.

## Publishing updates

Settings, Check for updates reads the latest GitHub release of `Config.UPDATE_REPO`. A release counts as newer when the versionCode in its APK name (`InspectorsGadget-{versionName}-{versionCode}.apk`) is higher than the installed one. The app downloads the APK and opens the system installer.

`publish-update.bat` (Windows) makes those releases. It needs Git, the GitHub CLI signed in (`gh auth login`), and `demo.keystore` in the repo folder (or `KEYSTORE` and `KEYSTORE_PASS`).

- `publish-update.bat`: asks for the new version name, bumps versionCode, builds and signs the APK with the Android SDK (Android Studio's build-tools, a `platforms/android-3x` folder and a JDK 17+), commits and pushes the bump, and creates release `v{versionName}` with the APK.
- `publish-update.bat path\to\app.apk`: releases an APK that is already built, using the version in `AndroidManifest.xml`.

Every update must be signed with the same key, or Android refuses to install it over the current app.

## Tests

`test/run.sh` runs Robolectric 4.18.1 tests at SDK 35 against `build/classes`. Two things are needed first:

- The jars listed in `test/pom.xml`, placed in `test/lib/` (this includes androidx.test stubs built from `test/stubs/`).
- Robolectric's `android-all-instrumented` jar for SDK 35, placed in `test/androidall/`.

The tests also save report page previews and screenshots to `test/out/`.
