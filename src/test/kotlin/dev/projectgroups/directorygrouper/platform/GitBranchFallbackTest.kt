package dev.projectgroups.directorygrouper.platform

import com.intellij.ide.ReopenProjectAction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executor
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitBranchFallbackTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `reader finds branch in project git directory`() {
        val project = temporaryDirectory.resolve("shop")
        writeHead(project.resolve(".git"), "ref: refs/heads/feature/widget")

        assertEquals("feature/widget", GitHeadBranchReader().read(project.toString()))
    }

    @Test
    fun `reader finds repository from nested project directory`() {
        val repository = temporaryDirectory.resolve("workspace")
        val project = Files.createDirectories(repository.resolve("services/api"))
        writeHead(repository.resolve(".git"), "ref: refs/heads/develop")

        assertEquals("develop", GitHeadBranchReader().read(project.toString()))
    }

    @Test
    fun `reader supports git directory files used by worktrees`() {
        val project = Files.createDirectories(temporaryDirectory.resolve("worktree"))
        val gitDirectory = temporaryDirectory.resolve("git-data")
        writeHead(gitDirectory, "ref: refs/heads/release/262")
        Files.writeString(project.resolve(".git"), "gitdir: ../git-data\n")

        assertEquals("release/262", GitHeadBranchReader().read(project.toString()))
    }

    @Test
    fun `reader uses short commit for detached head`() {
        val project = temporaryDirectory.resolve("detached")
        writeHead(project.resolve(".git"), "0123456789abcdef0123456789abcdef01234567")

        assertEquals("01234567", GitHeadBranchReader().read(project.toString()))
    }

    @Test
    fun `resolver reads only missing branches and reuses cache`() {
        val missingPath = Files.createDirectories(temporaryDirectory.resolve("missing")).toString()
        val nativePath = Files.createDirectories(temporaryDirectory.resolve("native")).toString()
        val missingBranch = projectAction(missingPath, null)
        val nativeBranch = projectAction(nativePath, "main")
        val reads = mutableListOf<String>()
        val resolver = GitBranchFallbackResolver(
            reader = GitBranchReader { projectPath ->
                reads += projectPath
                "fallback"
            },
            executor = Executor(Runnable::run),
            currentTimeMillis = { 1L },
            cacheTtlMillis = 1_000L,
        )

        val first = resolver.resolve(listOf(missingBranch, nativeBranch))
        first.loading?.join()
        val second = resolver.resolve(listOf(missingBranch))
        second.loading?.join()

        assertEquals("fallback", first.get(missingBranch, missingBranch.branchName))
        assertEquals("main", first.get(nativeBranch, nativeBranch.branchName))
        assertEquals("fallback", second.get(missingBranch, missingBranch.branchName))
        assertEquals(listOf(missingPath), reads)
    }

    @Test
    fun `resolver creates no background work when platform branch exists`() {
        val projectPath = Files.createDirectories(temporaryDirectory.resolve("native-only")).toString()
        val action = projectAction(projectPath, "main")
        val resolver = GitBranchFallbackResolver(
            reader = GitBranchReader { error("reader must not be called") },
            executor = Executor(Runnable::run),
        )

        val names = resolver.resolve(listOf(action))

        assertNull(names.loading)
        assertEquals("main", names.get(action, action.branchName))
    }

    private fun writeHead(gitDirectory: Path, value: String) {
        Files.createDirectories(gitDirectory)
        Files.writeString(gitDirectory.resolve("HEAD"), "$value\n")
    }

    private fun projectAction(projectPath: String, branchName: String?): ReopenProjectAction =
        ReopenProjectAction(projectPath, "project", "project", branchName, null)
}
