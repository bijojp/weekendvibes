# Weekend Vibes

A personal Android app for weekends: plan what's coming up, remember what you did, and see every weekend together in one scrolling timeline.

## Language

### Days

**Free day**:
A Saturday, a Sunday, or an Extra day. The Timeline lists every coming Free day; other days appear only when they have plans.

**Extra day**:
A weekday the user marks as free, such as a public holiday or a day of leave. It may have a label, such as "Diwali". Removing an Extra day removes its Activities too, after a confirmation.
_Avoid_: Holiday, Leave, Day off (as a term; "day off" is fine in UI copy)

**Break**:
A run of consecutive Free days, shown as one card in the Timeline. A Break that includes a Saturday or Sunday is a **Weekend**; a Weekend with Extra days in it is a **Long weekend** (a Friday off joins the Saturday after it). A Break made only of Extra days, such as a Wednesday holiday, stands on its own. A weekday that isn't an Extra day but has plans is a one-day card of its own (a **Weekday** card) and never joins a Weekend. Breaks are worked out from the dates; they are never stored.
_Avoid_: Card, Block, Stretch

**This weekend**:
The Break of Free days that contains today, or the next one if today isn't a Free day. Weekday plans don't count. The Timeline opens on it.

### What you do

**Activity**:
Something on one day, usually a Free day but any day will do: a title, plus an optional time, location, link, notes and picture. It never spans days and never repeats; a trip is one Activity per day.
_Avoid_: Event (that word is kept for pages on ticketing sites), Plan, Task

**Anytime**:
An Activity with no time. Within a day, timed Activities come first in time order, then the Anytime ones.

**Picture**:
The one image an Activity can have: downloaded from an imported link, or picked from the gallery (which replaces a downloaded one). Stored on the phone.

### Seeing it

**Timeline**:
The home screen. One list of Breaks in date order: past ones above (only those with Activities), then This weekend, then every coming Break, empty or not.

### Being told

**Digest**:
A notification at 6 PM the evening before a Break starts, listing its Activities or saying it's free.

**Reminder**:
An opt-in notification for a timed Activity, 30 minutes, 1 hour, 3 hours or 1 day before it.

### Getting things in

**Link import**:
Sharing an event page's link into the app. The app reads the page and opens the Activity form filled in with what it found, for the user to check and save.
_Avoid_: Scrape, Sync

**Home city**:
The city whose venues are offered first when an event page lists many (Settings; Bengaluru by default).

**Backup**:
One zip file (data plus Pictures) that the user saves anywhere and can restore from, replacing everything.
