package gbLang

import java.io.File
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.system.exitProcess

private fun wrapObject(obj: GBLangObject) = object : GBLangFunction {
    override fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any = obj
}

object ParseFactory {

    val classExtensions: Map<KClass<*>, MutableMap<String, GBLangFunction>> = mapOf(
        String::class to mutableMapOf(
            "split" to func {
                val str = it[0] as String
                str.split(it[1] as String)
            },
            "contains" to objFunc<String> { obj, _, args, _ ->
                val s = args.cast<String>(0)
                obj.contains(s)
            },
            "toCharList" to objFunc<String> { obj, _, _, _ ->
                obj.toCharArray().toList()
            },
            "replace" to objFunc<String> { obj, _, args, _ ->
                val rp = args.cast<String>(0)
                val expr = args.cast<String>(1)
                obj.replace(rp, expr)
            },
            "removePrefix" to func {
                val str = it.cast<String>(0)
                str.removePrefix(it.cast(1))
            },
            "removeSuffix" to func {
                val str = it.cast<String>(0)
                str.removeSuffix(it[1] as String)
            },
            "iterate" to func2 { instance, args, _ ->
                val str = args[0] as String
                when(val consumer = args[1]) {
                    is Consumer<*> -> {
                        for (c in str) {
                            (consumer as Consumer<Any>).accept(c)
                        }
                    }
                    is Scope -> {
                        for (c in str) {
                            Interpreter(instance.variables).apply { variables.setLocal("it", c) }.interpret(consumer.tokens)
                        }
                    }
                }

            }
        ),
        IntRange::class to mutableMapOf(
            "iterate" to func2 { instance, args, fnName ->
                val range = args[0] as IntRange
                when(val consumer = args[1]) {
                    is Consumer<*> -> {
                        for (c in range) {
                            (consumer as Consumer<Any>).accept(c)
                        }
                    }
                    is Scope -> {
                        for (c in range) {
                            Interpreter(instance.variables).apply { variables["it"]=c.toDouble() }.interpret(consumer.tokens)
                        }
                    }
                }
            }
        ),
        Double::class to mutableMapOf(
            "javaInt" to objFunc<Double> { obj, _, _, _ ->
                obj.toInt()
            }
        ),
        ArrayList::class to mutableMapOf(
            "pop" to objFunc<ArrayList<*>> { obj, _, _, _ ->
                obj.removeLast()
            },
            "iterate" to func2 { instance, args, _ ->
                val str = args.cast<ArrayList<Any>>(0)
                when(val consumer = args[1]) {
                    is Consumer<*> -> {
                        for (c in str) {
                            (consumer as Consumer<Any>).accept(c)
                        }
                    }
                    is Scope -> {
                        for (c in str) {
                            Interpreter(instance.variables).apply { variables.setLocal("it", c) }.interpret(consumer.tokens)
                        }
                    }
                }
            }
        )
    )

    private val stdLib = hashMapOf(
        "Math" to object : GBLangObject {
            override fun getFunction(name: String): Result<GBLangFunction> =
                when (name) {
                    "equals" -> Result.success(func { args ->
                        if (args.size != 2)
                            throw IllegalStateException()
                        args[0] == args[1]
                    })
                    "notEquals" -> Result.success(func { args ->
                        if (args.size != 2)
                            throw IllegalStateException()
                        args[0] != args[1]
                    })
                    "add" -> Result.success(object : GBLangFunction {
                        override fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any {
                            var num = 0.0
                            if (args.any { it !is Double })
                                throw IllegalStateException("Cannot add non numbers ...")
                            args.filterIsInstance<Double>().forEach { num+= it }
                            return num
                        }
                    })
                    "sub" -> Result.success(object : GBLangFunction {
                        override fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any {
                            if (args.any { it !is Double })
                                throw IllegalStateException("Cannot add non numbers ...")
                            var before: Double? = null
                            args.filterIsInstance<Double>().forEach { before = before?.minus(it) ?: it }
                            return before ?: 0
                        }
                    })
                    "mul" -> Result.success(object : GBLangFunction {
                        override fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any {
                            if (args.any { it !is Double })
                                throw IllegalStateException("Cannot add non numbers ...")
                            var before: Double? = null
                            args.filterIsInstance<Double>().forEach { before = before?.times(it) ?: it }
                            return before ?: 0
                        }
                    })
                    "div" -> Result.success(object : GBLangFunction {
                        override fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any {
                            if (args.any { it !is Double })
                                throw IllegalStateException("Cannot add non numbers ...")
                            var before: Double? = null
                            args.filterIsInstance<Double>().forEach { before = before?.div(it) ?: it }
                            return before ?: 0
                        }
                    })
                    else -> Result.failure(IllegalStateException())
                }

        },
        "Numbers" to mod(
            "toNumber" to func { args ->
                when(val num = args[0]) {
                    is String -> num.toDouble()
                    is Double -> num
                    is Int -> num.toDouble()
                    is Long -> num.toDouble()
                    else -> error("")
                }
            }
        ),
        "IO" to mod(
            "print" to func {
                args-> println(args.joinToString(""))
            },
            "read" to func { args ->
                if (args.size == 1)
                    print(args[0])
                readln()
            }
        ),
        "List" to mod(
            "create" to func {
                arrayListOf(*it)
            },
            "toJavaArray" to func {
                val (list) = it
                if (list !is ArrayList<*>)
                    error("List.toJavaArray takes in a list")
                list.toArray()
            }
        ),
        "Func" to mod(
            "wrapPure" to func { args ->
                val (name) = args
                if (name !is String) error("")
                val results: HashMap<List<Any>, Any> = hashMapOf()
                val function: GBLangFunction= functions[name] ?: error("Function $name does not exist")
                functions[name] = func2 { instance, fnArgs, fnName ->
                    val args2 = fnArgs.toList()
                    return@func2 results[args2]?: function.execute(instance, *fnArgs, fnName=fnName).also { results[args2] = it }
                }
                Unit
            },
            "define" to func { _ ->
                mod(
                    default = func2 { _, args2, fnName ->
                        val scope = args2.last() as Scope
                        val parameters = mutableListOf<String>()
                        for (i in 0 until args2.lastIndex)
                            parameters+=args2[i] as String
                        functions[fnName] = object : GBLangFunction {
                            override fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any
                                    = Interpreter(instance.variables).apply { parameters.mapIndexed { index, s ->  this.variables.setLocal(s, args[index])} }.interpret(scope.tokens)
                        }
                        Unit
                    }
                )
            },
            default = func2 { instance, args, fnName ->
                functions[fnName]?.execute(instance, *args, fnName=fnName) ?: error("Function $fnName not found")
            }
        ),
        "Range" to mod(
            "to" to func {
                IntRange(0, (it[0] as Double).toInt())
            }
        ),
        "Runtime" to mod(
            "executeScope" to func2 { instance, args, _ ->
                Interpreter(instance.variables).interpret((args[0] as Scope).tokens)
            },
            "Functions" to func { mod(
                default = func2 { instance, args, fnName ->
                    functions[fnName]?.execute(instance, *args, fnName=fnName) ?: Unit
                }
            )},
            "getFunctionManager" to func {  mod(
                "registerFunction" to func2 { _, args, _ ->
                    val name = args.first() as String
                    val scope = args.last() as Scope
                    val parameters = mutableListOf<String>()
                    for (i in 1 until args.lastIndex)
                        parameters+=args[i] as String
                    functions[name] = object : GBLangFunction {
                        override fun execute(instance: Interpreter, vararg args: Any, fnName: String): Any
                        = Interpreter(instance.variables).apply { parameters.mapIndexed { index, s ->  this.variables[s] = args[index]} }.interpret(scope.tokens)
                    }
                    Unit
                }
            )},
            "Match" to func { par ->
                val (arg) = par
                var called = false
                val conditions = hashMapOf<Any, Scope>()
                var elseContent: Scope? = null
                return@func mod2 {
                    functions["when"] = func { args->
                        conditions[args[0]] = (args[1] as Scope)
                        this
                    }
                    functions["else"] = func { args ->
                        elseContent = (args[0] as Scope)
                        this
                    }
                    functions["run"] = func2 { instance, _, _ ->
                        conditions.forEach { (value, scope) ->
                            if (arg == value) {
                                called = true
                                return@func2 Interpreter(instance.variables).interpret(scope.tokens)
                            }
                        }
                        if (!called && elseContent != null)
                            return@func2 Interpreter(instance.variables).interpret(elseContent!!.tokens)
                        Unit
                    }
                }
            },
            "If" to func2 { instance, args, _ ->
                val cond = args[0] as Scope
                val interpreter = Interpreter(instance.variables)
                if (interpreter.interpret(cond.tokens).let { it is Boolean && it }) {
                    val content = args[1] as Scope
                    interpreter.interpret(content.tokens)
                } else {
                    if (args.size == 3) {
                        val content = args[2] as Scope
                        interpreter.interpret(content.tokens)
                    } else Unit
                }
                      },
            "While" to func2 { instance, args, _ ->
                val (cond, content) = args
                if (cond !is Scope  || content !is Scope)
                    throw IllegalStateException()
                val interpreter = Interpreter(instance.variables)
                while (interpreter.interpret(cond.tokens).let { it is Boolean && it }) {
                    interpreter.interpret(content.tokens)
                }
            }
        ),
        "System" to mod(
            "getIOProvider" to func { mod("getIOHandler" to func { mod(
                "write" to func { args-> println(args.joinToString("")) },
                "read" to func { args-> args.forEach { print(it) }; readln() },
                "readNumber" to func { args-> args.forEach { print(it) }; readln().toDoubleOrNull() ?: Unit },
                "_info" to func { println("IO 1.0") }
            )})},
            "getClock" to func { mod(
                "getTimeMillis" to func { System.currentTimeMillis().toDouble() },
                "getTime" to func { DateWrapper(Date()) }
            )},
            "getProgramControl" to func { mod(
                "exit" to func {
                    exitProcess(it.cast<Double>(0).toInt())
                },
                "getOsName" to func { System.getProperty("os.name") }
            )},
            "getFileSystem" to func { mod(
                "openFile" to func { args ->
                    FileWrapper(File(args.cast<String>(0)))
                },
                "_info" to func { println("FileSystemAPI 1.0") }
            ) }
        ),
        "Vars" to mod(
            "getVar" to func2 { instance, args, _ ->
                instance.variables[args.cast(0)]
            },
            "setVar" to func2 { instance, args, _ ->
                instance.variables[args.cast(0)] = args[1]
                true
            },
            default = func2 { instance, args, name ->
                if (args.isEmpty())
                    instance.variables[name]
                else
                    instance.variables[name] = args[0]
            }
        ),
        "Consumer" to mod(
            default = func2 { instance, args, fnName ->
                Consumer<Any> { Interpreter(instance.variables).apply { this.variables.setLocal(fnName, it)}.interpret((args[0] as Scope).tokens) }
            }
        ),
        "VariableManager" to mod(
            "_info" to func { println("VariableManager 1.0") },
            "setVar" to func {
                val (name, value) = it
                if (name !is String)
                    false
                else {
                    globalState[name] = value
                    true
                }
            },
            "getVar" to func {
                val name = it[0] as String
                globalState[name] ?: Unit
            },
            "clearVars" to func {
                it.forEach { nm -> globalState[nm.toString()] = Unit }
            },
            "clearAll" to func { globalState.clear() }
        )
    )

    fun createWithCustom(lib: HashMap<String, GBLangObject>) = Parser(stdLib+lib)
    fun createStd() = Parser(stdLib)
}

