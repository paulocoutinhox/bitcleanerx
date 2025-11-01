package com.bitcleanerx.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.bitcleanerx.app.helper.FileHelper
import com.bitcleanerx.app.model.ItemType
import com.bitcleanerx.app.model.ScannedItem
import com.bitcleanerx.app.viewmodel.GroupedItems
import com.bitcleanerx.app.viewmodel.ResultsViewModel
import com.bitcleanerx.app.viewmodel.ScanState
import com.bitcleanerx.app.viewmodel.SimpleCleanerViewModel
import java.io.File

@Composable
fun SimpleCleanerScreen(
    resultsViewModel: ResultsViewModel,
    viewModel: SimpleCleanerViewModel = viewModel { SimpleCleanerViewModel(resultsViewModel) },
    onDeletingChange: (Boolean) -> Unit = {}
) {
    val scanState by viewModel.scanState.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<String?>(null) }
    var deleteDialogTitle by remember { mutableStateOf("") }
    var deleteDialogMessage by remember { mutableStateOf("") }

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
            is ScanState.Idle -> {
                IdleState(onStartClick = { viewModel.startScan() })
            }

            is ScanState.Scanning -> {
                ScanningState(currentPath = state.currentPath, onCancelClick = { viewModel.cancelScan() })
            }

            is ScanState.Completed -> {
                CompletedState(
                    groups = state.groups,
                    onDeleteClick = { path ->
                        itemToDelete = path
                        deleteDialogTitle = "Delete Item"
                        deleteDialogMessage = "Are you sure you want to delete this item?"
                        showDeleteDialog = true
                    },
                    onDeleteGroup = { groupName ->
                        itemToDelete = groupName
                        deleteDialogTitle = "Delete Group"
                        deleteDialogMessage = "Are you sure you want to delete all items in this group?"
                        showDeleteDialog = true
                    },
                    onRestartClick = { viewModel.cancelScan() }
                )
            }

            is ScanState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetryClick = { viewModel.startScan() },
                    onCancelClick = { viewModel.cancelScan() }
                )
            }
        }

        if (showDeleteDialog && itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(deleteDialogTitle) },
                text = { Text(deleteDialogMessage) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (deleteDialogTitle == "Delete Group") {
                                viewModel.deleteGroup(itemToDelete!!)
                            } else {
                                viewModel.deleteItem(itemToDelete!!)
                            }
                            showDeleteDialog = false
                            itemToDelete = null
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
                            itemToDelete = null
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
private fun IdleState(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Simple Cleaner",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Clean predefined system files and folders",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .width(200.dp)
                .height(50.dp)
        ) {
            Text("Start Scan", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.width(140.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onRetryClick,
                modifier = Modifier.width(140.dp)
            ) {
                Text("Retry")
            }
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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
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
    groups: List<GroupedItems>,
    onDeleteClick: (String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onRestartClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Scan Results",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val totalSize = groups.sumOf { group -> group.items.sumOf { it.size } }
                Text(
                    text = "Total: ${FileHelper.formatSize(totalSize)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            OutlinedButton(onClick = onRestartClick) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Scan")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groups.forEach { group ->
                    item {
                        GroupCard(
                            group = group,
                            onDeleteClick = onDeleteClick,
                            onDeleteGroup = onDeleteGroup
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: GroupedItems,
    onDeleteClick: (String) -> Unit,
    onDeleteGroup: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val groupTotalSize = group.items.sumOf { it.size }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    GroupIcon(groupImage = group.groupImage)

                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = group.groupName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${group.items.size} items - ${FileHelper.formatSize(groupTotalSize)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onDeleteGroup(group.groupName) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All")
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.padding(start = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    group.items.forEach { item ->
                        ScannedItemRow(item = item, onDeleteClick = onDeleteClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedItemRow(item: ScannedItem, onDeleteClick: (String) -> Unit) {
    var foldersExpanded by remember { mutableStateOf(true) }

    // Resolve specific folder names
    val displayName = remember(item.path, item.name) {
        if (item.type == ItemType.folder) {
            FileHelper.resolveFolderName(item.path, item.name)
        } else {
            item.name
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        when (item.type) {
            ItemType.file, ItemType.folder -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (item.type == ItemType.file) Icons.Default.Info else Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = FileHelper.formatSize(item.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(
                        onClick = { onDeleteClick(item.path) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            ItemType.folders -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Total: ${FileHelper.formatSize(item.size)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(
                        onClick = { foldersExpanded = !foldersExpanded },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (foldersExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (foldersExpanded && item.subFolders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 44.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item.subFolders.forEach { subFolder ->
                            val subFolderDisplayName = remember(subFolder.path, subFolder.name) {
                                FileHelper.resolveFolderName(subFolder.path, subFolder.name)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SubdirectoryArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = subFolderDisplayName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = FileHelper.formatSize(subFolder.size),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteClick(subFolder.path) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (subFolder != item.subFolders.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupIcon(groupImage: String) {
    val imageFile = File("composeResources/drawable/icons/${groupImage}.png")
    val imageExists = remember(groupImage) { imageFile.exists() }

    if (imageExists) {
        AsyncImage(
            model = imageFile,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
    } else {
        Icon(
            imageVector = getDefaultIconForGroup(groupImage),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

private fun getDefaultIconForGroup(groupImage: String): ImageVector {
    return Icons.Default.Folder
}

