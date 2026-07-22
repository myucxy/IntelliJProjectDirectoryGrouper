package dev.projectgroups.directorygrouper.domain

import java.nio.file.Path
import java.util.Locale

enum class HostFileSystem {
    WINDOWS,
    MACOS,
    LINUX,
}
class PathKeyStrategy private constructor(
    private val caseSensitive: Boolean,
) {
    fun normalize(path: Path): Path? = runCatching {
        path.takeIf(Path::isAbsolute)?.normalize()
    }.getOrNull()

    fun key(path: Path): String = normalizeForComparison(
        path.normalize().toString().replace('\\', '/'),
    )

    fun normalizeForComparison(value: String): String = if (caseSensitive) {
        value
    } else {
        value.lowercase(Locale.ROOT)
    }

    companion object {
        fun forHost(hostFileSystem: HostFileSystem): PathKeyStrategy = when (hostFileSystem) {
            HostFileSystem.WINDOWS, HostFileSystem.MACOS -> PathKeyStrategy(caseSensitive = false)
            HostFileSystem.LINUX -> PathKeyStrategy(caseSensitive = true)
        }

        fun forCurrentHost(): PathKeyStrategy {
            val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
            val host = when {
                osName.contains("win") -> HostFileSystem.WINDOWS
                osName.contains("mac") -> HostFileSystem.MACOS
                else -> HostFileSystem.LINUX
            }
            return forHost(host)
        }
    }
}
