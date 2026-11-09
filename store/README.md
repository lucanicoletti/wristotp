# Release checklist

Version 1.0: watch apps `1.0.0`; phone app `1.0` (build `1`).

Files in this folder:
- `listing.md`
- `data-declarations.md`
- `graphics/`: store icons and the Play feature graphic
- `review/`: notes for reviewers and `sample-export-qr.png` (four fake accounts)
- `screenshots/`: phone and watch screenshots

The privacy policy and support pages live in `../site/` (GitHub Pages).

## 0. Before any store

- [x] Publish `../site/` with GitHub Pages: repo `lucanicoletti/wristotp-site`, served at https://lucanicoletti.github.io/wristotp-site/. The phone app links to `/privacy` and `/support` from its footer. When `site/` changes, copy the files into that repo and push.
- [x] Take store screenshots (`screenshots/`, demo data). Watch screenshots for Connect IQ are still to do:
  - Watch: Connect IQ simulator, File → Save Screenshot.
  - Phone: iPhone 6.9" and 6.5", Android phone.

## 1. Connect IQ Store

- [ ] Merchant account for the paid edition: Connect IQ developer dashboard → merchant onboarding (USD 100/year, ID and tax documents, several days to verify). Set the price in the first submission; adding a price to an approved app sends it back to review.
- [ ] Give the reviewer a TestFlight / Play test link for the phone app, or publish the phone apps first (see `review/review-notes.md`).

- [ ] Upload `watch/bin/store/WristOTP.iq` as **WristOTP**.
  - Set it as a paid app; this needs merchant setup in the Connect IQ developer dashboard.
  - Set the price.
- [ ] Upload `watch/bin/store/WristOTPFree.iq` as **WristOTP Free**, free.
- [ ] For both apps:
  - Use the descriptions from `listing.md`.
  - Store icon: `graphics/connectiq-icon-500.png`.
  - Add screenshots.
- [ ] Each app id is fixed by its manifest, and the phone app looks for both ids. Don't regenerate them.

Rebuild the packages with:
```
monkeyc -e -r -f monkey.jungle -o bin/store/WristOTP.iq -y <developer_key> -l 2
monkeyc -e -r -f free.jungle  -o bin/store/WristOTPFree.iq -y <developer_key> -l 2
```
Use the same `developer_key` for every future update. A different key can't update the published apps.

## 2. App Store

- [ ] In App Store Connect, create the app:
  - Bundle id `com.lucanicoletti.wristotp`. It's already registered by Xcode's automatic signing.
  - Name **WristOTP**, free.
- [ ] Archive the app:
  - In Xcode: scheme `iosApp` → Any iOS Device → Product → Archive.
  - Then Distribute → App Store Connect.
- [ ] Fill in the App Privacy answers from `data-declarations.md`.
- [ ] Declare EU trader status (Digital Services Act) in App Store Connect, or the app is removed from EU storefronts.
- [ ] Support URL and copyright from `listing.md`. iPhone only: no iPad screenshots needed.
- [ ] Add the privacy policy URL, the listing text and the screenshots.
- [ ] **Review notes:** `review/review-notes.md`, with `sample-export-qr.png` and a sync video.

## 3. Google Play

- [ ] Create an upload keystore and add release signing. This isn't wired yet: it needs your keystore.
- [ ] Build the bundle: `./gradlew :composeApp:bundleRelease`.
- [ ] Create the app, package `com.lucanicoletti.wristotp`, free.
  - Fill in Data safety, content rating and target audience from `data-declarations.md`.
  - Feature graphic: `graphics/play-feature-graphic-1024x500.png`.
  - High-res icon: `graphics/icon-512.png`.
- [ ] Personal developer accounts created after November 2023 must run a closed test (12 testers, 14 days) before production access.
- [ ] App content → App access: paste the notes from `review/review-notes.md`.
- [ ] Bump `versionCode` in `composeApp/build.gradle.kts` for every upload.
- [ ] Untested so far: Android hasn't been run on a real device yet. Run a sync on an Android phone before submitting.
