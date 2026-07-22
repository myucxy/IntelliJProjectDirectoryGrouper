package dev.projectgroups.directorygrouper.lifecycle

import com.intellij.openapi.actionSystem.AnAction

internal interface ActionRegistry {
    fun getAction(actionId: String): AnAction?
    fun replaceAction(actionId: String, action: AnAction)
}

internal enum class InstallResult {
    INSTALLED,
    ALREADY_INSTALLED,
    ACTION_MISSING_OR_UNSUPPORTED_TYPE,
    REPLACED_BY_ANOTHER_OWNER,
}

internal enum class RestoreResult {
    RESTORED,
    NOT_INSTALLED,
    SKIPPED_DIFFERENT_CURRENT_ACTION,
}

internal class ActionReplacementController(
    private val actionId: String,
    private val actionRegistry: ActionRegistry,
    private val wrapperFactory: (AnAction) -> AnAction?,
) {
    private var originalAction: AnAction? = null
    private var replacementAction: AnAction? = null

    @Synchronized
    fun install(): InstallResult {
        if (replacementAction != null) return InstallResult.ALREADY_INSTALLED
        val original = actionRegistry.getAction(actionId)
            ?: return InstallResult.ACTION_MISSING_OR_UNSUPPORTED_TYPE
        val replacement = wrapperFactory(original)
            ?: return InstallResult.ACTION_MISSING_OR_UNSUPPORTED_TYPE

        originalAction = original
        replacementAction = replacement
        try {
            actionRegistry.replaceAction(actionId, replacement)
        } catch (error: Throwable) {
            rollbackPartialInstall(original, replacement, error)
            throw error
        }

        return if (actionRegistry.getAction(actionId) === replacement) {
            InstallResult.INSTALLED
        } else {
            InstallResult.REPLACED_BY_ANOTHER_OWNER
        }
    }

    @Synchronized
    fun restore(): RestoreResult {
        val original = originalAction ?: return RestoreResult.NOT_INSTALLED
        val replacement = replacementAction ?: return RestoreResult.NOT_INSTALLED
        originalAction = null
        replacementAction = null

        if (actionRegistry.getAction(actionId) !== replacement) {
            return RestoreResult.SKIPPED_DIFFERENT_CURRENT_ACTION
        }
        actionRegistry.replaceAction(actionId, original)
        return RestoreResult.RESTORED
    }

    private fun rollbackPartialInstall(
        original: AnAction,
        replacement: AnAction,
        installError: Throwable,
    ) {
        if (actionRegistry.getAction(actionId) === replacement) {
            try {
                actionRegistry.replaceAction(actionId, original)
            } catch (rollbackError: Throwable) {
                installError.addSuppressed(rollbackError)
            }
        }
        originalAction = null
        replacementAction = null
    }
}
