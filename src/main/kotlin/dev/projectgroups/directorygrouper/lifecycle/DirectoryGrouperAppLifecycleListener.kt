package dev.projectgroups.directorygrouper.lifecycle

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager

class DirectoryGrouperAppLifecycleListener : AppLifecycleListener {
    override fun appFrameCreated(commandLineArgs: List<String>) {
        ApplicationManager.getApplication().getService(ActionReplacementService::class.java)
    }
}
