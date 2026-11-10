package com.example.snustracker

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate

/**
 * Hält den Zustand der App und speichert nach jeder Änderung.
 * Compose zeichnet die Oberfläche automatisch neu, sobald sich `state` ändert.
 */
class TrackerViewModel(app: Application) : AndroidViewModel(app) {

    var state by mutableStateOf(Storage.load(app))
        private set

    /** Wird bei Wiederherstellen/Löschen erhöht, damit die Eingabefelder neu befüllt werden. */
    var version by mutableIntStateOf(0)
        private set

    private fun update(newState: AppState) {
        state = newState
        Storage.save(getApplication(), newState)
    }

    fun add(day: LocalDate) = update(state.addSnus(day))
    fun remove(day: LocalDate) = update(state.removeSnus(day))
    fun setPouches(value: Int?) = update(state.withPouches(value))
    fun setPrice(value: Double?) = update(state.withPrice(value))

    fun exportJson(): String = Storage.toJson(state)

    /** Gibt true zurück, wenn der Text eine gültige Sicherung war. */
    fun restore(json: String): Boolean = try {
        update(Storage.parse(json))
        version++
        true
    } catch (e: Exception) {
        false
    }

    fun reset() {
        update(AppState())
        version++
    }
}
