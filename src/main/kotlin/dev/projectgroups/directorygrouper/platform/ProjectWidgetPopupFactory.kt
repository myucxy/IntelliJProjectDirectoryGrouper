package dev.projectgroups.directorygrouper.platform

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.SpeedSearchFilter
import com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetPresentable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.JBColor
import com.intellij.ui.popup.ActionPopupOptions
import com.intellij.ui.popup.ActionPopupStep
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.projectgroups.directorygrouper.actions.RecentProjectVisualSection
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.util.IdentityHashMap
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.border.MatteBorder

class ProjectWidgetPopupFactory {
    fun create(
        project: Project,
        actionGroup: ActionGroup,
        dataContext: DataContext,
        visualSections: List<RecentProjectVisualSection> = emptyList(),
        branchNames: ProjectWidgetBranchNames = ProjectWidgetBranchNames.EMPTY,
    ): JBPopup {
        val step = ActionPopupStep.createActionsStep(
            null,
            actionGroup,
            dataContext,
            PROJECT_WIDGET_POPUP_PLACE,
            PresentationFactory(),
            { dataContext },
            ActionPopupOptions.showDisabled().withSpeedSearchFilter(ProjectWidgetSpeedSearchFilter(branchNames)),
        )

        val popup = JBPopupFactory.getInstance().createListPopup(project, step) { baseRenderer ->
            @Suppress("UNCHECKED_CAST")
            ProjectWidgetActionItemRenderer(
                baseRenderer as ListCellRenderer<PopupFactoryImpl.ActionItem>,
                visualSections,
                branchNames,
            )
        }
        branchNames.loading?.whenComplete { _, _ ->
            ApplicationManager.getApplication().invokeLater {
                if (popup.content.isShowing) {
                    popup.content.revalidate()
                    popup.content.repaint()
                }
            }
        }
        return popup
    }

    private class ProjectWidgetActionItemRenderer(
        private val fallback: ListCellRenderer<PopupFactoryImpl.ActionItem>,
        visualSections: List<RecentProjectVisualSection>,
        private val branchNames: ProjectWidgetBranchNames,
    ) : ListCellRenderer<PopupFactoryImpl.ActionItem> {
        private val groupedRows = createGroupedRows(visualSections)

        override fun getListCellRendererComponent(
            list: JList<out PopupFactoryImpl.ActionItem>,
            value: PopupFactoryImpl.ActionItem,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val projectAction = value.action as? ProjectToolbarWidgetPresentable
                ?: return fallback.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus,
                )

            val foreground = when {
                !value.isEnabled -> UIUtil.getLabelDisabledForeground()
                isSelected -> list.selectionForeground
                else -> list.foreground
            }
            val secondaryForeground = when {
                !value.isEnabled -> UIUtil.getLabelDisabledForeground()
                isSelected -> list.selectionForeground
                else -> UIUtil.getContextHelpForeground()
            }
            val background = if (isSelected) list.selectionBackground else list.background
            val branchName = branchNames.get(value.action, projectAction.branchName)
            val displayDetails = projectWidgetDisplayDetails(
                projectName = projectAction.projectNameToDisplay,
                providerPath = projectAction.providerPathToDisplay,
                projectPath = projectAction.projectPathToDisplay,
                branchName = branchName,
            )
            val details = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(JBLabel(displayDetails.textLines.first()).apply {
                    font = list.font
                    this.foreground = foreground
                    alignmentX = Component.LEFT_ALIGNMENT
                })
                displayDetails.textLines.drop(1).forEach { addSecondaryLine(it, secondaryForeground) }
                displayDetails.branchName?.let { resolvedBranchName ->
                    add(
                        JPanel().apply {
                            layout = BoxLayout(this, BoxLayout.X_AXIS)
                            isOpaque = false
                            alignmentX = Component.LEFT_ALIGNMENT
                            add(JBLabel(AllIcons.Vcs.Branch))
                            add(Box.createHorizontalStrut(JBUI.scale(4)))
                            add(secondaryLabel(resolvedBranchName, secondaryForeground))
                        },
                    )
                }
            }
            val projectRow = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                isOpaque = true
                this.background = background
                border = JBUI.Borders.empty(6, 8)
                add(JBLabel(projectAction.projectIcon), BorderLayout.WEST)
                add(details, BorderLayout.CENTER)
                accessibleContext.accessibleName = listOfNotNull(
                    projectAction.projectNameToDisplay,
                    projectAction.providerPathToDisplay,
                    projectAction.projectPathToDisplay,
                    branchName,
                ).joinToString(" - ")
            }

            val groupedRow = groupedRows[value.action]
            return if (groupedRow == null) {
                addSeparator(value, projectRow, background, secondaryForeground)
            } else {
                createGroupedProjectRow(projectRow, groupedRow, list.background)
            }
        }

        private fun JPanel.addSecondaryLine(text: String, foreground: java.awt.Color) {
            add(secondaryLabel(text, foreground))
        }

        private fun secondaryLabel(text: String, foreground: java.awt.Color): JBLabel =
            JBLabel(text).apply {
                font = JBFont.small()
                this.foreground = foreground
                alignmentX = Component.LEFT_ALIGNMENT
            }

        private fun createGroupedProjectRow(
            projectRow: JComponent,
            groupedRow: GroupedRow,
            listBackground: Color,
        ): JComponent {
            val drawBottom = groupedRow.position == GroupRowPosition.SINGLE ||
                groupedRow.position == GroupRowPosition.LAST
            val framedRow = JPanel(BorderLayout()).apply {
                isOpaque = true
                background = projectRow.background
                border = MatteBorder(
                    0,
                    JBUI.scale(FRAME_THICKNESS),
                    if (drawBottom) JBUI.scale(FRAME_THICKNESS) else 0,
                    JBUI.scale(FRAME_THICKNESS),
                    groupedRow.color,
                )
                add(projectRow, BorderLayout.CENTER)
            }
            val insetRow = JPanel(BorderLayout()).apply {
                isOpaque = true
                background = listBackground
                border = JBUI.Borders.empty(0, FRAME_HORIZONTAL_INSET)
                add(framedRow, BorderLayout.CENTER)
            }
            if (groupedRow.position != GroupRowPosition.FIRST &&
                groupedRow.position != GroupRowPosition.SINGLE
            ) {
                return insetRow
            }

            return JPanel(BorderLayout()).apply {
                isOpaque = true
                background = listBackground
                add(
                    GroupLegendPanel(
                        title = groupedRow.title,
                        lineColor = groupedRow.color,
                        panelBackground = listBackground,
                    ),
                    BorderLayout.NORTH,
                )
                add(insetRow, BorderLayout.CENTER)
            }
        }

        private fun addSeparator(
            value: PopupFactoryImpl.ActionItem,
            projectRow: JComponent,
            background: java.awt.Color,
            foreground: java.awt.Color,
        ): JComponent {
            if (!value.isPrependWithSeparator) return projectRow

            return JPanel(BorderLayout()).apply {
                isOpaque = true
                this.background = background
                add(
                    JBLabel(value.separatorText.orEmpty()).apply {
                        font = JBFont.small()
                        this.foreground = foreground
                        border = JBUI.Borders.empty(4, 8, 2, 8)
                    },
                    BorderLayout.NORTH,
                )
                add(projectRow, BorderLayout.CENTER)
            }
        }

        private fun createGroupedRows(
            visualSections: List<RecentProjectVisualSection>,
        ): IdentityHashMap<AnAction, GroupedRow> {
            val result = IdentityHashMap<AnAction, GroupedRow>()
            var rainbowIndex = 0
            visualSections.forEach { section ->
                val color = if (section.useNeutralColor) {
                    NEUTRAL_GROUP_COLOR
                } else {
                    rainbowGroupColor(rainbowIndex++)
                }
                section.actions.forEachIndexed { index, action ->
                    val position = when {
                        section.actions.size == 1 -> GroupRowPosition.SINGLE
                        index == 0 -> GroupRowPosition.FIRST
                        index == section.actions.lastIndex -> GroupRowPosition.LAST
                        else -> GroupRowPosition.MIDDLE
                    }
                    result[action] = GroupedRow(section.title, color, position)
                }
            }
            return result
        }
    }

    private class GroupLegendPanel(
        title: String,
        private val lineColor: Color,
        panelBackground: Color,
    ) : JPanel(BorderLayout()) {
        private val titleLabel = JBLabel(title).apply {
            isOpaque = true
            background = panelBackground
            foreground = lineColor
            font = JBFont.small().asBold()
            border = JBUI.Borders.empty(0, LEGEND_TEXT_HORIZONTAL_PADDING)
        }

        init {
            isOpaque = true
            background = panelBackground
            border = JBUI.Borders.empty(
                LEGEND_TOP_GAP,
                FRAME_HORIZONTAL_INSET + LEGEND_LEFT_INSET,
                0,
                FRAME_HORIZONTAL_INSET,
            )
            add(titleLabel, BorderLayout.WEST)
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            val left = JBUI.scale(FRAME_HORIZONTAL_INSET)
            val right = width - left - 1
            val lineY = titleLabel.y + titleLabel.height / 2
            val lineGraphics = graphics.create()
            try {
                lineGraphics.color = lineColor
                lineGraphics.drawLine(left, lineY, right, lineY)
                lineGraphics.drawLine(left, lineY, left, height - 1)
                lineGraphics.drawLine(right, lineY, right, height - 1)
            } finally {
                lineGraphics.dispose()
            }
        }
    }

    private class ProjectWidgetSpeedSearchFilter(
        private val branchNames: ProjectWidgetBranchNames,
    ) : SpeedSearchFilter<PopupFactoryImpl.ActionItem> {
        override fun getIndexedString(value: PopupFactoryImpl.ActionItem): String {
            val projectAction = value.action as? ProjectToolbarWidgetPresentable
                ?: return value.text
            return listOfNotNull(
                projectAction.projectNameToDisplay,
                projectAction.providerPathToDisplay,
                projectAction.projectPathToDisplay,
                branchNames.get(value.action, projectAction.branchName),
            ).joinToString(" ")
        }
    }

    private companion object {
        const val PROJECT_WIDGET_POPUP_PLACE = "ProjectWidgetPopup"
        const val FRAME_THICKNESS = 1
        const val FRAME_HORIZONTAL_INSET = 6
        const val LEGEND_LEFT_INSET = 6
        const val LEGEND_TEXT_HORIZONTAL_PADDING = 4
        const val LEGEND_TOP_GAP = 5

        val NEUTRAL_GROUP_COLOR: Color = JBColor(Color(0x66707A), Color(0x9AA4AE))

        fun rainbowGroupColor(index: Int): Color {
            val hue = ((index * RAINBOW_HUE_STEP) % RAINBOW_HUE_COUNT).toFloat() / RAINBOW_HUE_COUNT
            return JBColor(
                Color.getHSBColor(hue, 0.72f, 0.68f),
                Color.getHSBColor(hue, 0.55f, 0.95f),
            )
        }

        const val RAINBOW_HUE_STEP = 47
        const val RAINBOW_HUE_COUNT = 360
    }

    private data class GroupedRow(
        val title: String,
        val color: Color,
        val position: GroupRowPosition,
    )

    private enum class GroupRowPosition {
        SINGLE,
        FIRST,
        MIDDLE,
        LAST,
    }
}

internal data class ProjectWidgetDisplayDetails(
    val textLines: List<String>,
    val branchName: String?,
)

internal fun projectWidgetDisplayDetails(
    projectName: String,
    providerPath: String?,
    projectPath: String?,
    branchName: String?,
): ProjectWidgetDisplayDetails = ProjectWidgetDisplayDetails(
    textLines = listOfNotNull(projectName, providerPath, projectPath),
    branchName = branchName?.takeUnless(String::isBlank),
)
