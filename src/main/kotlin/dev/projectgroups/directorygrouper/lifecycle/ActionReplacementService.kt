package dev.projectgroups.directorygrouper.lifecycle

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetAction
import dev.projectgroups.directorygrouper.actions.GroupedProjectToolbarWidgetAction
import dev.projectgroups.directorygrouper.actions.GroupedRecentProjectsActionGroup
import dev.projectgroups.directorygrouper.platform.IntelliJRecentProjectSource
import dev.projectgroups.directorygrouper.platform.PlatformActionIds
import dev.projectgroups.directorygrouper.settings.DirectoryGrouperSettings

@Service(Service.Level.APP)
class ActionReplacementService : Disposable {
    private val actionRegistry = IntelliJActionRegistry(ActionManager.getInstance())
    private val recentProjectsController = ActionReplacementController(
        actionId = PlatformActionIds.RECENT_PROJECTS,
        actionRegistry = actionRegistry,
        wrapperFactory = { delegate ->
            (delegate as? ActionGroup)?.let { recentProjectsGroup ->
                GroupedRecentProjectsActionGroup(
                    delegate = recentProjectsGroup,
                    source = IntelliJRecentProjectSource(),
                    groupingEnabled = ::isGroupingEnabled,
                )
            }
        },
    )
    private val projectWidgetController = ActionReplacementController(
        actionId = PlatformActionIds.PROJECT_WIDGET,
        actionRegistry = actionRegistry,
        wrapperFactory = { delegate ->
            (delegate as? ProjectToolbarWidgetAction)?.let { projectWidget ->
                GroupedProjectToolbarWidgetAction(
                    delegate = projectWidget,
                    groupingEnabled = ::isGroupingEnabled,
                )
            }
        },
    )

    init {
        install(
            controller = recentProjectsController,
            targetName = "recent projects action",
        )
        install(
            controller = projectWidgetController,
            targetName = "project toolbar widget",
        )
    }

    override fun dispose() {
        restore(
            controller = projectWidgetController,
            targetName = "project toolbar widget",
        )
        restore(
            controller = recentProjectsController,
            targetName = "recent projects action",
        )
    }

    private fun install(
        controller: ActionReplacementController,
        targetName: String,
    ) {
        try {
            when (controller.install()) {
                InstallResult.INSTALLED,
                -> LOG.info("Installed directory grouping for $targetName")

                InstallResult.ALREADY_INSTALLED -> Unit

                InstallResult.ACTION_MISSING_OR_UNSUPPORTED_TYPE -> LOG.warn(
                    "Platform $targetName is unavailable or unsupported; this entry point will remain native",
                )

                InstallResult.REPLACED_BY_ANOTHER_OWNER -> LOG.warn(
                    "Platform $targetName changed during installation; this entry point will remain native",
                )
            }
        } catch (error: LinkageError) {
            LOG.error("Failed to install directory grouping for $targetName", error)
        } catch (error: RuntimeException) {
            LOG.error("Failed to install directory grouping for $targetName", error)
        }
    }

    private fun restore(
        controller: ActionReplacementController,
        targetName: String,
    ) {
        try {
            when (controller.restore()) {
                RestoreResult.RESTORED,
                -> LOG.info("Restored platform $targetName")

                RestoreResult.NOT_INSTALLED -> Unit

                RestoreResult.SKIPPED_DIFFERENT_CURRENT_ACTION -> LOG.warn(
                    "Platform $targetName was replaced by another owner; the plugin will not overwrite it during unload",
                )
            }
        } catch (error: LinkageError) {
            LOG.error("Failed to restore platform $targetName", error)
        } catch (error: RuntimeException) {
            LOG.error("Failed to restore platform $targetName", error)
        }
    }

    private fun isGroupingEnabled(): Boolean =
        DirectoryGrouperSettings.getInstance().state.autoGroupMenus

    private class IntelliJActionRegistry(
        private val actionManager: ActionManager,
    ) : ActionRegistry {
        override fun getAction(actionId: String): AnAction? = actionManager.getAction(actionId)

        override fun replaceAction(actionId: String, action: AnAction) {
            actionManager.replaceAction(actionId, action)
        }
    }

    private companion object {
        val LOG: Logger = Logger.getInstance(ActionReplacementService::class.java)
    }
}
