package com.music4music.tvguide.util

object ProgramDescriptionUtils {

    private val brandRegex = Regex("""(?i)\b(?:télé[- ]loisirs|programme[- ]tv\.net)\b""")

    private val boilerplatePhrases = listOf(
        "Chaque jour, Laurent Mariotte propose des recettes simples",
        "Les prévisions météo pour la journée",
        "Les prévisions météo pour le lendemain",
        "Le bulletin météorologique international",
        "Un point quotidien sur l'actualité météo",
        "Retrouvez tous vos programmes de nuit",
        "A suivre, des rediffusions des émissions de la chaîne",
        "Programmes de la nuit",
        "Fin des émissions"
    )

    fun clean(text: String?): String? {
        if (text.isNullOrBlank()) return null

        var cleaned = text.trim()

        if (boilerplatePhrases.any { cleaned.contains(it, ignoreCase = true) }) {
            for (phrase in boilerplatePhrases) {
                if (cleaned.contains(phrase, ignoreCase = true)) {
                    cleaned = cleaned.replace(Regex("""(?i)[^.\n]*""" + Regex.escape(phrase) + """[^.\n]*\.?"""), "")
                        .trim()
                }
            }
        }

        if (cleaned.contains(brandRegex)) {
            cleaned = cleaned.replace(Regex("""(?i)[^.\n]*\b(?:télé[- ]loisirs|programme[- ]tv\.net)\b[^.\n]*\.?"""), "")
                .trim()
        }

        cleaned = cleaned.replace(Regex("""\s{2,}"""), " ").trim()

        if (cleaned.length < 15 || cleaned.all { !it.isLetterOrDigit() }) {
            return null
        }

        return cleaned
    }

    fun cleanWithTitlePrefix(text: String?, title: String): String? {
        val base = clean(text) ?: return null
        val prefixColon = "$title :"
        val prefixDash = "$title -"
        return when {
            base.startsWith(prefixColon, ignoreCase = true) -> base.substring(prefixColon.length).trim()
            base.startsWith(prefixDash, ignoreCase = true) -> base.substring(prefixDash.length).trim()
            else -> base
        }
    }

    fun isRedundant(primary: String?, secondary: String?): Boolean {
        if (secondary.isNullOrBlank()) return true
        if (primary.isNullOrBlank()) return false

        val normPrimary = normalize(primary)
        val normSecondary = normalize(secondary)

        if (normPrimary == normSecondary) return true
        if (normPrimary.startsWith(normSecondary) || normSecondary.startsWith(normPrimary)) return true
        if (normPrimary.contains(normSecondary) || normSecondary.contains(normPrimary)) return true

        return false
    }

    private fun normalize(str: String): String {
        return str.lowercase()
            .replace(Regex("""[^a-z0-9]"""), "")
    }
}
