package dev.projectgroups.directorygrouper

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE_NAME = "messages.DirectoryGrouperBundle"

object DirectoryGrouperBundle : DynamicBundle(BUNDLE_NAME) {
    @JvmStatic
    @Nls
    fun message(
        @PropertyKey(resourceBundle = BUNDLE_NAME) key: String,
        vararg params: Any,
    ): String = getMessage(key, *params)
}
