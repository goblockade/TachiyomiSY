package eu.kanade.tachiyomi.ui.browse.shortcut

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.IconItem
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun Screen.shortcutsTab(
    screenModel: ShortcutsScreenModel = rememberScreenModel { ShortcutsScreenModel() },
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val state by screenModel.state.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }

    return TabContent(
        titleRes = SYMR.strings.shortcuts,
        searchEnabled = true,
        actions = persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = Icons.Outlined.FilterList,
                onClick = { showFilterDialog = true },
            ),
        ),
        content = { contentPadding, snackbarHostState ->
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.uiModels.isEmpty()) {
                    EmptyScreen(
                        stringRes = SYMR.strings.shortcuts_empty,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ScrollbarLazyColumn(
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = state.uiModels,
                            key = {
                                when (it) {
                                    is ShortcutUiModel.Header -> "header_${it.label}"
                                    is ShortcutUiModel.Item -> "item_${it.item.savedSearch.id}"
                                }
                            },
                        ) { uiModel ->
                            when (uiModel) {
                                is ShortcutUiModel.Header -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { screenModel.toggleGroup(uiModel.label) }
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = uiModel.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                        )
                                        Icon(
                                            imageVector = if (uiModel.isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = if (uiModel.isExpanded) "Collapse" else "Expand",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                is ShortcutUiModel.Item -> {
                                    ShortcutRow(
                                        item = uiModel.item,
                                        onClick = {
                                            navigator.push(
                                                BrowseSourceScreen(
                                                    sourceId = uiModel.item.savedSearch.source,
                                                    listingQuery = null,
                                                    savedSearch = uiModel.item.savedSearch.id,
                                                ),
                                            )
                                        },
                                        onEdit = { screenModel.requestEdit(uiModel.item) },
                                        onDelete = { screenModel.requestDelete(uiModel.item) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showFilterDialog) {
                FilterDialog(
                    sortMode = state.sortMode,
                    sortAscending = state.sortAscending,
                    groupMode = state.groupMode,
                    onSortModeChange = { screenModel.setSortMode(it) },
                    onSortAscendingChange = { screenModel.setSortAscending(it) },
                    onGroupModeChange = { screenModel.setGroupMode(it) },
                    onDismissRequest = { showFilterDialog = false },
                )
            }

            when (val dialog = state.dialog) {
                is ShortcutsScreenModel.ShortcutDialog.Delete -> {
                    AlertDialog(
                        onDismissRequest = screenModel::dismissDialog,
                        title = { Text(stringResource(SYMR.strings.shortcuts)) },
                        text = { Text(stringResource(SYMR.strings.delete_shortcut_confirmation)) },
                        confirmButton = {
                            TextButton(onClick = screenModel::confirmDelete) {
                                Text(stringResource(MR.strings.action_delete))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = screenModel::dismissDialog) {
                                Text(stringResource(MR.strings.action_cancel))
                            }
                        },
                    )
                }
                is ShortcutsScreenModel.ShortcutDialog.Edit -> {
                    var editName by remember { mutableStateOf(dialog.name) }
                    AlertDialog(
                        onDismissRequest = screenModel::dismissDialog,
                        title = { Text(stringResource(SYMR.strings.shortcuts)) },
                        text = {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                singleLine = true,
                                placeholder = { Text(stringResource(SYMR.strings.shortcut_name_hint)) },
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { screenModel.confirmEdit(editName) }) {
                                Text(stringResource(MR.strings.action_edit))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = screenModel::dismissDialog) {
                                Text(stringResource(MR.strings.action_cancel))
                            }
                        },
                    )
                }
                null -> {}
            }
        },
    )
}

@Composable
private fun FilterDialog(
    sortMode: ShortcutSortMode,
    sortAscending: Boolean,
    groupMode: ShortcutGroupMode,
    onSortModeChange: (ShortcutSortMode) -> Unit,
    onSortAscendingChange: (Boolean) -> Unit,
    onGroupModeChange: (ShortcutGroupMode) -> Unit,
    onDismissRequest: () -> Unit,
) {
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = persistentListOf(
            stringResource(MR.strings.action_sort),
            stringResource(SYMR.strings.shortcuts_group),
        ),
    ) { page ->
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> SortTab(
                    sortMode = sortMode,
                    sortAscending = sortAscending,
                    onSortModeChange = onSortModeChange,
                    onSortAscendingChange = onSortAscendingChange,
                )
                1 -> GroupTab(
                    groupMode = groupMode,
                    onGroupModeChange = onGroupModeChange,
                )
            }
        }
    }
}

@Composable
private fun SortTab(
    sortMode: ShortcutSortMode,
    sortAscending: Boolean,
    onSortModeChange: (ShortcutSortMode) -> Unit,
    onSortAscendingChange: (Boolean) -> Unit,
) {
    ShortcutSortMode.entries.forEach { mode ->
        val label = when (mode) {
            ShortcutSortMode.Alphabetical -> stringResource(SYMR.strings.shortcuts_sort_alphabetical)
            ShortcutSortMode.Date -> stringResource(SYMR.strings.shortcuts_sort_date)
            ShortcutSortMode.Random -> stringResource(SYMR.strings.shortcuts_sort_random)
        }
        if (mode == ShortcutSortMode.Random) {
            tachiyomi.presentation.core.components.BaseSortItem(
                label = label,
                icon = if (sortMode == ShortcutSortMode.Random) Icons.Default.Refresh else null,
                onClick = { onSortModeChange(mode) },
            )
        } else {
            SortItem(
                label = label,
                sortDescending = if (sortMode == mode) !sortAscending else null,
                onClick = {
                    if (sortMode == mode) {
                        onSortAscendingChange(!sortAscending)
                    } else {
                        onSortModeChange(mode)
                    }
                },
            )
        }
    }
}

@Composable
private fun GroupTab(
    groupMode: ShortcutGroupMode,
    onGroupModeChange: (ShortcutGroupMode) -> Unit,
) {
    IconItem(
        label = stringResource(SYMR.strings.shortcuts_group_source),
        icon = painterResource(R.drawable.ic_browse_filled_24dp),
        selected = groupMode == ShortcutGroupMode.Source,
        onClick = { onGroupModeChange(ShortcutGroupMode.Source) },
    )
    IconItem(
        label = stringResource(SYMR.strings.shortcuts_group_name),
        icon = Icons.AutoMirrored.Outlined.Label,
        onClick = { onGroupModeChange(ShortcutGroupMode.Name) },
    )
    IconItem(
        label = stringResource(SYMR.strings.shortcuts_group_ungrouped),
        icon = painterResource(R.drawable.ic_ungroup_24dp),
        selected = groupMode == ShortcutGroupMode.Ungrouped,
        onClick = { onGroupModeChange(ShortcutGroupMode.Ungrouped) },
    )
}

@Composable
private fun ShortcutRow(
    item: ShortcutItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.savedSearch.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.sourceName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val queryText = item.savedSearch.query
            if (!queryText.isNullOrBlank()) {
                Text(
                    text = queryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(MR.strings.action_edit),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(MR.strings.action_delete),
            )
        }
    }
}
