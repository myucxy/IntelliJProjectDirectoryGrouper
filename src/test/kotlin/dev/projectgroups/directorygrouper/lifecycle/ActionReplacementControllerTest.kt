package dev.projectgroups.directorygrouper.lifecycle

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ActionReplacementControllerTest {
    @Test
    fun `install replaces the action and restore puts the original back`() {
        val original = EmptyGroup("original")
        val replacement = EmptyGroup("replacement")
        val registry = FakeActionRegistry(original)
        val controller = controller(registry, replacement)

        assertEquals(InstallResult.INSTALLED, controller.install())
        assertSame(replacement, registry.current)
        assertEquals(InstallResult.ALREADY_INSTALLED, controller.install())
        assertEquals(RestoreResult.RESTORED, controller.restore())
        assertSame(original, registry.current)
    }

    @Test
    fun `restore does not overwrite an action installed by another owner`() {
        val original = EmptyGroup("original")
        val replacement = EmptyGroup("replacement")
        val foreign = EmptyGroup("foreign")
        val registry = FakeActionRegistry(original)
        val controller = controller(registry, replacement)

        controller.install()
        registry.current = foreign

        assertEquals(RestoreResult.SKIPPED_DIFFERENT_CURRENT_ACTION, controller.restore())
        assertSame(foreign, registry.current)
    }

    @Test
    fun `missing action is not replaced`() {
        val registry = FakeActionRegistry(null)
        val controller = controller(registry, EmptyGroup("replacement"))

        assertEquals(InstallResult.ACTION_MISSING_OR_UNSUPPORTED_TYPE, controller.install())
        assertEquals(0, registry.replaceCount)
    }

    @Test
    fun `unsupported action type is not replaced`() {
        val registry = FakeActionRegistry(NamedAction())
        val controller = ActionReplacementController(
            actionId = ACTION_ID,
            actionRegistry = registry,
            wrapperFactory = { original ->
                (original as? ActionGroup)?.let { EmptyGroup("replacement") }
            },
        )

        assertEquals(InstallResult.ACTION_MISSING_OR_UNSUPPORTED_TYPE, controller.install())
        assertEquals(0, registry.replaceCount)
    }

    @Test
    fun `partial replacement failure rolls back to the original action`() {
        val original = EmptyGroup("original")
        val registry = FakeActionRegistry(original).apply {
            failAfterNextReplace = true
        }
        val controller = controller(registry, EmptyGroup("replacement"))

        assertFailsWith<IllegalStateException> { controller.install() }
        assertSame(original, registry.current)
        assertEquals(RestoreResult.NOT_INSTALLED, controller.restore())
    }

    private fun controller(
        registry: FakeActionRegistry,
        replacement: ActionGroup,
    ): ActionReplacementController = ActionReplacementController(
        actionId = ACTION_ID,
        actionRegistry = registry,
        wrapperFactory = { replacement },
    )

    private class FakeActionRegistry(initial: AnAction?) : ActionRegistry {
        var current: AnAction? = initial
        var replaceCount: Int = 0
        var failAfterNextReplace: Boolean = false

        override fun getAction(actionId: String): AnAction? = current

        override fun replaceAction(actionId: String, action: AnAction) {
            replaceCount++
            current = action
            if (failAfterNextReplace) {
                failAfterNextReplace = false
                throw IllegalStateException("simulated replacement failure")
            }
        }
    }

    private class EmptyGroup(name: String) : ActionGroup(name, false) {
        override fun getChildren(event: AnActionEvent?): Array<AnAction> = emptyArray()
    }

    private class NamedAction : AnAction() {
        override fun actionPerformed(event: AnActionEvent) = Unit
    }

    private companion object {
        const val ACTION_ID = "RecentProjectListGroup"
    }
}
