# Breaks are computed from free days; an Activity is one day

Only two things are stored: Activities, each on one date, and Extra days, each a date with an optional label. Breaks (the cards in the Timeline) are worked out from those dates every time: any run of consecutive Free days is one Break. So a Friday marked as an Extra day joins the Weekend after it, and a Wednesday holiday stands alone, without any rule naming Friday or Monday specially.

A date that has Activities counts as free even if it isn't a Saturday, Sunday or Extra day, so nothing can drop out of the Timeline because its Extra day went missing (for example, from an edited backup).

An Activity never spans days and never repeats. A trip is one Activity per day.

## Considered Options

- **Store each Weekend as a row**: rejected. Weekends follow from the calendar; storing them would need generating, and would drift when an Extra day is added or removed.
- **Activities with a start and an end day**: rejected. It complicates the cards, Reminders and the editor for the rare multi-day trip.
- **Recurring Activities**: left out of v1. Weekly already handles things that repeat.
