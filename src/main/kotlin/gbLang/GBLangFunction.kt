package gbLang

interface GBLangFunction {
    fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any
}

fun func(func: (args: Array<out Any>) -> Any) = object : GBLangFunction {
    override fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any  = func(args)
}

fun func2(func: (instance: Interpreter, args: Array<out Any>, fnName: String) -> Any) = object : GBLangFunction {
    override fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any  = func(instance, args, fnName)
}

inline fun<reified T> objFunc(crossinline func: (obj: T, instance: Interpreter, args: List<Any>, fnName: String) -> Any) = object : GBLangFunction {
    override fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any  {
        val (obj) = args
        if (obj !is T)
            error("")
        return func(obj, instance, args.toList().subList(1, args.size), fnName)
    }
}

inline fun<reified T> List<*>.cast(index: Int): T = run {
    if (index >= size)
        error(this)
    val value = this[index]
    if (value !is T)
        error("")
    value
}

inline fun<reified T> Array<*>.cast(index: Int) = this.toList().cast<T>(index)