package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.repository.SavedSearchRepository

class UpdateSavedSearchById(
    private val savedSearchRepository: SavedSearchRepository,
) {

    suspend fun await(savedSearchId: Long, name: String) {
        savedSearchRepository.update(savedSearchId, name)
    }
}
