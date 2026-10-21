# Same stack and device-only data as Weekly

Weekend Vibes is built the same way as Weekly and ThingSpot: Kotlin, Jetpack Compose, Room and Material 3, for one person, with all data in a local database on the phone. There is no account, no sync and no backend. The reasons are recorded in ThingSpot's [ADR 0001](../../../WhereIsIt/docs/adr/0001-device-only-data.md) and [ADR 0002](../../../WhereIsIt/docs/adr/0002-kotlin-compose-kmp-ready.md), and this setup keeps a later F-Droid release easy.

Android's built-in system backup is turned off. When changing phones, the only protection is a backup file that the user saves and restores by hand from Settings (ADR 0004).

Unlike Weekly, the app has the INTERNET permission, used only for Link import (ADR 0002).

## Considered Options

- **Keep the web version (PersonalProjects/weekend) and its Firebase backend**: rejected. Personal data doesn't need a server, and its one Cloud Function (reading event pages) can run on the phone.
- **Add a Weekends screen to Weekly instead of a new app**: rejected. Weekly is about Routines and ticking things off; this is a plain timeline of free days with no done state, and joining them would make both harder to use.
