package com.emptycastle.novery.util.reader

import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Splits a Spanned object into a list of Spanned paragraphs.
 */
fun Spanned.splitByParagraphs(): List<CharSequence> {
    val result = mutableListOf<CharSequence>()
    val text = this.toString()
    var start = 0
    while (start < text.length) {
        var end = text.indexOf('\n', start)
        if (end == -1) end = text.length
        
        val segment = this.subSequence(start, end)
        if (segment.isNotBlank()) {
            result.add(segment)
        }
        start = end + 1
    }
    return result
}

/**
 * Converts a CharSequence (potentially Spanned) to Compose AnnotatedString.
 */
fun CharSequence.toAnnotatedString(): AnnotatedString {
    if (this !is Spanned) return AnnotatedString(this.toString())
    
    return buildAnnotatedString {
        append(this@toAnnotatedString.toString())
        
        // Convert Bold
        getSpans(0, length, StyleSpan::class.java).forEach { span ->
            val start = getSpanStart(span)
            val end = getSpanEnd(span)
            when (span.style) {
                android.graphics.Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                android.graphics.Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                android.graphics.Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
            }
        }
        
        // Convert Underline
        getSpans(0, length, UnderlineSpan::class.java).forEach { span ->
            val start = getSpanStart(span)
            val end = getSpanEnd(span)
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
        }
    }
}

/**
 * Converts a String to AnnotatedString (basic wrapper).
 */
fun String.toAnnotatedString(): AnnotatedString = AnnotatedString(this)
