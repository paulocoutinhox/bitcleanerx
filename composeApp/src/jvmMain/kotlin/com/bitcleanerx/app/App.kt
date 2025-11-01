package com.bitcleanerx.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitcleanerx.app.ui.AboutScreen
import com.bitcleanerx.app.ui.CustomCleanerScreen
import com.bitcleanerx.app.ui.ResultsScreen
import com.bitcleanerx.app.ui.SimpleCleanerScreen
import com.bitcleanerx.app.viewmodel.CustomCleanerViewModel
import com.bitcleanerx.app.viewmodel.ResultsViewModel
import com.bitcleanerx.app.viewmodel.StatsRepositoryImpl
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class Screen {
    SIMPLE_CLEANER,
    CUSTOM_CLEANER,
    RESULTS,
    ABOUT
}

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        val resultsViewModel = viewModel { ResultsViewModel(StatsRepositoryImpl()) }
        val customCleanerViewModel = viewModel { CustomCleanerViewModel(resultsViewModel) }
        var selectedScreen by remember { mutableStateOf(Screen.SIMPLE_CLEANER) }
        var isDeleting by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight().width(160.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "BitCleanerX",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    NavigationRailItem(
                        icon = {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = { Text("Simple", style = MaterialTheme.typography.titleSmall) },
                        selected = selectedScreen == Screen.SIMPLE_CLEANER,
                        onClick = { selectedScreen = Screen.SIMPLE_CLEANER },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationRailItem(
                        icon = {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = { Text("Custom", style = MaterialTheme.typography.titleSmall) },
                        selected = selectedScreen == Screen.CUSTOM_CLEANER,
                        onClick = { selectedScreen = Screen.CUSTOM_CLEANER },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationRailItem(
                        icon = {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = { Text("Results", style = MaterialTheme.typography.titleSmall) },
                        selected = selectedScreen == Screen.RESULTS,
                        onClick = { selectedScreen = Screen.RESULTS },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationRailItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(28.dp)) },
                        label = { Text("About", style = MaterialTheme.typography.titleSmall) },
                        selected = selectedScreen == Screen.ABOUT,
                        onClick = { selectedScreen = Screen.ABOUT },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (selectedScreen) {
                        Screen.SIMPLE_CLEANER -> SimpleCleanerScreen(
                            resultsViewModel = resultsViewModel,
                            onDeletingChange = { isDeleting = it }
                        )

                        Screen.CUSTOM_CLEANER -> CustomCleanerScreen(
                            resultsViewModel = resultsViewModel,
                            viewModel = customCleanerViewModel,
                            onDeletingChange = { isDeleting = it }
                        )

                        Screen.RESULTS -> ResultsScreen(resultsViewModel = resultsViewModel)
                        Screen.ABOUT -> AboutScreen()
                    }
                }
            }

            if (isDeleting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Deleting...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}