package com.aliraza.dailytodos

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The app's name lives in `app_name` only. Every other string takes it as a format argument, so a rename
 * is one edit and no stale name or abbreviation is left behind in the UI.
 */
class AppNameTripwireTest {

    @Test
    fun noStringButAppNameSpellsTheAppName() {
        val offenders = texts().filter { (name, text) -> name != APP_NAME && NAMES.containsMatchIn(text) }
        assertEquals(emptyList<Pair<String, String>>(), offenders)
    }

    /** Every string, string-array item and plural item as (resource name, text). */
    private fun texts(): List<Pair<String, String>> {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(STRINGS).documentElement
        return RESOURCE_TAGS.flatMap { tag ->
            val nodes = root.getElementsByTagName(tag)
            (0 until nodes.length).map { nodes.item(it) as Element }
        }.flatMap { element ->
            val name = element.getAttribute("name")
            val items = element.getElementsByTagName("item")
            if (items.length == 0) {
                listOf(name to element.textContent)
            } else {
                (0 until items.length).map { name to items.item(it).textContent }
            }
        }
    }

    private companion object {
        const val APP_NAME = "app_name"

        /** Unit tests run in the module directory. */
        val STRINGS = File("src/main/res/values/strings.xml")
        val RESOURCE_TAGS = listOf("string", "string-array", "plurals")
        val NAMES = Regex("""\bDT\b|Daily Tasks|Daily Todos""")
    }
}
