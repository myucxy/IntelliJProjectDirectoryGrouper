package dev.projectgroups.directorygrouper.domain

import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DirectoryGroupingEngineTest {
    private val engine = DirectoryGroupingEngine()
    private val windows = PathKeyStrategy.forHost(HostFileSystem.WINDOWS)
    private val macos = PathKeyStrategy.forHost(HostFileSystem.MACOS)
    private val linux = PathKeyStrategy.forHost(HostFileSystem.LINUX)

    @Test
    fun `empty input produces no entries`() {
        assertEquals(emptyList(), engine.group<String>(emptyList(), windows))
    }

    @Test
    fun `single project remains ungrouped`() {
        val result = engine.group(listOf(entry("D:\\work\\shop\\api", 0)), windows)

        assertEquals(1, result.size)
        assertIs<GroupedEntry.Item<String>>(result.single())
    }

    @Test
    fun `two projects under the same parent form a group`() {
        val result = engine.group(
            listOf(entry("D:\\work\\shop\\api", 0), entry("D:\\work\\shop\\web", 1)),
            windows,
        )

        val group = assertIs<GroupedEntry.Group<String>>(result.single())
        assertEquals("shop", group.name)
        assertEquals(listOf("api", "web"), group.children.map(ProjectEntry<String>::displayName))
    }

    @Test
    fun `groups and loose items retain their first source positions`() {
        val result = engine.group(
            listOf(
                entry("D:\\work\\alpha\\one", 0),
                entry("D:\\solo\\only", 1),
                entry("D:\\work\\beta\\one", 2),
                entry("D:\\work\\alpha\\two", 3),
                entry("D:\\work\\beta\\two", 4),
            ),
            windows,
        )

        assertEquals(
            listOf("group:alpha", "item:only", "group:beta"),
            result.map {
                when (it) {
                    is GroupedEntry.Group -> "group:${it.name}"
                    is GroupedEntry.Item -> "item:${it.project.displayName}"
                }
            },
        )
    }

    @Test
    fun `same directory names receive the shortest unique qualifier`() {
        val result = engine.group(
            listOf(
                entry("D:\\work-a\\shop\\api", 0),
                entry("D:\\work-a\\shop\\web", 1),
                entry("D:\\work-b\\shop\\api", 2),
                entry("D:\\work-b\\shop\\web", 3),
            ),
            windows,
        )

        assertEquals(
            listOf("shop \u2014 work-a", "shop \u2014 work-b"),
            result.filterIsInstance<GroupedEntry.Group<String>>().map(GroupedEntry.Group<String>::name),
        )
    }

    @Test
    fun `windows path keys ignore case`() {
        val result = engine.group(
            listOf(entry("D:\\Work\\Shop\\api", 0), entry("d:\\work\\shop\\web", 1)),
            windows,
        )

        assertIs<GroupedEntry.Group<String>>(result.single())
    }

    @Test
    fun `linux path keys preserve case`() {
        val result = engine.group(
            listOf(entry("C:\\Work\\Shop\\api", 0), entry("C:\\Work\\shop\\web", 1)),
            linux,
        )

        assertEquals(2, result.filterIsInstance<GroupedEntry.Item<String>>().size)
    }

    @Test
    fun `macos path keys ignore case`() {
        val result = engine.group(
            listOf(entry("C:\\Users\\Me\\Shop\\api", 0), entry("C:\\users\\me\\shop\\web", 1)),
            macos,
        )

        assertIs<GroupedEntry.Group<String>>(result.single())
    }

    @Test
    fun `UNC paths preserve server and share while grouping workspace children`() {
        val result = engine.group(
            listOf(
                entry("\\\\server\\share\\workspace\\api", 0),
                entry("\\\\server\\share\\workspace\\web", 1),
            ),
            windows,
        )

        val group = assertIs<GroupedEntry.Group<String>>(result.single())
        assertEquals("workspace", group.name)
        assertEquals(listOf("api", "web"), group.children.map(ProjectEntry<String>::displayName))
    }

    @Test
    fun `relative paths remain ungrouped`() {
        val relative = ProjectEntry<String>(
            stableId = "work/shop/api",
            displayName = "api",
            localPath = Path.of("work", "shop", "api"),
            sourceIndex = 0,
        )

        assertIs<GroupedEntry.Item<String>>(engine.group(listOf(relative), linux).single())
    }

    @Test
    fun `lexical dot segments are normalized`() {
        val result = engine.group(
            listOf(entry("D:\\work\\shop\\temp\\..\\api", 0), entry("D:\\work\\shop\\.\\web", 1)),
            windows,
        )

        assertIs<GroupedEntry.Group<String>>(result.single())
    }

    @Test
    fun `project directly below a filesystem root remains ungrouped`() {
        val result = engine.group(
            listOf(entry("D:\\api", 0), entry("D:\\web", 1)),
            windows,
        )

        assertEquals(2, result.filterIsInstance<GroupedEntry.Item<String>>().size)
    }

    @Test
    fun `entry without a local path remains visible`() {
        val remote = ProjectEntry<String>(
            stableId = "remote://host/project",
            displayName = "remote",
            localPath = null,
            sourceIndex = 1,
        )
        val result = engine.group(
            listOf(entry("D:\\work\\shop\\api", 0), remote, entry("D:\\work\\shop\\web", 2)),
            windows,
        )

        assertEquals(listOf("shop", "remote"), result.map {
            when (it) {
                is GroupedEntry.Group -> it.name
                is GroupedEntry.Item -> it.project.displayName
            }
        })
    }

    private fun entry(path: String, sourceIndex: Int): ProjectEntry<String> {
        val localPath = Path.of(path)
        return ProjectEntry(
            stableId = path,
            displayName = localPath.fileName.toString(),
            localPath = localPath,
            sourceIndex = sourceIndex,
        )
    }
}
