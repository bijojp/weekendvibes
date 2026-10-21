# Weekend Vibes plan

> "What am I doing this weekend, and what did I do last time?": a weekend-only planner and journal for Android, for personal use.
> Everything stays local while it's being built and tested. F-Droid or Play may come later.

Vocabulary (Free day, Extra day, Break, Weekend, Long weekend, This weekend, Activity, Anytime, Picture, Timeline, Digest, Reminder, Link import, Home city, Backup) is defined in [`GLOSSARY.md`](../GLOSSARY.md). The decisions that are hard to reverse are recorded in [`docs/adr/`](adr/). Agreed on 2026-10-21.

This replaces the older web version (PersonalProjects/weekend, "Weekend Vibes" on Firebase). Nothing is carried over except the name and its link-reading logic, which moves onto the phone.

---

## 1. Principles (non-negotiable)

1. **Only free days exist.** Saturdays, Sundays and the Extra days the user marks. Breaks are worked out from those dates, never stored. (ADR 0003)
2. **One Activity, one day.** No recurrence, no multi-day Activities. (ADR 0003)
3. **Data stays on the phone.** One user, no account, no sync, no backend, Android system backup off. Same stack as Weekly and ThingSpot. (ADR 0001)
4. **The network is used for Link import and nothing else.** Only when the user shares a link in. No analytics, no background traffic. (ADR 0002)

## 2. Scope

### v1

| Area | What ships |
|---|---|
| Timeline (home) | One list of Breaks in date order. Opens on **This weekend**; scroll up for the past, down for the future. A "This weekend" button jumps back. Every coming Break is shown, empty ones as "nothing planned"; past Breaks only if they have Activities. Each card shows its days (SAT, SUN, and any Extra days with their labels) with that day's Activities under it. Card titles: "This weekend", "Long weekend", the Extra day's label, or the date range. |
| Activities | Title, one Free day, optional time (start only), location, link, notes, Picture, Reminder. Timed first in time order, then Anytime. Add by tapping a day in a card (that day pre-filled) or from the **+** menu (next Saturday pre-filled). Tap an Activity to edit; delete from the editor after a confirmation. Past ones are as editable as future ones; there is no done or skipped state. |
| Extra days | **+** menu → "Add extra day": pick a weekday and an optional label. A Fri or Mon joins the Weekend next to it. Edit the label or remove the day from its row in the card; removing one that has Activities asks first and deletes them. |
| Pictures | One per Activity. From Link import (downloaded, scaled to at most 1080 px) or from Android's photo picker (no storage permission). Thumbnail in the card, full size in the editor. |
| Notifications | **Digest** at 6 PM the evening before each Break starts (on by default; can be turned off). **Reminders** are opt-in per timed Activity: 30 min, 1 hr, 3 hr or 1 day before. Windowed alarms like Weekly, so up to 10 minutes late and no exact-alarm permission. |
| Link import | Appears in Android's share sheet. Takes the first URL in the shared text and follows redirects. Opens the Activity form pre-filled; the link is always kept even if nothing else is found, and failures offer Retry. Details in §3. |
| Settings | Digest on/off, Home city (default Bengaluru), theme (system/light/dark), **Back up** / **Restore**, About. |
| Backup | **Back up** saves one zip (`data.json` plus a `pictures/` folder) wherever the user chooses. **Restore** replaces all data after a confirmation. (ADR 0004) |
| Look and feel | Premium and editorial rather than stock Material: a warm near-black canvas (warm paper in light mode) with slow drifting glows, Fraunces (serif, soft, large optical size) for headlines and big date numerals, Manrope for everything else, and one golden-hour gradient used for This weekend and the main actions. Saturday mint, Sunday rose, Extra days amber. Plans with a Picture are full-bleed tiles; empty days are dashed "Free · tap to plan" tiles. Cards sink slightly under the finger with a haptic tick. English only. |

### Deliberately not doing
Recurring Activities, Activities spanning several days, duplicating an Activity, categories or colours, done/skipped tracking, search, a home-screen widget, sharing with others, accounts or sync, analytics, Android system backup, and pretending to be a link-preview bot to get past sites that block apps.

## 3. Link import

Measured on real Bangalore pages on 2026-10-21 (plain HTTP fetch, Android Chrome user agent):

| Works with the generic reader | Needs more | Won't work |
|---|---|---|
| District, HighApe, allevents, Meetup, Luma, Eventbrite, blr.today event pages | **BookMyShow**: blocked by Cloudflare for anything that isn't a real browser, so it needs the WebView fallback (confirm on a phone). **Urbanaut**: date and venue only in its own embedded JSON, so it needs an adapter. | Instagram, Facebook, Ticketmaster, Resident Advisor (logins and bot walls); Townscript (shut down); old Insider links (dead; Insider is now District) |

**Order of attempts:** plain fetch → if the page is a block page or an empty JavaScript shell, load it in a hidden WebView and read it from there → give up and open the form with only the link.

**Reading a page:**
1. schema.org `Event` JSON-LD, including arrays, `@graph`, and any type ending in "Event" (MusicEvent, EducationEvent, …).
2. A site adapter, if one exists for the domain (Urbanaut first). One file per site.
3. OpenGraph title and image.

**Dates:** times with an offset are converted to the phone's time zone; times without one are taken as the phone's time zone; midnight UTC (`T00:00Z`) means a date with no time. When a page has several dates or venues (BookMyShow lists one per city), the form offers a picker: venues in the Home city first, then weekend dates first.

**Weekday events:** if the date isn't a Free day, the form says so ("Wed 28 Oct isn't a weekend, so it'll be added as an Extra day") and saving marks it.

## 4. Architecture

Same stack and module layout as Weekly (ADR 0001). The domain module is pure Kotlin, so the date rules are unit-tested without Android.

```
:core:domain   Pure Kotlin. Activity, Extra day models. Free days → Breaks; which Breaks the
               Timeline shows; This weekend; Activity order within a day; when Digests and
               Reminders are due. Event page reading (JSON-LD, OpenGraph, site adapters) and
               date normalising, as pure functions over HTML text.
:core:data     Room (coroutines and Flow); Settings (DataStore); repositories; Picture files;
               backup zip.
:app           Compose UI, Navigation 3, alarms and notifications, share target, fetching and
               the WebView fallback, hand-wired dependencies.
```

**Libraries:** Compose (BOM), Material 3, Navigation 3, Room, kotlinx.serialization, Coroutines, DataStore, Coil (local Pictures only), ExifInterface. For Link import: Jsoup (HTML parsing, pure Java) and the platform's `HttpURLConnection` and `WebView`. Fonts: Fraunces and Manrope, bundled, SIL OFL (`docs/licenses/`). All F-Droid-friendly; no Google Play Services or Firebase. Dates use `java.time`; dependencies are wired by hand.

**Data design:**
- Two tables: Activities (with their date) and Extra days (keyed by date, with a label). Breaks are computed from them on every change; there is no table of weekends.
- A date that has Activities always counts as free, even if it isn't a Saturday, Sunday or Extra day, so nothing restored from an older backup can go missing from the Timeline.
- The Activity table has `picture` and `reminderMinutes` from the first schema version, so later milestones don't need a migration.
- Every Activity has a random UUID plus `createdAt` and `updatedAt`.

**Notifications:** same pattern as Weekly's `ReminderScheduler`: only the next alarm is set, as a windowed AlarmManager alarm, and it's set again after any change, a reboot or a time-zone change. `POST_NOTIFICATIONS` is requested on first launch because the Digest is on by default.

**Tests (domain first):**
- Breaks: Sat+Sun, a Fri or Mon Extra day joining a Weekend, a Thu+Fri run, a standalone Wednesday, two Weekends joined by a whole week of Extra days.
- Timeline: past Breaks only with Activities, every coming one shown, This weekend on a weekday, on a Saturday and on a standalone Extra day.
- Order within a day: timed by time, then Anytime.
- Digest and Reminder due times, including a Thursday Digest for a Friday Long weekend.
- Event pages: saved real pages from each platform → expected title, date, time, venue, image; time zone rules; multi-venue picking.
- Backup round trip.

## 5. Build order

**Status (2026-10-21):** steps 1–8 are built, with the redesign. 58 domain and backup tests pass (including Link import against saved Bangalore pages) and lint has no errors. Installed on the phone; not yet checked by hand: the share target with real District and BookMyShow links (the WebView fallback is untested on a device), notifications firing, and backup/restore. Step 9 starts there.

| Step | Milestone | Done when |
|---|---|---|
| 1 | **Skeleton** | Gradle multi-module project builds and installs. Debug build has `applicationIdSuffix = ".debug"`. |
| 2 | **Domain** | Breaks, Timeline, ordering rules implemented and their tests green. |
| 3 | **Data** | Room schema v1 (with the Picture and Reminder columns), repositories, Settings. |
| 4 | **Timeline + editor** | Cards, tap a day to add, **+** menu, add/edit/delete Activities, add/edit/remove Extra days. **Start using it yourself.** |
| 5 | **Pictures** | Photo picker, stored files, thumbnails, cleanup when an Activity is deleted. |
| 6 | **Notifications** | Digest and Reminders, surviving reboots. |
| 7 | **Settings + Backup** | Settings screen, zip Back up and Restore. |
| 8 | **Link import** | Share target, fetch, JSON-LD/OpenGraph reader, Urbanaut adapter, WebView fallback, date/venue picker, Home city. Check BookMyShow on a real phone. |
| 9 | **Polish** | Empty states, accessibility (TalkBack, large fonts), fixes from a few weekends of use. |

## 6. Decide later (not blocking v1)
- Whether, when and how to publish (F-Droid, Play); licence; signing key (see ThingSpot's ADR 0003). "Weekend Vibes" may need checking for clashes in the stores.
- More site adapters, depending on which links fail in real use.
- Categories, search, a widget, and recurring Activities, if real use asks for them.
