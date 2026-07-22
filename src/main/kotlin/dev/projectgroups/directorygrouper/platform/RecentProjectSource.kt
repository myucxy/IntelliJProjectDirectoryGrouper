package dev.projectgroups.directorygrouper.platform

import com.intellij.ide.RecentProjectListActionProvider
import com.intellij.ide.ReopenProjectAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import dev.projectgroups.directorygrouper.domain.ProjectEntry
import java.nio.file.InvalidPathException
import java.nio.file.Path

interface RecentProjectSource {
    fun getEntries(currentProject: Project?): List<ProjectEntry<AnAction>>
}

class IntelliJRecentProjectSource : RecentProjectSource {
    private val entryMapper = RecentProjectEntryMapper()

    override fun getEntries(currentProject: Project?): List<ProjectEntry<AnAction>> =
        entryMapper.map(
            RecentProjectListActionProvider.getInstance()
                .getActionsWithoutGroups(true, currentProject),
        )
}

class RecentProjectEntryMapper {
    fun map(actions: List<AnAction>): List<ProjectEntry<AnAction>> =
        actions.mapIndexed(::toProjectEntry)

    private fun toProjectEntry(index: Int, action: AnAction): ProjectEntry<AnAction> {
        if (action !is ReopenProjectAction) {
            return ProjectEntry(
                stableId = "unmapped:${action.javaClass.name}:$index",
                displayName = action.templatePresentation.text.orEmpty(),
                localPath = null,
                sourceIndex = index,
                payload = action,
            )
        }

        val projectPath = action.projectPath
        return ProjectEntry(
            stableId = projectPath,
            displayName = action.projectNameToDisplay,
            localPath = projectPath.toLocalPathOrNull(),
            sourceIndex = index,
            lastOpenedAt = action.activationTimestamp,
            payload = action,
        )
    }

    private fun String.toLocalPathOrNull(): Path? = try {
        Path.of(this)
    } catch (_: InvalidPathException) {
        null
    }
}
