package dev.projectgroups.directorygrouper.lifecycle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class DirectoryGrouperStartupActivity : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().getService(ActionReplacementService::class.java)
    }
}
