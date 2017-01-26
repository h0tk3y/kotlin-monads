package com.github.h0tk3y.kotlinMonads

fun json(block: JsonContext.() -> Unit) = JsonContext().run {
    block()
    "{\n" + output.toString() + "\n}"
}

class JsonContext internal constructor() {
    internal val output = StringBuilder()

    private var indentation = 4

    private fun StringBuilder.indent() = apply {
        for (i in 1..indentation)
            append(' ')
    }

    private var needsSeparator = false

    private fun StringBuilder.separator() = apply {
        if (needsSeparator) append(",\n")
    }

    infix fun String.to(value: Any) {
        output.separator().indent().append("\"$this\": \"$value\"")
        needsSeparator = true
    }

    infix fun String.toJson(block: JsonContext.() -> Unit) {
        output.separator().indent().append("\"$this\": {\n")
        indentation += 4
        needsSeparator = false
        block(this@JsonContext)
        needsSeparator = true
        indentation -= 4
        output.append("\n").indent().append("}")
    }
}

fun main(args: Array<String>) {
    val j = json {
        "a" to 1
        "b" to "abc"
        "c" toJson {
            "d" to 123
            "e" toJson {
                "f" to "g"
            }
        }
    }

    println(j)
}