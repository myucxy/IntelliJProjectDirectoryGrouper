package dev.projectgroups.directorygrouper.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import dev.projectgroups.directorygrouper.domain.ProjectEntry
import dev.projectgroups.directorygrouper.platform.ProjectWidgetContent
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ProjectWidgetActionLayoutTest {
    @Test
    fun `layout shows recent project groups as flat sections and puts singletons in other`() {
        val update = NamedAction("update")
        val command = NamedAction("settings")
        val open = NamedAction("open")
        val api = NamedAction("api")
        val solo = NamedAction("solo")
        val web = NamedAction("web")
        val layout = ProjectWidgetActionLayout(
            openProjectsLabel = { "Open Projects" },
            otherProjectsLabel = { "Other" },
        ).createLayout(
            ProjectWidgetContent(
                updateActions = listOf(update),
                widgetActions = listOf(command),
                openProjectActions = listOf(open),
                recentProjectEntries = listOf(
                    entry("work/shop/api", 0, api),
                    entry("work/solo/only", 1, solo),
                    entry("work/shop/web", 2, web),
                ),
            ),
        )
        val actions = layout.actions

        assertSame(update, actions[0])
        assertIs<Separator>(actions[1])
        assertSame(command, actions[2])
        assertEquals("Open Projects", assertIs<Separator>(actions[3]).text)
        assertSame(open, actions[4])
        assertEquals("shop", assertIs<Separator>(actions[5]).text)
        assertSame(api, actions[6])
        assertSame(web, actions[7])
        assertEquals("Other", assertIs<Separator>(actions[8]).text)
        assertSame(solo, actions[9])

        assertEquals(2, layout.visualSections.size)
        assertEquals("shop", layout.visualSections[0].title)
        assertEquals(listOf(api, web), layout.visualSections[0].actions)
        assertFalse(layout.visualSections[0].useNeutralColor)
        assertEquals("Other", layout.visualSections[1].title)
        assertEquals(listOf(solo), layout.visualSections[1].actions)
        assertTrue(layout.visualSections[1].useNeutralColor)
    }

    private fun entry(relativePath: String, index: Int, action: AnAction): ProjectEntry<AnAction> {
        val path = Path.of(System.getProperty("java.io.tmpdir"))
            .toAbsolutePath()
            .normalize()
            .resolve(relativePath)
        return ProjectEntry(
            stableId = path.toString(),
            displayName = action.templatePresentation.text.orEmpty(),
            localPath = path,
            sourceIndex = index,
            payload = action,
        )
    }

    private class NamedAction(name: String) : AnAction(name) {
        override fun actionPerformed(event: AnActionEvent) = Unit
    }
}
