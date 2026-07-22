package dev.projectgroups.directorygrouper.platform

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ProjectWidgetDisplayLinesTest {
    @Test
    fun `branch is appended to project path when present`() {
        assertEquals(
            listOf("shop", "provider/work", "C:\\work\\shop  Git: main"),
            projectWidgetDisplayLines("shop", "provider/work", "C:\\work\\shop", "main"),
        )
    }

    @Test
    fun `branch is appended to provider path when project path is absent`() {
        assertEquals(
            listOf("shop", "provider/work  Git: develop"),
            projectWidgetDisplayLines("shop", "provider/work", null, "develop"),
        )
    }

    @Test
    fun `branch is appended to project name when no path is displayed`() {
        assertEquals(
            listOf("shop  Git: feature/widget"),
            projectWidgetDisplayLines("shop", null, null, "feature/widget"),
        )
    }

    @Test
    fun `missing or blank branch leaves existing lines unchanged`() {
        val expected = listOf("shop", "C:\\work\\shop")

        assertEquals(expected, projectWidgetDisplayLines("shop", null, "C:\\work\\shop", null))
        assertEquals(expected, projectWidgetDisplayLines("shop", null, "C:\\work\\shop", "  "))
    }
}
