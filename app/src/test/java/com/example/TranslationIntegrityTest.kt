package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the translation files against silent drift.
 *
 * The app offers English, Tamil and Hindi. A missing key degrades safely to
 * English, but two things do not: a mismatched format specifier crashes at
 * runtime the moment that string is shown, and a translation left identical to
 * the English reads as finished work when it is not.
 *
 * These are checked against the XML directly rather than through resource ids
 * so the test fails on the file that is actually wrong.
 */
class TranslationIntegrityTest {

    /** app_name stays "Urimai" in every language: it is the product's name. */
    private val intentionallyUntranslated = setOf("app_name")

    private fun readStrings(dir: String): Map<String, String> {
        val file = File("src/main/res/$dir/strings.xml")
        assertTrue("missing resource file: ${file.path}", file.exists())

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val nodes = document.getElementsByTagName("string")

        return buildMap {
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val name = node.attributes.getNamedItem("name").nodeValue
                put(name, node.textContent ?: "")
            }
        }
    }

    private val english by lazy { readStrings("values") }
    private val tamil by lazy { readStrings("values-ta") }
    private val hindi by lazy { readStrings("values-hi") }

    private val formatSpecifier = Regex("""%\d+\$[sd]""")

    @Test
    fun translationsDefineNoKeysMissingFromEnglish() {
        // A key only in a translation is unreachable: nothing resolves it, and
        // it will never be shown.
        val orphanedTamil = tamil.keys - english.keys
        val orphanedHindi = hindi.keys - english.keys
        assertEquals("Tamil keys with no English entry", emptySet<String>(), orphanedTamil)
        assertEquals("Hindi keys with no English entry", emptySet<String>(), orphanedHindi)
    }

    @Test
    fun formatSpecifiersMatchAcrossLanguages() {
        // A specifier mismatch is not cosmetic: formatting throws when the
        // string is rendered, so the crash lands on whoever switched language.
        english.forEach { (key, englishValue) ->
            val expected = formatSpecifier.findAll(englishValue).map { it.value }.toSet()
            listOf("Tamil" to tamil, "Hindi" to hindi).forEach { (language, strings) ->
                val translated = strings[key] ?: return@forEach
                val actual = formatSpecifier.findAll(translated).map { it.value }.toSet()
                assertEquals(
                    "$language translation of '$key' has different format specifiers",
                    expected,
                    actual
                )
            }
        }
    }

    @Test
    fun translatedStringsAreNotCopiesOfTheEnglish() {
        listOf("Tamil" to tamil, "Hindi" to hindi).forEach { (language, strings) ->
            val copied = strings.filter { (key, value) ->
                key !in intentionallyUntranslated && english[key] == value
            }.keys
            assertEquals(
                "$language entries identical to English (untranslated placeholders)",
                emptySet<String>(),
                copied
            )
        }
    }

    @Test
    fun navigationAndTitlesAreFullyTranslated() {
        // Chrome a user navigates by is the part that must not fall back: an
        // English tab bar in a Tamil app is the failure this work exists to fix.
        val mustTranslate = english.keys.filter {
            it.startsWith("nav_") || it.startsWith("title_") || it.startsWith("action_")
        }
        assertTrue("expected navigation keys to exist", mustTranslate.isNotEmpty())

        assertEquals(
            "navigation/title/action keys missing from Tamil",
            emptySet<String>(),
            mustTranslate.toSet() - tamil.keys
        )
        assertEquals(
            "navigation/title/action keys missing from Hindi",
            emptySet<String>(),
            mustTranslate.toSet() - hindi.keys
        )
    }

    @Test
    fun promisesToTheUserAreTranslated() {
        // These state guarantees the system actually enforces — anonymity,
        // contact privacy, that OCR output is advisory. Showing them in a
        // language the reader may not have is how a promise gets missed.
        val promises = english.keys.filter { it.startsWith("promise_") }
        assertTrue("expected promise keys to exist", promises.isNotEmpty())

        assertEquals("promises missing from Tamil", emptySet<String>(), promises.toSet() - tamil.keys)
        assertEquals("promises missing from Hindi", emptySet<String>(), promises.toSet() - hindi.keys)
    }
}
