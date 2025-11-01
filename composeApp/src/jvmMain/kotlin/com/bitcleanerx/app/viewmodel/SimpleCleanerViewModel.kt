package com.bitcleanerx.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitcleanerx.app.helper.FileHelper
import com.bitcleanerx.app.model.*
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class GroupedItems(
    val groupName: String,
    val groupImage: String,
    val items: List<ScannedItem>
)

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val currentPath: String) : ScanState()
    data class Completed(val groups: List<GroupedItems>) : ScanState()
    data class Error(val message: String) : ScanState()
}

class SimpleCleanerViewModel(
    private val resultsViewModel: ResultsViewModel
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    private var scanJob: Job? = null

    fun startScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                println("Starting scan...")
                val config = loadConfig()
                val platformList = getPlatformList(config)
                val totalItems = platformList.sumOf { it.items.size }
                println("Config loaded: ${platformList.size} groups with ${totalItems} total items to scan")
                val groupedResults = mutableListOf<GroupedItems>()

                for (group in platformList) {
                    val groupItems = mutableListOf<ScannedItem>()

                    for (item in group.items) {
                        if (!isActive) break

                        _scanState.value = ScanState.Scanning(item.path)
                        println("Scanning: ${item.path}")

                        try {
                            val scannedItem = scanItem(item, group.groupName)
                            if (scannedItem != null) {
                                groupItems.add(scannedItem)
                                println("Found: ${scannedItem.name} - ${scannedItem.size} bytes")
                            } else {
                                println("Not found: ${item.path}")
                            }
                        } catch (e: Exception) {
                            println("Error scanning ${item.path}: ${e.message}")
                        }
                    }

                    if (groupItems.isNotEmpty()) {
                        groupedResults.add(GroupedItems(group.groupName, group.groupImage, groupItems))
                    }
                }

                println("Scan completed. Found ${groupedResults.sumOf { it.items.size }} items in ${groupedResults.size} groups")
                if (isActive) {
                    _scanState.value = ScanState.Completed(groupedResults)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error during scan: ${e.message}")
                _scanState.value = ScanState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _scanState.value = ScanState.Idle
    }

    private suspend fun scanItem(item: CleanerItem, groupName: String): ScannedItem? {
        return withContext(Dispatchers.IO) {
            val expandedPath = FileHelper.expandPath(item.path)
            val file = File(expandedPath)

            if (!file.exists()) return@withContext null

            when (item.type) {
                ItemType.file -> {
                    ScannedItem(
                        name = item.name,
                        path = item.path,
                        size = file.length(),
                        type = ItemType.file
                    )
                }

                ItemType.folder -> {
                    ScannedItem(
                        name = item.name,
                        path = item.path,
                        size = FileHelper.calculateDirectorySize(item.path),
                        type = ItemType.folder
                    )
                }

                ItemType.folders -> {
                    val subFolders = FileHelper.getSubFolders(item.path)
                        .sortedBy { it.name.lowercase() }
                        .map { subFolder ->
                            SubFolderItem(
                                name = subFolder.name,
                                path = subFolder.absolutePath,
                                size = FileHelper.calculateDirectorySize(subFolder.absolutePath)
                            )
                        }
                    ScannedItem(
                        name = item.name,
                        path = item.path,
                        size = subFolders.sumOf { it.size },
                        type = ItemType.folders,
                        subFolders = subFolders
                    )
                }
            }
        }
    }

    fun deleteGroup(groupName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isDeleting.value = true
            if (_scanState.value is ScanState.Completed) {
                val currentGroups = (_scanState.value as ScanState.Completed).groups
                val groupToDelete = currentGroups.find { it.groupName == groupName }

                var totalDeleted = 0L
                groupToDelete?.items?.forEach { item ->
                    totalDeleted += item.size
                    FileHelper.deleteFileOrDirectory(item.path)
                    item.subFolders.forEach { subFolder ->
                        FileHelper.deleteFileOrDirectory(subFolder.path)
                    }
                }

                if (totalDeleted > 0) {
                    resultsViewModel.addCleanedSpace(totalDeleted)
                }

                val updatedGroups = currentGroups.filter { it.groupName != groupName }

                withContext(Dispatchers.Main) {
                    _scanState.value = ScanState.Completed(updatedGroups)
                }
            }
            _isDeleting.value = false
        }
    }

    fun deleteItem(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isDeleting.value = true

            var deletedSize = 0L
            if (_scanState.value is ScanState.Completed) {
                val currentGroups = (_scanState.value as ScanState.Completed).groups
                currentGroups.forEach { group ->
                    group.items.forEach { item ->
                        if (item.path == path) {
                            deletedSize = item.size
                        } else {
                            item.subFolders.find { it.path == path }?.let {
                                deletedSize = it.size
                            }
                        }
                    }
                }
            }

            FileHelper.deleteFileOrDirectory(path)

            if (deletedSize > 0) {
                resultsViewModel.addCleanedSpace(deletedSize)
            }

            if (_scanState.value is ScanState.Completed) {
                val currentGroups = (_scanState.value as ScanState.Completed).groups
                val updatedGroups = currentGroups.map { group ->
                    val updatedItems = group.items.mapNotNull { item ->
                        when {
                            // If the item itself is being deleted, remove it
                            item.path == path -> null
                            
                            // If it's a folders type item, update its subfolders
                            item.type == ItemType.folders -> {
                                val updatedSubFolders = item.subFolders.filter { it.path != path }
                                // Only keep the item if it still has subfolders after deletion
                                if (updatedSubFolders.isNotEmpty()) {
                                    item.copy(
                                        subFolders = updatedSubFolders,
                                        size = updatedSubFolders.sumOf { it.size }
                                    )
                                } else {
                                    null
                                }
                            }
                            
                            // For other types, keep the item
                            else -> item
                        }
                    }
                    group.copy(items = updatedItems)
                }.filter { it.items.isNotEmpty() }

                withContext(Dispatchers.Main) {
                    _scanState.value = ScanState.Completed(updatedGroups)
                }
            }
            _isDeleting.value = false
        }
    }

    private fun loadConfig(): SimpleCleanerConfig {
        val yamlContent = this::class.java.classLoader
            .getResourceAsStream("config.yaml")
            ?.bufferedReader()
            ?.readText()
            ?: throw Exception("Config file not found")

        return Yaml.default.decodeFromString(SimpleCleanerConfig.serializer(), yamlContent)
    }

    private fun getPlatformList(config: SimpleCleanerConfig): List<CleanerGroup> {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> config.windowsList
            osName.contains("nux") || osName.contains("nix") -> config.linuxList
            osName.contains("mac") || osName.contains("darwin") -> config.macosList
            osName.contains("ios") -> config.iosList
            osName.contains("android") -> config.androidList
            else -> emptyList()
        }
    }
}

