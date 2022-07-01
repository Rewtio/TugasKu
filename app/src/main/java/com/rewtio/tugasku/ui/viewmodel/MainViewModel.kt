package com.rewtio.tugasku.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rewtio.tugasku.Status
import com.rewtio.tugasku.TugasData
import com.rewtio.tugasku.dataStore
import com.rewtio.tugasku.preferences.TugasPref
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel(application: Application) : AndroidViewModel(application) {
    //SwipeRefresh State
    var isRefreshing = mutableStateOf(true)

    //List Tugas State
    val listTugas = mutableStateListOf<TugasData>()

    fun onRefresh() {
        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.data.first().let { preferences ->
                    preferences[TugasPref.KEY_LIST_TUGAS]?.forEach { judul ->
                        preferences[stringSetPreferencesKey("tugas_${judul.trim()}")]?.let { tugasStr ->
                            val list = tugasStr.toList()
                            val tugasData = TugasData(
                                Status.valueOf(list[0]),
                                list[1],
                                list[2],
                                list[3],
                                list[4],
                                list[5]
                            )
                            if (!listTugas.any { it.isMatch(tugasData) }) {
                                listTugas.add(tugasData)
                            }
                        }
                    }
                }
            }catch (e: IOException){
                Log.e("MainViewModel", e.message ?: "")
            } catch (e: Exception) {
                Log.e("MainViewModel", e.message ?: "")
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun addTugas(tugas: TugasData) {
        listTugas.add(tugas)
        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[TugasPref.KEY_LIST_TUGAS] =
                        listTugas.map { it.judul.trim() }.toSet()
                    preferences[stringSetPreferencesKey("tugas_${tugas.judul.trim()}")] = setOf(
                        tugas.status.name,
                        tugas.judul,
                        tugas.mapel,
                        tugas.deskripsi,
                        tugas.dibuat,
                        tugas.deadline
                    )
                }
            } catch (e: IOException) {
                // Handle error writing preferences to disk
                Log.i("MainViewModel", "Error: ${e.message}")
            } catch (e: Exception) {
                // Handle error thrown while executing transform block
                Log.i("MainViewModel", "Error: ${e.message}")
            }
        }
    }

    fun editTugas(tugas: TugasData) {
        listTugas.remove(tugas)
        listTugas.add(tugas)
    }

    fun deleteTugas(tugas: TugasData) {
        listTugas.remove(tugas)
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                preferences[TugasPref.KEY_LIST_TUGAS] = listTugas.map { it.judul.trim() }.toSet()
                preferences.remove(stringSetPreferencesKey("tugas_${tugas.judul.trim()}"))
            }
        }
    }
}