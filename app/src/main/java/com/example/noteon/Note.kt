package com.example.noteon

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val state: NoteState = NoteState.Active,
    val metadata: NoteMetadata = NoteMetadata(),
    val timestamp: Long = System.currentTimeMillis()
) {
    // Convenience methods for state
    fun isActive() = state.isActive()
    fun isFavorite() = state.isFavorite()
    fun isTrashed() = state.isTrashed()
    fun isNormal() = !isTrashed()

    // Convenience methods for metadata
    val folderId: Long get() = metadata.folderId
    val userId: String? get() = metadata.userId
    val isSynced: Boolean get() = metadata.syncStatus is SyncStatus.Synced

    // State transition methods
    fun withState(newState: NoteState) = copy(state = newState)
    fun favorite() = withState(NoteState.Favorite)
    fun unfavorite() = withState(NoteState.Active)
    fun moveToTrash() = withState(NoteState.Trash())
    fun restore() = withState(NoteState.Active)
}

sealed class NoteState {
    object Active : NoteState()
    object Favorite : NoteState()
    data class Trash(val deletedDate: Long = System.currentTimeMillis()) : NoteState()

    fun isActive() = this is Active
    fun isFavorite() = this is Favorite
    fun isTrashed() = this is Trash

    fun getAvailableOptions(): List<NoteOption> {
        return when (this) {
            is Active -> listOf(
                NoteOption.ToggleFavorite(false),
                NoteOption.MoveToFolder,
                NoteOption.MoveToTrash
            )
            is Favorite -> listOf(
                NoteOption.ToggleFavorite(true),
                NoteOption.MoveToFolder,
                NoteOption.MoveToTrash
            )
            is Trash -> listOf(
                NoteOption.Restore,
                NoteOption.DeletePermanently
            )
        }
    }

    companion object {
        // Helper for database conversion
        fun fromDatabaseValue(stateValue: Int, deletedDate: Long?): NoteState {
            return when (stateValue) {
                0 -> Active
                1 -> Favorite
                2 -> Trash(deletedDate ?: System.currentTimeMillis())
                else -> Active // Default to active for unknown states
            }
        }

        fun toDatabaseValue(state: NoteState): Int {
            return when (state) {
                is Active -> 0
                is Favorite -> 1
                is Trash -> 2
            }
        }
    }
}

sealed class NoteOption {
    data class ToggleFavorite(val currentlyFavorited: Boolean) : NoteOption()
    object MoveToFolder : NoteOption()
    object MoveToTrash : NoteOption()
    object AIOptions : NoteOption()
    object Restore : NoteOption()
    object DeletePermanently : NoteOption()

    fun getResourceString(): Int {
        return when (this) {
            is ToggleFavorite -> if (currentlyFavorited)
                R.string.remove_from_favorites else R.string.add_to_favorites
            is MoveToFolder -> R.string.move_to_folder
            is MoveToTrash -> R.string.move_to_trash
            is AIOptions -> R.string.ai_options
            is Restore -> R.string.restore
            is DeletePermanently -> R.string.delete_permanently
        }
    }
}

data class NoteMetadata(
    val folderId: Long = 0,
    val userId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.NotSynced
)

sealed class SyncStatus {
    object Synced : SyncStatus()
    object NotSynced : SyncStatus()
    data class SyncError(val error: String) : SyncStatus()
}