[![Build Status](https://github.com/dkandalov/quick-fix/workflows/CI/badge.svg)](https://github.com/dkandalov/quick-fix/actions)

This is a plugin for IntelliJ IDEs which:
1. Adds `Quick Fix` action to apply the first available quick fix (equivalent to pressing `alt+enter, enter`).<br/>
  `Quick Fix` is assigned to `F1` so you might need to un-assign `F1` from the `Context Help` action.
2. ~~Registers all available intentions as actions, so you can assign shortcuts to them~~ 
  Fixed in [this IJ IDEA issue](https://youtrack.jetbrains.com/issue/IDEA-217465).

### De-Prioritising Intentions
If the order of intentions as they appear in alt+enter popup is not ideal, you can tell `Quick Fix` to (de)prioritise intentions.  
For that, open `Search Everywhere` and search for `Registry`.  
There will be a key called `quickfix-plugin.intentionPriorities` which can be configured by setting a value.  
Example: `*;Introduce import alias;Introduce local variable` will push "Introduce import alias" and "Introduce local variable" inspections to the end of `QuickFix` priority list, so they are less likely to be invoked.

### What's wrong with alt+enter?
Pressing `alt+enter, enter` is too many key presses when you already know that the top inspection is going to do what you want.
To solve this problem `Quick Fix` applies the first available inspection.

Some intentions are used so frequently that pressing `alt+enter` and choosing the right row in the popup list becomes tedious.
It also doesn't help that depending on the context, the intention you're looking for might not be on the same row as it was before.
To solve this problem the plugin creates actions for all intentions, so you can assign keyboard shortcuts and invoke intentions without any popup windows.
Note that all actions generated by the plugin have "(Intention)" postfix, so you can distinguish them from other actions.

Finally, `alt+enter` is physically not the easiest shortcut to press, especially if you use it a lot...
As an experiment `Quick Fix` action is assigned to `F1` because it's a single key and located near `F2 - Next Highlighted Error`.
You might need though to un-assign `F1` from the `Context Help` action which is mostly useless anyway.

### What's wrong with `Silent Code Cleanup` action?
It fixes **all problems** in the current file. This is often too much. 
It also only fixes errors/warnings and doesn't apply inspections, e.g. it won't apply `Fix typo` intention.
