package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DroidDeckDatabase
import com.example.data.local.LibraryEntry
import com.example.data.model.DeckFilter
import com.example.data.model.DeckItem
import com.example.data.model.DeviceProfile
import com.example.data.model.HardwareTelemetry
import com.example.data.model.InstalledAppInfo
import com.example.data.repository.DeviceAppRepository
import com.example.data.telemetry.DeviceProfileProvider
import com.example.data.telemetry.TelemetryMonitor
import com.example.data.telemetry.ThermalTuningConfig
import com.example.data.telemetry.ThermalTuningStore
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
    val isSessionGuardEnabled: Boolean = true,
    val hasNotificationPermission: Boolean = true,
    val thermalLimitCelsius: Int = ThermalTuningConfig.DEFAULT_TEMP_C,
    val useFahrenheit: Boolean = false
)

class DroidDeckViewModel(
    application: Application,
    private val repository: DeviceAppRepository,
    private val telemetryMonitor: TelemetryMonitor
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val tuningStore = ThermalTuningStore(application)
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
    private val isSessionGuardEnabledFlow = MutableStateFlow(
        prefs.getBoolean(KEY_SESSION_GUARD_ENABLED, true)
    )
    private val hasNotificationPermissionFlow = MutableStateFlow(checkNotificationPermission())
    private val thermalLimitCelsiusFlow = MutableStateFlow(tuningStore.getThermalLimitCelsius())
    private val useFahrenheitFlow = MutableStateFlow(tuningStore.isUseFahrenheit())

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
        isLoadingAppsFlow,
        isRefreshingTelemetryFlow
    ) { diagnostics, picker, hidden, loadingApps, refreshingTelemetry ->
        SheetVisibilityFlags(
            isDiagnosticsOpen = diagnostics,
            isPickerOpen = picker,
            isHiddenSheetOpen = hidden,
            isLoadingApps = loadingApps,
            isRefreshingTelemetry = refreshingTelemetry
        )
    }

    private val guardAndThermalFlow = combine(
        isSessionGuardEnabledFlow,
        hasNotificationPermissionFlow,
        thermalLimitCelsiusFlow,
        useFahrenheitFlow
    ) { guardEnabled, hasNotifPerm, thermalLimit, useFahrenheit ->
        GuardAndThermalFlags(
            isSessionGuardEnabled = guardEnabled,
            hasNotificationPermission = hasNotifPerm,
            thermalLimitCelsius = thermalLimit,
            useFahrenheit = useFahrenheit
        )
    }

    private val sheetAndGuardStateFlow = combine(
        sheetVisibilityFlow,
        guardAndThermalFlow
    ) { sheet, guard ->
        SheetAndGuardFlags(
            isDiagnosticsOpen = sheet.isDiagnosticsOpen,
            isPickerOpen = sheet.isPickerOpen,
            isHiddenSheetOpen = sheet.isHiddenSheetOpen,
            isLoadingApps = sheet.isLoadingApps,
            isRefreshingTelemetry = sheet.isRefreshingTelemetry,
            isSessionGuardEnabled = guard.isSessionGuardEnabled,
            hasNotificationPermission = guard.hasNotificationPermission,
            thermalLimitCelsius = guard.thermalLimitCelsius,
            useFahrenheit = guard.useFahrenheit
        )
    }

    val uiState: StateFlow<DroidDeckUiState> = combine(
        installedAppsFlow,
        repository.libraryEntries,
        telemetryFlow,
        filterStateFlow,
        sheetAndGuardStateFlow
    ) { installedApps, entries, telemetry, filterParams, flags ->
        buildUiState(installedApps, entries, telemetry, filterParams, flags)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DroidDeckUiState(
            deviceProfile = deviceProfile,
            isSessionGuardEnabled = isSessionGuardEnabledFlow.value,
            hasNotificationPermission = hasNotificationPermissionFlow.value,
            thermalLimitCelsius = thermalLimitCelsiusFlow.value,
            useFahrenheit = useFahrenheitFlow.value
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
                val zone = ThermalTuningConfig.zoneForCelsius(thermalLimitCelsiusFlow.value)
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
        thermalLimitCelsiusFlow.value = clamped
        tuningStore.setThermalLimitCelsius(clamped)
    }

    fun toggleTemperatureUnit() {
        val next = !useFahrenheitFlow.value
        useFahrenheitFlow.value = next
        tuningStore.setUseFahrenheit(next)
    }

    fun toggleSessionGuard() {
        val next = !isSessionGuardEnabledFlow.value
        isSessionGuardEnabledFlow.value = next
        prefs.edit().putBoolean(KEY_SESSION_GUARD_ENABLED, next).apply()
        viewModelScope.launch {
            _events.emit(DeckEvent.SessionGuardChanged(next))
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        hasNotificationPermissionFlow.value = granted
        if (granted) {
            isSessionGuardEnabledFlow.value = true
            prefs.edit().putBoolean(KEY_SESSION_GUARD_ENABLED, true).apply()
            viewModelScope.launch {
                _events.emit(DeckEvent.SessionGuardChanged(true))
            }
        } else {
            viewModelScope.launch {
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
            val shouldStartMonitor = isSessionGuardEnabledFlow.value && checkNotificationPermission()
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
        flags: SheetAndGuardFlags
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
            isLoadingApps = flags.isLoadingApps,
            isRefreshingTelemetry = flags.isRefreshingTelemetry,
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
            isDiagnosticsOpen = flags.isDiagnosticsOpen,
            isPickerOpen = flags.isPickerOpen,
            isHiddenSheetOpen = flags.isHiddenSheetOpen,
            isSessionGuardEnabled = flags.isSessionGuardEnabled,
            hasNotificationPermission = flags.hasNotificationPermission,
            thermalLimitCelsius = flags.thermalLimitCelsius,
            useFahrenheit = flags.useFahrenheit
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
        val isLoadingApps: Boolean,
        val isRefreshingTelemetry: Boolean
    )

    private data class GuardAndThermalFlags(
        val isSessionGuardEnabled: Boolean,
        val hasNotificationPermission: Boolean,
        val thermalLimitCelsius: Int,
        val useFahrenheit: Boolean
    )

    private data class SheetAndGuardFlags(
        val isDiagnosticsOpen: Boolean,
        val isPickerOpen: Boolean,
        val isHiddenSheetOpen: Boolean,
        val isLoadingApps: Boolean,
        val isRefreshingTelemetry: Boolean,
        val isSessionGuardEnabled: Boolean,
        val hasNotificationPermission: Boolean,
        val thermalLimitCelsius: Int,
        val useFahrenheit: Boolean
    )

    companion object {
        private const val PREFS_NAME = "droiddeck_prefs"
        private const val KEY_SESSION_GUARD_ENABLED = "session_guard_enabled"

        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = DroidDeckDatabase.getInstance(application)
                    val repository = DeviceAppRepository(application, database.libraryDao())
                    val telemetryMonitor = TelemetryMonitor(application)
                    return DroidDeckViewModel(application, repository, telemetryMonitor) as T
                }
            }
        }
    }
}
