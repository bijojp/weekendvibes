# The network is used only for Link import

The app declares the INTERNET permission, which Weekly and ThingSpot don't. It is used for one thing: when the user shares an event page's link into the app, the app fetches that page (and its image) to fill in the Activity form. Nothing else goes over the network: no analytics, no crash reporting, no update checks, no background fetching.

The page is read on the phone. A plain HTTP fetch comes first. If it gets a block page or an empty JavaScript shell (BookMyShow is behind Cloudflare, which turns away anything that isn't a real browser), the page is loaded in a hidden WebView, which is Android's own Chrome engine, and read from there. If that fails too, the form opens with only the link.

The app never pretends to be another client, such as WhatsApp's or Facebook's link-preview bot, to get past a site.

## Considered Options

- **Stay offline and read only the text the share sheet passes in**: rejected. It rarely includes the date and never the picture, which is the point of the feature.
- **Port the old Firebase Cloud Function and call it from the app**: rejected. It needs a server and an account, and it gets blocked by the same sites.
- **Pretend to be a link-preview bot as a last resort**: rejected. It is fragile, arguably against those sites' terms, and of little use: Instagram only gives the post date that way, not the event date.
