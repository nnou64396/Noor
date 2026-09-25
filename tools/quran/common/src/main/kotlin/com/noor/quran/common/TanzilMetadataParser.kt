package com.noor.quran.common

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Parses Tanzil's metadata file (https://tanzil.net/res/text/metadata/quran-data.xml), version 1.0.
 *
 * Contains: 114 `<sura>` elements (index, ayas, start 0-based, name, tname, ename, type, order, rukus),
 * 30 `<juz index sura aya>`, 604 `<page index sura aya>`, 15 `<sajda sura aya>`.
 * Boundary ayah values are 1-based and inclusive.
 */
object TanzilMetadataParser {

    fun parse(file: File): QuranMetadata {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        val document = factory.newDocumentBuilder().parse(file)
        val quran = document.documentElement
        require(quran.tagName == "quran") { "unexpected root element: ${quran.tagName}" }

        fun itemElements(containerTag: String, itemTag: String): List<Element> =
            childElements(quran).firstOrNull { it.tagName == containerTag }
                ?.let { container -> childElements(container).filter { it.tagName == itemTag } }
                .orEmpty()

        val suras = itemElements("suras", "sura")
        val juzs = itemElements("juzs", "juz")
        val pages = itemElements("pages", "page")
        val sajdas = itemElements("sajdas", "sajda")

        val surahMeta = suras.map { el ->
            SurahMeta(
                surahNumber = el.getAttribute("index").toInt(),
                ayahCount = el.getAttribute("ayas").toInt(),
                startGlobalIndex = el.getAttribute("start").toInt(),
                nameArabic = el.getAttribute("name"),
                nameTransliteration = el.getAttribute("tname"),
                nameEnglish = el.getAttribute("ename"),
                revelationType = if (el.getAttribute("type").startsWith("Mec", ignoreCase = true)) {
                    RevelationType.MECCAN
                } else {
                    RevelationType.MEDINAN
                },
                revelationOrder = el.getAttribute("order").toInt(),
                rukus = el.getAttribute("rukus").toInt(),
            )
        }

        fun boundary(el: Element) = AyahRef(
            surahNumber = el.getAttribute("sura").toInt(),
            ayahNumberInSurah = el.getAttribute("aya").toInt(),
        )

        return QuranMetadata(
            surahs = surahMeta,
            juz = juzs.map { JuzBoundary(it.getAttribute("index").toInt(), boundary(it)) },
            pages = pages.map { PageBoundary(it.getAttribute("index").toInt(), boundary(it)) },
            sajdas = sajdas.map(::boundary),
        )
    }

    private fun childElements(node: Node): List<Element> =
        (0 until node.childNodes.length)
            .map { node.childNodes.item(it) }
            .filterIsInstance<Element>()
}