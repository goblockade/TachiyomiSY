package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.BrowseTabWrapper
import eu.kanade.presentation.util.Screen
import java.io.Serializable

class SourcesScreen(private val smartSearchConfig: SmartSearchConfig?) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SourcesScreenModel(smartSearchConfig = smartSearchConfig) }
        val state by screenModel.state.collectAsState()
        BrowseTabWrapper(
            tab = sourcesTab(screenModel = screenModel, smartSearchConfig = smartSearchConfig),
            onBackPressed = navigator::pop,
            searchQuery = state.searchQuery,
            onChangeSearchQuery = { screenModel.setSearchQuery(it) },
        )
    }

    data class SmartSearchConfig(val origTitle: String, val origMangaId: Long? = null) : Serializable
}
