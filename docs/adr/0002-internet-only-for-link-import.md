# The network is used only for Link import and update checks

The app declares the INTERNET permission. It is used for three things. First, when the user shares an event page's link into the app, the app fetches that page (and its image) to fill in the Activity form. Second, when the user taps **Check for updates** in Settings, the app asks GitHub's API for the latest release and compares its version with its own (added 2026-10-21). Third, for **Update alerts**, the app makes that same request on its own about once a day while the phone is online, and posts one notification per newer release (added 2026-10-29). Update alerts are on by default and turned off with "New versions" in Settings. Nothing else goes over the network: no analytics, no crash reporting, no other background fetching.

The page is read on the phone. A plain HTTP fetch comes first. If it gets a block page or an empty JavaScript shell (BookMyShow is behind Cloudflare, which turns away anything that isn't a real browser), the page is loaded in a hidden WebView, which is Android's own Chrome engine, and read from there. If that fails too, the form opens with only the link.

The app never pretends to be another client, such as WhatsApp's or Facebook's link-preview bot, to get past a site.

Neither update check downloads or installs anything. When a newer release is out, the app opens that release's GitHub page in the browser; the user downloads the APK and Android's installer updates the app in place, keeping its data, because every release is signed with the same key.

## Considered Options

- **Stay offline and read only the text the share sheet passes in**: rejected. It rarely includes the date and never the picture, which is the point of the feature.
- **Read event pages on a server (a cloud function) and call it from the app**: rejected. It needs a server and an account, and it gets blocked by the same sites.
- **Check for updates on launch**: rejected. It tells GitHub when the app is opened.
- **Check in the background**: rejected on 2026-10-21 as traffic the user didn't ask for, then adopted on 2026-10-29 as Update alerts, when the app started being shared with friends. Few people open Settings to check, so without it most installs would stay on their first version. It is one request a day to GitHub, carrying only the app's version in its User-Agent, on a fixed daily schedule rather than when the app is used, and it can be turned off.
- **Push notifications (Firebase Cloud Messaging) for new releases**: rejected. Instant, but it needs a Firebase project and Google Play Services, F-Droid won't accept it, and it breaks "no account, no backend".
- **ntfy or another push service the user subscribes to**: rejected. Every user would have to install a second app and subscribe first.
- **Download and install updates inside the app**: rejected. It needs the install-unknown-apps permission, F-Droid lists it as an anti-feature, and Play doesn't allow it. The browser and Android's installer already do this.
- **Pretend to be a link-preview bot as a last resort**: rejected. It is fragile, arguably against those sites' terms, and of little use: Instagram only gives the post date that way, not the event date.

## Consequences

If the app goes to F-Droid, its F-Droid build leaves out Check for updates and Update alerts, since F-Droid delivers the updates there.
