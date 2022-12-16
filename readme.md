[![Build Status](https://github.com/dkandalov/quick-fix/workflows/CI/badge.svg)](https://github.com/dkandalov/quick-fix/actions)

This is a plugin for IntelliJ IDEs which:
1. Adds `Quick Fix` action to apply the first available quick fix because `alt+enter, enter` 
   is too many key presses when you already know that the top inspection will do what you want.
2. ~~Registers all available intentions as actions, so you can assign shortcuts to them~~ 
  Fixed in [this IntelliJ issue](https://youtrack.jetbrains.com/issue/IDEA-217465).

As an experiment `Quick Fix` action is assigned to `F1` because it's a single key and located near `F2 - Next Highlighted Error`.
You might need to un-assign `F1` from the `Context Help` action which is mostly useless anyway.

### Re-Prioritising intentions
You can configure `Quick Fix` to re-prioritise intentions by using the `quickfix-plugin.intentionPriorities` key in IDE `Registry`
(use "Main Menu -> Help -> Find Action..." and type "registry").  
For example, `*;Introduce import alias;Introduce local variable` will push "Introduce import alias" and "Introduce local variable" 
inspections to the end of the `QuickFix` priority list, so they are less likely to be invoked.
Another option is to disable inspection (`alter+enter, right` and choose "Disable").

### What's wrong with the `Silent Code Cleanup` action?
It fixes **all problems** in the current file. This is often too much. 
It also only fixes errors/warnings and doesn't apply inspections, e.g. it won't apply `Fix typo` intention.
