import gbLang.Interpreter
import gbLang.ParseFactory
import gbLang.loader.ExtensionLoader
import java.io.File

var shellMode: Boolean = false

var sourceFile: File? = null

fun argsParser(args: Array<String>) {
    val iter = args.toList().listIterator()
    while (iter.hasNext()) {
        when(val word = iter.next()) {
            "-e" -> {
                do {
                    ExtensionLoader.loadJar(File(iter.next()))
                } while (iter.hasNext() && !iter.next().startsWith("-").also { iter.previous()})
            }
            "-s" -> sourceFile = File(iter.next())
            "-c" -> shellMode = iter.next().toBoolean()
        }
    }
}


fun main(args: Array<String>) {
    val parser = ParseFactory.createWithCustom(ExtensionLoader.extensions)
    val interpreter = Interpreter()
    argsParser(args)
    if (shellMode) {
        while (true) {
            println(interpreter.interpret(parser.parse("${readln()}\n")))
        }
    } else {
        val tokens = parser.parse(sourceFile?.readText() ?: error("Source must be specified"))
        println(tokens)
        interpreter.interpret(tokens)
    }
}