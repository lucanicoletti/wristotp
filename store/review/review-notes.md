# Review notes

Paste the matching section into each store's notes for the reviewer. Attach `sample-export-qr.png` and `wristotp-flow.mp4`, a 35-second recording of a full sync (Android emulator and the Connect IQ simulator, Venu 4).

To record a new video, use a **debug** Android build or iOS: Android release builds block screenshots and screen recording (FLAG_SECURE).

## App Store / Google Play

WristOTP is a companion app for the WristOTP watch app on Garmin Connect IQ watches. It has no login, no servers, no in-app purchases and collects no data.

Testing without a watch:
1. On the main screen, tap Import image and pick the attached image `sample-export-qr.png`. It's a Google Authenticator export containing four fake accounts.
2. The main screen lists the four accounts that were decoded on the phone. Drag the handles or tap A–Z to change the order they're sent in.
3. The Send button needs a connected watch; without one it shows the watch status instead.

Sending to a watch needs a Connect IQ watch paired in the Garmin Connect app, with WristOTP or WristOTP Free installed from the Connect IQ Store. The attached video shows a full sync: import the sample QR → Send → choose a PIN on the watch → codes on the watch.

## Connect IQ Store

WristOTP needs its free phone app to receive accounts. Until the phone apps are public, use these test builds: {TESTFLIGHT_LINK} / {PLAY_TEST_LINK}.

The attached video shows a sync from the phone, choosing a PIN on the watch, and the codes on the watch. `sample-export-qr.png` contains four fake accounts that you can import into the phone app and send.
