package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DeckFilter
import com.example.data.model.DeckItem
import com.example.data.model.InstalledAppInfo
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.TelemetryLabelStyle

@Composable
fun FilterAndSearchSection(
    selectedFilter: DeckFilter,
    searchQuery: String,
    deckItems: List<DeckItem>,
    hiddenCount: Int,
    onFilterSelected: (DeckFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenHiddenSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allCount = deckItems.size
    val favCount = deckItems.count { it.isFavorite }
    val gameCount = deckItems.count { it.isDetectedGame }
    val pinnedCount = deckItems.count { it.isPinned }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.cd_search_deck),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_clear_search)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("deck_search_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeckFilterChip(
                label = "${stringResource(R.string.filter_all)} ($allCount)",
                selected = selectedFilter == DeckFilter.ALL,
                onClick = { onFilterSelected(DeckFilter.ALL) },
                tag = "filter_chip_all"
            )
            DeckFilterChip(
                label = "${stringResource(R.string.filter_favorites)} ($favCount)",
                selected = selectedFilter == DeckFilter.FAVORITES,
                onClick = { onFilterSelected(DeckFilter.FAVORITES) },
                tag = "filter_chip_favorites"
            )
            DeckFilterChip(
                label = "${stringResource(R.string.filter_games)} ($gameCount)",
                selected = selectedFilter == DeckFilter.GAMES,
                onClick = { onFilterSelected(DeckFilter.GAMES) },
                tag = "filter_chip_games"
            )
            DeckFilterChip(
                label = "${stringResource(R.string.filter_pinned)} ($pinnedCount)",
                selected = selectedFilter == DeckFilter.PINNED,
                onClick = { onFilterSelected(DeckFilter.PINNED) },
                tag = "filter_chip_pinned"
            )

            if (hiddenCount > 0) {
                FilterChip(
                    selected = false,
                    onClick = onOpenHiddenSheet,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.action_show_hidden, hiddenCount),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.testTag("open_hidden_apps_chip")
                )
            }
        }
    }
}

@Composable
private fun DeckFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
fun GameDeckCard(
    item: DeckItem,
    isSpotlighted: Boolean,
    onLaunchClick: () -> Unit,
    onSpotlightClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onHideClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cyan = MaterialTheme.colorScheme.primary
    val accent = resolveAccentColor(item.accentColorArgb, cyan)
    val interactionSource = remember { MutableInteractionSource() }

    val borderColor = if (isSpotlighted) {
        cyan.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .springPressScale(interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onLaunchClick
            )
            .testTag("game_card_${item.packageName}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isSpotlighted) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSpotlighted) 1.4.dp else 1.dp,
            color = borderColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .iconTintedGlow(accentColor = accent, isSpotlighted = isSpotlighted)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    AppIconBadge(
                        iconBitmap = item.iconBitmap,
                        sizeDp = 50,
                        borderColor = accent
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("favorite_toggle_${item.packageName}")
                        ) {
                            Icon(
                                imageVector = if (item.isFavorite) {
                                    Icons.Rounded.Star
                                } else {
                                    Icons.Rounded.StarOutline
                                },
                                contentDescription = stringResource(R.string.cd_favorite_toggle),
                                tint = if (item.isFavorite) {
                                    AmberWarm
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("card_options_${item.packageName}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(R.string.cd_card_options),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_launch_app)) },
                                    onClick = {
                                        menuExpanded = false
                                        onLaunchClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_set_spotlight)) },
                                    onClick = {
                                        menuExpanded = false
                                        onSpotlightClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (item.isFavorite) {
                                                    R.string.action_unfavorite
                                                } else {
                                                    R.string.action_favorite
                                                }
                                            )
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleFavorite()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (item.isPinned) {
                                                    R.string.action_unpin
                                                } else {
                                                    R.string.action_pin
                                                }
                                            )
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onTogglePin()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_hide)) },
                                    onClick = {
                                        menuExpanded = false
                                        onHideClick()
                                    }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.isDetectedGame) {
                            Surface(
                                color = accent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_game),
                                    style = TelemetryLabelStyle,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (item.isPinned) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.badge_pinned),
                                        style = TelemetryLabelStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (isSpotlighted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(cyan.copy(alpha = 0.75f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterEmptyCard(
    selectedFilter: DeckFilter,
    searchQuery: String,
    onPinAppsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when {
        searchQuery.isNotBlank() -> stringResource(R.string.empty_filter_search)
        selectedFilter == DeckFilter.FAVORITES -> stringResource(R.string.empty_filter_favorites)
        selectedFilter == DeckFilter.PINNED -> stringResource(R.string.empty_filter_pinned)
        else -> stringResource(R.string.empty_filter_games)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.empty_filter_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedFilter == DeckFilter.PINNED || selectedFilter == DeckFilter.GAMES) {
                OutlinedButton(
                    onClick = onPinAppsClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.hero_select_cta))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerBottomSheet(
    installedApps: List<InstalledAppInfo>,
    pinnedPackageNames: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onTogglePin: (InstalledAppInfo, Boolean) -> Unit,
    onRescanClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filteredApps = remember(installedApps, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) installedApps
        else installedApps.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.packageName.contains(q, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .testTag("app_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.picker_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(R.string.picker_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onRescanClick,
                    modifier = Modifier.testTag("picker_rescan_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.picker_rescan))
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_apps_placeholder)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("picker_search_input")
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    val isPinned = app.packageName in pinnedPackageNames
                    val accent = resolveAccentColor(app.accentColorArgb, MaterialTheme.colorScheme.primary)
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isPinned) accent else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onTogglePin(app, isPinned) }
                            .testTag("picker_app_row_${app.packageName}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIconBadge(
                                iconBitmap = app.iconBitmap,
                                sizeDp = 40,
                                borderColor = accent
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (app.isDetectedGame) {
                                        Surface(
                                            color = accent.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.badge_game),
                                                style = TelemetryLabelStyle,
                                                color = accent,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = isPinned,
                                onCheckedChange = { onTogglePin(app, isPinned) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppsBottomSheet(
    hiddenItems: List<DeckItem>,
    onRestoreClick: (DeckItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.hidden_sheet_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.hidden_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = hiddenItems,
                    key = { it.packageName }
                ) { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIconBadge(iconBitmap = item.iconBitmap, sizeDp = 40)
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(onClick = { onRestoreClick(item) }) {
                                Text(stringResource(R.string.action_unhide))
                            }
                        }
                    }
                }
            }
        }
    }
}
