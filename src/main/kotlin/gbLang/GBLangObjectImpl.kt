package gbLang

import java.io.File
import java.util.Date

class GBLangObjectImpl(
    val functions: HashMap<String, GBLangFunction>,
    private val default: GBLangFunction? = null
) : GBLangObject {

    constructor(functions: Map<String, GBLangFunction>, default: GBLangFunction? = null) : this(HashMap(functions), default)

    override fun toString(): String = "Obj($functions)"

    override fun getFunction(name: String): Result<GBLangFunction> = if (name in functions)
        Result.success(functions[name]!!)
    else
        default?.let { Result.success(it) } ?: Result.failure(IllegalStateException())
}

class DateWrapper(private val date: Date) : GBLangObject {
    override fun getFunction(name: String): Result<GBLangFunction> = when(name) {
        "toNumber" -> Result.success(func { date.time.toDouble() })
        "toString" -> Result.success(func { date.toString() })
        else -> Result.failure(IllegalStateException())
    }

    override fun toString(): String = date.toString()
}

class FileWrapper(
    private val file: File
) : GBLangObject {
    override fun getFunction(name: String): Result<GBLangFunction> = when(name) {
        "readText" -> Result.success(func {
            try {
                Promise.success(file.readText())
            } catch (_: Exception) {
                Promise.failure(IllegalStateException())
            }
        })
        "writeText" -> Result.success(func { args ->
            file.writeText(cast(args[0]))
            file.canWrite()
        })
        "exists" -> Result.success(func { file.exists() })
        "path" -> Result.success(func { file.path })
        else -> Result.failure(IllegalStateException())
    }

    override fun toString(): String = "File(${file.name})"
}

class Promise<T>private constructor(
    val value: T?,
    val error: Exception?
): GBLangObject {

    companion object {
        fun<T> success(success: T) = Promise(success, null)
        fun<T> failure(exception: Exception) = Promise<T>(null, exception)
    }

    override fun getFunction(name: String): Result<GBLangFunction> {
        return when(name) {
            "expect" -> Result.success(func { value ?: throw error!! })
            "isSuccess" -> Result.success(func { value != null })
            "isError" -> Result.success(func { value == null })
            else -> Result.failure(IllegalStateException())
        }
    }

}

inline fun<reified T> cast(any: Any): T = if (any is T) any else throw IllegalStateException()

fun mod(vararg pairs: Pair<String, GBLangFunction>, default: GBLangFunction? = null) = GBLangObjectImpl(pairs.toMap(), default)

//provides self referencing
fun mod2(func: GBLangObjectImpl.() -> Unit) = GBLangObjectImpl(hashMapOf()).apply(func)


fun nestedMod(paths: List<String>, vararg pairs: Pair<String, GBLangFunction>): GBLangObjectImpl {
    var before = GBLangObjectImpl(pairs.toMap())
    for (path in paths.reversed()) {
        before = GBLangObjectImpl(hashMapOf(path to func { before }))
    }
    return before
}