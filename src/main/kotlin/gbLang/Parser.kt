package gbLang

val KEYWORDS = listOf(".", "(", ")", "{", "}", ",", "[", "]")

class Parser(
    private val context: Map<String, Any>
) {

    private val consts: MutableMap<String, Any> = mutableMapOf()

    private val strings: MutableList<String> = mutableListOf()

    private fun keywords(line: String): String {
        var code = line
            .replace("\n", " \n ")
            .replace("->", " _whileInit ")
            .replace("==", " _eqCmp ")
            .replace("!=", " _neqCmp ")
            .replace(">>", " _pipe ")
        KEYWORDS.forEach {
            code = code.replace(it, " $it ")
        }
        while(code.contains("  ")) code = code.replace("  ", " ")
        return code
    }

    private fun extractStrings(line: String): String {
        var recording = false
        var record = ""
        var code = ""
        for (c in line) {
            if (c == '\'') {
                if (recording) {
                    strings += record
                    record = ""
                    code+="° ${strings.lastIndex}"
                }
                recording = !recording
                continue
            }
            if (recording) record += c else code+=c
        }
        return code
    }

    private fun parseScope(words: ListIterator<String>, endLn: List<String>): List<Token> = mutableListOf<Token>().apply{
        while (words.previous() != "}") {
            words.next()
            this+=parseRec(words, endLn.toMutableList().apply {
                this += "}"
            })
        }
        words.next()
    }

    private fun parseFunction(words: ListIterator<String>, begin: String = "(", end: String=")"): Pair<Int, List<Token>> {
        val tokens = mutableListOf<Token>()
        if (words.next() != begin) {
            words.previous()
            return 0 to listOf()
        }
        if (words.next() == end)
            return 0 to tokens
        else words.previous()
        words.previous()
        var count = 0
        do {
            count++
            words.next()
            tokens+=parseRec(words, listOf(",", end))
        } while (words.previous() != end)
        words.next()
        return count to tokens
    }

    fun parse(code: String): List<Token> = mutableListOf<Token>().apply {
        val iter = keywords(extractStrings(code)).split(" ").listIterator()
        while (iter.hasNext())
            this+=parseRec(iter)
    }

    private fun parseRec(words: ListIterator<String>, endLn: List<String> = listOf("\n")): List<Token> {
        val tokens = mutableListOf<Token>()
        do {
            val word = words.next()
            if (word in endLn)
                return tokens
            if (word.trim().isEmpty())
                continue
            when(word) {
                in context.keys -> tokens+=ConstToken(context[word]!!)
                in consts.keys -> ConstToken(consts[word]!!)
                "." -> {
                    tokens+=ObjCallToken(words.next(), parseFunction(words).let{
                         tokens+=it.second
                        it.first
                    })
                }
                "def" -> consts[words.next()]=parseRec(words, endLn)
                "@" -> parseFunction(words)
                "[" -> words.previous().run {
                    tokens+=ConstToken(context["List"]!!)
                    val (size, tks) = parseFunction(words, begin = "[", end = "]")
                    tks.forEach { tokens+=it}
                    tokens+=ObjCallToken("create", size)
                }
                "{" -> tokens+=ConstToken(Scope(parseScope(words, endLn)))
                "°" -> tokens += ConstToken(strings[words.next().toInt()])
                "true" -> tokens+= ConstToken(true)
                "false" -> tokens+= ConstToken(false)
                else -> try {
                    tokens+=ConstToken(word.toDouble())
                } catch (e: Exception) {e.printStackTrace()}
            }

        } while (words.hasNext())
        return tokens
    }
}