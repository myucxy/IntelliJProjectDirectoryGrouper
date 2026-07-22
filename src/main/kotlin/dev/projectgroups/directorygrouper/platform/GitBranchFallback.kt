package dev.projectgroups.directorygrouper.platform

import com.intellij.ide.ReopenProjectAction
import com.intellij.openapi.actionSystem.AnAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

class ProjectWidgetBranchNames internal constructor(
    private val fallbackNames: Map<AnAction, String> = emptyMap(),
    val loading: CompletableFuture<Void>? = null,
) {
    fun get(action: AnAction, platformBranchName: String?): String? =
        platformBranchName?.takeUnless(String::isBlank) ?: fallbackNames[action]

    companion object {
        val EMPTY = ProjectWidgetBranchNames()
    }
}

fun interface GitBranchReader {
    fun read(projectPath: String): String?
}

class GitHeadBranchReader : GitBranchReader {
    override fun read(projectPath: String): String? = try {
        val project = Path.of(projectPath)
        if (!Files.exists(project)) return null

        val startDirectory = if (Files.isDirectory(project)) project else project.parent ?: return null
        val gitDirectory = findGitDirectory(startDirectory) ?: return null
        parseHead(readFirstLine(gitDirectory.resolve(HEAD_FILE)) ?: return null)
    } catch (_: InvalidPathException) {
        null
    } catch (_: SecurityException) {
        null
    } catch (_: java.io.IOException) {
        null
    }

    private fun findGitDirectory(startDirectory: Path): Path? {
        var directory: Path? = startDirectory.toAbsolutePath().normalize()
        while (directory != null) {
            val marker = directory.resolve(GIT_MARKER)
            when {
                Files.isDirectory(marker) -> return marker
                Files.isRegularFile(marker) -> parseGitDirectoryFile(marker)?.let { return it }
            }
            directory = directory.parent
        }
        return null
    }

    private fun parseGitDirectoryFile(marker: Path): Path? {
        val line = readFirstLine(marker) ?: return null
        if (!line.startsWith(GIT_DIRECTORY_PREFIX, ignoreCase = true)) return null

        val value = line.substring(GIT_DIRECTORY_PREFIX.length).trim()
        if (value.isEmpty()) return null
        val path = Path.of(value)
        return (if (path.isAbsolute) path else marker.parent.resolve(path)).normalize()
    }

    private fun parseHead(head: String): String? {
        if (head.startsWith(REF_PREFIX)) {
            val reference = head.substring(REF_PREFIX.length).trim()
            return reference.removePrefix(LOCAL_BRANCH_PREFIX).takeUnless(String::isBlank)
        }
        return head.takeIf(::isCommitHash)?.take(DETACHED_COMMIT_LENGTH)
    }

    private fun readFirstLine(path: Path): String? =
        Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
            reader.readLine()?.trim()?.takeUnless(String::isBlank)
        }

    private fun isCommitHash(value: String): Boolean =
        value.length >= DETACHED_COMMIT_LENGTH && value.all { character -> character.isDigit() || character in 'a'..'f' }

    private companion object {
        const val GIT_MARKER = ".git"
        const val HEAD_FILE = "HEAD"
        const val GIT_DIRECTORY_PREFIX = "gitdir:"
        const val REF_PREFIX = "ref:"
        const val LOCAL_BRANCH_PREFIX = "refs/heads/"
        const val DETACHED_COMMIT_LENGTH = 8
    }
}

class GitBranchFallbackResolver(
    private val reader: GitBranchReader = GitHeadBranchReader(),
    private val executor: Executor = ForkJoinPool.commonPool(),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val cacheTtlMillis: Long = DEFAULT_CACHE_TTL_MILLIS,
) {
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val inFlight = ConcurrentHashMap<String, CompletableFuture<String?>>()

    fun resolve(actions: List<AnAction>): ProjectWidgetBranchNames {
        val fallbackNames = ConcurrentHashMap<AnAction, String>()
        val pending = actions.mapNotNull { action ->
            val projectAction = action as? ReopenProjectAction ?: return@mapNotNull null
            if (!projectAction.branchName.isNullOrBlank()) return@mapNotNull null

            resolve(projectAction.projectPath).thenAccept { branchName ->
                branchName?.let { fallbackNames[action] = it }
            }
        }
        val loading = pending.takeIf(List<*>::isNotEmpty)
            ?.let { futures -> CompletableFuture.allOf(*futures.toTypedArray()) }
        return ProjectWidgetBranchNames(fallbackNames, loading)
    }

    private fun resolve(projectPath: String): CompletableFuture<String?> {
        val cacheKey = projectPath.toCacheKey()
        cache[cacheKey]
            ?.takeIf { entry -> entry.validUntil >= currentTimeMillis() }
            ?.let { entry -> return CompletableFuture.completedFuture(entry.branchName) }

        val future = inFlight.computeIfAbsent(cacheKey) {
            CompletableFuture.supplyAsync(
                {
                    val branchName = runCatching { reader.read(projectPath) }
                        .getOrNull()
                        ?.takeUnless(String::isBlank)
                    cache[cacheKey] = CacheEntry(
                        branchName = branchName,
                        validUntil = currentTimeMillis() + cacheTtlMillis,
                    )
                    branchName
                },
                executor,
            )
        }
        future.whenComplete { _, _ -> inFlight.remove(cacheKey, future) }
        return future
    }

    private fun String.toCacheKey(): String = try {
        Path.of(this).toAbsolutePath().normalize().toString()
    } catch (_: InvalidPathException) {
        this
    }

    private data class CacheEntry(
        val branchName: String?,
        val validUntil: Long,
    )

    private companion object {
        const val DEFAULT_CACHE_TTL_MILLIS = 15_000L
    }
}
