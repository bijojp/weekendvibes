package io.github.bijojp.weekendvibes.ui

import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.data.PictureStore
import io.github.bijojp.weekendvibes.data.backup.Backups
import io.github.bijojp.weekendvibes.domain.ActivityFields
import io.github.bijojp.weekendvibes.domain.ActivityId
import io.github.bijojp.weekendvibes.domain.Agenda
import io.github.bijojp.weekendvibes.domain.AgendaRepository
import io.github.bijojp.weekendvibes.domain.Settings
import io.github.bijojp.weekendvibes.domain.SettingsRepository
import io.github.bijojp.weekendvibes.domain.eventpage.SharedText
import io.github.bijojp.weekendvibes.imports.LinkImporter
import io.github.bijojp.weekendvibes.imports.LinkResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Everything a screen needs to draw, at one moment. */
data class AppState(val agenda: Agenda, val settings: Settings, val today: LocalDate)

/** One shared view of the data for every screen, plus the changes screens can ask for. */
class WeekendVibesViewModel(
    private val agenda: AgendaRepository,
    private val settingsRepository: SettingsRepository,
    private val pictures: PictureStore,
    private val backups: Backups,
    private val links: LinkImporter,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())

    /** Null only until the first snapshot loads. */
    val state: StateFlow<AppState?> = combine(agenda.agenda, settingsRepository.settings, today, ::AppState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val messageChannel = Channel<Int>(Channel.BUFFERED)

    /** Short confirmations ("Plan added") as string resources, for the snackbar. */
    val messages: Flow<Int> = messageChannel.receiveAsFlow()

    private val linkChannel = Channel<String>(Channel.BUFFERED)

    /** Event links shared into the app, each opening the editor to import it. */
    val sharedLinks: Flow<String> = linkChannel.receiveAsFlow()

    init {
        // Rolls today over at midnight while the app stays open, so a finished Weekend becomes past.
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1_000)
                refreshToday()
            }
        }
        // Pictures of deleted Activities, and ones picked in an editor that was never saved.
        viewModelScope.launch {
            pictures.sweep(agenda.agenda.first().activities.mapNotNull { it.picture }.toSet())
        }
    }

    /** The date or time zone may have changed while the app was in the background. */
    fun refreshToday() {
        today.value = LocalDate.now()
    }

    fun addActivity(date: LocalDate, fields: ActivityFields) =
        perform(R.string.msg_activity_added) { agenda.addActivity(date, fields) }

    fun editActivity(id: ActivityId, date: LocalDate, fields: ActivityFields) =
        perform(R.string.msg_saved) { agenda.editActivity(id, date, fields) }

    fun deleteActivity(id: ActivityId) = perform(R.string.msg_activity_deleted) { agenda.deleteActivity(id) }

    fun setExtraDay(date: LocalDate, label: String) = perform(null) { agenda.setExtraDay(date, label) }

    fun removeExtraDay(date: LocalDate) = perform(R.string.msg_extra_day_removed) { agenda.removeExtraDay(date) }

    fun updateSettings(change: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(change) }
    }

    fun pictureFile(name: String): File = pictures.file(name)

    /** Copies a picture from the photo picker; it only becomes permanent when the editor is saved. */
    fun importPicture(source: Uri, onImported: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { pictures.import(source) }
                .onSuccess(onImported)
                .onFailure {
                    Log.w(TAG, "Picture import failed", it)
                    messageChannel.send(R.string.msg_picture_failed)
                }
        }
    }

    /** Text another app shared; opens the editor on the first link in it. */
    fun receiveShared(text: String) {
        viewModelScope.launch {
            val link = SharedText.firstUrl(text)
            if (link != null) linkChannel.send(link) else messageChannel.send(R.string.msg_no_link)
        }
    }

    /** Reads an event page for Link import; null when nothing could be found. */
    suspend fun readLink(link: String): LinkResult? = links.read(link)

    fun backupFileName(): String = backups.suggestedFileName(LocalDate.now())

    fun backUp(uri: Uri) = perform(R.string.msg_backed_up, failure = R.string.msg_backup_failed) { backups.write(uri) }

    fun restore(uri: Uri) {
        viewModelScope.launch {
            runCatching { backups.restore(uri) }
                .onSuccess { messageChannel.send(R.string.msg_restored) }
                .onFailure {
                    Log.w(TAG, "Restore failed", it)
                    messageChannel.send(if (it is IllegalArgumentException) R.string.msg_not_a_backup else R.string.msg_restore_failed)
                }
        }
    }

    private fun perform(@StringRes success: Int?, @StringRes failure: Int = R.string.msg_failed, action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { success?.let { messageChannel.send(it) } }
                .onFailure {
                    Log.w(TAG, "Change failed", it)
                    messageChannel.send(failure)
                }
        }
    }

    private companion object {
        const val TAG = "WeekendVibes"
    }
}
