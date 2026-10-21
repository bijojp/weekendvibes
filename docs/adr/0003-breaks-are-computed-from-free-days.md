# Breaks are computed from free days; an Activity is one day

Only two things are stored: Activities, each on one date, and Extra days, each a date with an optional label. Breaks (the cards in the Timeline) are worked out from those dates every time: any run of consecutive Free days is one Break. So a Friday marked as an Extra day joins the Weekend after it, and a Wednesday holiday stands alone, without any rule naming Friday or Monday specially.

An Activity can be on any day. A weekday that isn't an Extra day but has Activities is shown as a one-day card of its own, so nothing ever drops out of the Timeline. It never joins the Weekend next to it: otherwise a few weekday plans in a row would chain two Weekends into one card. Saving a weekday plan doesn't mark an Extra day either; that stays a deliberate choice.

An Activity never spans days and never repeats. A trip is one Activity per day.

## Considered Options

- **Store each Weekend as a row**: rejected. Weekends follow from the calendar; storing them would need generating, and would drift when an Extra day is added or removed.
- **Activities with a start and an end day**: rejected. It complicates the cards, Reminders and the editor for the rare multi-day trip.
- **Recurring Activities**: left out of v1. Weekly already handles things that repeat.
