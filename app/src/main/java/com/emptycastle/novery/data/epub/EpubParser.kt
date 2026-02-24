package com.emptycastle.novery.data.epub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Parser for EPUB files.
 * Extracts metadata, chapters, and cover image from EPUB format.
 */
class EpubParser(private val context: Context) {

    /**
     * Parse an EPUB file from a URI (content:// or file://)
     */
    suspend fun parseEpub(uri: Uri): Result<EpubBook> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(EpubParseException("Cannot open file"))

            parseEpubFromStream(inputStream, getFileName(uri))
        } catch (e: Exception) {
            Result.failure(EpubParseException("Failed to parse EPUB: ${e.message}", e))
        }
    }

    /**
     * Parse an EPUB file from an input stream
     */
    private suspend fun parseEpubFromStream(
        inputStream: InputStream,
        fileName: String? = null
    ): Result<EpubBook> = withContext(Dispatchers.IO) {
        try {
            val files = mutableMapOf<String, ByteArray>()

            // Extract ZIP contents
            ZipInputStream(inputStream).use { zipStream ->
                var entry: ZipEntry? = zipStream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        files[entry.name] = zipStream.readBytes()
                    }
                    entry = zipStream.nextEntry
                }
            }

            // Find container.xml to locate the OPF file
            val containerXml = files["META-INF/container.xml"]
                ?: return@withContext Result.failure(
                    EpubParseException("Invalid EPUB: Missing container.xml")
                )

            val opfPath = parseContainerXml(String(containerXml))
                ?: return@withContext Result.failure(
                    EpubParseException("Invalid EPUB: Cannot find OPF file path")
                )

            val opfContent = files[opfPath]
                ?: return@withContext Result.failure(
                    EpubParseException("Invalid EPUB: OPF file not found at $opfPath")
                )

            val opfDir = opfPath.substringBeforeLast("/", "")

            // Parse OPF file
            val opfData = parseOpfFile(String(opfContent), opfDir)

            // Extract cover image
            val coverImage = extractCoverImage(files, opfData, opfDir)

            // Parse chapters
            val chapters = parseChapters(files, opfData, opfDir)

            Result.success(
                EpubBook(
                    title = opfData.title ?: fileName?.removeSuffix(".epub") ?: "Unknown Title",
                    author = opfData.author,
                    description = opfData.description,
                    coverImage = coverImage,
                    chapters = chapters,
                    language = opfData.language,
                    publisher = opfData.publisher,
                    publishDate = opfData.publishDate,
                    identifier = opfData.identifier,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(EpubParseException("Failed to parse EPUB: ${e.message}", e))
        }
    }

    /**
     * Parse container.xml to find OPF file path
     */
    private fun parseContainerXml(xml: String): String? {
        return try {
            val doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser())
            doc.select("rootfile[media-type='application/oebps-package+xml']")
                .firstOrNull()
                ?.attr("full-path")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse the OPF (Open Packaging Format) file
     */
    private fun parseOpfFile(xml: String, opfDir: String): OpfData {
        val doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser())

        // Parse metadata - handle both namespaced and non-namespaced elements
        val metadata = doc.select("metadata").first()

        val title = metadata?.let {
            it.select("dc\\:title, title").text().takeIf { t -> t.isNotBlank() }
        }

        val author = metadata?.let {
            it.select("dc\\:creator, creator").text().takeIf { t -> t.isNotBlank() }
        }

        val description = metadata?.let {
            it.select("dc\\:description, description").text().takeIf { t -> t.isNotBlank() }
        }

        val language = metadata?.let {
            it.select("dc\\:language, language").text().takeIf { t -> t.isNotBlank() }
        }

        val publisher = metadata?.let {
            it.select("dc\\:publisher, publisher").text().takeIf { t -> t.isNotBlank() }
        }

        val publishDate = metadata?.let {
            it.select("dc\\:date, date").text().takeIf { t -> t.isNotBlank() }
        }

        val identifier = metadata?.let {
            it.select("dc\\:identifier, identifier").text().takeIf { t -> t.isNotBlank() }
        }

        // Find cover image ID from metadata
        val coverMeta = metadata?.select("meta[name='cover']")?.attr("content")

        // Parse manifest (all items in the EPUB)
        val manifestItems = mutableMapOf<String, ManifestItem>()
        doc.select("manifest item").forEach { item ->
            val id = item.attr("id")
            val href = item.attr("href")
            val mediaType = item.attr("media-type")
            val properties = item.attr("properties")

            if (id.isNotBlank() && href.isNotBlank()) {
                manifestItems[id] = ManifestItem(
                    id = id,
                    href = resolvePath(opfDir, href),
                    mediaType = mediaType,
                    properties = properties
                )
            }
        }

        // Parse spine (reading order)
        val spineItems = mutableListOf<String>()
        doc.select("spine itemref").forEach { itemref ->
            val idref = itemref.attr("idref")
            if (idref.isNotBlank()) {
                spineItems.add(idref)
            }
        }

        // Find cover from manifest - check multiple patterns
        val coverId = coverMeta
            ?: manifestItems.entries.find { it.value.properties.contains("cover-image") }?.key
            ?: manifestItems.entries.find {
                it.key.contains("cover", ignoreCase = true) &&
                        it.value.mediaType.startsWith("image/")
            }?.key

        return OpfData(
            title = title,
            author = author,
            description = description,
            language = language,
            publisher = publisher,
            publishDate = publishDate,
            identifier = identifier,
            coverId = coverId,
            manifestItems = manifestItems,
            spineItems = spineItems
        )
    }

    /**
     * Extract cover image from EPUB
     */
    private fun extractCoverImage(
        files: Map<String, ByteArray>,
        opfData: OpfData,
        opfDir: String
    ): Bitmap? {
        // Try to find cover from manifest
        val coverItem = opfData.coverId?.let { opfData.manifestItems[it] }

        if (coverItem != null) {
            val imageData = files[coverItem.href]
                ?: files[resolvePath(opfDir, coverItem.href)]
            if (imageData != null) {
                return try {
                    BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                } catch (e: Exception) {
                    null
                }
            }
        }

        // Fallback: look for common cover image patterns
        val coverEntry = files.entries.find { (path, _) ->
            val lowerPath = path.lowercase()
            (lowerPath.contains("cover") || lowerPath.contains("frontcover")) &&
                    (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") ||
                            lowerPath.endsWith(".png") || lowerPath.endsWith(".gif"))
        }

        return coverEntry?.value?.let { imageData ->
            try {
                BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Parse chapters from spine order
     */
    private fun parseChapters(
        files: Map<String, ByteArray>,
        opfData: OpfData,
        opfDir: String
    ): List<EpubChapter> {
        val chapters = mutableListOf<EpubChapter>()
        var chapterIndex = 0

        opfData.spineItems.forEach { itemId ->
            val manifestItem = opfData.manifestItems[itemId] ?: return@forEach

            // Only process HTML/XHTML content
            if (!manifestItem.mediaType.contains("html", ignoreCase = true) &&
                !manifestItem.mediaType.contains("xml", ignoreCase = true)
            ) {
                return@forEach
            }

            val filePath = manifestItem.href
            val content = files[filePath]
                ?: files[resolvePath(opfDir, manifestItem.href)]

            if (content != null) {
                val htmlContent = String(content, Charsets.UTF_8)
                val parsedChapter = parseChapterContent(htmlContent, chapterIndex)

                // Only add chapters with actual content (skip navigation, etc.)
                if (parsedChapter.content.length > 50) {
                    chapters.add(parsedChapter)
                    chapterIndex++
                }
            }
        }

        return chapters
    }

    /**
     * Parse individual chapter HTML content
     */
    private fun parseChapterContent(html: String, index: Int): EpubChapter {
        val doc = Jsoup.parse(html)

        // Remove scripts, styles, and navigation elements
        doc.select("script, style, link, nav, header, footer").remove()

        // Extract title from various sources
        val title = doc.select("title").text().takeIf { it.isNotBlank() && it.length < 100 }
            ?: doc.select("h1").first()?.text()?.takeIf { it.isNotBlank() && it.length < 100 }
            ?: doc.select("h2").first()?.text()?.takeIf { it.isNotBlank() && it.length < 100 }
            ?: doc.select(".chapter-title, .chaptertitle").first()?.text()
                ?.takeIf { it.isNotBlank() && it.length < 100 }
            ?: "Chapter ${index + 1}"

        // Extract body content
        val body = doc.select("body").first() ?: doc

        // Convert to clean text with paragraph structure
        val content = cleanHtmlContent(body)

        return EpubChapter(
            title = title.trim(),
            content = content,
            index = index
        )
    }

    /**
     * Clean HTML content to readable text while preserving structure
     */
    private fun cleanHtmlContent(element: org.jsoup.nodes.Element): String {
        val sb = StringBuilder()

        // Process paragraphs and headers
        element.select("p, h1, h2, h3, h4, h5, h6, div.paragraph, div.p").forEach { el ->
            val text = el.text().trim()
            if (text.isNotBlank()) {
                sb.appendLine(text)
                sb.appendLine()
            }
        }

        // If no structured content found, fall back to body text
        if (sb.isBlank()) {
            val text = element.text()
                .replace(Regex("\\s+"), " ")
                .trim()

            // Try to split into paragraphs at sentence breaks
            text.split(Regex("(?<=[.!?])\\s+(?=[A-Z])")).forEach { paragraph ->
                if (paragraph.isNotBlank()) {
                    sb.appendLine(paragraph.trim())
                    sb.appendLine()
                }
            }
        }

        return sb.toString()
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    /**
     * Resolve relative path from OPF directory
     */
    private fun resolvePath(opfDir: String, href: String): String {
        val cleanHref = java.net.URLDecoder.decode(href, "UTF-8")
        return if (opfDir.isBlank()) {
            cleanHref
        } else {
            "$opfDir/$cleanHref"
        }.replace("//", "/")
    }

    /**
     * Get file name from URI
     */
    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
}

/**
 * Internal data classes for OPF parsing
 */
private data class OpfData(
    val title: String?,
    val author: String?,
    val description: String?,
    val language: String?,
    val publisher: String?,
    val publishDate: String?,
    val identifier: String?,
    val coverId: String?,
    val manifestItems: Map<String, ManifestItem>,
    val spineItems: List<String>
)

private data class ManifestItem(
    val id: String,
    val href: String,
    val mediaType: String,
    val properties: String = ""
)

/**
 * Represents a parsed EPUB book
 */
data class EpubBook(
    val title: String,
    val author: String?,
    val description: String?,
    val coverImage: Bitmap?,
    val chapters: List<EpubChapter>,
    val language: String? = null,
    val publisher: String? = null,
    val publishDate: String? = null,
    val identifier: String? = null,
    val fileName: String? = null
)

/**
 * Represents a chapter in an EPUB
 */
data class EpubChapter(
    val title: String,
    val content: String,
    val index: Int
)

/**
 * Exception for EPUB parsing errors
 */
class EpubParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)