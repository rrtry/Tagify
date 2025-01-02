package com.rrtry.tagify.util

import java.nio.charset.Charset
import java.util.regex.Pattern

fun decodeString(s: String, from: Charset, to: Charset): String {
    return s.toByteArray(from)
        .toString(to)
}

fun String.isIncorrectlyEncoded(): Boolean {
    if (isEmpty()) return false
    return any {
        it.code in 128..255 ||
        it.code in 0..31
    }
}

fun isNumPairInvalid(s: String) = if (s.isBlank()) false else !Pattern.matches("\\d+|\\d+/\\d+", s)

fun String.luceneEscape(): String {
    val specialChars = setOf(
        '+', '-', '&', '|', '!', '(', ')', '{', '}',
        '[', ']', '^', '"', '~', '*', '?', ':', '\\'
    )
    var sanitized = ""
    forEach { char ->
        if (char in specialChars) {
            sanitized += "\\"
        }
        sanitized += char
    }
    return sanitized
}

fun String.safeSubstring(from: Int, to: Int): String {
    return substring(from, if (to > length) length else to)
}

fun String.joinIfNotBlank(s: String, delimiter: String): String {
    if (isNotBlank() && s.isNotBlank()) return "$this $delimiter $s"
    return ifBlank { s }
}

fun String.parseNumberPair(): Pair<Int, Int> {
    val parts = this.split("/")
    val pos   = parts[0].parseNum()
    val total = if (parts.size == 2) parts[1].parseNum() else 0
    return Pair(pos, total)
}

fun String.parseNum(): Int {
    return try {
        Integer.parseInt(this)
    } catch (e: NumberFormatException) { 0 }
}

fun String.parseYear(): Int {
    if (this.length < 4) return 0
    return slice(0..<4).parseNum()
}

fun levenshtein(s: String, t: String): Int {

    if (s == t)  return 0
    if (s == "") return t.length
    if (t == "") return s.length

    val v0 = IntArray(t.length + 1) { it }
    val v1 = IntArray(t.length + 1)

    var cost: Int
    for (i in 0 until s.length) {
        v1[0] = i + 1
        for (j in 0 until t.length) {
            cost = if (s[i] == t[j]) 0 else 1
            v1[j + 1] = Math.min(v1[j] + 1, Math.min(v0[j + 1] + 1, v0[j] + cost))
        }
        for (j in 0 .. t.length) v0[j] = v1[j]
    }
    return v1[t.length]
}