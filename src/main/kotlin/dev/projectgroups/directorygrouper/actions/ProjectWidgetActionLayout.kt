package dev.projectgroups.directorygrouper.actions

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Separator
import dev.projectgroups.directorygrouper.DirectoryGrouperBundle
import dev.projectgroups.directorygrouper.domain.DirectoryGroupingEngine
import dev.projectgroups.directorygrouper.domain.PathKeyStrategy
import dev.projectgroups.directorygrouper.platform.ProjectWidgetContent
import dev.projectgroups.directorygrouper.platform.ProjectWidgetBranchNames

data class ProjectWidgetLayout(
    val actions: List<AnAction>,
    val visualSections: List<RecentProjectVisualSection>,
    val branchNames: ProjectWidgetBranchNames = ProjectWidgetBranchNames.EMPTY,
) {
    fun createActionGroup(): DefaultActionGroup = DefaultActionGroup().apply {
        actions.forEach(::add)
    }
}

class ProjectWidgetActionLayout(
    groupingEngine: DirectoryGroupingEngine = DirectoryGroupingEngine(),
    private val openProjectsLabel: () -> String = {
        DirectoryGrouperBundle.message("projectWidget.openProjects")
    },
    otherProjectsLabel: () -> String = {
        DirectoryGrouperBundle.message("projectWidget.otherProjects")
    },
) {
    private val recentProjectLayout = FlatRecentProjectActionLayout(groupingEngine, otherProjectsLabel)

    fun createGroup(content: ProjectWidgetContent): DefaultActionGroup = createLayout(content).createActionGroup()

    fun createActions(content: ProjectWidgetContent): List<AnAction> = createLayout(content).actions

    fun createLayout(content: ProjectWidgetContent): ProjectWidgetLayout {
        val visualSections = mutableListOf<RecentProjectVisualSection>()
        val actions = buildList {
            if (content.updateActions.isNotEmpty()) {
                addAll(content.updateActions)
                add(Separator.getInstance())
            }

            addAll(content.widgetActions)

            if (content.openProjectActions.isNotEmpty()) {
                add(Separator.create(openProjectsLabel()))
                addAll(content.openProjectActions)
            }

            val recentProjects = recentProjectLayout.create(
                entries = content.recentProjectEntries,
                pathKeyStrategy = PathKeyStrategy.forCurrentHost(),
            )
            addAll(recentProjects.actions)
            visualSections += recentProjects.visualSections
        }
        return ProjectWidgetLayout(actions, visualSections, content.branchNames)
    }
}
