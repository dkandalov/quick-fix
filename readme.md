This is a plugin for IntelliJ IDEs which:
- registers all available intentions as actions so you can assign shortcuts to them (see [this youtrack issue](https://youtrack.jetbrains.com/issue/IDEA-217465))
- adds `Quick Fix` action which applies the first available quick fix at the current location (equivalent to `alt+enter, enter`)

### What's wrong with Alt+Enter?
Some intentions are used quite often so it's too tedious to press `alt+enter` and choose the right one from the popup list
where depending on the context, the intention might be at a different position.
To solve this problem the plugin creates actions for all intentions so you can assign keyboard shortcuts to invoke intentions.

Pressing `alt+enter, enter` is too many key presses when you already know that the top inspection is going to do what you want.
To avoid this you can use `Quick Fix` action to apply the first available inspection.

Finally, `alt+enter` is not the easiest shortcut to be used frequently...
As an experiment `Quick Fix` is assigned to `F1` because it's a single key and located near `F2 - Next Highlighted Error`.
You might need though to unassign `F1` from the `Context Help` action which is mostly useless anyway.
