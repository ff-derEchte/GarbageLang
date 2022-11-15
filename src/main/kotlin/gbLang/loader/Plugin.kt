package gbLang.loader

abstract class Plugin {
    abstract fun onEnable()
    open fun disable() {}
}