package dev.projectgroups.directorygrouper.platform

import com.intellij.ide.RecentProjectListActionProvider
import com.intellij.ide.ReopenProjectAction
import com.intellij.ide.UpdatesInfoProviderManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.ProjectManager
import dev.projectgroups.directorygrouper.domain.PathKeyStrategy
import dev.projectgroups.directorygrouper.domain.ProjectEntry
import dev.projectgroups.directorygrouper.settings.DirectoryGrouperSettings
import java.nio.file.InvalidPathException
import java.nio.file.Path

data class ProjectWidgetContent(
    val updateActions: List<AnAction>,
    val widgetActions: List<AnAction>,
    val openProjectActions: List<AnAction>,
    val recentProjectEntries: List<ProjectEntry<AnAction>>,
    val branchNames: ProjectWidgetBranchNames = ProjectWidgetBranchNames.EMPTY,
)

fun interface ProjectWidgetSource {
    fun getContent(event: AnActionEvent): ProjectWidgetContent
}

class IntelliJProjectWidgetSource(
    private val entryMapper: RecentProjectEntryMapper = RecentProjectEntryMapper(),
    private val branchResolver: GitBranchFallbackResolver = GitBranchFallbackResolver(),
    private val fallbackEnabled: () -> Boolean = {
        DirectoryGrouperSettings.getInstance().state.autoLoadGitBranchesWhenMissing
    },
) : ProjectWidgetSource {
    override fun getContent(event: AnActionEvent): ProjectWidgetContent {
        val pathKeyStrategy = PathKeyStrategy.forCurrentHost()
        val openProjectKeys = ProjectManager.getInstance().openProjects
            .flatMap(::identityPaths)
            .mapNotNull { path -> path.toPathKey(pathKeyStrategy) }
            .toSet()
        val projectActions = RecentProjectListActionProvider.getInstance()
            .getActions(event.project)
            .take(MAX_PROJECT_ACTIONS)
        val (openProjects, recentProjects) = projectActions.partition { action ->
            action is ReopenProjectAction &&
                action.projectPath.toPathKey(pathKeyStrategy) in openProjectKeys
        }
        val widgetActions = (ActionManager.getInstance().getAction(PlatformActionIds.PROJECT_WIDGET_ACTIONS) as? ActionGroup)
            ?.let(::listOf)
            .orEmpty()

        return ProjectWidgetContent(
            updateActions = UpdatesInfoProviderManager.getInstance().getUpdateActions(),
            widgetActions = widgetActions,
            openProjectActions = openProjects,
            recentProjectEntries = entryMapper.map(recentProjects),
            branchNames = if (fallbackEnabled()) {
                branchResolver.resolve(projectActions)
            } else {
                ProjectWidgetBranchNames.EMPTY
            },
        )
    }

    private fun identityPaths(project: com.intellij.openapi.project.Project): List<String> = buildList {
        project.basePath?.let(::add)
        project.projectFilePath?.let(::add)
    }

    private fun String.toPathKey(pathKeyStrategy: PathKeyStrategy): String? = try {
        Path.of(this)
            .let(pathKeyStrategy::normalize)
            ?.let(pathKeyStrategy::key)
    } catch (_: InvalidPathException) {
        null
    }

    private companion object {
        const val MAX_PROJECT_ACTIONS = 100
    }
}
