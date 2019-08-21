package com.stonesoupprogramming.automotivecms.offers.offerscraper.functions

fun removeRepeatedString(str: String): String {
    val trimmed = str.trim()
    val first = trimmed.split(" ").first()
    val cutoff = trimmed.indexOf(first, startIndex = first.length)
    return trimmed.substring(0, cutoff).trim()
}
