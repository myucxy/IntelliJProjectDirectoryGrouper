package dev.projectgroups.directorygrouper.actions

import com.intellij.openapi.actionSystem.AnAction
import dev.projectgroups.directorygrouper.domain.PathKeyStrategy
import dev.projectgroups.directorygrouper.domain.ProjectEntry

class RecentProjectActionGrouper(
    private val layout: FlatRecentProjectActionLayout = FlatRecentProjectActionLayout(),
) {
    fun group(
        entries: List<ProjectEntry<AnAction>>,
        pathKeyStrategy: PathKeyStrategy,
    ): Array<AnAction> = layout.create(entries, pathKeyStrategy)
        .actions
        .toTypedArray()
}
