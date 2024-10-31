package quickfix

import com.intellij.codeInsight.intention.AbstractEmptyIntentionAction
import com.intellij.codeInsight.intention.CustomizableIntentionAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionDelegate
import com.intellij.codeInsight.intention.impl.IntentionActionWithTextCaching
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValue
import com.intellij.openapi.util.registry.RegistryValueListener
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile

class QuickFixAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.currentEditor ?: return
        val psiFile = event.currentPsiFile ?: return

        // The code below is roughly based on com.intellij.analysis.problemsView.toolWindow.ShowQuickFixesAction
        // It would be ideal to reorder intentions in the alt+enter popup,
        // however, this is currently not supported by IntelliJ API.

        @Suppress("UnstableApiUsage")
        val cachedIntentions = ShowIntentionActionsHandler.calcCachedIntentions(project, editor, psiFile)

        // The code below is a bit convoluted because it's trying to:
        //  - keep intentions overall in the same order as in UI, i.e. as returned by `cachedIntentions.allActions`
        //  - keep intentions within the same categories, i.e. errors always come first regardless of additional sorting
        //  - reorder subset of intentions according to QuickFix plugin priorities
        // (Note that an "obvious" solution to sort all intentions using the same sorting as `cachedIntentions.allActions`
        // and then applying QuickFix sort won't work, because in this case QuickFix sort can put intentions above errors, etc.
        // This will also require copy-pasting private sorting code from CachedIntentions.)
        val allActions = cachedIntentions.allActions

        val docChars = editor.document.immutableCharSequence
        val offset = editor.caretModel.offset
        if (offset >= docChars.length) return

        val currentChar = docChars[offset]
        val lineStartOffset = editor.document.getLineStartOffset(editor.document.getLineNumber(offset))
        val startOfCodeOnTheLine = (lineStartOffset..<offset).all { docChars[it].isWhitespace() }

        val previousChar = if (offset >= 2) docChars[offset - 1] else null
        val currentWord =
            docChars.subSequence(
                (0..offset).reversed().takeWhile { docChars[it].isLetter() }.lastOrNull() ?: offset,
                ((offset..offset + 100).takeWhile { docChars[it].isLetter() }.lastOrNull() ?: offset) + 1
            )

        val startOfWord = currentChar.isLetter() && (previousChar == null || previousChar.isWhitespace())
        val nearParens = currentChar == '(' || previousChar == '('

        fun IntentionActionWithTextCaching.quickFixPriority(): Int {
            quickFixConfig.priorityOf(action.text)?.let { return it }
            if (!quickFixConfig.enableHeuristics) return 0

            val highPriority = Int.MIN_VALUE
            val lowPriority = Int.MAX_VALUE
            val sanitisedText = text.replace(Regex("Import members from .*"), "Import members from...")
            return when (sanitisedText) {
                "Introduce local variable" -> if (startOfCodeOnTheLine) 0 else lowPriority
                "Import members from..." -> if (startOfWord) 1 else 0
                "Add names to call arguments" -> if (nearParens) 0 else lowPriority
                "Replace 'it' with explicit parameter" -> if (currentWord == "it") highPriority else 0
                "Replace with '-'" -> if (currentWord == "minus") highPriority else 0
                "Replace with '+'" -> if (currentWord == "plus") highPriority else 0
                "Replace with a 'forEach' function call" -> if (currentWord == "for") highPriority else 0
                "Convert concatenation to template" -> if (currentChar == '+') highPriority else 0
                "Remove redundant 'this'" -> if (currentWord == "this") highPriority else 0
                "Remove unnecessary parenthesis'" -> highPriority
                "Add explicit 'this'" -> lowPriority
                "Iterate over 'String'" -> lowPriority
                else -> 0
            }
        }

        val fix = allActions
            .reorderSublist(allActions.subsetOf(cachedIntentions.errorFixes).sortedBy { it.quickFixPriority() })
            .reorderSublist(allActions.subsetOf(cachedIntentions.inspectionFixes).sortedBy { it.quickFixPriority() })
            .reorderSublist(allActions.subsetOf(cachedIntentions.intentions).sortedBy { it.quickFixPriority() })
            .reorderSublist(allActions.subsetOf(cachedIntentions.gutters).sortedBy { it.quickFixPriority() })
            .reorderSublist(allActions.subsetOf(cachedIntentions.notifications).sortedBy { it.quickFixPriority() })
            .firstOrNull { it.action.canBeInvoked() } ?: return

        val commandName = StringUtil.capitalizeWords(fix.action.text, true)
        ShowIntentionActionsHandler.chooseActionAndInvoke(psiFile, editor, fix.action, commandName)
    }

    private val quickFixConfig = service<QuickFixConfig>()
}

@Service
class QuickFixConfig : Disposable {
    private val intentionPrioritiesValue: RegistryValue = Registry.get("quickfix-plugin.intentionPriorities").also {
        it.addListener(object : RegistryValueListener {
            override fun afterValueChanged(value: RegistryValue) {
                intentionPriorities = value.toIntentionPriorityMap()
            }
        }, this)
    }
    private val enableHeuristicsValue: RegistryValue = Registry.get("quickfix-plugin.enableHeuristics").also {
        it.addListener(object : RegistryValueListener {
            override fun afterValueChanged(value: RegistryValue) {
                enableHeuristics = value.asBoolean()
            }
        }, this)
    }

    private var intentionPriorities = intentionPrioritiesValue.toIntentionPriorityMap()
    var enableHeuristics = enableHeuristicsValue.asBoolean()

    fun priorityOf(intentionName: String): Int? =
        intentionPriorities[intentionName] ?: intentionPriorities["*"]

    private fun RegistryValue.toIntentionPriorityMap() =
        asString().split(";")
            .mapIndexed { index, value -> value to index }
            .toMap()

    override fun dispose() {
    }
}

private fun IntentionAction.canBeInvoked() =
    (this as? CustomizableIntentionAction)?.isSelectable != false &&
    (this as? IntentionActionDelegate)?.delegate !is AbstractEmptyIntentionAction

private val AnActionEvent.currentPsiFile: PsiFile?
    get() = getData(PSI_FILE)

private val AnActionEvent.currentEditor: Editor?
    get() = getData(EDITOR)

private fun <T> List<T>.subsetOf(set: Set<T>): List<T> = filter { it in set }

fun <T> List<T>.reorderSublist(order: List<T>): List<T> {
    if (order.isEmpty()) return this
    val (indices, values) = indices.zip(this)
        .filter { (_, value) -> value in order }
        .unzip()
    val sortedValues = values.sortedBy { order.indexOf(it) }
    val result = ArrayList(this)
    indices.zip(sortedValues).forEach { (index, value) ->
        result[index] = value
    }
    return result
}
