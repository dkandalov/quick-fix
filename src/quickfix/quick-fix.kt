@file:Suppress("UnstableApiUsage")

package quickfix

import com.intellij.codeInsight.intention.*
import com.intellij.codeInsight.intention.impl.CachedIntentions
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionConfigurationCustomizer
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class QuickFixAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = project.currentEditor ?: return
        val psiFile = project.currentPsiFile ?: return

        // Based on com.intellij.analysis.problemsView.toolWindow.ShowQuickFixesAction
        val intentionsInfo = ShowIntentionActionsHandler.calcIntentions(project, editor, psiFile)
        val cachedIntentions = CachedIntentions.createAndUpdateActions(project, psiFile, editor, intentionsInfo)
        val fix = cachedIntentions.allActions.firstOrNull { it.action.canBeInvoked() } ?: return

        val commandName = StringUtil.capitalizeWords(fix.action.text, true)
        ShowIntentionActionsHandler.chooseActionAndInvoke(psiFile, editor, fix.action, commandName)
    }
}

class AddIntentionActions : ActionConfigurationCustomizer {
    override fun customize(actionManager: ActionManager) {
        IntentionManager.getInstance().intentionActions
            .filter { it.canBeInvoked() }
            // Group by name because there are intentions with the same name,
            // e.g. "Put arguments on separate lines" for Java and Kotlin.
            .groupBy { it.familyName }
            .forEach { (familyName, intentionActions) ->
                val actionId = "$familyName (Intention)"
                val action = IntentionAsAction(actionId, intentionActions)
                actionManager.registerAction(actionId, action)
            }
    }

    private class IntentionAsAction(
        actionId: String,
        private val intentionActions: List<IntentionAction>
    ) : AnAction(actionId) {
        override fun actionPerformed(event: AnActionEvent) {
            val project = event.project ?: return
            val editor = project.currentEditor ?: return
            val psiFile = project.currentPsiFile ?: return

            intentionActions.forEach {
                if (it.isAvailable(project, editor, psiFile)) {
                    val commandName = StringUtil.capitalizeWords(it.text, true)
                    ShowIntentionActionsHandler.chooseActionAndInvoke(psiFile, editor, it, commandName)
                }
            }
        }
    }
}

fun IntentionAction.canBeInvoked() =
    (this as? CustomizableIntentionAction)?.isSelectable ?: true &&
    (this as? IntentionActionDelegate)?.delegate !is AbstractEmptyIntentionAction


val Project.currentFile: VirtualFile?
    get() = (FileEditorManagerEx.getInstance(this) as FileEditorManagerEx).currentFile

val Project.currentPsiFile: PsiFile?
    get() = currentFile?.let { PsiManager.getInstance(this).findFile(it) }

val Project.currentEditor: Editor?
    get() = (FileEditorManagerEx.getInstance(this) as FileEditorManagerEx).selectedTextEditor

val VirtualFile.document: Document?
    get() = FileDocumentManager.getInstance().getDocument(this)

val Project.currentDocument: Document?
    get() = currentFile?.document
