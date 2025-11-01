package com.bitcleanerx.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.prefs.Preferences

class ResultsViewModel(private val statsRepository: StatsRepository) : ViewModel() {
    private val _totalCleaned = MutableStateFlow(statsRepository.getTotalCleaned())
    val totalCleaned: StateFlow<Long> = _totalCleaned

    private val _itemsDeleted = MutableStateFlow(statsRepository.getItemsDeleted())
    val itemsDeleted: StateFlow<Int> = _itemsDeleted

    fun addCleanedSpace(bytes: Long) {
        _totalCleaned.value += bytes
        _itemsDeleted.value += 1
        statsRepository.saveTotalCleaned(_totalCleaned.value)
        statsRepository.saveItemsDeleted(_itemsDeleted.value)
    }

    fun resetStats() {
        _totalCleaned.value = 0L
        _itemsDeleted.value = 0
        statsRepository.saveTotalCleaned(0L)
        statsRepository.saveItemsDeleted(0)
    }
}

interface StatsRepository {
    fun getTotalCleaned(): Long
    fun getItemsDeleted(): Int
    fun saveTotalCleaned(value: Long)
    fun saveItemsDeleted(value: Int)
}

class StatsRepositoryImpl : StatsRepository {
    private val prefs = Preferences.userRoot().node("bitcleanerx")

    override fun getTotalCleaned(): Long {
        return prefs.getLong("total_cleaned", 0L)
    }

    override fun getItemsDeleted(): Int {
        return prefs.getInt("items_deleted", 0)
    }

    override fun saveTotalCleaned(value: Long) {
        prefs.putLong("total_cleaned", value)
        prefs.flush()
    }

    override fun saveItemsDeleted(value: Int) {
        prefs.putInt("items_deleted", value)
        prefs.flush()
    }
}

