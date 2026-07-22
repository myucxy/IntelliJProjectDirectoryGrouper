package dev.projectgroups.directorygrouper.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "ProjectDirectoryGrouper",
    storages = [Storage("ProjectDirectoryGrouper.xml")],
)
class DirectoryGrouperSettings : PersistentStateComponent<DirectoryGrouperSettings.State> {
    private var currentState = State()

    override fun getState(): State = currentState

    override fun loadState(state: State) {
        currentState = state
    }

    data class State(
        var autoGroupMenus: Boolean = true,
        var autoLoadGitBranchesWhenMissing: Boolean = true,
    )

    companion object {
        fun getInstance(): DirectoryGrouperSettings =
            ApplicationManager.getApplication().getService(DirectoryGrouperSettings::class.java)
    }
}
