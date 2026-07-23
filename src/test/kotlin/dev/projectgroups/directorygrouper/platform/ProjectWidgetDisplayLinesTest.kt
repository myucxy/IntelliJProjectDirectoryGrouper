package dev.projectgroups.directorygrouper.platform

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ProjectWidgetDisplayLinesTest {
    @Test
    fun `branch remains separate from project path`() {
        assertEquals(
            ProjectWidgetDisplayDetails(
                textLines = listOf("shop", "provider/work", "C:\\work\\shop"),
                branchName = "main",
            ),
            projectWidgetDisplayDetails("shop", "provider/work", "C:\\work\\shop", "main"),
        )
    }

    @Test
    fun `branch remains separate when only provider path is present`() {
        assertEquals(
            ProjectWidgetDisplayDetails(
                textLines = listOf("shop", "provider/work"),
                branchName = "develop",
            ),
            projectWidgetDisplayDetails("shop", "provider/work", null, "develop"),
        )
    }

    @Test
    fun `branch remains separate when no path is displayed`() {
        assertEquals(
            ProjectWidgetDisplayDetails(
                textLines = listOf("shop"),
                branchName = "feature/widget",
            ),
            projectWidgetDisplayDetails("shop", null, null, "feature/widget"),
        )
    }

    @Test
    fun `missing or blank branch omits branch line`() {
        val expected = ProjectWidgetDisplayDetails(
            textLines = listOf("shop", "C:\\work\\shop"),
            branchName = null,
        )

        assertEquals(expected, projectWidgetDisplayDetails("shop", null, "C:\\work\\shop", null))
        assertEquals(expected, projectWidgetDisplayDetails("shop", null, "C:\\work\\shop", "  "))
    }
}
