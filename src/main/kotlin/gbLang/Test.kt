package gbLang

import gbLang.annotaions.Function

class Test {
    @Function("doStuff")
    fun print(message: String) = println(message)
}