package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AppPickerBottomSheet
import com.example.ui.components.CustomizeBottomSheet
import com.example.ui.components.CyberArcadeBackdrop
import com.example.ui.components.DiagnosticsBottomSheet
import com.example.ui.components.EmptyHeroDeckCard
import com.example.ui.components.FilterAndSearchSection
import com.example.ui.components.FilterEmptyCard
import com.example.ui.components.GameDeckCard
import com.example.ui.components.HeroSpotlightCard
import com.example.ui.components.HiddenAppsBottomSheet
import com.example.ui.components.TelemetryStrip
import com.example.ui.components.TopConsoleHud
import com.example.ui.viewmodel.DeckEvent
import com.example.ui.viewmodel.DeckStatusType
import com.example.ui.viewmodel.DroidDeckUiState
import com.example.ui.viewmodel.DroidDeckViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DroidDeckScreen(
    uiState: DroidDeckUiState,
    viewModel: DroidDeckViewModel,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.onNotificationPermissionResult(granted)
        }
    )

    val handleSessionGuardClick = remember(uiState.hasNotificationPermission) {
        {
            if (!uiState.hasNotificationPermission &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.toggleSessionGuard()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            val msg = when (event) {
                is DeckEvent.LaunchFailed ->
                    context.getString(R.string.error_launch_unavailable, event.appName)
                is DeckEvent.SessionGuardChanged ->
                    context.getString(
                        if (event.enabled) {
                            R.string.msg_session_guard_enabled
                        } else {
                            R.string.msg_session_guard_disabled
                        }
                    )
                DeckEvent.NotificationPermissionDenied ->
                    context.getString(R.string.msg_notification_perm_denied)
                is DeckEvent.MemoryCleaned -> when {
                    event.freedRamMb > 0L -> context.getString(
                        R.string.msg_memory_cleaned_mb,
                        event.freedRamMb,
                        event.availableRamMb
                    )
                    event.clearedCacheKb > 0L -> context.getString(
                        R.string.msg_memory_cleaned_cache,
                        event.clearedCacheKb,
                        event.availableRamMb
                    )
                    else -> context.getString(
                        R.string.msg_memory_cleaned_trimmed,
                        event.availableRamMb
                    )
                }
                is DeckEvent.StatusMessage -> when (event.messageType) {
                    DeckStatusType.PINNED ->
                        context.getString(R.string.msg_pinned_added, event.appName)
                    DeckStatusType.UNPINNED ->
                        context.getString(R.string.msg_pinned_removed, event.appName)
                    DeckStatusType.HIDDEN ->
                        context.getString(R.string.msg_hidden_item, event.appName)
                    DeckStatusType.RESTORED ->
                        context.getString(R.string.msg_restored_item, event.appName)
                }
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = uiState.searchQuery.isNotEmpty()) {
        viewModel.setSearchQuery("")
    }

    CyberArcadeBackdrop(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                val isExpanded = maxWidth >= 720.dp

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TopConsoleHud(
                        isRefreshing = uiState.isRefreshingTelemetry,
                        onRefreshClick = {
                            viewModel.refreshTelemetry()
                            viewModel.rescanInstalledApps()
                        },
                        onCustomizeClick = { viewModel.setCustomizeOpen(true) },
                        onAddPinClick = { viewModel.setPickerOpen(true) }
                    )

                    if (isExpanded) {
                        ExpandedCockpitLayout(
                            uiState = uiState,
                            viewModel = viewModel,
                            onSessionGuardClick = handleSessionGuardClick,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        CompactConsoleLayout(
                            uiState = uiState,
                            viewModel = viewModel,
                            onSessionGuardClick = handleSessionGuardClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (uiState.isDiagnosticsOpen) {
        DiagnosticsBottomSheet(
            deviceProfile = uiState.deviceProfile,
            telemetry = uiState.telemetry,
            thermalLimitCelsius = uiState.thermalLimitCelsius,
            useFahrenheit = uiState.useFahrenheit,
            isRefreshing = uiState.isRefreshingTelemetry,
            isCleaningMemory = uiState.isCleaningMemory,
            onThermalLimitChange = { viewModel.setThermalLimitCelsius(it) },
            onToggleUnit = { viewModel.toggleTemperatureUnit() },
            onRefresh = { viewModel.refreshTelemetry() },
            onCleanMemory = { viewModel.cleanMemory() },
            onDismiss = { viewModel.setDiagnosticsOpen(false) }
        )
    }

    if (uiState.isPickerOpen) {
        AppPickerBottomSheet(
            installedApps = uiState.allInstalledApps,
            pinnedPackageNames = uiState.pinnedPackageNames,
            searchQuery = uiState.pickerSearchQuery,
            onSearchQueryChange = { viewModel.setPickerSearchQuery(it) },
            onTogglePin = { app, currentlyPinned ->
                viewModel.togglePinned(app.packageName, app.name, currentlyPinned)
            },
            onRescanClick = { viewModel.rescanInstalledApps() },
            onDismiss = { viewModel.setPickerOpen(false) }
        )
    }

    if (uiState.isHiddenSheetOpen) {
        HiddenAppsBottomSheet(
            hiddenItems = uiState.hiddenItems,
            onRestoreClick = { item ->
                viewModel.restoreToDeck(item)
                if (uiState.hiddenItems.size <= 1) {
                    viewModel.setHiddenSheetOpen(false)
                }
            },
            onDismiss = { viewModel.setHiddenSheetOpen(false) }
        )
    }

    if (uiState.isCustomizeOpen) {
        CustomizeBottomSheet(
            accentTheme = uiState.accentTheme,
            showRamGauge = uiState.showRamGauge,
            showBatteryGauge = uiState.showBatteryGauge,
            showThermalGauge = uiState.showThermalGauge,
            showStorageGauge = uiState.showStorageGauge,
            thermalLimitCelsius = uiState.thermalLimitCelsius,
            currentTempCelsius = uiState.telemetry.batteryTempCelsius,
            useFahrenheit = uiState.useFahrenheit,
            onAccentThemeSelected = { viewModel.setAccentTheme(it) },
            onToggleRamGauge = { viewModel.setShowRamGauge(it) },
            onToggleBatteryGauge = { viewModel.setShowBatteryGauge(it) },
            onToggleThermalGauge = { viewModel.setShowThermalGauge(it) },
            onToggleStorageGauge = { viewModel.setShowStorageGauge(it) },
            onThermalLimitChange = { viewModel.setThermalLimitCelsius(it) },
            onToggleUnit = { viewModel.toggleTemperatureUnit() },
            onResetDefaults = { viewModel.resetCustomization() },
            onDismiss = { viewModel.setCustomizeOpen(false) }
        )
    }
}

@Composable
private fun CompactConsoleLayout(
    uiState: DroidDeckUiState,
    viewModel: DroidDeckViewModel,
    onSessionGuardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TelemetryStrip(
            telemetry = uiState.telemetry,
            isSessionGuardEnabled = uiState.isSessionGuardEnabled,
            hasNotificationPermission = uiState.hasNotificationPermission,
            thermalLimitCelsius = uiState.thermalLimitCelsius,
            useFahrenheit = uiState.useFahrenheit,
            showRamGauge = uiState.showRamGauge,
            showBatteryGauge = uiState.showBatteryGauge,
            showThermalGauge = uiState.showThermalGauge,
            showStorageGauge = uiState.showStorageGauge,
            isCleaningMemory = uiState.isCleaningMemory,
            onOpenDiagnostics = { viewModel.setDiagnosticsOpen(true) },
            onSessionGuardClick = onSessionGuardClick,
            onCleanMemoryClick = { viewModel.cleanMemory() }
        )

        if (uiState.isLoadingApps && uiState.deckItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val spotlight = uiState.spotlightItem
                    if (spotlight != null) {
                        HeroSpotlightCard(
                            item = spotlight,
                            cpuLoadPercent = uiState.telemetry.cpuLoadPercent,
                            cpuLoadFraction = uiState.telemetry.cpuLoadFraction,
                            onLaunchClick = { viewModel.launchApp(spotlight) },
                            onToggleFavorite = { viewModel.toggleFavorite(spotlight) }
                        )
                    } else {
                        EmptyHeroDeckCard(
                            deviceProfile = uiState.deviceProfile,
                            cpuLoadPercent = uiState.telemetry.cpuLoadPercent,
                            cpuLoadFraction = uiState.telemetry.cpuLoadFraction,
                            onPinAppsClick = { viewModel.setPickerOpen(true) }
                        )
                    }
                }

                if (uiState.deckItems.isNotEmpty() || uiState.hiddenItems.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FilterAndSearchSection(
                            selectedFilter = uiState.selectedFilter,
                            searchQuery = uiState.searchQuery,
                            deckItems = uiState.deckItems,
                            hiddenCount = uiState.hiddenItems.size,
                            onFilterSelected = { viewModel.setFilter(it) },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onOpenHiddenSheet = { viewModel.setHiddenSheetOpen(true) }
                        )
                    }
                }

                if (uiState.filteredDeckItems.isEmpty() && uiState.deckItems.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FilterEmptyCard(
                            selectedFilter = uiState.selectedFilter,
                            searchQuery = uiState.searchQuery,
                            onPinAppsClick = { viewModel.setPickerOpen(true) }
                        )
                    }
                } else {
                    items(
                        items = uiState.filteredDeckItems,
                        key = { it.packageName }
                    ) { item ->
                        GameDeckCard(
                            item = item,
                            isSpotlighted = item.packageName == uiState.spotlightItem?.packageName,
                            onLaunchClick = { viewModel.launchApp(item) },
                            onSpotlightClick = { viewModel.selectSpotlight(item.packageName) },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onTogglePin = {
                                viewModel.togglePinned(item.packageName, item.name, item.isPinned)
                            },
                            onHideClick = { viewModel.hideFromDeck(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedCockpitLayout(
    uiState: DroidDeckUiState,
    viewModel: DroidDeckViewModel,
    onSessionGuardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryStrip(
                telemetry = uiState.telemetry,
                isSessionGuardEnabled = uiState.isSessionGuardEnabled,
                hasNotificationPermission = uiState.hasNotificationPermission,
                thermalLimitCelsius = uiState.thermalLimitCelsius,
                useFahrenheit = uiState.useFahrenheit,
                showRamGauge = uiState.showRamGauge,
                showBatteryGauge = uiState.showBatteryGauge,
                showThermalGauge = uiState.showThermalGauge,
                showStorageGauge = uiState.showStorageGauge,
                isCleaningMemory = uiState.isCleaningMemory,
                onOpenDiagnostics = { viewModel.setDiagnosticsOpen(true) },
                onSessionGuardClick = onSessionGuardClick,
                onCleanMemoryClick = { viewModel.cleanMemory() }
            )

            val spotlight = uiState.spotlightItem
            if (spotlight != null) {
                HeroSpotlightCard(
                    item = spotlight,
                    cpuLoadPercent = uiState.telemetry.cpuLoadPercent,
                    cpuLoadFraction = uiState.telemetry.cpuLoadFraction,
                    onLaunchClick = { viewModel.launchApp(spotlight) },
                    onToggleFavorite = { viewModel.toggleFavorite(spotlight) }
                )
            } else {
                EmptyHeroDeckCard(
                    deviceProfile = uiState.deviceProfile,
                    cpuLoadPercent = uiState.telemetry.cpuLoadPercent,
                    cpuLoadFraction = uiState.telemetry.cpuLoadFraction,
                    onPinAppsClick = { viewModel.setPickerOpen(true) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.deckItems.isNotEmpty() || uiState.hiddenItems.isNotEmpty()) {
                FilterAndSearchSection(
                    selectedFilter = uiState.selectedFilter,
                    searchQuery = uiState.searchQuery,
                    deckItems = uiState.deckItems,
                    hiddenCount = uiState.hiddenItems.size,
                    onFilterSelected = { viewModel.setFilter(it) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onOpenHiddenSheet = { viewModel.setHiddenSheetOpen(true) }
                )
            }

            if (uiState.isLoadingApps && uiState.deckItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.filteredDeckItems.isEmpty() && uiState.deckItems.isNotEmpty()) {
                FilterEmptyCard(
                    selectedFilter = uiState.selectedFilter,
                    searchQuery = uiState.searchQuery,
                    onPinAppsClick = { viewModel.setPickerOpen(true) }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = uiState.filteredDeckItems,
                        key = { it.packageName }
                    ) { item ->
                        GameDeckCard(
                            item = item,
                            isSpotlighted = item.packageName == uiState.spotlightItem?.packageName,
                            onLaunchClick = { viewModel.launchApp(item) },
                            onSpotlightClick = { viewModel.selectSpotlight(item.packageName) },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onTogglePin = {
                                viewModel.togglePinned(item.packageName, item.name, item.isPinned)
                            },
                            onHideClick = { viewModel.hideFromDeck(item) }
                        )
                    }
                }
            }
        }
    }
}
