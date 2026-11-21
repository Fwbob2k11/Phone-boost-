package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DroidDeckDatabase
import com.example.data.local.DroidDeckPreferences
import com.example.data.local.DroidDeckPreferencesStore
import com.example.data.local.LibraryEntry
import com.example.data.model.DeckAccentTheme
import com.example.data.model.DeckFilter
import com.example.data.model.DeckItem
import com.example.data.model.DeviceProfile
import com.example.data.model.HardwareTelemetry
import com.example.data.model.InstalledAppInfo
import com.example.data.repository.DeviceAppRepository
import com.example.data.telemetry.DeviceProfileProvider
import com.example.data.telemetry.TelemetryMonitor
import com.example.data.telemetry.ThermalTuningConfig
import com.example.service.GameSessionMonitorService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface DeckEvent {
    data class LaunchFailed(val appName: String) : DeckEvent
    data class StatusMessage(val messageType: DeckStatusType, val appName: String) : DeckEvent
    data class SessionGuardChanged(val enabled: Boolean) : DeckEvent
    data object NotificationPermissionDenied : DeckEvent
}

enum class DeckStatusType {
    PINNED,
    UNPINNED,
    HIDDEN,
    RESTORED
}

data class DroidDeckUiState(
    val isLoadingApps: Boolean = true,
    val isRefreshingTelemetry: Boolean = false,
    val deviceProfile: DeviceProfile = DeviceProfile(),
    val telemetry: HardwareTelemetry = HardwareTelemetry(),
    val selectedFilter: DeckFilter = DeckFilter.ALL,
    val searchQuery: String = "",
    val pickerSearchQuery: String = "",
    val allInstalledApps: List<InstalledAppInfo> = emptyList(),
    val pinnedPackageNames: Set<String> = emptySet(),
    val deckItems: List<DeckItem> = emptyList(),
    val filteredDeckItems: List<DeckItem> = emptyList(),
    val hiddenItems: List<DeckItem> = emptyList(),
    val spotlightItem: DeckItem? = null,
    val isDiagnosticsOpen: Boolean = false,
    val isPickerOpen: Boolean = false,
    val isHiddenSheetOpen: Boolean = false,
    val isCustomizeOpen: Boolean = false,
    val isSessionGuardEnabled: Boolean = true,
    val hasNotificationPermission: Boolean = true,
    val thermalLimitCelsius: Int = ThermalTuningConfig.DEFAULT_TEMP_C,
    val useFahrenheit: Boolean = false,
    val accentTheme: DeckAccentTheme = DeckAccentTheme.CYBER_CYAN,
    val showRamGauge: Boolean = true,
    val showBatteryGauge: Boolean = true,
    val showThermalGauge: Boolean = true,
    val showStorageGauge: Boolean = true
)

class DroidDeckViewModel(
    application: Application,
    private val repository: DeviceAppRepository,
    private val telemetryMonitor: TelemetryMonitor,
    private val preferencesStore: DroidDeckPreferencesStore
) : AndroidViewModel(application) {

    private val deviceProfile = DeviceProfileProvider(application).read()

    private val installedAppsFlow = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val isLoadingAppsFlow = MutableStateFlow(true)
    private val telemetryFlow = MutableStateFlow(HardwareTelemetry())
    private val isRefreshingTelemetryFlow = MutableStateFlow(false)
    private val selectedFilterFlow = MutableStateFlow(DeckFilter.ALL)
    private val searchQueryFlow = MutableStateFlow("")
    private val pickerSearchQueryFlow = MutableStateFlow("")
    private val selectedSpotlightPackageFlow = MutableStateFlow<String?>(null)
    private val isDiagnosticsOpenFlow = MutableStateFlow(false)
    private val isPickerOpenFlow = MutableStateFlow(false)
    private val isHiddenSheetOpenFlow = MutableStateFlow(false)
    private val isCustomizeOpenFlow = MutableStateFlow(false)
    private val hasNotificationPermissionFlow = MutableStateFlow(checkNotificationPermission())

    @Volatile
    private var latestThermalLimitCelsius: Int = ThermalTuningConfig.DEFAULT_TEMP_C

    @Volatile
    private var latestPreferences: DroidDeckPreferences = DroidDeckPreferences()

    private val _events = MutableSharedFlow<DeckEvent>()
    val events: SharedFlow<DeckEvent> = _events.asSharedFlow()

    private val filterStateFlow = combine(
        selectedFilterFlow,
        searchQueryFlow,
        pickerSearchQueryFlow,
        selectedSpotlightPackageFlow
    ) { filter, query, pickerQuery, spotlightPkg ->
        FilterParams(filter, query, pickerQuery, spotlightPkg)
    }

    private val sheetVisibilityFlow = combine(
        isDiagnosticsOpenFlow,
        isPickerOpenFlow,
        isHiddenSheetOpenFlow,
        isCustomizeOpenFlow,
        isLoadingAppsFlow
    ) { diagnostics, picker, hidden, customize, loadingApps ->
        SheetVisibilityFlags(
            isDiagnosticsOpen = diagnostics,
            isPickerOpen = picker,
            isHiddenSheetOpen = hidden,
            isCustomizeOpen = customize,
            isLoadingApps = loadingApps
        )
    }

    private val uiControlStateFlow = combine(
        sheetVisibilityFlow,
        preferencesStore.preferencesFlow,
        isRefreshingTelemetryFlow,
        hasNotificationPermissionFlow
    ) { sheet, prefs, refreshingTelemetry, hasNotifPerm ->
        latestPreferences = prefs
        latestThermalLimitCelsius = prefs.thermalLimitCelsius
        UiControlFlags(
            sheet = sheet,
            prefs = prefs,
            isRefreshingTelemetry = refreshingTelemetry,
            hasNotificationPermission = hasNotifPerm
        )
    }

    val uiState: StateFlow<DroidDeckUiState> = combine(
        installedAppsFlow,
        repository.libraryEntries,
        telemetryFlow,
        filterStateFlow,
        uiControlStateFlow
    ) { installedApps, entries, telemetry, filterParams, controls ->
        buildUiState(installedApps, entries, telemetry, filterParams, controls)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DroidDeckUiState(
            deviceProfile = deviceProfile,
            hasNotificationPermission = hasNotificationPermissionFlow.value
        )
    )

    init {
        rescanInstalledApps()
        startTelemetryPolling()
    }

    private fun startTelemetryPolling() {
        viewModelScope.launch {
            while (isActive) {
                telemetryFlow.value = telemetryMonitor.sample()
                val zone = ThermalTuningConfig.zoneForCelsius(latestThermalLimitCelsius)
                delay(zone.foregroundSampleMs)
            }
        }
    }

    fun refreshTelemetry() {
        viewModelScope.launch {
            isRefreshingTelemetryFlow.value = true
            telemetryFlow.value = telemetryMonitor.sample()
            delay(220L)
            isRefreshingTelemetryFlow.value = false
        }
    }

    fun rescanInstalledApps() {
        viewModelScope.launch {
            isLoadingAppsFlow.value = true
            installedAppsFlow.value = repository.scanLaunchableApps()
            isLoadingAppsFlow.value = false
        }
    }

    fun onResumeRefresh() {
        GameSessionMonitorService.stopSession(getApplication())
        hasNotificationPermissionFlow.value = checkNotificationPermission()
        refreshTelemetry()
        rescanInstalledApps()
    }

    fun setThermalLimitCelsius(tempC: Int) {
        val clamped = ThermalTuningConfig.clampCelsius(tempC)
        latestThermalLimitCelsius = clamped
        viewModelScope.launch {
            preferencesStore.setThermalLimitCelsius(clamped)
        }
    }

    fun toggleTemperatureUnit() {
        val next = !latestPreferences.useFahrenheit
        viewModelScope.launch {
            preferencesStore.setUseFahrenheit(next)
        }
    }

    fun setAccentTheme(theme: DeckAccentTheme) {
        viewModelScope.launch {
            preferencesStore.setAccentTheme(theme)
        }
    }

    fun setShowRamGauge(visible: Boolean) {
        viewModelScope.launch {
            preferencesStore.setShowRamGauge(visible)
        }
    }

    fun setShowBatteryGauge(visible: Boolean) {
        viewModelScope.launch {
            preferencesStore.setShowBatteryGauge(visible)
        }
    }

    fun setShowThermalGauge(visible: Boolean) {
        viewModelScope.launch {
            preferencesStore.setShowThermalGauge(visible)
        }
    }

    fun setShowStorageGauge(visible: Boolean) {
        viewModelScope.launch {
            preferencesStore.setShowStorageGauge(visible)
        }
    }

    fun resetCustomization() {
        latestThermalLimitCelsius = ThermalTuningConfig.DEFAULT_TEMP_C
        viewModelScope.launch {
            preferencesStore.resetCustomization()
        }
    }

    fun toggleSessionGuard() {
        val next = !latestPreferences.isSessionGuardEnabled
        viewModelScope.launch {
            preferencesStore.setSessionGuardEnabled(next)
            _events.emit(DeckEvent.SessionGuardChanged(next))
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        hasNotificationPermissionFlow.value = granted
        viewModelScope.launch {
            if (granted) {
                preferencesStore.setSessionGuardEnabled(true)
                _events.emit(DeckEvent.SessionGuardChanged(true))
            } else {
                _events.emit(DeckEvent.NotificationPermissionDenied)
            }
        }
    }

    fun setFilter(filter: DeckFilter) {
        selectedFilterFlow.value = filter
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun setPickerSearchQuery(query: String) {
        pickerSearchQueryFlow.value = query
    }

    fun setDiagnosticsOpen(open: Boolean) {
        isDiagnosticsOpenFlow.value = open
        if (open) refreshTelemetry()
    }

    fun setCustomizeOpen(open: Boolean) {
        isCustomizeOpenFlow.value = open
    }

    fun setPickerOpen(open: Boolean) {
        isPickerOpenFlow.value = open
        if (!open) pickerSearchQueryFlow.value = ""
    }

    fun setHiddenSheetOpen(open: Boolean) {
        isHiddenSheetOpenFlow.value = open
    }

    fun selectSpotlight(packageName: String) {
        selectedSpotlightPackageFlow.value = packageName
    }

    fun launchApp(item: DeckItem) {
        selectedSpotlightPackageFlow.value = item.packageName
        viewModelScope.launch {
            val shouldStartMonitor = latestPreferences.isSessionGuardEnabled && checkNotificationPermission()
            if (shouldStartMonitor) {
                GameSessionMonitorService.startSession(getApplication(), item.name)
            }
            val launched = repository.launchPackage(item.packageName)
            if (!launched) {
                if (shouldStartMonitor) {
                    GameSessionMonitorService.stopSession(getApplication())
                }
                _events.emit(DeckEvent.LaunchFailed(item.name))
            }
        }
    }

    fun toggleFavorite(item: DeckItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item.packageName, !item.isFavorite)
        }
    }

    fun togglePinned(packageName: String, appName: String, currentlyPinned: Boolean) {
        viewModelScope.launch {
            val nextPinned = !currentlyPinned
            repository.togglePin(packageName, nextPinned)
            _events.emit(
                DeckEvent.StatusMessage(
                    if (nextPinned) DeckStatusType.PINNED else DeckStatusType.UNPINNED,
                    appName
                )
            )
        }
    }

    fun hideFromDeck(item: DeckItem) {
        viewModelScope.launch {
            repository.setHidden(item.packageName, true)
            if (selectedSpotlightPackageFlow.value == item.packageName) {
                selectedSpotlightPackageFlow.value = null
            }
            _events.emit(DeckEvent.StatusMessage(DeckStatusType.HIDDEN, item.name))
        }
    }

    fun restoreToDeck(item: DeckItem) {
        viewModelScope.launch {
            repository.setHidden(item.packageName, false)
            _events.emit(DeckEvent.StatusMessage(DeckStatusType.RESTORED, item.name))
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildUiState(
        installedApps: List<InstalledAppInfo>,
        entries: List<LibraryEntry>,
        telemetry: HardwareTelemetry,
        filterParams: FilterParams,
        controls: UiControlFlags
    ): DroidDeckUiState {
        val entryMap = entries.associateBy { it.packageName }
        val pinnedSet = entries.filter { it.isPinned && !it.isHidden }.map { it.packageName }.toSet()

        val allMerged = installedApps.map { app ->
            val entry = entryMap[app.packageName]
            DeckItem(
                packageName = app.packageName,
                name = app.name,
                isDetectedGame = app.isDetectedGame,
                isPinned = entry?.isPinned ?: false,
                isFavorite = entry?.isFavorite ?: false,
                isHidden = entry?.isHidden ?: false,
                lastLaunchedAt = entry?.lastLaunchedAt ?: 0L,
                launchCount = entry?.launchCount ?: 0,
                iconBitmap = app.iconBitmap,
                accentColorArgb = app.accentColorArgb
            )
        }

        val visibleDeck = allMerged
            .filter { (it.isDetectedGame || it.isPinned || it.isFavorite) && !it.isHidden }
            .sortedWith(
                compareByDescending<DeckItem> { it.isFavorite }
                    .thenByDescending { it.lastLaunchedAt }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )

        val hiddenItems = allMerged
            .filter { it.isHidden }
            .sortedBy { it.name.lowercase() }

        val query = filterParams.searchQuery.trim()
        val filtered = visibleDeck.filter { item ->
            val matchesFilter = when (filterParams.selectedFilter) {
                DeckFilter.ALL -> true
                DeckFilter.FAVORITES -> item.isFavorite
                DeckFilter.GAMES -> item.isDetectedGame
                DeckFilter.PINNED -> item.isPinned
            }
            val matchesQuery = query.isEmpty() ||
                item.name.contains(query, ignoreCase = true) ||
                item.packageName.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }

        val spotlight = filterParams.spotlightPackage
            ?.let { pkg -> visibleDeck.firstOrNull { it.packageName == pkg } }
            ?: visibleDeck.maxByOrNull { it.lastLaunchedAt }?.takeIf { it.lastLaunchedAt > 0L }
            ?: visibleDeck.firstOrNull { it.isFavorite }
            ?: visibleDeck.firstOrNull()

        return DroidDeckUiState(
            isLoadingApps = controls.sheet.isLoadingApps,
            isRefreshingTelemetry = controls.isRefreshingTelemetry,
            deviceProfile = deviceProfile,
            telemetry = telemetry,
            selectedFilter = filterParams.selectedFilter,
            searchQuery = filterParams.searchQuery,
            pickerSearchQuery = filterParams.pickerSearchQuery,
            allInstalledApps = installedApps,
            pinnedPackageNames = pinnedSet,
            deckItems = visibleDeck,
            filteredDeckItems = filtered,
            hiddenItems = hiddenItems,
            spotlightItem = spotlight,
            isDiagnosticsOpen = controls.sheet.isDiagnosticsOpen,
            isPickerOpen = controls.sheet.isPickerOpen,
            isHiddenSheetOpen = controls.sheet.isHiddenSheetOpen,
            isCustomizeOpen = controls.sheet.isCustomizeOpen,
            isSessionGuardEnabled = controls.prefs.isSessionGuardEnabled,
            hasNotificationPermission = controls.hasNotificationPermission,
            thermalLimitCelsius = controls.prefs.thermalLimitCelsius,
            useFahrenheit = controls.prefs.useFahrenheit,
            accentTheme = controls.prefs.accentTheme,
            showRamGauge = controls.prefs.showRamGauge,
            showBatteryGauge = controls.prefs.showBatteryGauge,
            showThermalGauge = controls.prefs.showThermalGauge,
            showStorageGauge = controls.prefs.showStorageGauge
        )
    }

    private data class FilterParams(
        val selectedFilter: DeckFilter,
        val searchQuery: String,
        val pickerSearchQuery: String,
        val spotlightPackage: String?
    )

    private data class SheetVisibilityFlags(
        val isDiagnosticsOpen: Boolean,
        val isPickerOpen: Boolean,
        val isHiddenSheetOpen: Boolean,
        val isCustomizeOpen: Boolean,
        val isLoadingApps: Boolean
    )

    private data class UiControlFlags(
        val sheet: SheetVisibilityFlags,
        val prefs: DroidDeckPreferences,
        val isRefreshingTelemetry: Boolean,
        val hasNotificationPermission: Boolean
    )

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = DroidDeckDatabase.getInstance(application)
                    val repository = DeviceAppRepository(application, database.libraryDao())
                    val telemetryMonitor = TelemetryMonitor(application)
                    val preferencesStore = DroidDeckPreferencesStore(application)
                    return DroidDeckViewModel(
                        application = application,
                        repository = repository,
                        telemetryMonitor = telemetryMonitor,
                        preferencesStore = preferencesStore
                    ) as T
                }
            }
        }
    }
}
