package dev.projectgroups.directorygrouper.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetAction
import dev.projectgroups.directorygrouper.platform.IntelliJProjectWidgetSource
import dev.projectgroups.directorygrouper.platform.ProjectWidgetPopupFactory
import dev.projectgroups.directorygrouper.platform.ProjectWidgetSource
import java.util.concurrent.atomic.AtomicBoolean

class GroupedProjectToolbarWidgetAction(
    private val delegate: ProjectToolbarWidgetAction,
    private val groupingEnabled: () -> Boolean,
    private val source: ProjectWidgetSource = IntelliJProjectWidgetSource(),
    private val layout: ProjectWidgetActionLayout = ProjectWidgetActionLayout(),
    private val popupFactory: ProjectWidgetPopupFactory = ProjectWidgetPopupFactory(),
) : ProjectToolbarWidgetAction() {
    private val compatibilityFailureLogged = AtomicBoolean()

    init {
        templatePresentation.copyFrom(delegate.templatePresentation)
    }

    override fun createPopup(event: AnActionEvent): JBPopup? {
        if (!groupingEnabled()) return delegate.createPopup(event)

        return try {
            val widgetLayout = layout.createLayout(source.getContent(event))
            if (widgetLayout.actions.isEmpty()) {
                null
            } else {
                event.project?.let { project ->
                    popupFactory.create(
                        project = project,
                        actionGroup = widgetLayout.createActionGroup(),
                        dataContext = event.dataContext,
                        visualSections = widgetLayout.visualSections,
                        branchNames = widgetLayout.branchNames,
                    )
                }
            }
        } catch (error: LinkageError) {
            if (compatibilityFailureLogged.compareAndSet(false, true)) {
                LOG.warn("Project widget grouping is unavailable; using the platform widget", error)
            }
            delegate.createPopup(event)
        }
    }

    private companion object {
        val LOG: Logger = Logger.getInstance(GroupedProjectToolbarWidgetAction::class.java)
    }
}
