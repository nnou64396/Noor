package com.noor.quran.common

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Parses the Tanzil Uthmani XML export (`outType=xml`).
 *
 * Structure: `<sura index="N" name="...">` containing `<aya index="N" text="..."`[` bismillah="..."]`/>`.
 * The `bismillah` attribute is present on the first ayah of every surah except 1 (basmala is 1:1) and 9 (none).
 */
object TanzilXmlParser {

    fun parse(file: File): QuranText {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        val document = factory.newDocumentBuilder().parse(file)
        val surahs = mutableListOf<SurahText>()
        val quran = document.documentElement
        require(quran.tagName == "quran") { "unexpected root element: ${quran.tagName}" }
        for (suraNode in childElements(quran)) {
            if (suraNode.tagName != "sura") continue
            val surahNumber = suraNode.getAttribute("index").toInt()
            val nameArabic = suraNode.getAttribute("name")
            var basmala: String? = null
            val ayahs = mutableListOf<QuranAyah>()
            for (ayaNode in childElements(suraNode)) {
                if (ayaNode.tagName != "aya") continue
                val ayaNumber = ayaNode.getAttribute("index").toInt()
                val text = ayaNode.getAttribute("text")
                if (ayaNumber == 1 && ayaNode.hasAttribute("bismillah")) {
                    basmala = ayaNode.getAttribute("bismillah")
                }
                ayahs += QuranAyah(surahNumber, ayaNumber, text)
            }
            surahs += SurahText(surahNumber, nameArabic, basmala, ayahs)
        }
        return QuranText(surahs)
    }

    private fun childElements(node: Node): List<Element> =
        (0 until node.childNodes.length)
            .map { node.childNodes.item(it) }
            .filterIsInstance<Element>()
}