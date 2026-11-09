# Store data declarations

## App Store Connect

**App Privacy**
- Data collection: **"No, we do not collect data from this app."** The label then reads **Data Not Collected**.

**Export compliance**
- `ITSAppUsesNonExemptEncryption = NO` is set in `iosApp/Info.plist`, so App Store Connect won't ask about encryption for each build.
- The app uses only exempt encryption: a SHA-256 checksum of the sync payload, from the operating system.
- If App Store Connect asks anyway, answer that the app uses encryption, but only exempt encryption (OS-provided).

**Permissions shown to users**
- Camera: "WristOTP uses the camera to scan authenticator QR codes."
- Bluetooth.
- Photo library access isn't requested, because the system picker doesn't need it.

## Google Play Console

**Data safety**
- Does your app collect or share any of the required user data types? **No.**
  - This is true since QR scanning moved from ML Kit to ZXing: the release build has no `INTERNET` permission and no Google data-transport service. Recheck the merged manifest if dependencies change.
- Is all user data encrypted in transit? Answer **Yes**: transfer happens over the Connect IQ link to the user's own watch.
- Do you provide a way for users to request that their data is deleted? Nothing is stored or collected: accounts live only in memory while the app is open. Play may not ask this.

Review note: the account secrets leave the phone only towards the user's own watch, relayed by the Garmin Connect app. Google's definition counts data as "collected" only when it's sent off the device to the developer or a third party. A user-initiated transfer to the user's own device is generally not collected. Re-read the current Play policy wording when you fill this in.

**Permissions**
- `CAMERA`: scanning QR codes. Asked in context, with a retry button.
- `ACCESS_NETWORK_STATE`: added by a CameraX dependency (media3-common). It reads connectivity state only; the app has no `INTERNET` permission.
- No photo or storage permission: the image picker is the system photo picker.

**Content rating**
- The questionnaire answers are all "No", which gives Everyone / PEGI 3.

**Target audience**
- 18+. Not designed for children.

## Connect IQ Store

**Permissions:** `Communications`, for receiving accounts from the phone app.

**Privacy policy:** https://lucanicoletti.github.io/wristotp/privacy

## Privacy manifest (iOS)

`iosApp/iosApp/PrivacyInfo.xcprivacy` declares no tracking, no collected data, and these required-reason APIs:
- UserDefaults `CA92.1`: the app's own settings and chosen watch.
- File timestamp `C617.1`, system boot time `35F9.1`, disk space `E174.1`: used by the Kotlin/Compose runtime.

Before submitting, check the Release build with JetBrains' `required_reason_finder.py` and add anything it reports.
