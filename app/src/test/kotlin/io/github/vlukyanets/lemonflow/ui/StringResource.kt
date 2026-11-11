package io.github.vlukyanets.lemonflow.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** One translatable resource: a `<string>`, or a `<plurals>` with all its items' texts. */
data class StringResource(val name: String, val isPlural: Boolean, val texts: List<String>)

/** Reads `src/main/res/<folder>/strings.xml`; unit tests run with the module as working dir. */
fun readStrings(folder: String): List<StringResource> {
    val file = File("src/main/res/$folder/strings.xml")
    check(file.exists()) { "missing ${file.absolutePath}" }
    val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
    val strings = root.elements("string")
        .filter { it.getAttribute("translatable") != "false" }
        .map { StringResource(it.getAttribute("name"), isPlural = false, listOf(it.textContent)) }
    val plurals = root.elements("plurals").map { plural ->
        StringResource(
            plural.getAttribute("name"),
            isPlural = true,
            plural.elements("item").map { it.textContent }
        )
    }
    return strings + plurals
}

private fun Element.elements(tag: String): List<Element> {
    val nodes = getElementsByTagName(tag)
    return (0 until nodes.length).map { nodes.item(it) as Element }
}
