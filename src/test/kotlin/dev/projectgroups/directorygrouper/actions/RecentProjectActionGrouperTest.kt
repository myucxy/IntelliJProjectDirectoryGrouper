package dev.projectgroups.directorygrouper.actions

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.Project
import dev.projectgroups.directorygrouper.domain.PathKeyStrategy
import dev.projectgroups.directorygrouper.domain.ProjectEntry
import dev.projectgroups.directorygrouper.platform.RecentProjectSource
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RecentProjectActionGrouperTest {
    private val pathStrategy = PathKeyStrategy.forCurrentHost()
    private val testRoot = Path.of(System.getProperty("java.io.tmpdir"))
        .toAbsolutePath()
        .normalize()
        .resolve("directory-grouper-tests")

    @Test
    fun `file menu uses flat sections and puts singletons in other`() {
        val api = NamedAction("api")
        val command = NamedAction("command")
        val web = NamedAction("web")
        val result = RecentProjectActionGrouper().group(
            entries = listOf(
                entry(testRoot.resolve("work/shop/api"), 0, api),
                entry(null, 1, command),
                entry(testRoot.resolve("work/shop/web"), 2, web),
            ),
            pathKeyStrategy = pathStrategy,
        )

        assertEquals(5, result.size)
        assertEquals("shop", assertIs<Separator>(result[0]).text)
        assertSame(api, result[1])
        assertSame(web, result[2])
        assertEquals("Other", assertIs<Separator>(result[3]).text)
        assertSame(command, result[4])
    }

    @Test
    fun `disabled wrapper delegates without consulting the source`() {
        val nativeActions: Array<AnAction> = arrayOf(NamedAction("one"), NamedAction("two"))
        val delegate = StaticActionGroup(nativeActions)
        val source = object : RecentProjectSource {
            override fun getEntries(currentProject: Project?): List<ProjectEntry<AnAction>> {
                error("source must not be called while grouping is disabled")
            }
        }
        val wrapper = GroupedRecentProjectsActionGroup(
            delegate = delegate,
            source = source,
            groupingEnabled = { false },
        )

        assertContentEquals(nativeActions, wrapper.getChildren(null))
    }

    @Test
    fun `enabled wrapper passes the current project to the source`() {
        var receivedProject: Project? = null
        val action = NamedAction("only")
        val source = object : RecentProjectSource {
            override fun getEntries(currentProject: Project?): List<ProjectEntry<AnAction>> {
                receivedProject = currentProject
                return listOf(entry(testRoot.resolve("work/solo/only"), 0, action))
            }
        }
        val wrapper = GroupedRecentProjectsActionGroup(
            delegate = StaticActionGroup(emptyArray()),
            source = source,
            groupingEnabled = { true },
        )

        val actions = wrapper.getChildren(null)
        assertEquals("Other", assertIs<Separator>(actions[0]).text)
        assertSame(action, actions[1])
        assertEquals(null, receivedProject)
    }

    @Test
    fun `wrapper preserves the native popup group presentation`() {
        val delegate = StaticActionGroup(emptyArray(), popup = true).apply {
            setSearchable(false)
            templatePresentation.description = "Native recent projects"
        }
        val wrapper = GroupedRecentProjectsActionGroup(
            delegate = delegate,
            source = object : RecentProjectSource {
                override fun getEntries(currentProject: Project?): List<ProjectEntry<AnAction>> = emptyList()
            },
            groupingEnabled = { true },
        )

        assertTrue(wrapper.isPopup)
        assertFalse(wrapper.isSearchable)
        assertEquals("Recent", wrapper.templatePresentation.text)
        assertEquals("Native recent projects", wrapper.templatePresentation.description)
    }

    private fun entry(path: Path?, index: Int, action: AnAction): ProjectEntry<AnAction> = ProjectEntry(
        stableId = path?.toString() ?: "unmapped:$index",
        displayName = action.templatePresentation.text.orEmpty(),
        localPath = path,
        sourceIndex = index,
        payload = action,
    )

    private class NamedAction(name: String) : AnAction(name) {
        override fun actionPerformed(event: AnActionEvent) = Unit
    }

    private class StaticActionGroup(
        private val actions: Array<AnAction>,
        popup: Boolean = false,
    ) : ActionGroup("Recent", popup) {
        override fun getChildren(event: AnActionEvent?): Array<AnAction> = actions
    }
}
