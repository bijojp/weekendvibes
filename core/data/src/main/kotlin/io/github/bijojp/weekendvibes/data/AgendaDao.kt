package io.github.bijojp.weekendvibes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert

@Dao
abstract class AgendaDao {
    @Query("SELECT * FROM activities")
    abstract suspend fun activities(): List<ActivityEntity>

    @Query("SELECT * FROM extra_days")
    abstract suspend fun extraDays(): List<ExtraDayEntity>

    /** Everything at one consistent moment. */
    @Transaction
    open suspend fun snapshot(): Snapshot = Snapshot(activities(), extraDays())

    @Query("SELECT * FROM activities WHERE id = :id")
    abstract suspend fun activity(id: String): ActivityEntity?

    @Query("SELECT * FROM extra_days WHERE date = :date")
    abstract suspend fun extraDay(date: Long): ExtraDayEntity?

    @Insert
    abstract suspend fun insertActivity(activity: ActivityEntity)

    @Update
    abstract suspend fun updateActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    abstract suspend fun deleteActivity(id: String)

    @Query("DELETE FROM activities WHERE date = :date")
    abstract suspend fun deleteActivitiesOn(date: Long)

    @Upsert
    abstract suspend fun upsertExtraDay(extraDay: ExtraDayEntity)

    @Query("DELETE FROM extra_days WHERE date = :date")
    abstract suspend fun deleteExtraDay(date: Long)

    @Query("DELETE FROM activities")
    abstract suspend fun deleteAllActivities()

    @Query("DELETE FROM extra_days")
    abstract suspend fun deleteAllExtraDays()

    @Insert
    abstract suspend fun insertActivities(activities: List<ActivityEntity>)

    @Insert
    abstract suspend fun insertExtraDays(extraDays: List<ExtraDayEntity>)
}
