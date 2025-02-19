package com.example.noteon

data class Folder(
    val id: Long,
    val name: String,
    val description: String,
    val metadata: FolderMetadata = FolderMetadata(),
    val timestamp: Long = System.currentTimeMillis()
) {
    // Convenience getters for metadata properties
    val userId: String? get() = metadata.userId
    val isSynced: Boolean get() = metadata.syncStatus is SyncStatus.Synced

    // Helper methods for updating metadata
    fun withSyncStatus(newStatus: SyncStatus) = copy(
        metadata = metadata.copy(syncStatus = newStatus)
    )

    fun withUserId(newUserId: String?) = copy(
        metadata = metadata.copy(userId = newUserId)
    )
}

data class FolderMetadata(
    val userId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.NotSynced
)