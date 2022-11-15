package gbLang

interface GBLangObject {
    fun getFunction(name: String): Result<GBLangFunction>
}