# Backup is a zip with the Pictures in it

Weekly and ThingSpot back up to a single JSON file. Weekend Vibes has Pictures, so a backup is one zip file instead: `data.json` (a format version, every Activity and Extra day, and the Settings) plus a `pictures/` folder. It is written and read through the system file pickers, so no storage permission is needed. Restore replaces everything after a confirmation, and can read older format versions.

## Considered Options

- **JSON only, downloading link Pictures again after a restore**: rejected. Pictures picked from the gallery would be lost, and event pages often disappear after the event, so old link Pictures couldn't be fetched again.
- **Pictures embedded in the JSON as base64**: rejected. It makes the file about a third larger and the whole backup has to be held in memory to read it.
