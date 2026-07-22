package dev.projectgroups.directorygrouper.actions

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import dev.projectgroups.directorygrouper.domain.PathKeyStrategy
import dev.projectgroups.directorygrouper.platform.RecentProjectSource
import java.util.concurrent.atomic.AtomicBoolean

class GroupedRecentProjectsActionGroup(
    private val delegate: ActionGroup,
    private val source: RecentProjectSource,
    private val groupingEnabled: () -> Boolean,
    private val actionGrouper: RecentProjectActionGrouper = RecentProjectActionGrouper(),
) : ActionGroup(), DumbAware {
    private val compatibilityFailureLogged = AtomicBoolean()

    init {
        copyFrom(delegate)
        setPopup(delegate.isPopup)
        setSearchable(delegate.isSearchable)
    }

    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        if (!groupingEnabled()) return delegate.getChildren(event)

        return try {
            val groupedActions = actionGrouper.group(
                entries = source.getEntries(event?.project),
                pathKeyStrategy = PathKeyStrategy.forCurrentHost(),
            )
            groupedActions.ifEmpty { delegate.getChildren(event) }
        } catch (error: LinkageError) {
            if (compatibilityFailureLogged.compareAndSet(false, true)) {
                LOG.warn("Recent projects grouping is unavailable; using the platform action group", error)
            }
            delegate.getChildren(event)
        }
    }

    override fun update(event: AnActionEvent) {
        delegate.update(event)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = delegate.actionUpdateThread

    private companion object {
        val LOG: Logger = Logger.getInstance(GroupedRecentProjectsActionGroup::class.java)
    }
}
