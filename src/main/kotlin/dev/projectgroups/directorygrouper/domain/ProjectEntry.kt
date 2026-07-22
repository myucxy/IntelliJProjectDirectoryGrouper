package dev.projectgroups.directorygrouper.domain

import java.nio.file.Path

data class ProjectEntry<T>(
    val stableId: String,
    val displayName: String,
    val localPath: Path?,
    val sourceIndex: Int,
    val lastOpenedAt: Long? = null,
    val payload: T? = null,
)

sealed interface GroupedEntry<out T> {
    data class Group<T>(
        val key: String,
        val name: String,
        val children: List<ProjectEntry<T>>,
    ) : GroupedEntry<T>

    data class Item<T>(val project: ProjectEntry<T>) : GroupedEntry<T>
}
