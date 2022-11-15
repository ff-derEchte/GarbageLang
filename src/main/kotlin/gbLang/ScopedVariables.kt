package gbLang

class ScopedVariables(
    val variables: HashMap<String, Any> = hashMapOf(),
    val parent: ScopedVariables?
) {
    operator fun get(name: String): Any = variables[name] ?: parent?.get(name) ?: error("")
    operator fun contains(name: String): Boolean = name in variables || parent?.contains(name) ?: false
    operator fun set(name: String, value: Any): Unit = if (name in variables) variables[name] = value else parent?.set(name, value) ?: run {variables[name] = value}
    fun setLocal(name: String, value: Any) = run { variables[name] = value }
}