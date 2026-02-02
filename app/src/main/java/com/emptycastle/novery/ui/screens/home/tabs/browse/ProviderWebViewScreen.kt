package com.emptycastle.novery.ui.screens.home.tabs.browse

import android.R.attr.name
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.emptycastle.novery.data.remote.CloudflareManager
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ============================================================================
// Color Constants
// ============================================================================

private object WebViewColors {
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Info = Color(0xFF3B82F6)
    val Secure = Color(0xFF22C55E)
    val Generic = Color(0xFF8B5CF6)  // Purple for generic/unknown sites
}

// ============================================================================
// Extraction System for Novel Sites
// ============================================================================

/**
 * Improved page data model with more novel-specific fields
 */
@Serializable
data class ExtractedPageData(
    // Basic info
    val url: String = "",
    val title: String = "",
    val alternativeTitles: List<String> = emptyList(),
    val description: String = "",
    val coverImage: String = "",

    // Novel metadata
    val author: String = "",
    val artist: String = "",
    val status: String = "",  // Ongoing, Completed, Hiatus
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val rating: String = "",
    val ratingCount: String = "",
    val views: String = "",
    val bookmarks: String = "",

    // Chapter info
    val chapterCount: Int = 0,
    val latestChapter: String = "",
    val firstChapterUrl: String = "",
    val hasChapterList: Boolean = false,

    // Site info
    val siteName: String = "",
    val siteType: String = "",  // "novel_site", "mtl_site", "webnovel", "generic"
    val pageType: String = "",  // "novel", "chapter", "search", "list", "unknown"

    // Raw data for debugging/custom processing
    val rawMetadata: Map<String, String> = emptyMap(),

    // Confidence score (0-100)
    val confidence: Int = 0
)

/**
 * Comprehensive JavaScript extraction script
 * Handles multiple novel site formats with site-specific and generic extractors
 */
private val ENHANCED_PAGE_EXTRACTION_SCRIPT = """
(function() {
    'use strict';
    
    // ========================================
    // Utility Functions
    // ========================================
    
    const utils = {
        // Safely get text content from selector
        getText(selector, context = document) {
            const el = context.querySelector(selector);
            return el ? el.textContent.trim().replace(/\s+/g, ' ') : '';
        },
        
        // Get all matching texts
        getAllTexts(selector, context = document) {
            return Array.from(context.querySelectorAll(selector))
                .map(el => el.textContent.trim())
                .filter(Boolean);
        },
        
        // Get attribute value
        getAttr(selector, attr, context = document) {
            const el = context.querySelector(selector);
            return el ? (el.getAttribute(attr) || '') : '';
        },
        
        // Get meta content
        getMeta(name) {
            return this.getAttr(`meta[property="${name}"]`, 'content') ||
                   this.getAttr(`meta[name="${name}"]`, 'content') || '';
        },
        
        // Get JSON-LD data
        getJsonLd() {
            const scripts = document.querySelectorAll('script[type="application/ld+json"]');
            const data = [];
            scripts.forEach(script => {
                try {
                    const json = JSON.parse(script.textContent);
                    if (Array.isArray(json)) {
                        data.push(...json);
                    } else {
                        data.push(json);
                    }
                } catch (e) {}
            });
            return data;
        },
        
        // Find text by label (e.g., "Author: Name" -> "Name")
        getValueByLabel(labels, context = document) {
            const labelPatterns = Array.isArray(labels) ? labels : [labels];
            
            for (const pattern of labelPatterns) {
                // Try finding in definition lists
                const dts = context.querySelectorAll('dt, .info-label, .meta-label, th');
                for (const dt of dts) {
                    if (dt.textContent.toLowerCase().includes(pattern.toLowerCase())) {
                        const dd = dt.nextElementSibling;
                        if (dd) return dd.textContent.trim();
                    }
                }
                
                // Try finding in info items
                const infoItems = context.querySelectorAll('.info-item, .meta-item, li, tr, p, span, div');
                for (const item of infoItems) {
                    const text = item.textContent;
                    const regex = new RegExp(pattern + '[:\\s]+(.+)', 'i');
                    const match = text.match(regex);
                    if (match) {
                        // Get just the value part, not the whole item
                        const valueEl = item.querySelector('a, span:last-child, .value');
                        if (valueEl && !valueEl.textContent.toLowerCase().includes(pattern.toLowerCase())) {
                            return valueEl.textContent.trim();
                        }
                        return match[1].trim().split(/[,\n]/)[0].trim();
                    }
                }
            }
            return '';
        },
        
        // Get image src with fallbacks
        getImageSrc(selectors) {
            const selectorList = Array.isArray(selectors) ? selectors : [selectors];
            for (const selector of selectorList) {
                const img = document.querySelector(selector);
                if (img) {
                    const src = img.getAttribute('data-src') || 
                               img.getAttribute('data-lazy-src') ||
                               img.getAttribute('data-original') ||
                               img.src;
                    if (src && !src.includes('data:image') && !src.includes('loading')) {
                        // Make absolute URL
                        if (src.startsWith('//')) return 'https:' + src;
                        if (src.startsWith('/')) return window.location.origin + src;
                        return src;
                    }
                }
            }
            return '';
        },
        
        // Clean text (remove extra whitespace, etc.)
        cleanText(text) {
            return text
                .replace(/\s+/g, ' ')
                .replace(/\n+/g, ' ')
                .trim();
        },
        
        // Extract number from text
        extractNumber(text) {
            const match = text.match(/[\d,]+/);
            return match ? match[0].replace(/,/g, '') : '';
        },
        
        // Get domain
        getDomain() {
            return window.location.hostname.replace('www.', '').toLowerCase();
        }
    };
    
    // ========================================
    // Site-Specific Extractors
    // ========================================
    
    const siteExtractors = {
        // ====== SonicMTL ======
        'sonicmtl.com': {
            name: 'SonicMTL',
            type: 'mtl_site',
            extract() {
                return {
                    title: utils.getText('.novel-title, .post-title, h1.entry-title, h1'),
                    description: utils.getText('.novel-description, .description, .summary, .entry-content p'),
                    coverImage: utils.getImageSrc([
                        '.novel-cover img',
                        '.book-cover img',
                        '.post-thumbnail img',
                        '.wp-post-image',
                        'article img'
                    ]),
                    author: utils.getValueByLabel(['Author', 'Writer', '作者']),
                    status: utils.getValueByLabel(['Status', '状态']),
                    genres: utils.getAllTexts('.genre a, .genres a, .category a, [rel="tag"]'),
                    chapterCount: document.querySelectorAll('.chapter-list a, .chapters a, article a[href*="chapter"]').length,
                    latestChapter: utils.getText('.latest-chapter a, .chapter-list li:first-child a, .chapters li:first-child a')
                };
            }
        },
        
        // ====== FreeWebNovel ======
        'freewebnovel.com': {
            name: 'FreeWebNovel',
            type: 'novel_site',
            extract() {
                const infoDiv = document.querySelector('.m-desc') || document.querySelector('.m-info');
                return {
                    title: utils.getText('.m-desc h1, h1.tit, .book-name'),
                    alternativeTitles: utils.getAllTexts('.m-desc .alt-name, .alternative'),
                    description: utils.getText('.m-desc .txt, .m-info .txt, .desc, .summary'),
                    coverImage: utils.getImageSrc([
                        '.m-book img',
                        '.m-imgtxt img',
                        '.book-img img',
                        '.cover img'
                    ]),
                    author: utils.getText('.m-desc a[href*="author"], .m-info a[href*="author"], .info a[href*="author"]') ||
                            utils.getValueByLabel(['Author']),
                    status: utils.getText('.m-desc .s1, .m-info .s1') || 
                            utils.getValueByLabel(['Status']),
                    genres: utils.getAllTexts('.m-desc a[href*="genre"], .m-info a[href*="genre"], .genres a'),
                    rating: utils.getText('.m-desc .score, .rating .num, .score'),
                    chapterCount: (() => {
                        const countEl = document.querySelector('.m-newest em, .chapter-count, .count');
                        if (countEl) return parseInt(utils.extractNumber(countEl.textContent)) || 0;
                        return document.querySelectorAll('.m-list2 a, #chapters a, .chapter-list a').length;
                    })(),
                    latestChapter: utils.getText('.m-newest a, .latest a, .chapter-list li:last-child a'),
                    firstChapterUrl: utils.getAttr('.m-list2 a:first-child, #chapters a:first-child', 'href')
                };
            }
        },
        
        // ====== NovelFull ======
        'novelfull.com': {
            name: 'NovelFull', 
            type: 'novel_site',
            extract() {
                const infoDiv = document.querySelector('.info') || document.querySelector('.book');
                return {
                    title: utils.getText('.title, h1, h3.title'),
                    description: utils.getText('.desc-text, .description, #desc-text, .summary'),
                    coverImage: utils.getImageSrc([
                        '.book img',
                        '.cover img',
                        '.books img',
                        '.info-holder img'
                    ]),
                    author: utils.getText('a[href*="author"], .author a, .info a[href*="/author/"]') ||
                            utils.getValueByLabel(['Author']),
                    status: (() => {
                        const statusEl = document.querySelector('.info a[href*="status"], .status');
                        if (statusEl) return statusEl.textContent.trim();
                        return utils.getValueByLabel(['Status']);
                    })(),
                    genres: utils.getAllTexts('.info a[href*="genre"], a[href*="/genre/"], .genres a'),
                    rating: utils.getText('.rating .small, .rate .num, .score'),
                    chapterCount: (() => {
                        const pagination = document.querySelector('.pagination li:nth-last-child(2) a, .page-last');
                        if (pagination) {
                            const pageNum = parseInt(pagination.textContent);
                            const itemsPerPage = document.querySelectorAll('#list-chapter a, .list-chapter a').length;
                            if (pageNum && itemsPerPage) return pageNum * itemsPerPage;
                        }
                        return document.querySelectorAll('#list-chapter a, .list-chapter a, .chapter-list a').length;
                    })(),
                    latestChapter: utils.getText('.l-chapter a, .latest a, #list-chapter li:last-child a'),
                    firstChapterUrl: utils.getAttr('#list-chapter a:first-child, .list-chapter a:first-child', 'href')
                };
            }
        },
        
        // ====== NovelBuddy ======
        'novelbuddy.com': {
            name: 'NovelBuddy',
            type: 'novel_site',
            extract() {
                return {
                    title: utils.getText('.name h1, .novel-title h1, h1'),
                    alternativeTitles: utils.getAllTexts('.name h2, .alt-name, .alternative'),
                    description: utils.getText('.summary .content, .description .content, .desc, #syn-target'),
                    coverImage: utils.getImageSrc([
                        '.img-cover img',
                        '.cover img',
                        '.thumb img',
                        'figure img'
                    ]),
                    author: utils.getText('a[href*="/authors/"], .author a, span[itemprop="author"]') ||
                            utils.getValueByLabel(['Author', 'Authors']),
                    status: utils.getText('.status, .meta span:contains("Status")') ||
                            utils.getValueByLabel(['Status']),
                    genres: utils.getAllTexts('a[href*="/genres/"], .genre a, .categories a'),
                    tags: utils.getAllTexts('a[href*="/tags/"], .tag a'),
                    rating: utils.getText('.rating .score, .rate-num, [itemprop="ratingValue"]'),
                    ratingCount: utils.extractNumber(utils.getText('.rating .count, [itemprop="ratingCount"]')),
                    views: utils.extractNumber(utils.getText('.views, .view-count, span:contains("View")')),
                    chapterCount: (() => {
                        const countText = utils.getText('.count-chapter, .chapter-count, .meta:contains("Chapter")');
                        const count = parseInt(utils.extractNumber(countText));
                        if (count) return count;
                        return document.querySelectorAll('.chapter-list a, #chapter-list a, .chapters a').length;
                    })(),
                    latestChapter: utils.getText('.latest-chapter a, .chapter-list li:first-child a'),
                    firstChapterUrl: utils.getAttr('.chapter-list a:last-child, #chapter-list a:last-child', 'href')
                };
            }
        },
        
        // ====== NovelBin ======
        'novelbin.com': {
            name: 'NovelBin',
            type: 'novel_site',
            extract() {
                return {
                    title: utils.getText('.novel-title, h1.title, h3.title'),
                    description: utils.getText('.desc-text, .summary-content, .description'),
                    coverImage: utils.getImageSrc(['.cover img', '.book img', '.thumb img']),
                    author: utils.getText('a[href*="/author/"]') || utils.getValueByLabel(['Author']),
                    status: utils.getValueByLabel(['Status']),
                    genres: utils.getAllTexts('a[href*="/genre/"]'),
                    rating: utils.getText('.rating .num, .score'),
                    chapterCount: document.querySelectorAll('.chapter-list a, #list-chapter a').length,
                    latestChapter: utils.getText('.latest a, .chapter-list li:last-child a')
                };
            }
        },
        
        // ====== NovelFire ======
        'novelfire.net': {
            name: 'NovelFire',
            type: 'novel_site',
            extract() {
                return {
                    title: utils.getText('.novel-title h1, h1.novel-title, .name'),
                    alternativeTitles: utils.getAllTexts('.alternative, .alt-name'),
                    description: utils.getText('.summary, .novel-summary, #editdescription'),
                    coverImage: utils.getImageSrc(['.novel-cover img', '.cover img', 'figure.cover img']),
                    author: utils.getText('a[href*="/author/"], .author a'),
                    status: utils.getText('.status, .novel-status'),
                    genres: utils.getAllTexts('.categories a, .genre a, a[href*="/genre/"]'),
                    rating: utils.getText('.rating .num'),
                    chapterCount: document.querySelectorAll('.chapter-list a, #chapter-list a').length,
                    latestChapter: utils.getText('.chapter-list li:first-child a')
                };
            }
        },
        
        // ====== LightNovelPub / LightNovelWorld ======
        'lightnovelpub.com': { alias: 'lightnovel_generic' },
        'lightnovelworld.com': { alias: 'lightnovel_generic' },
        'lightnovel_generic': {
            name: 'LightNovel',
            type: 'novel_site',
            extract() {
                return {
                    title: utils.getText('.novel-title, h1.title'),
                    description: utils.getText('.summary, .description, .content'),
                    coverImage: utils.getImageSrc(['.cover img', 'figure img']),
                    author: utils.getText('.author a, a[href*="/author/"]'),
                    status: utils.getValueByLabel(['Status']),
                    genres: utils.getAllTexts('.categories a, .genres a'),
                    rating: utils.getText('.rating-num'),
                    chapterCount: document.querySelectorAll('.chapter-list a').length
                };
            }
        },
        
        // ====== WebNovel ======
        'webnovel.com': {
            name: 'WebNovel',
            type: 'webnovel',
            extract() {
                // WebNovel has dynamic content, try multiple approaches
                const jsonLd = utils.getJsonLd().find(j => j['@type'] === 'Book' || j['@type'] === 'WebPage');
                
                return {
                    title: jsonLd?.name || utils.getText('h1, .g_title, ._mn'),
                    description: jsonLd?.description || utils.getText('.g_txt, ._mn .dsc, .j_synopsis'),
                    coverImage: jsonLd?.image || utils.getImageSrc(['._img img', '.g_thumb img', '.book-img img']),
                    author: jsonLd?.author?.name || utils.getText('.g_info a, ._mn .ell a, .author'),
                    status: utils.getText('.g_status, ._mn .ell:contains("Status")'),
                    genres: utils.getAllTexts('.g_tags a, .j_tagWrap a'),
                    rating: utils.getText('.g_star .num, ._score'),
                    views: utils.extractNumber(utils.getText('.g_num, ._vw')),
                    chapterCount: (() => {
                        const text = utils.getText('.g_chap, ._num, .j_chapNum');
                        return parseInt(utils.extractNumber(text)) || 0;
                    })()
                };
            }
        },
        
        // ====== ReadNovelFull ======
        'readnovelfull.com': {
            name: 'ReadNovelFull',
            type: 'novel_site', 
            extract() {
                return {
                    title: utils.getText('h1.title, .novel-title h1'),
                    description: utils.getText('.desc-text, .summary'),
                    coverImage: utils.getImageSrc(['.book img', '.cover img']),
                    author: utils.getText('.info a[href*="/author/"]'),
                    status: utils.getValueByLabel(['Status']),
                    genres: utils.getAllTexts('.info a[href*="/genre/"]'),
                    chapterCount: document.querySelectorAll('.list-chapter a').length
                };
            }
        },
        
        // ====== BoxNovel ======
        'boxnovel.com': {
            name: 'BoxNovel',
            type: 'novel_site',
            extract() {
                return {
                    title: utils.getText('.post-title h1, h1'),
                    description: utils.getText('.summary__content, .description-summary'),
                    coverImage: utils.getImageSrc(['.summary_image img', '.thumb img']),
                    author: utils.getText('.author-content a'),
                    status: utils.getText('.post-status .summary-content'),
                    genres: utils.getAllTexts('.genres-content a'),
                    rating: utils.getText('.post-total-rating .score'),
                    chapterCount: document.querySelectorAll('.wp-manga-chapter a').length
                };
            }
        },
        
        // ====== AllNovelFull ======
        'allnovelfull.com': { alias: 'novelfull.com' },
        'allnovelbin.net': { alias: 'novelbin.com' },
        
        // ====== RoyalRoad ======
        'royalroad.com': {
            name: 'RoyalRoad',
            type: 'novel_site',
            extract() {
                return {
                    title: utils.getText('h1[property="name"], .fic-title h1'),
                    description: utils.getText('.description, .summary, [property="description"]'),
                    coverImage: utils.getImageSrc(['.cover-image img', '.fic-header img']),
                    author: utils.getText('a[href*="/profile/"], .author span'),
                    status: utils.getText('.label-status, .status'),
                    genres: utils.getAllTexts('.tags a, .fiction-tags a'),
                    rating: utils.getText('[property="ratingValue"], .star-rating .score'),
                    ratingCount: utils.extractNumber(utils.getText('[property="ratingCount"]')),
                    views: utils.extractNumber(utils.getText('.stats .views, .statistics .views')),
                    chapterCount: (() => {
                        const text = utils.getText('#chapters .chapter-count, .stats .chapters');
                        return parseInt(utils.extractNumber(text)) || 
                               document.querySelectorAll('#chapters tbody tr, .chapter-list a').length;
                    })()
                };
            }
        },
        
        // ====== ScribbleHub ======
        'scribblehub.com': {
            name: 'ScribbleHub',
            type: 'novel_site',
            extract() {
                return {
                    title: utils.getText('.fic_title, h1'),
                    description: utils.getText('.wi_fic_desc, .description'),
                    coverImage: utils.getImageSrc(['.fic_image img', '.cover img']),
                    author: utils.getText('.auth_name_fic, a[href*="/profile/"]'),
                    status: utils.getText('.widget_fic_stat:contains("Status") .sp_value'),
                    genres: utils.getAllTexts('.fic_genre a, .genres a'),
                    rating: utils.getText('.fic_rating .rating'),
                    views: utils.extractNumber(utils.getText('.st_item:contains("Views") .st_value')),
                    chapterCount: parseInt(utils.extractNumber(
                        utils.getText('.st_item:contains("Chapters") .st_value')
                    )) || document.querySelectorAll('.toc_ol a').length
                };
            }
        },
        
        // ====== WuxiaWorld ======
        'wuxiaworld.com': {
            name: 'WuxiaWorld',
            type: 'novel_site',
            extract() {
                return {
                    title: utils.getText('.novel-title, h1'),
                    alternativeTitles: utils.getAllTexts('.alt-title, .aka'),
                    description: utils.getText('.novel-summary, .description'),
                    coverImage: utils.getImageSrc(['.novel-cover img', '.cover img']),
                    author: utils.getText('.author span, a[href*="/authors/"]'),
                    status: utils.getText('.novel-status, .status'),
                    genres: utils.getAllTexts('.genres a, .genre-tags a'),
                    chapterCount: document.querySelectorAll('.chapter-list a').length
                };
            }
        }
    };
    
    // ========================================
    // Generic Extractor (fallback)
    // ========================================
    
    const genericExtractor = {
        name: 'Generic',
        type: 'generic',
        
        extract() {
            const jsonLd = utils.getJsonLd();
            const bookData = jsonLd.find(j => 
                j['@type'] === 'Book' || 
                j['@type'] === 'WebPage' ||
                j['@type'] === 'Article' ||
                j['@type'] === 'CreativeWork'
            ) || {};
            
            // Comprehensive title selectors
            const titleSelectors = [
                'h1[itemprop="name"]', 'h1[property="name"]',
                '.novel-title h1', '.book-title h1', '.story-title h1',
                '.title h1', 'h1.title', 'h1.name',
                '.post-title h1', '.entry-title',
                'article h1', 'main h1', 'h1'
            ];
            
            // Comprehensive cover selectors
            const coverSelectors = [
                '.novel-cover img', '.book-cover img', '.story-cover img',
                '.cover img', '.thumb img', '.thumbnail img',
                'figure.cover img', 'figure img',
                '.summary_image img', '.book-img img',
                '[itemprop="image"]', 'article img:first-of-type'
            ];
            
            // Description selectors
            const descSelectors = [
                '[itemprop="description"]', '[property="description"]',
                '.novel-summary', '.book-summary', '.story-summary',
                '.summary', '.synopsis', '.description', '.desc',
                '.desc-text', '.summary-content', '.content-synopsis',
                '#synopsis', '#description', '#summary',
                '.entry-content p:first-of-type'
            ];
            
            // Extract genres/tags
            const genreSelectors = [
                'a[href*="/genre/"]', 'a[href*="/genres/"]',
                'a[href*="/category/"]', 'a[href*="/tag/"]',
                '.genres a', '.genre a', '.categories a', '.tags a',
                '[rel="tag"]', '.tag-list a'
            ];
            
            // Try to find author
            const authorSelectors = [
                '[itemprop="author"]', '[property="author"]',
                'a[href*="/author/"]', 'a[href*="/authors/"]',
                '.author a', '.author-name', '.writer'
            ];
            
            // Chapter list selectors
            const chapterSelectors = [
                '.chapter-list a', '.chapters a', '#chapter-list a',
                '.list-chapter a', '.wp-manga-chapter a',
                'a[href*="/chapter"]', 'a[href*="-chapter-"]',
                '.toc a', '.table-of-contents a'
            ];
            
            let title = bookData.name || utils.getMeta('og:title');
            if (!title) {
                for (const sel of titleSelectors) {
                    title = utils.getText(sel);
                    if (title) break;
                }
            }
            // Clean title (remove site name suffix)
            const siteName = utils.getMeta('og:site_name') || '';
            if (siteName && title.includes(' - ' + siteName)) {
                title = title.replace(' - ' + siteName, '');
            }
            if (siteName && title.includes(' | ' + siteName)) {
                title = title.replace(' | ' + siteName, '');
            }
            
            let description = bookData.description || utils.getMeta('og:description') || utils.getMeta('description');
            if (!description || description.length < 50) {
                for (const sel of descSelectors) {
                    const text = utils.getText(sel);
                    if (text && text.length > (description?.length || 0)) {
                        description = text;
                    }
                }
            }
            
            let coverImage = bookData.image || utils.getMeta('og:image');
            if (!coverImage) {
                coverImage = utils.getImageSrc(coverSelectors);
            }
            
            let author = bookData.author?.name || bookData.author;
            if (!author) {
                for (const sel of authorSelectors) {
                    author = utils.getText(sel);
                    if (author) break;
                }
            }
            if (!author) {
                author = utils.getValueByLabel(['Author', 'Writer', 'Creator', '作者', 'Auteur']);
            }
            
            const genres = [];
            for (const sel of genreSelectors) {
                genres.push(...utils.getAllTexts(sel));
            }
            // Deduplicate
            const uniqueGenres = [...new Set(genres)].slice(0, 20);
            
            const status = utils.getValueByLabel(['Status', 'State', '状态', 'État']) ||
                          utils.getText('.status, .novel-status');
            
            const rating = utils.getText('[itemprop="ratingValue"], .rating .num, .score, .rate-num') ||
                          utils.getMeta('og:rating');
            
            const chapterLinks = document.querySelectorAll(chapterSelectors.join(', '));
            const chapterCount = chapterLinks.length;
            
            // Find first chapter URL
            let firstChapterUrl = '';
            const firstChapterSelectors = [
                '.chapter-list a:first-child', '.chapter-list a:last-child',
                '#chapter-list a:first-child', '.chapters a:first-child',
                'a[href*="chapter-1"]', 'a[href*="chapter-01"]',
                'a:contains("Chapter 1")', 'a:contains("Ch. 1")'
            ];
            for (const sel of firstChapterSelectors) {
                try {
                    const el = document.querySelector(sel);
                    if (el) {
                        firstChapterUrl = el.href;
                        break;
                    }
                } catch (e) {}
            }
            
            return {
                title: utils.cleanText(title || document.title),
                description: utils.cleanText(description || ''),
                coverImage: coverImage || '',
                author: utils.cleanText(author || ''),
                status: utils.cleanText(status || ''),
                genres: uniqueGenres,
                rating: rating || '',
                chapterCount: chapterCount,
                firstChapterUrl: firstChapterUrl
            };
        }
    };
    
    // ========================================
    // Page Type Detection
    // ========================================
    
    function detectPageType() {
        const path = window.location.pathname.toLowerCase();
        const html = document.documentElement.outerHTML.toLowerCase();
        
        // Check for chapter page
        const chapterIndicators = [
            path.includes('/chapter'),
            path.includes('-chapter-'),
            path.includes('-ch-'),
            path.match(/\/c\d+/),
            path.match(/chapter[_-]?\d+/),
            document.querySelector('.chapter-content, .reading-content, #chapter-content, .chapter-text'),
            document.querySelector('[itemtype*="Chapter"]')
        ];
        if (chapterIndicators.filter(Boolean).length >= 2) {
            return 'chapter';
        }
        
        // Check for novel/book detail page
        const novelIndicators = [
            document.querySelector('.novel-info, .book-info, .story-info'),
            document.querySelector('.chapter-list, .chapters, #chapter-list'),
            document.querySelector('[itemtype*="Book"]'),
            document.querySelector('[itemtype*="CreativeWork"]'),
            document.querySelectorAll('a[href*="chapter"]').length > 5,
            path.includes('/novel/'),
            path.includes('/book/'),
            path.includes('/series/')
        ];
        if (novelIndicators.filter(Boolean).length >= 2) {
            return 'novel';
        }
        
        // Check for search/list page
        const listIndicators = [
            path.includes('/search'),
            path.includes('/genre'),
            path.includes('/category'),
            path.includes('/tag'),
            path.includes('/latest'),
            path.includes('/popular'),
            document.querySelectorAll('.novel-item, .book-item, .story-item').length > 3
        ];
        if (listIndicators.filter(Boolean).length >= 2) {
            return 'list';
        }
        
        // Try to determine if it's a novel page by content analysis
        const hasLongDescription = (utils.getText('.summary, .description, .synopsis').length > 200);
        const hasChapterLinks = document.querySelectorAll('a[href*="chapter"]').length > 3;
        const hasCover = document.querySelector('.cover img, .book-cover img, .novel-cover img');
        
        if (hasLongDescription && (hasChapterLinks || hasCover)) {
            return 'novel';
        }
        
        return 'unknown';
    }
    
    // ========================================
    // Confidence Score Calculation
    // ========================================
    
    function calculateConfidence(data, pageType) {
        let score = 0;
        
        // Title (20 points)
        if (data.title && data.title.length > 3) score += 20;
        
        // Description (15 points)
        if (data.description && data.description.length > 50) score += 15;
        else if (data.description && data.description.length > 0) score += 5;
        
        // Cover image (15 points)
        if (data.coverImage && data.coverImage.startsWith('http')) score += 15;
        
        // Author (10 points)
        if (data.author && data.author.length > 0) score += 10;
        
        // Genres (10 points)
        if (data.genres && data.genres.length > 0) score += 10;
        
        // Chapters (15 points)
        if (data.chapterCount > 10) score += 15;
        else if (data.chapterCount > 0) score += 8;
        
        // Page type match (15 points)
        if (pageType === 'novel') score += 15;
        
        return Math.min(100, score);
    }
    
    // ========================================
    // Main Extraction Logic
    // ========================================
    
    try {
        const domain = utils.getDomain();
        let extractor = siteExtractors[domain];
        
        // Handle aliases
        if (extractor && extractor.alias) {
            extractor = siteExtractors[extractor.alias];
        }
        
        // Use site-specific or generic extractor
        const activeExtractor = extractor || genericExtractor;
        const extractedData = activeExtractor.extract();
        
        const pageType = detectPageType();
        
        // Build result object
        const result = {
            url: window.location.href,
            title: extractedData.title || '',
            alternativeTitles: extractedData.alternativeTitles || [],
            description: extractedData.description || '',
            coverImage: extractedData.coverImage || '',
            author: extractedData.author || '',
            artist: extractedData.artist || '',
            status: extractedData.status || '',
            genres: extractedData.genres || [],
            tags: extractedData.tags || [],
            rating: extractedData.rating || '',
            ratingCount: extractedData.ratingCount || '',
            views: extractedData.views || '',
            bookmarks: extractedData.bookmarks || '',
            chapterCount: extractedData.chapterCount || 0,
            latestChapter: extractedData.latestChapter || '',
            firstChapterUrl: extractedData.firstChapterUrl || '',
            hasChapterList: (extractedData.chapterCount || 0) > 0,
            siteName: activeExtractor.name || utils.getMeta('og:site_name') || domain,
            siteType: activeExtractor.type || 'generic',
            pageType: pageType,
            rawMetadata: {
                ogTitle: utils.getMeta('og:title'),
                ogDescription: utils.getMeta('og:description'),
                ogImage: utils.getMeta('og:image'),
                ogType: utils.getMeta('og:type')
            },
            confidence: 0
        };
        
        // Calculate confidence
        result.confidence = calculateConfidence(result, pageType);
        
        return JSON.stringify(result);
        
    } catch (error) {
        return JSON.stringify({
            url: window.location.href,
            title: document.title,
            description: '',
            coverImage: '',
            author: '',
            artist: '',
            status: '',
            genres: [],
            tags: [],
            rating: '',
            ratingCount: '',
            views: '',
            bookmarks: '',
            chapterCount: 0,
            latestChapter: '',
            firstChapterUrl: '',
            hasChapterList: false,
            siteName: window.location.hostname,
            siteType: 'error',
            pageType: 'error',
            rawMetadata: {},
            alternativeTitles: [],
            confidence: 0
        });
    }
})();
""".trimIndent()
// ============================================================================
// URL Validation - UPDATED to accept any URL
// ============================================================================

/**
 * Result of URL validation - now supports any URL
 */
private sealed class UrlValidationResult {
    /**
     * URL matches a known provider
     */
    data class KnownProvider(
        val url: String,
        val provider: MainProvider,
        val isNovelUrl: Boolean
    ) : UrlValidationResult()

    /**
     * Valid URL but doesn't match any known provider
     */
    data class GenericUrl(
        val url: String,
        val domain: String
    ) : UrlValidationResult()

    /**
     * Invalid URL format
     */
    data class InvalidFormat(val input: String) : UrlValidationResult()
}

/**
 * Site type for current URL
 */
private sealed class SiteType {
    data class KnownProvider(val provider: MainProvider, val isNovelPage: Boolean) : SiteType()
    data class GenericSite(val domain: String, val isLikelyNovelPage: Boolean = false) : SiteType()
    data object Unknown : SiteType()
}

/**
 * Validates and normalizes a URL input - accepts ANY valid URL
 */
private fun validateUrl(input: String): UrlValidationResult {
    val trimmed = input.trim()

    // Handle empty input
    if (trimmed.isBlank()) {
        return UrlValidationResult.InvalidFormat(trimmed)
    }

    // Try to normalize the URL
    val normalizedUrl = when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") -> "https://$trimmed"
        // Could be a search query - for now treat as invalid
        else -> return UrlValidationResult.InvalidFormat(trimmed)
    }

    // Basic URL validation
    if (!isValidUrl(normalizedUrl)) {
        return UrlValidationResult.InvalidFormat(trimmed)
    }

    // Try to find matching provider
    val provider = findProviderForUrl(normalizedUrl)

    return if (provider != null) {
        val isNovelUrl = isLikelyNovelUrl(normalizedUrl, provider)
        UrlValidationResult.KnownProvider(
            url = normalizedUrl,
            provider = provider,
            isNovelUrl = isNovelUrl
        )
    } else {
        // Accept the URL even without a known provider
        UrlValidationResult.GenericUrl(
            url = normalizedUrl,
            domain = extractDomain(normalizedUrl)
        )
    }
}

/**
 * Basic URL validation
 */
private fun isValidUrl(url: String): Boolean {
    return try {
        val regex = Regex(
            "^https?://[a-zA-Z0-9][-a-zA-Z0-9]*(?:\\.[a-zA-Z0-9][-a-zA-Z0-9]*)+(?:[/?#].*)?$",
            RegexOption.IGNORE_CASE
        )
        regex.matches(url)
    } catch (e: Exception) {
        false
    }
}

/**
 * Find which provider a URL belongs to
 */
private fun findProviderForUrl(url: String): MainProvider? {
    return MainProvider.getProviders().find { provider ->
        val providerDomain = extractDomain(provider.mainUrl)
        val urlDomain = extractDomain(url)

        urlDomain == providerDomain ||
                urlDomain.endsWith(".$providerDomain")
    }
}

/**
 * Extract domain from URL
 */
private fun extractDomain(url: String): String {
    return url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .substringBefore("/")
        .substringBefore("?")
        .lowercase()
}

/**
 * Extract readable site name from domain
 */
private fun extractSiteName(url: String): String {
    val domain = extractDomain(url)
    return domain
        .substringBefore(".")
        .replaceFirstChar { it.uppercase() }
}

// Keep existing provider-specific functions for known providers
private fun getProviderNovelPatterns(providerName: String): List<String> {
    return when (providerName.lowercase()) {
        "novelbin" -> listOf("^b/[^/]+$", "^novel/[^/]+$")
        "libread" -> listOf("^libread/[^/]+(-\\d+)?$", "^book/[^/]+$")
        "novelfire" -> listOf("^novel/[^/]+$")
        "webnovel" -> listOf(
            "^(?:https?://)?(?:m\\.)?(?:www\\.)?webnovel\\.com/book/[a-z0-9\\-]+_[0-9]+(?:[/?#].*)?$",
            "^book/[a-z0-9\\-]+_[0-9]+(?:[/?#].*)?$",
            "^book/[a-z0-9\\-]+(?:[/?#].*)?$"
        )
        "novelfull" -> listOf("^[^/]+-novel/?$", "^novel/[^/]+$")
        "lightnovelpub", "lightnovelworld" -> listOf("^novel/[^/]+$")
        "readnovelfull" -> listOf("^[^/]+\\.html$", "^novel/[^/]+$")
        "freewebnovel" -> listOf("^[^/]+\\.html$")
        "allnovelbin" -> listOf("^novel/[^/]+$", "^book/[^/]+$")
        else -> emptyList()
    }
}

private fun isLikelyNovelUrl(url: String, provider: MainProvider): Boolean {
    val path = url
        .removePrefix(provider.mainUrl)
        .removePrefix("/")
        .removeSuffix("/")
        .substringBefore("?")
        .substringBefore("#")
        .lowercase()

    if (path.isBlank()) return false

    // Check provider-specific patterns
    val providerPatterns = getProviderNovelPatterns(provider.name)
    for (pattern in providerPatterns) {
        if (Regex(pattern, RegexOption.IGNORE_CASE).matches(path)) {
            if (!isChapterPath(path)) {
                return true
            }
        }
    }

    // Check exclusions
    if (isExcludedPath(path)) return false

    // Generic patterns
    val genericPatterns = listOf(
        "^novel/[^/]+$", "^book/[^/]+$", "^series/[^/]+$",
        "^title/[^/]+$", "^story/[^/]+$", "^read/[^/]+$",
        "^[a-z]/[^/]+$", "^[a-z]{2}/[^/]+$"
    )

    for (pattern in genericPatterns) {
        if (Regex(pattern, RegexOption.IGNORE_CASE).matches(path) && !isChapterPath(path)) {
            return true
        }
    }

    return false
}

private fun isChapterPath(path: String): Boolean {
    val indicators = listOf("chapter-", "-chapter-", "/chapter", "-ch-", "/ch/")
    return indicators.any { path.lowercase().contains(it) } ||
            Regex("[-/]chapter[-_]?\\d+", RegexOption.IGNORE_CASE).containsMatchIn(path)
}

private fun isExcludedPath(path: String): Boolean {
    val excludePatterns = listOf(
        "^search", "^genre/", "^category/", "^tag/", "^author/",
        "^login", "^register", "^user/", "^profile", "^ajax/", "^api/"
    )
    return excludePatterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(path.lowercase()) }
}

// ============================================================================
// Provider WebView Screen - UPDATED
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderWebViewScreen(
    providerName: String,
    initialUrl: String?,
    onBack: () -> Unit,
    onOpenNovelInApp: (novelUrl: String) -> Unit,
    // New callback for extracted data from generic sites
    onExtractedData: ((ExtractedPageData) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl ?: "") }
    var pageTitle by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }

    // URL editing state
    var isEditingUrl by remember { mutableStateOf(false) }
    var editUrlValue by remember { mutableStateOf(TextFieldValue("")) }

    // Site detection state
    var currentSiteType by remember { mutableStateOf<SiteType>(SiteType.Unknown) }

    // Cookie status
    var cookieStatus by remember { mutableStateOf(CookieDisplayStatus.NONE) }
    var showCookieSavedMessage by remember { mutableStateOf(false) }

    // Extracted data state
    var extractedData by remember { mutableStateOf<ExtractedPageData?>(null) }
    var showExtractedDataSheet by remember { mutableStateOf(false) }
    var isExtracting by remember { mutableStateOf(false) }

    // Get initial provider (may be null for generic URLs)
    val initialProvider = remember(providerName) {
        MainProvider.getProvider(providerName)
    }

    val startUrl = remember(initialProvider, initialUrl) {
        initialUrl ?: initialProvider?.mainUrl ?: "https://google.com"
    }

    val domain = remember(currentUrl) {
        extractDomain(currentUrl)
    }

    val isSecure by remember(currentUrl) {
        derivedStateOf { currentUrl.startsWith("https://") }
    }

    // Analyze current URL when it changes
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank()) {
            when (val result = validateUrl(currentUrl)) {
                is UrlValidationResult.KnownProvider -> {
                    currentSiteType = SiteType.KnownProvider(
                        provider = result.provider,
                        isNovelPage = result.isNovelUrl
                    )
                }
                is UrlValidationResult.GenericUrl -> {
                    currentSiteType = SiteType.GenericSite(
                        domain = result.domain,
                        isLikelyNovelPage = false  // Will be updated by JS extraction
                    )
                }
                is UrlValidationResult.InvalidFormat -> {
                    currentSiteType = SiteType.Unknown
                }
            }
            // Reset extracted data when URL changes
            extractedData = null
        }
    }

    // Hide cookie saved message after delay
    LaunchedEffect(showCookieSavedMessage) {
        if (showCookieSavedMessage) {
            delay(4000)
            showCookieSavedMessage = false
        }
    }

    // Extract page data using JavaScript
    val extractPageData: () -> Unit = {
        webView?.let { wv ->
            isExtracting = true
            wv.evaluateJavascript(ENHANCED_PAGE_EXTRACTION_SCRIPT) { result ->
                try {
                    val jsonString = result
                        .removeSurrounding("\"")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")

                    val json = Json { ignoreUnknownKeys = true }
                    val data = json.decodeFromString<ExtractedPageData>(jsonString)
                    extractedData = data

                    // Update site type if novel detected
                    if (data.pageType == "novel" || data.hasChapterList) {
                        val current = currentSiteType
                        if (current is SiteType.GenericSite) {
                            currentSiteType = current.copy(isLikelyNovelPage = true)
                        }
                    }

                    showExtractedDataSheet = true
                } catch (e: Exception) {
                    android.util.Log.e("WebView", "Failed to parse extracted data", e)
                    scope.launch {
                        snackbarHostState.showSnackbar("Failed to extract page data")
                    }
                }
                isExtracting = false
            }
        }
    }

    // Handle back
    val handleBack: () -> Unit = {
        CloudflareManager.flushWebViewCookies()
        val cookies = CloudflareManager.extractCookiesFromWebView(currentUrl)
        if (cookies?.contains("cf_clearance") == true) {
            CloudflareManager.saveCookiesForDomain(
                domain = domain,
                cookies = cookies,
                userAgent = CloudflareManager.WEBVIEW_USER_AGENT
            )
        }
        onBack()
    }

    // Handle URL submission
    val handleUrlSubmit: (String) -> Unit = { input ->
        when (val result = validateUrl(input)) {
            is UrlValidationResult.KnownProvider -> {
                isEditingUrl = false
                webView?.loadUrl(result.url)
            }
            is UrlValidationResult.GenericUrl -> {
                isEditingUrl = false
                webView?.loadUrl(result.url)  // Now accepts generic URLs!
            }
            is UrlValidationResult.InvalidFormat -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Invalid URL format")
                }
            }
        }
    }

    // Handle opening novel in app
    val handleOpenInApp: () -> Unit = {
        val siteType = currentSiteType
        when (siteType) {
            is SiteType.KnownProvider -> {
                handleBack()
                onOpenNovelInApp(currentUrl)
            }
            is SiteType.GenericSite -> {
                // For generic sites, extract data first then callback
                if (extractedData != null && onExtractedData != null) {
                    handleBack()
                    onExtractedData(extractedData!!)
                } else {
                    extractPageData()
                }
            }
            SiteType.Unknown -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Cannot open this page in app")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EnhancedWebViewTopBar(
                title = pageTitle,
                url = currentUrl,
                isLoading = isLoading,
                loadingProgress = loadingProgress,
                canGoBack = canGoBack,
                isSecure = isSecure,
                cookieStatus = cookieStatus,
                siteType = currentSiteType,
                isEditingUrl = isEditingUrl,
                editUrlValue = editUrlValue,
                onEditUrlValueChange = { editUrlValue = it },
                onStartEditingUrl = {
                    editUrlValue = TextFieldValue(
                        text = currentUrl,
                        selection = TextRange(0, currentUrl.length)
                    )
                    isEditingUrl = true
                },
                onCancelEditingUrl = { isEditingUrl = false },
                onSubmitUrl = handleUrlSubmit,
                onBack = {
                    if (isEditingUrl) {
                        isEditingUrl = false
                    } else if (canGoBack) {
                        webView?.goBack()
                    } else {
                        handleBack()
                    }
                },
                onClose = handleBack,
                onRefresh = { webView?.reload() },
                onClearCookies = {
                    CloudflareManager.clearCookiesForDomain(domain)
                    cookieStatus = CookieDisplayStatus.NONE
                },
                onExtractData = extractPageData
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EnhancedProviderWebView(
                startUrl = startUrl,
                userAgent = CloudflareManager.WEBVIEW_USER_AGENT,
                onWebViewCreated = { webView = it },
                onPageStarted = { url ->
                    isLoading = true
                    loadingProgress = 0
                    currentUrl = url
                },
                onPageFinished = { url ->
                    isLoading = false
                    loadingProgress = 100
                    currentUrl = url
                    canGoBack = webView?.canGoBack() ?: false

                    CloudflareManager.flushWebViewCookies()

                    checkAndSaveCookies(
                        url = url,
                        domain = domain,
                        onCookiesSaved = {
                            cookieStatus = CookieDisplayStatus.VALID
                            showCookieSavedMessage = true
                        },
                        onCookieStatusUpdate = { cookieStatus = it }
                    )

                    // Auto-extract for generic sites after page loads
                    if (currentSiteType is SiteType.GenericSite) {
                        webView?.postDelayed({
                            webView?.evaluateJavascript(ENHANCED_PAGE_EXTRACTION_SCRIPT) { result ->
                                try {
                                    val jsonString = result
                                        .removeSurrounding("\"")
                                        .replace("\\\"", "\"")
                                        .replace("\\\\", "\\")

                                    val json = Json { ignoreUnknownKeys = true }
                                    val data = json.decodeFromString<ExtractedPageData>(jsonString)

                                    if (data.pageType == "novel" || data.hasChapterList) {
                                        val current = currentSiteType
                                        if (current is SiteType.GenericSite) {
                                            currentSiteType = current.copy(isLikelyNovelPage = true)
                                        }
                                        extractedData = data
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.d("WebView", "Auto-extract failed: ${e.message}")
                                }
                            }
                        }, 1000)
                    }
                },
                onTitleChanged = { pageTitle = it },
                onProgressChanged = { loadingProgress = it }
            )

            // Loading progress
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                EnhancedProgressIndicator(progress = loadingProgress)
            }

            // Cookie saved notification
            AnimatedVisibility(
                visible = showCookieSavedMessage,
                enter = slideInVertically { -it } + fadeIn() + scaleIn(initialScale = 0.9f),
                exit = slideOutVertically { -it } + fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                EnhancedCookieSavedBanner()
            }

            // Action button - different for known vs generic sites
            AnimatedVisibility(
                visible = shouldShowActionButton(currentSiteType),
                enter = slideInVertically { it } + fadeIn() + scaleIn(initialScale = 0.8f),
                exit = slideOutVertically { it } + fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                when (val siteType = currentSiteType) {
                    is SiteType.KnownProvider -> {
                        if (siteType.isNovelPage) {
                            EnhancedOpenInAppButton(
                                providerName = siteType.provider.name,
                                onClick = handleOpenInApp
                            )
                        }
                    }
                    is SiteType.GenericSite -> {
                        GenericSiteActionButton(
                            siteName = extractSiteName(currentUrl),
                            hasExtractedData = extractedData != null,
                            isExtracting = isExtracting,
                            isLikelyNovel = siteType.isLikelyNovelPage,
                            onExtract = extractPageData,
                            onOpenData = { showExtractedDataSheet = true }
                        )
                    }

                    SiteType.Unknown -> { }
                }
            }

            // Dismiss keyboard when clicking outside
            if (isEditingUrl) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isEditingUrl = false }
                )
            }
        }
    }

    // Extracted data bottom sheet
    if (showExtractedDataSheet && extractedData != null) {
        ExtractedDataBottomSheet(
            data = extractedData!!,
            onDismiss = { showExtractedDataSheet = false },
            onUseData = {
                showExtractedDataSheet = false
                if (onExtractedData != null) {
                    handleBack()
                    onExtractedData(extractedData!!)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Novel data extracted! (Integration not configured)")
                    }
                }
            }
        )
    }
}

private fun shouldShowActionButton(siteType: SiteType): Boolean {
    return when (siteType) {
        is SiteType.KnownProvider -> siteType.isNovelPage
        is SiteType.GenericSite -> true
        SiteType.Unknown -> false
    }
}

// ============================================================================
// Generic Site Action Button
// ============================================================================

@Composable
private fun GenericSiteActionButton(
    siteName: String,
    hasExtractedData: Boolean,
    isExtracting: Boolean,
    isLikelyNovel: Boolean = false,  // ← Add this parameter
    onExtract: () -> Unit,
    onOpenData: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "button_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Different colors based on state
    val buttonColor = when {
        hasExtractedData && isLikelyNovel -> WebViewColors.Success
        hasExtractedData -> WebViewColors.Generic
        else -> WebViewColors.Info
    }

    Surface(
        onClick = if (hasExtractedData) onOpenData else onExtract,
        shape = RoundedCornerShape(24.dp),
        color = buttonColor,
        shadowElevation = 16.dp,
        tonalElevation = 4.dp,
        modifier = Modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (isExtracting) {
                        val rotation by rememberInfiniteTransition(label = "").animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
                            label = ""
                        )
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = rotation },
                            tint = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = when {
                                hasExtractedData && isLikelyNovel -> Icons.Rounded.AutoStories
                                hasExtractedData -> Icons.Rounded.Info
                                else -> Icons.Rounded.Search
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Column {
                Text(
                    text = when {
                        isExtracting -> "Extracting..."
                        hasExtractedData && isLikelyNovel -> "Open Novel Data"
                        hasExtractedData -> "View Extracted Data"
                        else -> "Extract Page Data"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "from $siteName",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
        }
    }
}
// ============================================================================
// Enhanced Extracted Data Bottom Sheet
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtractedDataBottomSheet(
    data: ExtractedPageData,
    onDismiss: () -> Unit,
    onUseData: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with confidence indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Extracted Data",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "from ${data.siteName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ConfidenceBadge(confidence = data.confidence, pageType = data.pageType)
            }

            HorizontalDivider()

            // Cover and basic info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cover image
                if (data.coverImage.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(width = 100.dp, height = 140.dp)
                    ) {
                        AsyncImage(
                            model = data.coverImage,
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Title and author
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (data.title.isNotBlank()) {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (data.author.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = data.author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Status badge
                    if (data.status.isNotBlank()) {
                        StatusBadge(status = data.status)
                    }

                    // Stats row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (data.chapterCount > 0) {
                            StatItem(
                                icon = Icons.Rounded.MenuBook,
                                value = "${data.chapterCount}",
                                label = "chapters"
                            )
                        }
                        if (data.rating.isNotBlank()) {
                            StatItem(
                                icon = Icons.Rounded.Star,
                                value = data.rating,
                                label = "rating"
                            )
                        }
                    }
                }
            }

            // Genres
            if (data.genres.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Genres",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data.genres.take(10).forEach { genre ->
                            GenreChip(genre = genre)
                        }
                    }
                }
            }

            // Description
            if (data.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var expanded by remember { mutableStateOf(false) }
                    val maxLines = if (expanded) Int.MAX_VALUE else 4

                    Column {
                        Text(
                            text = data.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { expanded = !expanded }
                        )
                        if (data.description.length > 200) {
                            Text(
                                text = if (expanded) "Show less" else "Show more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { expanded = !expanded }
                            )
                        }
                    }
                }
            }

            // Alternative titles
            if (data.alternativeTitles.isNotEmpty()) {
                DataRow(
                    label = "Alternative Titles",
                    value = data.alternativeTitles.joinToString(", ")
                )
            }

            // Latest chapter
            if (data.latestChapter.isNotBlank()) {
                DataRow(label = "Latest Chapter", value = data.latestChapter)
            }

            // URL
            DataRow(label = "Source URL", value = data.url)

            HorizontalDivider()

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onUseData,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = data.confidence >= 30 && data.pageType == "novel"
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Library")
                }
            }

            // Confidence warning
            if (data.confidence < 50) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WebViewColors.Warning.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = WebViewColors.Warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Low confidence extraction. Some data may be missing or incorrect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Page type warning
            if (data.pageType != "novel") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "This page was detected as '${data.pageType}', not a novel page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom spacing for gesture navigation
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: Int, pageType: String) {
    val (color, icon) = when {
        confidence >= 70 && pageType == "novel" -> Pair(WebViewColors.Success, Icons.Rounded.CheckCircle)
        confidence >= 40 -> Pair(WebViewColors.Warning, Icons.Rounded.Info)
        else -> Pair(MaterialTheme.colorScheme.error, Icons.Rounded.Warning)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text = "$confidence%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val statusLower = status.lowercase()
    val (color, text) = when {
        statusLower.contains("ongoing") || statusLower.contains("updating") ->
            Pair(WebViewColors.Info, status)
        statusLower.contains("completed") || statusLower.contains("finished") ->
            Pair(WebViewColors.Success, status)
        statusLower.contains("hiatus") || statusLower.contains("dropped") ->
            Pair(WebViewColors.Warning, status)
        else -> Pair(MaterialTheme.colorScheme.outline, status)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GenreChip(genre: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = genre,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// Cookie Status Components (unchanged)
// ============================================================================

enum class CookieDisplayStatus {
    NONE, CHECKING, VALID, EXPIRED
}

@Composable
private fun EnhancedCookieStatusIndicator(status: CookieDisplayStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "cookie_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val (color, icon, bgColor) = when (status) {
        CookieDisplayStatus.NONE -> Triple(
            MaterialTheme.colorScheme.outline,
            Icons.Rounded.Cookie,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
        CookieDisplayStatus.CHECKING -> Triple(
            MaterialTheme.colorScheme.tertiary,
            Icons.Rounded.HourglassEmpty,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        CookieDisplayStatus.VALID -> Triple(
            WebViewColors.Success,
            Icons.Rounded.VerifiedUser,
            WebViewColors.Success.copy(alpha = 0.15f)
        )
        CookieDisplayStatus.EXPIRED -> Triple(
            WebViewColors.Warning,
            Icons.Rounded.Warning,
            WebViewColors.Warning.copy(alpha = 0.15f)
        )
    }

    val scale by animateFloatAsState(
        targetValue = if (status == CookieDisplayStatus.VALID) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "status_scale"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (status == CookieDisplayStatus.CHECKING) pulseAlpha else 1f
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
    }
}

@Composable
private fun EnhancedCookieSavedBanner() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WebViewColors.Success,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
            }
            Column {
                Text(
                    text = "Bypass Cookies Saved!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "You can go back to browsing now",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// ============================================================================
// Enhanced Progress Indicator
// ============================================================================

@Composable
private fun EnhancedProgressIndicator(progress: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0, 100) / 100f,
        animationSpec = tween(200, easing = EaseOutCubic),
        label = "progress_animation"
    )

    if (animatedProgress < 0.05f) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    } else {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}

// ============================================================================
// Enhanced Top Bar - UPDATED with site type
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedWebViewTopBar(
    title: String,
    url: String,
    isLoading: Boolean,
    loadingProgress: Int,
    canGoBack: Boolean,
    isSecure: Boolean,
    cookieStatus: CookieDisplayStatus,
    siteType: SiteType,
    isEditingUrl: Boolean,
    editUrlValue: TextFieldValue,
    onEditUrlValueChange: (TextFieldValue) -> Unit,
    onStartEditingUrl: () -> Unit,
    onCancelEditingUrl: () -> Unit,
    onSubmitUrl: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onClearCookies: () -> Unit,
    onExtractData: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotation"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = if (isEditingUrl) Icons.Rounded.Close
                        else Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = when {
                            isEditingUrl -> "Cancel"
                            canGoBack -> "Go back"
                            else -> "Close"
                        }
                    )
                }

                AnimatedContent(
                    targetState = isEditingUrl,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    },
                    modifier = Modifier.weight(1f),
                    label = "url_bar_mode"
                ) { editing ->
                    if (editing) {
                        EditableUrlBar(
                            value = editUrlValue,
                            onValueChange = onEditUrlValueChange,
                            onSubmit = { onSubmitUrl(editUrlValue.text) },
                            onCancel = onCancelEditingUrl,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        DisplayUrlBar(
                            title = title,
                            url = url,
                            isSecure = isSecure,
                            cookieStatus = cookieStatus,
                            siteType = siteType,
                            onClick = onStartEditingUrl,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (!isEditingUrl) {
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (isLoading) rotation else 0f
                            },
                            tint = if (isLoading)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (isEditingUrl) {
                    IconButton(onClick = { onSubmitUrl(editUrlValue.text) }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "Go",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!isEditingUrl) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        EnhancedDropdownMenu(
                            expanded = showMenu,
                            onDismiss = { showMenu = false },
                            cookieStatus = cookieStatus,
                            siteType = siteType,
                            onClearCookies = {
                                onClearCookies()
                                showMenu = false
                            },
                            onEditUrl = {
                                showMenu = false
                                onStartEditingUrl()
                            },
                            onExtractData = {
                                showMenu = false
                                onExtractData()
                            },
                            onClose = {
                                onClose()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Display URL Bar - UPDATED with site type indicator
// ============================================================================

@Composable
private fun DisplayUrlBar(
    title: String,
    url: String,
    isSecure: Boolean,
    cookieStatus: CookieDisplayStatus,
    siteType: SiteType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Site type indicator
            val (indicatorColor, indicatorIcon) = when (siteType) {
                is SiteType.KnownProvider -> Pair(
                    if (isSecure) WebViewColors.Secure else MaterialTheme.colorScheme.error,
                    if (isSecure) Icons.Rounded.Lock else Icons.Rounded.LockOpen
                )
                is SiteType.GenericSite -> Pair(
                    WebViewColors.Generic,
                    Icons.Rounded.Public
                )
                SiteType.Unknown -> Pair(
                    MaterialTheme.colorScheme.outline,
                    if (isSecure) Icons.Rounded.Lock else Icons.Rounded.LockOpen
                )
            }

            Surface(
                shape = CircleShape,
                color = indicatorColor.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = indicatorIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = indicatorColor
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.ifBlank { "Loading..." },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Provider badge for known sites
                    if (siteType is SiteType.KnownProvider) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = siteType.provider.name,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = formatDisplayUrl(url),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }

            EnhancedCookieStatusIndicator(status = cookieStatus)
        }
    }
}

// ============================================================================
// Editable URL Bar
// ============================================================================

@Composable
private fun EditableUrlBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        modifier = modifier.padding(horizontal = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        keyboardController?.hide()
                        onSubmit()
                    }
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.text.isEmpty()) {
                            Text(
                                text = "Enter any URL...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (value.text.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange(TextFieldValue("")) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// Dropdown Menu - UPDATED with extract option
// ============================================================================

@Composable
private fun EnhancedDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    cookieStatus: CookieDisplayStatus,
    siteType: SiteType,
    onClearCookies: () -> Unit,
    onEditUrl: () -> Unit,
    onExtractData: () -> Unit,
    onClose: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp)
    ) {
        // Site type info
        when (siteType) {
            is SiteType.KnownProvider -> {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = siteType.provider.name,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Supported provider",
                                style = MaterialTheme.typography.labelSmall,
                                color = WebViewColors.Success
                            )
                        }
                    },
                    onClick = { },
                    enabled = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
            is SiteType.GenericSite -> {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = siteType.domain,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (siteType.isLikelyNovelPage) "Novel page detected" else "Generic website",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (siteType.isLikelyNovelPage) WebViewColors.Success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = { },
                    enabled = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Public,
                            contentDescription = null,
                            tint = WebViewColors.Generic
                        )
                    }
                )
            }
            SiteType.Unknown -> { }
        }

        if (siteType !is SiteType.Unknown) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        // Extract data option for generic sites
        if (siteType is SiteType.GenericSite) {
            EnhancedMenuItem(
                text = "Extract Page Data",
                description = "Analyze page for novel content",
                icon = Icons.Rounded.ContentPaste,
                iconTint = WebViewColors.Generic,
                onClick = onExtractData
            )
        }

        EnhancedMenuItem(
            text = "Edit URL",
            description = "Navigate to any website",
            icon = Icons.Rounded.Edit,
            onClick = onEditUrl
        )

        if (cookieStatus != CookieDisplayStatus.NONE) {
            EnhancedMenuItem(
                text = "Clear Bypass Cookies",
                description = "Remove saved Cloudflare cookies",
                icon = Icons.Rounded.DeleteOutline,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = onClearCookies
            )
        }

        EnhancedMenuItem(
            text = "Close Browser",
            description = "Return to app",
            icon = Icons.Rounded.Close,
            onClick = onClose
        )
    }
}

@Composable
private fun EnhancedMenuItem(
    text: String,
    description: String? = null,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(text = text, fontWeight = FontWeight.Medium)
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        onClick = onClick,
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        }
    )
}

// ============================================================================
// Enhanced Open in App Button (for known providers)
// ============================================================================

@Composable
private fun EnhancedOpenInAppButton(
    providerName: String,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "button_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 16.dp,
        tonalElevation = 4.dp,
        modifier = Modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }

            Column {
                Text(
                    text = "Open in Novery",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "via $providerName",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
        }
    }
}

// ============================================================================
// Enhanced WebView Component
// ============================================================================

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EnhancedProviderWebView(
    startUrl: String,
    userAgent: String,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Int) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    userAgentString = userAgent
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    cacheMode = WebSettings.LOAD_DEFAULT
                    databaseEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    allowContentAccess = true
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onPageStarted(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let { onPageFinished(it) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = false
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }
                }

                onWebViewCreated(this)
                loadUrl(startUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ============================================================================
// Cookie Extraction
// ============================================================================

private fun checkAndSaveCookies(
    url: String,
    domain: String,
    onCookiesSaved: () -> Unit,
    onCookieStatusUpdate: (CookieDisplayStatus) -> Unit
) {
    if (domain.isBlank()) return

    try {
        CloudflareManager.flushWebViewCookies()
        val cookies = CloudflareManager.extractCookiesFromWebView(url)

        if (cookies != null && cookies.contains("cf_clearance")) {
            val existingCookies = CloudflareManager.getCookiesForDomain(domain)
            val isNewOrUpdated = !existingCookies.contains("cf_clearance") ||
                    CloudflareManager.areCookiesExpired(domain)

            CloudflareManager.saveCookiesForDomain(
                domain = domain,
                cookies = cookies,
                userAgent = CloudflareManager.WEBVIEW_USER_AGENT
            )

            if (isNewOrUpdated) {
                onCookiesSaved()
            }
            onCookieStatusUpdate(CookieDisplayStatus.VALID)
        } else {
            val status = CloudflareManager.getCookieStatus(url)
            onCookieStatusUpdate(
                when (status) {
                    CloudflareManager.CookieStatus.VALID -> CookieDisplayStatus.VALID
                    CloudflareManager.CookieStatus.EXPIRED -> CookieDisplayStatus.EXPIRED
                    CloudflareManager.CookieStatus.NONE -> CookieDisplayStatus.NONE
                }
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("ProviderWebView", "Error checking cookies", e)
    }
}

// ============================================================================
// Utility Functions
// ============================================================================

private fun formatDisplayUrl(url: String): String {
    return url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .take(50)
        .let { if (url.length > 50) "$it..." else it }
}