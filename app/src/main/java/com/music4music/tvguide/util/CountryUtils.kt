package com.music4music.tvguide.util

import java.util.Locale

object CountryUtils {

    private val aliases = mapOf(
        "grande-bretagne" to "GB",
        "grande bretagne" to "GB",
        "royaume-uni" to "GB",
        "royaume uni" to "GB",
        "angleterre" to "GB",
        "uk" to "GB",
        "u.k." to "GB",
        "great britain" to "GB",
        "états-unis" to "US",
        "etats-unis" to "US",
        "etats unis" to "US",
        "états unis" to "US",
        "usa" to "US",
        "u.s.a." to "US",
        "u.s." to "US",
        "united states" to "US",
        "hollande" to "NL",
        "pays-bas" to "NL",
        "pays bas" to "NL",
        "netherlands" to "NL",
        "republique tcheque" to "CZ",
        "république tchèque" to "CZ",
        "czech republic" to "CZ",
        "coree du sud" to "KR",
        "corée du sud" to "KR",
        "south korea" to "KR",
        "coree du nord" to "KP",
        "corée du nord" to "KP",
        "north korea" to "KP",
        "nouvelle-zelande" to "NZ",
        "nouvelle-zélande" to "NZ",
        "nouvelle zelande" to "NZ",
        "nouvelle zélande" to "NZ",
        "new zealand" to "NZ",
        "afrique du sud" to "ZA",
        "south africa" to "ZA"
    )

    private val nameToIso: Map<String, String> by lazy {
        val map = HashMap<String, String>()
        for (code in Locale.getISOCountries()) {
            val loc = Locale("", code)
            val frName = loc.getDisplayCountry(Locale.FRENCH).lowercase()
            val enName = loc.getDisplayCountry(Locale.ENGLISH).lowercase()
            if (frName.isNotBlank()) map[frName] = code
            if (enName.isNotBlank()) map[enName] = code
        }
        map.putAll(aliases)
        map
    }

    fun formatCountry(countryRaw: String?, targetLocale: Locale): String? {
        if (countryRaw.isNullOrBlank()) return null
        if (targetLocale.language.equals("fr", ignoreCase = true)) {
            return countryRaw.trim()
        }

        val parts = if (countryRaw.contains("/") || countryRaw.contains(",") || countryRaw.contains("•")) {
            countryRaw.split(Regex("""\s*[/,•]\s*"""))
        } else {
            listOf(countryRaw)
        }

        val translated = parts.map { part ->
            val clean = part.trim()
            val lower = clean.lowercase()
            val isoCode = nameToIso[lower]
            if (isoCode != null) {
                val display = Locale("", isoCode).getDisplayCountry(targetLocale)
                if (display.isNotBlank()) display else clean
            } else if (clean.length == 2 && clean.all { it.isLetter() }) {
                val display = Locale("", clean.uppercase()).getDisplayCountry(targetLocale)
                if (display.isNotBlank()) display else clean
            } else {
                clean
            }
        }

        return translated.filter { it.isNotBlank() }.distinct().joinToString(", ")
    }
}
