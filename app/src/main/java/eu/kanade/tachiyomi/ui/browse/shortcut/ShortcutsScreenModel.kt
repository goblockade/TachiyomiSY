package eu.kanade.tachiyomi.ui.browse.shortcut

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.ui.UiPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.source.interactor.DeleteSavedSearchById
import tachiyomi.domain.source.interactor.UpdateSavedSearchById
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.SavedSearchRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.random.Random

enum class ShortcutSortMode {
    Alphabetical,
    Date,
    Random,
}

enum class ShortcutGroupMode {
    Source,
    Ungrouped,
}

data class ShortcutItem(
    val savedSearch: SavedSearch,
    val sourceName: String,
)

data class ShortcutGroup(
    val sourceName: String,
    val items: ImmutableList<ShortcutItem>,
)

sealed interface ShortcutUiModel {
    data class Header(val sourceName: String) : ShortcutUiModel
    data class Item(val item: ShortcutItem) : ShortcutUiModel
}

class ShortcutsScreenModel(
    private val savedSearchRepository: SavedSearchRepository = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val deleteSavedSearchById: DeleteSavedSearchById = Injekt.get(),
    private val updateSavedSearchById: UpdateSavedSearchById = Injekt.get(),
    private val uiPreferences: UiPreferences = Injekt.get(),
) : StateScreenModel<ShortcutsScreenModel.State>(
    State(
        sortMode = ShortcutSortMode.entries.getOrElse(uiPreferences.shortcutsSortMode.get()) { ShortcutSortMode.Date },
        sortAscending = uiPreferences.shortcutsSortAscending.get(),
        groupMode = ShortcutGroupMode.entries.getOrElse(uiPreferences.shortcutsGroupMode.get()) { ShortcutGroupMode.Ungrouped },
    ),
) {

    data class State(
        val uiModels: ImmutableList<ShortcutUiModel> = persistentListOf(),
        val searchQuery: String? = null,
        val sortMode: ShortcutSortMode = ShortcutSortMode.Date,
        val sortAscending: Boolean = false,
        val groupMode: ShortcutGroupMode = ShortcutGroupMode.Ungrouped,
        val dialog: ShortcutDialog? = null,
    )

    sealed interface ShortcutDialog {
        data class Delete(val id: Long, val name: String) : ShortcutDialog
        data class Edit(val id: Long, val name: String) : ShortcutDialog
    }

    private val searchQueryFlow = MutableStateFlow<String?>(null)
    private val sortModeFlow = MutableStateFlow(state.value.sortMode)
    private val sortAscendingFlow = MutableStateFlow(state.value.sortAscending)
    private val groupModeFlow = MutableStateFlow(state.value.groupMode)

    init {
        combine(
            savedSearchRepository.getAllAsFlow(),
            searchQueryFlow,
            sortModeFlow,
            sortAscendingFlow,
            groupModeFlow,
        ) { savedSearches: List<SavedSearch>, query: String?, sort: ShortcutSortMode, ascending: Boolean, group: ShortcutGroupMode ->
            val items = savedSearches
                .map { savedSearch ->
                    val source = sourceManager.get(savedSearch.source)
                    ShortcutItem(
                        savedSearch = savedSearch,
                        sourceName = source?.name ?: savedSearch.source.toString(),
                    )
                }
                .let { list ->
                    if (query.isNullOrBlank()) {
                        list
                    } else {
                        list.filter {
                            it.savedSearch.name.contains(query, ignoreCase = true) ||
                                it.sourceName.contains(query, ignoreCase = true)
                        }
                    }
                }

            val sortFn: (List<ShortcutItem>) -> List<ShortcutItem> = { list ->
                when (sort) {
                    ShortcutSortMode.Date -> {
                        if (ascending) {
                            list.sortedBy { it.savedSearch.id }
                        } else {
                            list.sortedByDescending { it.savedSearch.id }
                        }
                    }
                    ShortcutSortMode.Alphabetical -> {
                        if (ascending) {
                            list.sortedBy { it.savedSearch.name }
                        } else {
                            list.sortedByDescending { it.savedSearch.name }
                        }
                    }
                    ShortcutSortMode.Random -> {
                        val seed = items.fold(0) { acc, item -> acc + item.savedSearch.id.toInt() }
                        list.shuffled(Random(seed))
                    }
                }
            }

            val uiModels = when (group) {
                ShortcutGroupMode.Ungrouped -> sortFn(items).map { ShortcutUiModel.Item(it) as ShortcutUiModel }
                ShortcutGroupMode.Source -> {
                    items.groupBy { it.sourceName }.toSortedMap()
                        .flatMap { (sourceName, groupItems) ->
                            listOf(ShortcutUiModel.Header(sourceName)) + sortFn(groupItems).map { ShortcutUiModel.Item(it) }
                        }
                }
            }.toImmutableList()

            State(uiModels = uiModels, searchQuery = query, sortMode = sort, sortAscending = ascending, groupMode = group)
        }
            .onEach { newState -> mutableState.value = newState }
            .launchIn(screenModelScope)
    }

    fun setSearchQuery(query: String?) {
        searchQueryFlow.value = query
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun setSortMode(mode: ShortcutSortMode) {
        sortModeFlow.value = mode
        uiPreferences.shortcutsSortMode.set(mode.ordinal)
    }

    fun setSortAscending(ascending: Boolean) {
        sortAscendingFlow.value = ascending
        uiPreferences.shortcutsSortAscending.set(ascending)
    }

    fun setGroupMode(mode: ShortcutGroupMode) {
        groupModeFlow.value = mode
        uiPreferences.shortcutsGroupMode.set(mode.ordinal)
    }

    fun requestDelete(item: ShortcutItem) {
        mutableState.value = state.value.copy(
            dialog = ShortcutDialog.Delete(item.savedSearch.id, item.savedSearch.name),
        )
    }

    fun requestEdit(item: ShortcutItem) {
        mutableState.value = state.value.copy(
            dialog = ShortcutDialog.Edit(item.savedSearch.id, item.savedSearch.name),
        )
    }

    fun confirmDelete() {
        val dialog = state.value.dialog as? ShortcutDialog.Delete ?: return
        screenModelScope.launch {
            deleteSavedSearchById.await(dialog.id)
            dismissDialog()
        }
    }

    fun confirmEdit(name: String) {
        val dialog = state.value.dialog as? ShortcutDialog.Edit ?: return
        if (name.isBlank()) return
        screenModelScope.launch {
            updateSavedSearchById.await(dialog.id, name)
            dismissDialog()
        }
    }

    fun dismissDialog() {
        mutableState.value = state.value.copy(dialog = null)
    }
}
