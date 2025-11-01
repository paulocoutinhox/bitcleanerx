package com.bitcleanerx.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bitcleanerx.app.helper.FileHelper
import com.bitcleanerx.app.model.FileTreeNode
import com.bitcleanerx.app.viewmodel.CustomCleanerViewModel
import com.bitcleanerx.app.viewmodel.CustomScanState
import com.bitcleanerx.app.viewmodel.PieChartData
import com.bitcleanerx.app.viewmodel.ResultsViewModel
import javax.swing.JFileChooser

@Composable
fun CustomCleanerScreen(
    resultsViewModel: ResultsViewModel,
    viewModel: CustomCleanerViewModel,
    onDeletingChange: (Boolean) -> Unit = {}
) {
    val scanState by viewModel.scanState.collectAsState()
    val selectedPath by viewModel.selectedPath.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val selectedNode by viewModel.selectedNode.collectAsState()
    val expandedNodes by viewModel.expandedNodes.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var nodeToDelete by remember { mutableStateOf<FileTreeNode?>(null) }

    LaunchedEffect(isDeleting) {
        onDeletingChange(isDeleting)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        when (val state = scanState) {
            is CustomScanState.Idle -> {
                IdleState(
                    selectedPath = selectedPath,
                    onSelectFolder = { path -> viewModel.setSelectedPath(path) },
                    onStartClick = { viewModel.startScan(selectedPath) }
                )
            }

            is CustomScanState.Scanning -> {
                ScanningState(
                    currentPath = state.currentPath,
                    onCancelClick = { viewModel.cancelScan() }
                )
            }

            is CustomScanState.Completed -> {
                CompletedState(
                    rootNode = state.rootNode,
                    selectedNode = selectedNode,
                    expandedNodes = expandedNodes,
                    onSelectNode = { viewModel.selectNode(it) },
                    onToggleExpansion = { viewModel.toggleNodeExpansion(it) },
                    onOpenFolder = { viewModel.openFolder(it.path) },
                    onDeleteNode = { node ->
                        nodeToDelete = node
                        showDeleteDialog = true
                    },
                    onCancelScan = { viewModel.cancelScan() },
                    getPieChartData = { viewModel.getPieChartData(it) }
                )
            }
        }

        if (showDeleteDialog && nodeToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Folder") },
                text = {
                    Text(
                        "Are you sure you want to delete \"${nodeToDelete?.name}\"?\n\nSize: ${
                            FileHelper.formatSize(
                                nodeToDelete?.size ?: 0L
                            )
                        }"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            nodeToDelete?.let { viewModel.deleteNode(it) }
                            showDeleteDialog = false
                            nodeToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showDeleteDialog = false
                            nodeToDelete = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun IdleState(
    selectedPath: String,
    onSelectFolder: (String) -> Unit,
    onStartClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Custom Cleaner",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Select a folder to analyze its contents",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = selectedPath,
            onValueChange = onSelectFolder,
            modifier = Modifier.width(500.dp),
            label = { Text("Folder Path") },
            trailingIcon = {
                IconButton(onClick = {
                    val fileChooser = JFileChooser()
                    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    val result = fileChooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        onSelectFolder(fileChooser.selectedFile.absolutePath)
                    }
                }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Browse")
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .width(200.dp)
                .height(50.dp),
            enabled = selectedPath.isNotEmpty()
        ) {
            Text("Start Scan", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ScanningState(currentPath: String, onCancelClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Scanning...",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = currentPath,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier.width(200.dp)
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun CompletedState(
    rootNode: FileTreeNode,
    selectedNode: FileTreeNode?,
    expandedNodes: Set<String>,
    onSelectNode: (FileTreeNode) -> Unit,
    onToggleExpansion: (String) -> Unit,
    onOpenFolder: (FileTreeNode) -> Unit,
    onDeleteNode: (FileTreeNode) -> Unit,
    onCancelScan: () -> Unit,
    getPieChartData: (FileTreeNode) -> List<PieChartData>
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        ) {
            Text(
                text = "Folders",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCancelScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset / Choose New Folder")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        TreeNodeItem(
                            node = rootNode,
                            level = 0,
                            selectedNode = selectedNode,
                            expandedNodes = expandedNodes,
                            onNodeClick = onSelectNode,
                            onToggleExpansion = onToggleExpansion
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            if (selectedNode != null) {
                Text(
                    text = selectedNode!!.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = FileHelper.formatSize(selectedNode!!.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onOpenFolder(selectedNode!!) },
                        enabled = !selectedNode!!.isDeleted
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open")
                    }
                    Button(
                        onClick = { onDeleteNode(selectedNode!!) },
                        enabled = !selectedNode!!.isDeleted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PieChartView(
                            data = getPieChartData(selectedNode!!),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeNodeItem(
    node: FileTreeNode,
    level: Int,
    selectedNode: FileTreeNode?,
    expandedNodes: Set<String>,
    onNodeClick: (FileTreeNode) -> Unit,
    onToggleExpansion: (String) -> Unit
) {
    val expanded = expandedNodes.contains(node.path)
    val isSelected = selectedNode == node

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onNodeClick(node)
                    if (node.isDirectory && node.children.isNotEmpty() && !expanded) {
                        onToggleExpansion(node.path)
                    }
                }
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(vertical = 8.dp, horizontal = 8.dp)
                .padding(start = (level * 20).dp)
                .alpha(if (node.isDeleted) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isDirectory && node.children.isNotEmpty()) {
                IconButton(
                    onClick = { onToggleExpansion(node.path) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            Icon(
                imageVector = if (node.isDeleted) Icons.Default.DeleteOutline else Icons.Default.Folder,
                contentDescription = null,
                tint = if (node.isDeleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = FileHelper.formatSize(node.size),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (expanded && node.children.isNotEmpty()) {
            Column {
                node.children.filter { !it.isDeleted }.forEach { child ->
                    TreeNodeItem(
                        node = child,
                        level = level + 1,
                        selectedNode = selectedNode,
                        expandedNodes = expandedNodes,
                        onNodeClick = onNodeClick,
                        onToggleExpansion = onToggleExpansion
                    )
                }
            }
        }
    }
}

@Composable
private fun PieChartView(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No data to display",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
        ) {
            val total = data.sumOf { it.size }.toFloat()
            if (total == 0f) return@Canvas

            val radius = minOf(size.width, size.height) / 2f * 0.8f
            val center = Offset(size.width / 2f, size.height / 2f)

            var startAngle = -90f
            data.forEach { item ->
                val sweepAngle = (item.size / total) * 360f
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.3f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2f)
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(0.4f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(data) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(item.color, shape = RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = FileHelper.formatSize(item.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

