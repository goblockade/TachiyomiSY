package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.category.interactor.SetSortModeForCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.library.model.FilterKey
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryFilterFlags
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class LibrarySettingsScreenModel(
    val preferences: BasePreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val setDisplayMode: SetDisplayMode = Injekt.get(),
    private val setSortModeForCategory: SetSortModeForCategory = Injekt.get(),
    private val categoryRepository: CategoryRepository = Injekt.get(),
    trackerManager: TrackerManager = Injekt.get(),
) : ScreenModel {

    val trackersFlow = trackerManager.loggedInTrackersFlow()
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
            initialValue = trackerManager.loggedInTrackers(),
        )

    // SY -->
    val grouping by libraryPreferences.groupLibraryBy.asState(screenModelScope)

    // SY <--
    fun toggleFilter(category: Category?, key: FilterKey) {
        if (category != null && libraryPreferences.categorizedFilterSettings.get()) {
            screenModelScope.launchIO {
                val cat = categoryRepository.get(category.id) ?: return@launchIO
                val newFlags = when (key) {
                    FilterKey.DOWNLOADED -> LibraryFilterFlags.toggleDownloaded(cat.flags)
                    FilterKey.UNREAD -> LibraryFilterFlags.toggleUnread(cat.flags)
                    FilterKey.STARTED -> LibraryFilterFlags.toggleStarted(cat.flags)
                    FilterKey.BOOKMARKED -> LibraryFilterFlags.toggleBookmarked(cat.flags)
                    FilterKey.COMPLETED -> LibraryFilterFlags.toggleCompleted(cat.flags)
                    FilterKey.INTERVAL_CUSTOM -> LibraryFilterFlags.toggleIntervalCustom(cat.flags)
                    FilterKey.LEWD -> LibraryFilterFlags.toggleLewd(cat.flags)
                }
                categoryRepository.updatePartial(CategoryUpdate(id = cat.id, flags = newFlags))
            }
        } else {
            val pref: Preference<TriState> = when (key) {
                FilterKey.DOWNLOADED -> libraryPreferences.filterDownloaded
                FilterKey.UNREAD -> libraryPreferences.filterUnread
                FilterKey.STARTED -> libraryPreferences.filterStarted
                FilterKey.BOOKMARKED -> libraryPreferences.filterBookmarked
                FilterKey.COMPLETED -> libraryPreferences.filterCompleted
                FilterKey.INTERVAL_CUSTOM -> libraryPreferences.filterIntervalCustom
                FilterKey.LEWD -> libraryPreferences.filterLewd
            }
            pref.getAndSet { it.next() }
        }
    }

    fun toggleTracker(id: Int) {
        libraryPreferences.filterTracking(id).getAndSet { it.next() }
    }

    fun toggleFilterMode(category: Category?) {
        if (category != null && libraryPreferences.categorizedFilterSettings.get()) {
            screenModelScope.launchIO {
                val cat = categoryRepository.get(category.id) ?: return@launchIO
                val newFlags = LibraryFilterFlags.setMode(
                    cat.flags,
                    !LibraryFilterFlags.getMode(cat.flags),
                )
                categoryRepository.updatePartial(CategoryUpdate(id = cat.id, flags = newFlags))
            }
        } else {
            libraryPreferences.filterMode.set(!libraryPreferences.filterMode.get())
        }
    }

    fun setDisplayMode(mode: LibraryDisplayMode) {
        setDisplayMode.await(mode)
    }

    fun setSort(category: Category?, mode: LibrarySort.Type, direction: LibrarySort.Direction) {
        screenModelScope.launchIO {
            setSortModeForCategory.await(category, mode, direction)
        }
    }

    // SY -->
    fun setGrouping(grouping: Int) {
        screenModelScope.launchIO {
            libraryPreferences.groupLibraryBy.set(grouping)
        }
    }
    // SY <--
}
