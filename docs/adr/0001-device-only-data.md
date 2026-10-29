# Device-only data in a native Kotlin app

Weekend Vibes is a native Android app built with Kotlin, Jetpack Compose, Room and Material 3, for one person, with all data in a local database on the phone. There is no account, no sync and no backend. Weekend plans only matter on the phone that holds them, so a server would add an account, running costs and a privacy risk for nothing. Leaving out Google Play Services and Firebase also keeps a later F-Droid release easy.

Android's built-in system backup is turned off. When changing phones, the only protection is a backup file that the user saves and restores by hand from Settings (ADR 0004).

The app has the INTERNET permission, used only for Link import and update checks (ADR 0002).

## Considered Options

- **A web app with a Firebase backend**: rejected. Personal data doesn't need a server, and the one thing a server would do here, reading event pages, can run on the phone.
