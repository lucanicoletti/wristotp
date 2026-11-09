# Review notes

Paste the matching section into each store's notes for the reviewer. Attach `sample-export-qr.png` and a short screen recording of a sync.

Record the video from a **debug** Android build or from iOS: Android release builds block screenshots and screen recording (FLAG_SECURE).

## App Store / Google Play

WristOTP is a companion app for the WristOTP watch app on Garmin Connect IQ watches. It has no login, no servers, no in-app purchases and collects no data.

Testing without a watch:
1. Open the Scan QR tab and choose Upload, then pick the attached image `sample-export-qr.png`. It's a Google Authenticator export containing four fake accounts.
2. The main screen lists the four accounts that were decoded on the phone.
3. The Send button needs a connected watch; without one it shows the watch status instead.

Sending to a watch needs a Connect IQ watch paired in the Garmin Connect app, with WristOTP or WristOTP Free installed from the Connect IQ Store. The attached video shows a full sync: choose watch → scan → Send → codes on the watch.

## Connect IQ Store

WristOTP needs its free phone app to receive accounts. Until the phone apps are public, use these test builds: {TESTFLIGHT_LINK} / {PLAY_TEST_LINK}.

The attached video shows a sync from the phone and the codes on the watch. `sample-export-qr.png` contains four fake accounts that you can import into the phone app and send.
