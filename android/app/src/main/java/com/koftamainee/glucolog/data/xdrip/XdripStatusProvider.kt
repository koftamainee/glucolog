package com.koftamainee.glucolog.data.xdrip

import com.koftamainee.glucolog.data.DayRepository
import com.koftamainee.glucolog.data.SettingsDataStore
import com.koftamainee.glucolog.domain.floatToTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class XdripStatus(
    val connected: Boolean = false,
    val lastDate: String? = null,
    val lastTime: String? = null,
    val lastValue: Float? = null,
)

class XdripStatusProvider(
    private val repo: DayRepository,
    private val settings: SettingsDataStore,
) {
    val status: Flow<XdripStatus> =
        combine(repo.observeLastXdrip(), settings.xdripConnected) { last, connected ->
            XdripStatus(
                connected = connected,
                lastDate = last?.date,
                lastTime = last?.h?.let(::floatToTime),
                lastValue = last?.g,
            )
        }
}
