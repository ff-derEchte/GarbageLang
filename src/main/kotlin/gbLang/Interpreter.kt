package gbLang

import gbLang.annotaions.Function
import gbLang.loader.Component
import kotlin.reflect.KType

class Interpreter(
    parent: ScopedVariables? = null
) {

    companion object {
        var instance: Interpreter? = null
    }

    private val stack = mutableListOf<Any>()

    var variables: ScopedVariables = ScopedVariables(hashMapOf(), parent)

    fun interpret(tokens: List<Token>): Any = interpretRec(tokens.listIterator())

    private fun pop() = runCatching {stack.removeLast()}.getOrNull() ?: Unit
        //.also { println("Stack: $it") }

    private fun pop(num: Int): List<Any> = mutableListOf<Any>().apply {
        for (i in 0 until num)
            this.add(pop())
    }

    private fun interpretRec(iter: ListIterator<Token>): Any {
        val before = instance
        instance=this
        while (iter.hasNext()) {
            when(val token = iter.next()) {
                is ConstToken -> stack+=token.value
                is ObjCallToken -> stack.add(pop(token.argc).reversed().toTypedArray().let { args ->
                    when (val obj = pop()) {
                        is GBLangObject -> (obj.getFunction(token.name).getOrNull() ?: throw IllegalStateException("Function ${token.name} does not exist: $obj")).execute(this, *args, fnName = token.name)
                        is Component -> (obj::class.java.methods.find { it.name == token.name && it.isAnnotationPresent(Function::class.java) } ?: TODO()).invoke(obj, *args)
                        else -> if (obj::class in ParseFactory.classExtensions) {

                            if (ParseFactory.classExtensions[obj::class]?.get(token.name) != null)
                                ParseFactory.classExtensions[obj::class]?.get(token.name)
                                    ?.execute(this, *(listOf(obj) + args.toList()).toTypedArray(), fnName = token.name)
                                    ?: error("")
                           else (obj::class.java.methods.find { it.name == token.name && it.parameters.size == args.size} ?: TODO()).invoke(obj, *args)
                        } else {
                            error("invalid type $obj")
                        }
                    }
                }).also { if (stack.last() == Unit) pop() }

            }
        }
        instance = before
        return pop()
    }
}