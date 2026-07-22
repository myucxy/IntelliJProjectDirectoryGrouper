package dev.projectgroups.directorygrouper.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Separator
import dev.projectgroups.directorygrouper.DirectoryGrouperBundle
import dev.projectgroups.directorygrouper.domain.DirectoryGroupingEngine
import dev.projectgroups.directorygrouper.domain.GroupedEntry
import dev.projectgroups.directorygrouper.domain.PathKeyStrategy
import dev.projectgroups.directorygrouper.domain.ProjectEntry

data class RecentProjectVisualSection(
    val key: String,
    val title: String,
    val actions: List<AnAction>,
    val useNeutralColor: Boolean = false,
)

data class FlatRecentProjectLayout(
    val actions: List<AnAction>,
    val visualSections: List<RecentProjectVisualSection>,
)

class FlatRecentProjectActionLayout(
    private val groupingEngine: DirectoryGroupingEngine = DirectoryGroupingEngine(),
    private val otherProjectsLabel: () -> String = {
        DirectoryGrouperBundle.message("projectWidget.otherProjects")
    },
) {
    fun create(
        entries: List<ProjectEntry<AnAction>>,
        pathKeyStrategy: PathKeyStrategy,
    ): FlatRecentProjectLayout {
        val visualSections = mutableListOf<RecentProjectVisualSection>()
        val otherActions = mutableListOf<AnAction>()
        val actions = buildList {
            groupingEngine.group(entries, pathKeyStrategy).forEach { entry ->
                when (entry) {
                    is GroupedEntry.Group -> {
                        val sectionActions = entry.children.mapNotNull(ProjectEntry<AnAction>::payload)
                        if (sectionActions.isNotEmpty()) {
                            add(Separator.create(entry.name))
                            addAll(sectionActions)
                            visualSections += RecentProjectVisualSection(
                                key = entry.key,
                                title = entry.name,
                                actions = sectionActions,
                            )
                        }
                    }

                    is GroupedEntry.Item -> entry.project.payload?.let(otherActions::add)
                }
            }

            if (otherActions.isNotEmpty()) {
                val title = otherProjectsLabel()
                add(Separator.create(title))
                addAll(otherActions)
                visualSections += RecentProjectVisualSection(
                    key = OTHER_SECTION_KEY,
                    title = title,
                    actions = otherActions.toList(),
                    useNeutralColor = true,
                )
            }
        }
        return FlatRecentProjectLayout(actions, visualSections)
    }

    private companion object {
        const val OTHER_SECTION_KEY = "recent-projects:other"
    }
}
