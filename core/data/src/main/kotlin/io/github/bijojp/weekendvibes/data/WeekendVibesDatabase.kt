package io.github.bijojp.weekendvibes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.bijojp.weekendvibes.data.backup.Backups
import io.github.bijojp.weekendvibes.domain.AgendaRepository
import io.github.bijojp.weekendvibes.domain.SettingsRepository

@Database(
    entities = [ActivityEntity::class, ExtraDayEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WeekendVibesDatabase : RoomDatabase() {
    abstract fun dao(): AgendaDao
}

/** Everything Weekend Vibes keeps on the device. */
class WeekendVibesData private constructor(context: Context, appVersion: String) {
    private val database = Room.databaseBuilder(context.applicationContext, WeekendVibesDatabase::class.java, "weekendvibes.db")
        .build()

    val settings: SettingsRepository = DataStoreSettingsRepository(context.applicationContext)
    val agenda: AgendaRepository = RoomAgendaRepository(database)
    val pictures = PictureStore(context.applicationContext)
    val backups = Backups(context.applicationContext, database, settings, pictures, appVersion)

    companion object {
        /** The app's single entry point into stored data; call once. */
        fun open(context: Context, appVersion: String) = WeekendVibesData(context, appVersion)
    }
}
