# wristotp

Your two-factor codes on your Garmin watch, alongside Google Authenticator.

- `watch/`: Connect IQ watch app (Monkey C). `monkey.jungle` builds WristOTP, `free.jungle` builds WristOTP Free.
- `companion/`: phone app (Kotlin Multiplatform + Compose Multiplatform) for Android and iOS. Imports Google Authenticator export QR codes and sends the accounts to the watch.
- `store/`: store listings, data declarations, graphics, screenshots and reviewer notes.
- `site/`: privacy policy and support pages.
