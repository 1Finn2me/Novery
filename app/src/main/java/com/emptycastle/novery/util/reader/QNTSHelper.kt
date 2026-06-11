package com.emptycastle.novery.util.reader

import org.jsoup.Jsoup

object QNTSHelper {

    data class TTSLine(
        val speakOutMsg: String,
        val startChar: Int,
        val endChar: Int,
        val chapterIndex: Int,
    )

    fun preParseHtml(text: String, authorNotes: Boolean): String {
        val document = Jsoup.parse(text)

        // Remove useless stuff
        document.select("style").remove()
        document.select("script").remove()
        document.select("img").removeAttr("alt")

        // This is for poorly generated epubs
        val titleElement = document.selectFirst("title")
        if (titleElement != null) {
            val titleText = titleElement.text().trim()
            val pathRegex = Regex("^(/|[a-zA-Z]:[\\\\/]).*")
            if (pathRegex.matches(titleText)) {
                titleElement.remove()
            }
        }

        if (!authorNotes) {
            document.select("div.qnauthornotecontainer").remove()
        }

        return document.html()
            .replace("</td>", " </td>")
            .replace("</tr>", "<br/></tr>")
            .replace("...", "…")
            // Remove common junk in web novels
            .replace("<p>.*<strong>Translator:.*?Editor:.*>".toRegex(), "")
            .replace("<.*?Translator:.*?Editor:.*?>".toRegex(), "")
    }

    private fun isValidSpeakOutMsg(msg: String): Boolean {
        return msg.isNotEmpty() && msg.isNotBlank() && msg.contains("[A-za-z0-9]".toRegex())
    }

    /**
     * Ports the granular text segmentation logic from QuickNovel.
     * Splits text into logical sentences for TTS, handling abbreviations like Dr. and Mr.
     */
    fun ttsParseText(text: String, chapterIndex: Int): List<TTSLine> {
        val cleanText = text
            .replace("\\.([A-za-z])".toRegex(), ",$1")
            .replace("([.:])([0-9])".toRegex(), ",$2") // Decimals
            .replace(
                "(^|[ \"“‘'])(Dr|Mr|Mrs)\\. ([A-Z])".toRegex(),
                "$1$2, $3"
            )

        val ttsLines = mutableListOf<TTSLine>()

        val invalidStartChars = arrayOf(
            ' ', '.', ',', '\n', '\"',
            '\'', '’', '‘', '“', '”', '«', '»', '「', '」', '…', '[', ']'
        )
        val endingCharacters = arrayOf(".", "\n", ";", "?", ":")
        
        var index = 0
        while (true) {
            if (index >= text.length) break
            
            while (index < text.length && invalidStartChars.contains(text[index])) {
                index++
            }
            if (index >= text.length) break

            var endIndex = Int.MAX_VALUE
            for (a in endingCharacters) {
                val indexEnd = cleanText.indexOf(a, index)
                if (indexEnd != -1 && indexEnd < endIndex) {
                    endIndex = indexEnd + 1
                }
            }

            if (endIndex > text.length) {
                endIndex = text.length
            }

            // Trim newline from end
            var finalEnd = endIndex
            while (finalEnd > index && text[finalEnd - 1] == '\n') {
                finalEnd--
            }

            try {
                val message = text.substring(index, finalEnd)
                var msg = message
                val invalidChars = arrayOf(
                    "-", "<", ">", "_", "^", "«", "»", "「", "」", "—", "–", "¿", "*", "~", "\u200c"
                )
                for (c in invalidChars) {
                    msg = msg.replace(c, " ")
                }
                msg = msg.replace("...", " ")

                if (msg.isNotBlank() && isValidSpeakOutMsg(msg)) {
                    ttsLines.add(TTSLine(msg, index, finalEnd, chapterIndex))
                }
            } catch (t: Throwable) {
                break
            }
            
            index = endIndex
            if (text.getOrNull(index)?.isWhitespace() == true) {
                index++
            }
        }

        return ttsLines
    }
}
