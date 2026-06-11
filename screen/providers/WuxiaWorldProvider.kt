package com.lagradost.quicknovel.providers

import com.lagradost.quicknovel.*
import com.lagradost.quicknovel.MainActivity.Companion.app
import com.lagradost.quicknovel.R
import org.jsoup.Jsoup
import java.util.*
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import java.util.zip.GZIPInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class WuxiaWorldProvider : MainAPI() {
    override val name = "Wuxia World"
    override val mainUrl = "https://www.wuxiaworld.com"
    private val apiPrefix = "https://api2.wuxiaworld.com/wuxiaworld.api.v2."
    override val hasMainPage = true
    override val iconId = R.drawable.icon_wuxiaworld
    override val iconBackgroundId = R.color.colorPrimaryWhite

    private val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val TAG = "WuxiaWorldProvider"

    // JSON Data models for Search/Popular
    data class WuxiaSearchItem(
        @JsonProperty("name") val name: String,
        @JsonProperty("slug") val slug: String,
        @JsonProperty("coverUrl") val coverUrl: String?
    )
    data class WuxiaSearchResponse(
        @JsonProperty("items") val items: List<WuxiaSearchItem>
    )

    override suspend fun loadMainPage(
        page: Int,
        mainCategory: String?,
        orderBy: String?,
        tag: String?
    ): HeadMainPageResponse {
        val link = "$mainUrl/api/novels"
        val response = app.get(link).text
        val data = mapper.readValue(response, WuxiaSearchResponse::class.java)
        val novels = data.items.map { novel ->
            newSearchResponse(name = novel.name, url = "novel/${novel.slug}/") {
                posterUrl = novel.coverUrl
            }
        }
        return HeadMainPageResponse(link, novels)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/api/novels/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val response = app.get(url).text
        val data = mapper.readValue(response, WuxiaSearchResponse::class.java)
        return data.items.map { novel ->
            newSearchResponse(name = novel.name, url = "novel/${novel.slug}/") {
                posterUrl = novel.coverUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        // Robust slug extraction: /novel/martial-god-asura/ -> martial-god-asura
        val slug = url.trimEnd('/').substringAfterLast('/')
        Log.d(TAG, "Loading novel with slug: $slug from url: $url")
        
        // Manual Protobuf for GetNovelRequest: { slug (tag 2): slug }
        val requestBody = ProtoWriter().writeString(2, slug).toGrpcFrame()
        val response = app.post(
            "${apiPrefix}Novels/GetNovel",
            headers = mapOf("Content-Type" to "application/grpc-web+proto"),
            requestBody = requestBody.toRequestBody("application/grpc-web+proto".toMediaTypeOrNull())
        ).body.bytes()

        val novelNode = ProtoReader(unframe(response)).readMessage(1) ?: return null
        
        val name = novelNode.readString(2) ?: "Untitled"
        val novelId = novelNode.readInt(1)
        Log.d(TAG, "Found name: $name, novelId: $novelId")
        
        // Load Chapters: GetChapterListRequest { novelId (tag 1): novelId }
        val chapterListBody = ProtoWriter().writeInt(1, novelId).toGrpcFrame()
        val chapterResponse = app.post(
            "${apiPrefix}Chapters/GetChapterList",
            headers = mapOf("Content-Type" to "application/grpc-web+proto"),
            requestBody = chapterListBody.toRequestBody("application/grpc-web+proto".toMediaTypeOrNull())
        ).body.bytes()

        val chapters = mutableListOf<ChapterData>()
        val chapterReader = ProtoReader(unframe(chapterResponse))
        while (true) {
            val groupBytes = chapterReader.readFieldNext(1) as? ByteArray ?: break
            val groupNode = ProtoReader(groupBytes)
            
            // Read repeated ChapterItem (tag 6) in group
            val groupContentReader = ProtoReader(groupBytes)
            while (true) {
                val chapBytes = groupContentReader.readFieldNext(6) as? ByteArray ?: break
                val chapItem = ProtoReader(chapBytes)
                val cName = chapItem.readString(2) ?: ""
                val cSlug = chapItem.readString(3) ?: ""
                
                chapters.add(newChapterData(name = cName, url = "novel/$slug/$cSlug"))
            }
        }
        Log.d(TAG, "Found ${chapters.size} chapters")

        val cover = novelNode.readMessage(10)?.readString(1)
        val description = novelNode.readMessage(8)?.readString(1)
        val synopsisText = novelNode.readMessage(9)?.readString(1)
        val mergedSynopsis = listOfNotNull(description, synopsisText)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .takeIf { it.isNotBlank() }
            ?.let { Jsoup.parse(it).text() }
        val genres = novelNode.readAllStrings(16)

        return newStreamResponse(url = url, name = name, data = chapters) {
            posterUrl = cover
            synopsis = mergedSynopsis
            author = novelNode.readMessage(13)?.readString(1)
            tags = genres
            Log.d(TAG, "Found ${synopsis} synopsis")
            val statusVal = novelNode.readInt(4)
            status = when (statusVal) {
                1 -> ReleaseStatus.Ongoing
                2 -> ReleaseStatus.Paused
                0 -> ReleaseStatus.Completed
                else -> null
            }
        }
    }

    override suspend fun loadHtml(url: String): String? {
        val paths = url.trim('/').split("/")
        val novelSlug = paths.getOrNull(paths.indexOf("novel") + 1) ?: return null
        val chapterSlug = paths.getOrNull(paths.lastIndex) ?: return null

        val slugsWriter = ProtoWriter().writeString(1, novelSlug).writeString(2, chapterSlug)
        val propertyWriter = ProtoWriter().writeMessage(2, slugsWriter.toByteArray())
        val requestBody = ProtoWriter().writeMessage(1, propertyWriter.toByteArray()).toGrpcFrame()

        val response = app.post(
            "${apiPrefix}Chapters/GetChapter",
            headers = mapOf("Content-Type" to "application/grpc-web+proto"),
            requestBody = requestBody.toRequestBody("application/grpc-web+proto".toMediaTypeOrNull())
        ).body.bytes()

        val itemNode = ProtoReader(unframe(response)).readMessage(1)
        return itemNode?.readMessage(5)?.readString(1)
    }

    // --- MINI PROTOBUF HELPERS ---

    private fun unframe(data: ByteArray): ByteArray {
        if (data.size < 5) return data
        val flag = data[0].toInt()
        val length = ByteBuffer.wrap(data, 1, 4).int
        val targetSize = 5 + length
        val payload = if (data.size >= targetSize) data.copyOfRange(5, targetSize) else data.copyOfRange(5, data.size)
        
        return if (flag == 1) { // GZIP compression flag in gRPC-web
            try {
                GZIPInputStream(payload.inputStream()).readBytes()
            } catch (e: Exception) {
                payload
            }
        } else {
            payload
        }
    }

    class ProtoWriter {
        private val out = ByteArrayOutputStream()

        fun writeInt(tag: Int, value: Int): ProtoWriter {
            writeTag(tag, 0)
            writeVarint(value.toLong())
            return this
        }

        fun writeString(tag: Int, value: String): ProtoWriter {
            writeTag(tag, 2)
            val bytes = value.toByteArray()
            writeVarint(bytes.size.toLong())
            out.write(bytes)
            return this
        }

        fun writeMessage(tag: Int, payload: ByteArray): ProtoWriter {
            writeTag(tag, 2)
            writeVarint(payload.size.toLong())
            out.write(payload)
            return this
        }

        private fun writeTag(tag: Int, wireType: Int) {
            writeVarint(((tag shl 3) or wireType).toLong())
        }

        private fun writeVarint(value: Long) {
            var v = value
            while (v and -128L != 0L) {
                out.write(((v and 127L) or 128L).toInt())
                v = v ushr 7
            }
            out.write(v.toInt())
        }

        fun toByteArray() = out.toByteArray()
        fun toGrpcFrame(): ByteArray {
            val payload = toByteArray()
            val frame = ByteBuffer.allocate(5 + payload.size)
            frame.put(0.toByte())
            frame.putInt(payload.size)
            frame.put(payload)
            return frame.array()
        }
    }

    class ProtoReader(val payload: ByteArray) {
        private var pos = 0

        fun readInt(targetTag: Int): Int {
            return (readField(targetTag) as? Long)?.toInt() ?: 0
        }


        fun readString(targetTag: Int): String? {
            val bytes = readField(targetTag) as? ByteArray ?: return null
            return String(bytes)
        }

        fun readMessage(targetTag: Int): ProtoReader? {
            val bytes = readField(targetTag) as? ByteArray ?: return null
            return ProtoReader(bytes)
        }

        /** Simple search from the start (useful for out-of-order fields) */
        fun readField(targetTag: Int): Any? {
            pos = 0 
            return readFieldNext(targetTag)
        }
fun readAllStrings(targetTag: Int): List<String> {
            pos = 0
            val results = mutableListOf<String>()
            while (true) {
                val bytes = readFieldNext(targetTag) as? ByteArray ?: break
                results.add(String(bytes))
            }
            return results
        }
        /** Search from the current position (useful for repeated fields) */
        fun readFieldNext(targetTag: Int): Any? {
            try {
                while (pos < payload.size) {
                    val tagAndWire = readVarint()
                    val tag = (tagAndWire shr 3).toInt()
                    val wire = (tagAndWire and 7).toInt()
                    
                    if (tag == targetTag) {
                        return when (wire) {
                            0 -> readVarint()
                            2 -> {
                                val len = readVarint().toInt()
                                if (pos + len > payload.size) return null
                                val bytes = payload.copyOfRange(pos, pos + len)
                                pos += len
                                bytes
                            }
                            else -> skipField(wire)
                        }
                    } else {
                        skipField(wire)
                    }
                }
            } catch (e: Exception) { }
            return null
        }

        private fun readVarint(): Long {
            var res = 0L
            var shift = 0
            while (pos < payload.size) {
                val b = payload[pos++].toInt()
                res = res or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) break
                shift += 7
                if (shift > 64) break
            }
            return res
        }

        private fun skipField(wire: Int): Any? {
            when (wire) {
                0 -> readVarint()
                1 -> pos += 8
                2 -> {
                    val len = readVarint().toInt()
                    pos += len
                }
                5 -> pos += 4
            }
            return null
        }
    }
}
