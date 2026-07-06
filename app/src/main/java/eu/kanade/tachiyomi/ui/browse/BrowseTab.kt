package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.core.preference.asState
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsScreenModel
import eu.kanade.tachiyomi.ui.browse.extension.extensionsTab
import eu.kanade.tachiyomi.ui.browse.feed.feedTab
import eu.kanade.tachiyomi.ui.browse.migration.sources.migrateSourceTab
import eu.kanade.tachiyomi.ui.browse.shortcut.ShortcutsScreenModel
import eu.kanade.tachiyomi.ui.browse.shortcut.shortcutsTab
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.sourcesTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalSearchScreen())
    }

    private val switchToExtensionTabChannel = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToExtensionTabChannel.trySend(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        // SY -->
        val hideFeedTab by remember { Injekt.get<UiPreferences>().hideFeedTab.asState(scope) }
        val feedTabInFront by remember { Injekt.get<UiPreferences>().feedTabInFront.asState(scope) }
        val hideShortcutsTab by remember { Injekt.get<UiPreferences>().hideShortcutsTab.asState(scope) }
        val hideMigrateTab by remember { Injekt.get<UiPreferences>().hideMigrateTab.asState(scope) }
        // SY <--

        // SY -->
        val sourcesScreenModel = rememberScreenModel { SourcesScreenModel(smartSearchConfig = null) }
        val sourcesState by sourcesScreenModel.state.collectAsState()
        val extensionsScreenModel = rememberScreenModel { ExtensionsScreenModel() }
        val extensionsState by extensionsScreenModel.state.collectAsState()
        val shortcutsScreenModel = rememberScreenModel { ShortcutsScreenModel() }
        val shortcutsState by shortcutsScreenModel.state.collectAsState()
        // SY <--

        // SY -->
        val tabs = buildList {
            if (!hideFeedTab && feedTabInFront) {
                add(feedTab())
            }
            add(sourcesTab(screenModel = sourcesScreenModel))
            if (!hideShortcutsTab) {
                add(shortcutsTab(screenModel = shortcutsScreenModel))
            }
            add(extensionsTab(extensionsScreenModel))
            if (!hideMigrateTab) {
                add(migrateSourceTab())
            }
            if (!hideFeedTab && !feedTabInFront) {
                add(feedTab())
            }
        }.toImmutableList()
        // SY <--

        val state = rememberPagerState { tabs.size }

        // SY -->
        val searchQuery = when (tabs.getOrNull(state.currentPage)?.titleRes) {
            MR.strings.label_sources -> sourcesState.searchQuery
            SYMR.strings.shortcuts -> shortcutsState.searchQuery
            else -> extensionsState.searchQuery
        }
        val onChangeSearchQuery: (String?) -> Unit = { query ->
            when (tabs.getOrNull(state.currentPage)?.titleRes) {
                MR.strings.label_sources -> sourcesScreenModel.setSearchQuery(query)
                SYMR.strings.shortcuts -> shortcutsScreenModel.setSearchQuery(query)
                else -> extensionsScreenModel.search(query)
            }
        }
        // SY <--

        TabbedScreen(
            titleRes = MR.strings.browse,
            tabs = tabs,
            state = state,
            searchQuery = searchQuery,
            onChangeSearchQuery = onChangeSearchQuery,
        )
        LaunchedEffect(Unit) {
            switchToExtensionTabChannel.receiveAsFlow()
                .collectLatest { state.scrollToPage(/* SY --> */2/* SY <-- */) }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
