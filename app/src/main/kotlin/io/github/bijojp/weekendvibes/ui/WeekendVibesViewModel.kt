package io.github.bijojp.weekendvibes.ui

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.ActivityFields
import io.github.bijojp.weekendvibes.domain.ActivityId
import io.github.bijojp.weekendvibes.domain.Agenda
import io.github.bijojp.weekendvibes.domain.AgendaRepository
import io.github.bijojp.weekendvibes.domain.Settings
import io.github.bijojp.weekendvibes.domain.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Everything a screen needs to draw, at one moment. */
data class AppState(val agenda: Agenda, val settings: Settings, val today: LocalDate)

/** One shared view of the data for every screen, plus the changes screens can ask for. */
class WeekendVibesViewModel(
    private val agenda: AgendaRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())

    /** Null only until the first snapshot loads. */
    val state: StateFlow<AppState?> = combine(agenda.agenda, settingsRepository.settings, today, ::AppState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val messageChannel = Channel<Int>(Channel.BUFFERED)

    /** Short confirmations ("Activity added") as string resources, for the snackbar. */
    val messages: Flow<Int> = messageChannel.receiveAsFlow()

    init {
        // Rolls today over at midnight while the app stays open, so a finished Weekend becomes past.
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1_000)
                refreshToday()
            }
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
