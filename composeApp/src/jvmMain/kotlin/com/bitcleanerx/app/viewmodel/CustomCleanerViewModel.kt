package com.bitcleanerx.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitcleanerx.app.helper.FileHelper
import com.bitcleanerx.app.model.FileTreeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class CustomScanState {
    object Idle : CustomScanState()
    data class Scanning(val currentPath: String) : CustomScanState()
    data class Completed(val rootNode: FileTreeNode) : CustomScanState()
}

data class PieChartData(
    val label: String,
    val size: Long,
    val color: androidx.compose.ui.graphics.Color
)

class CustomCleanerViewModel(
    private val resultsViewModel: ResultsViewModel
) : ViewModel() {

    private val _scanState = MutableStateFlow<CustomScanState>(CustomScanState.Idle)
    val scanState: StateFlow<CustomScanState> = _scanState

    private val _selectedNode = MutableStateFlow<FileTreeNode?>(null)
    val selectedNode: StateFlow<FileTreeNode?> = _selectedNode

    private val _selectedPath = MutableStateFlow("")
    val selectedPath: StateFlow<String> = _selectedPath

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    private val _expandedNodes = MutableStateFlow<Set<String>>(emptySet())
    val expandedNodes: StateFlow<Set<String>> = _expandedNodes

    private var scanJob: Job? = null

    fun setSelectedPath(path: String) {
        _selectedPath.value = path
    }

    fun startScan(rootPath: String) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                println("Starting custom scan...")
                val expandedPath = FileHelper.expandPath(rootPath)
                val file = File(expandedPath)
                println("Scanning directory: $expandedPath")

                if (!file.exists() || !file.isDirectory) {
                    println("Directory does not exist or is not a directory")
                    _scanState.value = CustomScanState.Idle
                    return@launch
                }

                val rootNode = scanDirectory(file)
                if (scanJob?.isActive == true) {
                    println("Scan completed. Total size: ${FileHelper.formatSize(rootNode.size)}")
                    _scanState.value = CustomScanState.Completed(rootNode)
                    _selectedNode.value = rootNode
                    // Expand root node by default
                    _expandedNodes.value = setOf(rootNode.path)
                }
            } catch (e: Exception) {
                println("Error during custom scan: ${e.message}")
                e.printStackTrace()
                _scanState.value = CustomScanState.Idle
            }
        }
    }

    private suspend fun scanDirectory(directory: File): FileTreeNode {
        return withContext(Dispatchers.IO) {
            _scanState.value = CustomScanState.Scanning(directory.absolutePath)
            println("Scanning: ${directory.absolutePath}")

            // Use optimized FileHelper to calculate total size (includes files)
            val totalSize = FileHelper.calculateDirectorySize(directory.absolutePath)

            val node = FileTreeNode(
                name = directory.name,
                path = directory.absolutePath,
                size = totalSize,
                isDirectory = true
            )

            try {
                // Only get subdirectories using optimized FileHelper function
                val subFolders = FileHelper.getSubFolders(directory.absolutePath)
                    .sortedBy { it.name.lowercase() }

                println("Found ${subFolders.size} subdirectories in ${directory.name}")

                for (subFolder in subFolders) {
                    if (scanJob?.isActive != true) {
                        println("Scan cancelled")
                        break
                    }

                    try {
                        val childNode = scanDirectory(subFolder)
                        node.children.add(childNode)
                    } catch (e: Exception) {
                        println("Error accessing: ${subFolder.absolutePath} - ${e.message}")
                    }
                }

                println("Completed: ${directory.name} - ${FileHelper.formatSize(node.size)}")
            } catch (e: Exception) {
                println("Error listing subdirectories in: ${directory.absolutePath} - ${e.message}")
            }

            node
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _scanState.value = CustomScanState.Idle
        _selectedNode.value = null
        _expandedNodes.value = emptySet()
    }

    fun selectNode(node: FileTreeNode) {
        _selectedNode.value = node
    }

    fun toggleNodeExpansion(nodePath: String) {
        val currentExpanded = _expandedNodes.value.toMutableSet()
        if (currentExpanded.contains(nodePath)) {
            currentExpanded.remove(nodePath)
        } else {
            currentExpanded.add(nodePath)
        }
        _expandedNodes.value = currentExpanded
    }

    fun isNodeExpanded(nodePath: String): Boolean {
        return _expandedNodes.value.contains(nodePath)
    }

    fun deleteNode(node: FileTreeNode) {
        viewModelScope.launch(Dispatchers.IO) {
            _isDeleting.value = true
            val deletedSize = node.size
            val success = FileHelper.deleteFileOrDirectory(node.path)
            if (success) {
                node.isDeleted = true
                node.size = 0

                if (deletedSize > 0) {
                    resultsViewModel.addCleanedSpace(deletedSize)
                }

                updateParentSizes(node, deletedSize)
                withContext(Dispatchers.Main) {
                    if (_scanState.value is CustomScanState.Completed) {
                        val rootNode = (_scanState.value as CustomScanState.Completed).rootNode
                        _scanState.value = CustomScanState.Completed(rootNode)
                    }
                }
            }
            _isDeleting.value = false
        }
    }

    private fun updateParentSizes(deletedNode: FileTreeNode, deletedSize: Long) {
        if (_scanState.value is CustomScanState.Completed) {
            val rootNode = (_scanState.value as CustomScanState.Completed).rootNode
            updateParentSizesRecursive(rootNode, deletedNode, deletedSize)
        }
    }

    private fun updateParentSizesRecursive(node: FileTreeNode, deletedNode: FileTreeNode, deletedSize: Long): Boolean {
        if (node == deletedNode) {
            return true
        }

        for (child in node.children) {
            if (updateParentSizesRecursive(child, deletedNode, deletedSize)) {
                node.size -= deletedSize
                return true
            }
        }

        return false
    }

    fun openFolder(path: String) {
        FileHelper.openInFileManager(path)
    }

    fun getPieChartData(node: FileTreeNode): List<PieChartData> {
        val colors = listOf(
            androidx.compose.ui.graphics.Color(0xFF2196F3),
            androidx.compose.ui.graphics.Color(0xFF4CAF50),
            androidx.compose.ui.graphics.Color(0xFFFFC107),
            androidx.compose.ui.graphics.Color(0xFFFF5722),
            androidx.compose.ui.graphics.Color(0xFF9C27B0),
            androidx.compose.ui.graphics.Color(0xFF00BCD4),
            androidx.compose.ui.graphics.Color(0xFF8BC34A),
            androidx.compose.ui.graphics.Color(0xFFFF9800),
            androidx.compose.ui.graphics.Color(0xFF673AB7),
            androidx.compose.ui.graphics.Color(0xFF009688)
        )

        val sortedChildren = node.children
            .filter { !it.isDeleted }
            .sortedByDescending { it.size }

        val top10 = sortedChildren.take(10)
        val others = sortedChildren.drop(10)

        val result = mutableListOf<PieChartData>()
        top10.forEachIndexed { index, child ->
            result.add(
                PieChartData(
                    label = child.name,
                    size = child.size,
                    color = colors[index % colors.size]
                )
            )
        }

        if (others.isNotEmpty()) {
            result.add(
                PieChartData(
                    label = "Others",
                    size = others.sumOf { it.size },
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            )
        }

        return result
    }
}

