# The network is used only for Link import and tapped update checks

The app declares the INTERNET permission, which Weekly and ThingSpot don't. It is used for two things. First, when the user shares an event page's link into the app, the app fetches that page (and its image) to fill in the Activity form. Second, when the user taps **Check for updates** in Settings, the app asks GitHub's API for the latest release and compares its version with its own (added 2026-10-21). Nothing else goes over the network: no analytics, no crash reporting, no automatic update checks, no background fetching.

The page is read on the phone. A plain HTTP fetch comes first. If it gets a block page or an empty JavaScript shell (BookMyShow is behind Cloudflare, which turns away anything that isn't a real browser), the page is loaded in a hidden WebView, which is Android's own Chrome engine, and read from there. If that fails too, the form opens with only the link.

The app never pretends to be another client, such as WhatsApp's or Facebook's link-preview bot, to get past a site.

The update check never downloads or installs anything. When a newer release is out, it opens that release's GitHub page in the browser; the user downloads the APK and Android's installer updates the app in place, keeping its data, because every release is signed with the same key.

## Considered Options

- **Stay offline and read only the text the share sheet passes in**: rejected. It rarely includes the date and never the picture, which is the point of the feature.
- **Port the old Firebase Cloud Function and call it from the app**: rejected. It needs a server and an account, and it gets blocked by the same sites.
- **Check for updates on launch or in the background**: rejected. It is background traffic the user didn't ask for, and it tells GitHub when the app is opened.
- **Download and install updates inside the app**: rejected. It needs the install-unknown-apps permission, F-Droid lists it as an anti-feature, and Play doesn't allow it. The browser and Android's installer already do this.
- **Pretend to be a link-preview bot as a last resort**: rejected. It is fragile, arguably against those sites' terms, and of little use: Instagram only gives the post date that way, not the event date.

## Consequences

If the app goes to F-Droid, its F-Droid build leaves out Check for updates, since F-Droid delivers the updates there.
