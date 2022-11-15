package gbLang.loader

import gbLang.GBLangFunction
import gbLang.GBLangObject
import gbLang.annotaions.Function
import gbLang.func
import java.io.File
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader


object ExtensionLoader {

    val extensions = hashMapOf<String, GBLangObject>()

    fun registerExtension(extension: Extension) {
        val functions = extension::class.java.methods
            .filter { method ->
                method.isAnnotationPresent(Function::class.java)
            }
        extensions+=extension::class.simpleName!! to object : GBLangObject {
            override fun toString(): String = functions.map { it.name }.toString()

            override fun getFunction(name: String): Result<GBLangFunction> =
                functions
                    .find { it.name == name }
                    ?.let { Result.success(func { args -> it.invoke(extension, *args) ?: Unit  }) }
                    ?: Result.failure(IllegalStateException())
        }
        println(extensions)
    }

    fun loadJar(jar: File) {
        val loader = URLClassLoader(
            arrayOf<URL>(jar.toURI().toURL()),
            this.javaClass.classLoader
        )
        val path: String = loader.getResourceAsStream("extension.gbc")?.use { String(it.readAllBytes()).removePrefix("main:").trim() } ?: error("extension.gbc missing")
        val classToLoad = Class.forName(path, true, loader)
        if (classToLoad.superclass != Plugin::class.java)
            TODO()
        val method: Method = classToLoad.getDeclaredMethod("onEnable")
        val instance = classToLoad.newInstance()
        method.invoke(instance)
    }
}