package com.emptycastle.novery.data.reader

import me.ag2s.epublib.domain.EpubBook
import me.ag2s.epublib.domain.TOCReference
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.ArrayDeque

class RegularBook(val epub: EpubBook) : AbstractBook() {
    private val allTocReferences: List<TOCReference>

    init {
        val flatTOC = mutableListOf<TOCReference>()
        fun flatten(refs: List<TOCReference>) {
            refs.forEach { ref ->
                val isValid = epub.spine.getResourceIndex(ref.resource) != -1
                if (isValid) {
                    flatTOC.add(ref)
                    if (ref.children != null && ref.children.isNotEmpty()) {
                        flatten(ref.children)
                    }
                }
            }
        }
        flatten(epub.tableOfContents.tocReferences)

        allTocReferences = if (flatTOC.size <= 1) {
            epub.spine.spineReferences
                .filter { it.isLinear }
                .mapIndexed { index, spineRef ->
                    val res = spineRef.resource
                    TOCReference(res.title ?: "Chapter ${index + 1}", res)
                }
        } else {
            flatTOC
        }
    }

    override val canReload: Boolean = false

    override fun size(): Int = allTocReferences.size

    override fun title(): String = epub.title ?: "Unknown Book"

    override fun getChapterTitle(index: Int): String {
        return allTocReferences.getOrNull(index)?.title ?: "Chapter ${index + 1}"
    }

    override fun getLoadingStatus(index: Int): String? = null

    override fun author(): String? {
        val author = epub.metadata.authors.firstOrNull() ?: return null
        return listOfNotNull(author.firstname, author.lastname).joinToString(" ").ifBlank { null }
    }

    override fun loadImage(image: String): ByteArray? {
        val decodedImage = try { URLDecoder.decode(image, "UTF-8") } catch (e: Exception) { image }

        epub.resources.resourceMap[decodedImage]?.data?.let { return it }
        epub.resources.resourceMap[image]?.data?.let { return it }

        val fileName = decodedImage.substringAfterLast("/")
        return epub.resources.resourceMap.values.find {
            val entryName = it.href.substringAfterLast("/")
            entryName.equals(fileName, ignoreCase = true)
        }?.data
    }

    override suspend fun getChapterData(index: Int, reload: Boolean): String {
        val start = allTocReferences[index].resource
        val startIdx = epub.spine.getResourceIndex(start)

        val end = allTocReferences.getOrNull(index + 1)?.resource
        var endIdx = epub.spine.getResourceIndex(end)
        if (endIdx == -1) {
            endIdx = epub.spine.spineReferences.size
        }
        val builder = StringBuilder()

        for (i in startIdx until endIdx) {
            try {
                val ref = epub.spine.spineReferences[i]
                if (!ref.isLinear && i != startIdx) continue

                val html = ref.resource.reader.readText()
                val doc = Jsoup.parse(html)
                val basePath = ref.resource.href.substringBeforeLast("/", "")

                doc.select("img, image").forEach { img ->
                    val attrName = if (img.tagName() == "image") "xlink:href" else "src"
                    var src = img.attr(attrName)
                    if (src.isNotEmpty() && !src.startsWith("http") && !src.startsWith("data:")) {
                        try {
                            val decodedSrc = URLDecoder.decode(src, "UTF-8")
                            src = resolveRelativePath(basePath, decodedSrc)
                        } catch (e: Throwable) {
                            // Ignore error
                        }
                    }
                    if (img.tagName() == "image") {
                        val newImg = doc.createElement("img")
                        newImg.attr("src", src)
                        img.replaceWith(newImg)
                    } else {
                        img.attr("src", src)
                    }
                }

                builder.append(doc.body().html())
            } catch (t: Throwable) {
                // Ignore error
            }
        }
        return builder.toString()
    }

    private fun resolveRelativePath(basePath: String, relativePath: String): String {
        val cleanRelative = relativePath.substringBefore("?").substringBefore("#")

        val fullPath = if (cleanRelative.startsWith("/") || basePath.isEmpty()) {
            cleanRelative
        } else {
            "$basePath/$cleanRelative"
        }

        val parts = fullPath.split("/")
        val resolvedParts = ArrayDeque<String>()

        for (part in parts) {
            when (part) {
                "", "." -> continue
                ".." -> if (resolvedParts.isNotEmpty()) resolvedParts.removeLast()
                else -> resolvedParts.addLast(part)
            }
        }
        return resolvedParts.joinToString("/")
    }

    override fun expand(last: String): Boolean = false

    override suspend fun posterBytes(): ByteArray? = epub.coverImage?.data
}
