package io.github.bijojp.weekendvibes.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import io.github.bijojp.weekendvibes.data.PictureStore
import io.github.bijojp.weekendvibes.data.WeekendVibesDatabase
import io.github.bijojp.weekendvibes.data.toAgenda
import io.github.bijojp.weekendvibes.data.toEntity
import io.github.bijojp.weekendvibes.domain.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Saves everything to one zip file the user picks, and restores from one (ADR 0004).
 * Files are reached only through the system file pickers, so no storage permission is needed.
 */
class Backups internal constructor(
    private val context: Context,
    private val db: WeekendVibesDatabase,
    private val settings: SettingsRepository,
    private val pictures: PictureStore,
    private val appVersion: String,
) {
    fun suggestedFileName(today: LocalDate): String = "weekend-vibes-backup-$today.zip"

    /** Writes a complete backup to [uri], which the user picked with the system "save" picker. */
    suspend fun write(uri: Uri) = withContext(Dispatchers.IO) {
        val agenda = db.dao().snapshot().toAgenda()
        val text = BackupFormat.encode(BackupFormat.Contents(agenda, settings.settings.first()), appVersion, System.currentTimeMillis())
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Can't write to $uri")
        ZipOutputStream(stream.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA))
            zip.write(text.toByteArray())
            zip.closeEntry()
            agenda.activities.mapNotNull { it.picture }.toSet().forEach { name ->
                val file = pictures.file(name)
                if (file.isFile) {
                    zip.putNextEntry(ZipEntry(PICTURES + name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /**
     * Replaces all current data and Pictures with the backup at [uri]. Fails with
     * [IllegalArgumentException] before changing anything if the file isn't a backup this version can read.
     */
    suspend fun restore(uri: Uri) = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            var text: String? = null
            val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Can't read $uri")
            ZipInputStream(stream.buffered()).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val name = entry.name
                    when {
                        name == DATA -> text = zip.readBytes().decodeToString()
                        // Only plain file names are accepted, so an entry can't write outside the staging folder.
                        name.startsWith(PICTURES) && SAFE_NAME.matches(name.removePrefix(PICTURES)) ->
                            File(staging, name.removePrefix(PICTURES)).outputStream().use { zip.copyTo(it) }
                    }
                }
            }
            val contents = BackupFormat.decode(text ?: throw IllegalArgumentException("No $DATA in this file"))
            val dao = db.dao()
            db.withTransaction {
                dao.deleteAllActivities()
                dao.deleteAllExtraDays()
                dao.insertExtraDays(contents.agenda.extraDays.map { it.toEntity() })
                dao.insertActivities(contents.agenda.activities.map { it.toEntity() })
            }
            settings.update { contents.settings }
            pictures.directory.deleteRecursively()
            pictures.directory.mkdirs()
            staging.listFiles()?.forEach { it.renameTo(pictures.file(it.name)) }
        } finally {
            staging.deleteRecursively()
        }
    }

    private companion object {
        const val DATA = "data.json"
        const val PICTURES = "pictures/"
        val SAFE_NAME = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]*")
    }
}
