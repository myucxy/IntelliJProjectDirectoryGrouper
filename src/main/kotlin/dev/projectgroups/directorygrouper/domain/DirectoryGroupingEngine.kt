package dev.projectgroups.directorygrouper.domain

import java.nio.file.Path

class DirectoryGroupingEngine {
    fun <T> group(
        entries: List<ProjectEntry<T>>,
        pathKeyStrategy: PathKeyStrategy,
    ): List<GroupedEntry<T>> {
        val orderedEntries = entries.withIndex()
            .sortedWith(compareBy<IndexedValue<ProjectEntry<T>>> { it.value.sourceIndex }.thenBy { it.index })
            .map(IndexedValue<ProjectEntry<T>>::value)

        val locatedEntries = orderedEntries.map { entry ->
            LocatedEntry(entry, parentDirectory(entry.localPath, pathKeyStrategy))
        }
        val membersByParent = linkedMapOf<String, MutableList<ProjectEntry<T>>>()
        val parentsByKey = linkedMapOf<String, ParentDirectory>()

        locatedEntries.forEach { located ->
            val parent = located.parent ?: return@forEach
            membersByParent.getOrPut(parent.key) { mutableListOf() }.add(located.entry)
            parentsByKey.putIfAbsent(parent.key, parent)
        }

        val groupedKeys = membersByParent
            .filterValues { it.size >= MINIMUM_GROUP_SIZE }
            .keys
        val groupNames = GroupNameDisambiguator(pathKeyStrategy).names(
            parentsByKey.filterKeys(groupedKeys::contains).values,
        )
        val emittedGroups = mutableSetOf<String>()

        return buildList {
            locatedEntries.forEach { located ->
                val parentKey = located.parent?.key
                if (parentKey == null || parentKey !in groupedKeys) {
                    add(GroupedEntry.Item(located.entry))
                } else if (emittedGroups.add(parentKey)) {
                    add(
                        GroupedEntry.Group(
                            key = parentKey,
                            name = checkNotNull(groupNames[parentKey]),
                            children = checkNotNull(membersByParent[parentKey]).toList(),
                        ),
                    )
                }
            }
        }
    }

    private fun parentDirectory(
        projectPath: Path?,
        pathKeyStrategy: PathKeyStrategy,
    ): ParentDirectory? {
        val normalizedProject = projectPath?.let(pathKeyStrategy::normalize) ?: return null
        val parent = normalizedProject.parent ?: return null
        if (parent.parent == null) return null
        return ParentDirectory(
            key = pathKeyStrategy.key(parent),
            path = parent,
        )
    }

    private data class LocatedEntry<T>(
        val entry: ProjectEntry<T>,
        val parent: ParentDirectory?,
    )

    private companion object {
        const val MINIMUM_GROUP_SIZE = 2
    }
}
