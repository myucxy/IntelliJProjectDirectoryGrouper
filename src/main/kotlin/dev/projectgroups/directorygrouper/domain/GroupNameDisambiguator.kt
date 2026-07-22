package dev.projectgroups.directorygrouper.domain

import java.nio.file.Path

internal data class ParentDirectory(
    val key: String,
    val path: Path,
) {
    val baseName: String = path.fileName?.toString().orEmpty().ifEmpty { path.toString() }
    val ancestorNames: List<String> = path.map(Path::toString).toList().dropLast(1)

    fun qualifier(depth: Int): String = ancestorNames
        .takeLast(depth)
        .joinToString("/")
        .ifEmpty { path.toString().replace('\\', '/') }
}

internal class GroupNameDisambiguator(
    private val pathKeyStrategy: PathKeyStrategy,
) {
    fun names(parents: Collection<ParentDirectory>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        parents.groupBy { pathKeyStrategy.normalizeForComparison(it.baseName) }
            .values
            .forEach { sameBaseName ->
                if (sameBaseName.size == 1) {
                    val parent = sameBaseName.single()
                    result[parent.key] = parent.baseName
                } else {
                    result.putAll(disambiguate(sameBaseName))
                }
            }
        return result
    }

    private fun disambiguate(parents: List<ParentDirectory>): Map<String, String> {
        val maximumDepth = parents.maxOf { it.ancestorNames.size }.coerceAtLeast(1)
        for (depth in 1..maximumDepth) {
            val qualifiers = parents.associateWith { it.qualifier(depth) }
            val distinctCount = qualifiers.values
                .map(pathKeyStrategy::normalizeForComparison)
                .distinct()
                .size
            if (distinctCount == parents.size) {
                return qualifiers.map { (parent, qualifier) ->
                    parent.key to "${parent.baseName} \u2014 $qualifier"
                }.toMap()
            }
        }

        return parents.associate { parent ->
            parent.key to "${parent.baseName} \u2014 ${parent.path.toString().replace('\\', '/')}"
        }
    }
}
