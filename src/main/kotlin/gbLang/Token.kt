package gbLang

abstract class Token
data class ObjCallToken(val name: String,val argc: Int) : Token()
data class ConstToken(val value: Any) : Token()