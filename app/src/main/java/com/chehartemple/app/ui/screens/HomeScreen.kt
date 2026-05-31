package com.chehartemple.app.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.chehartemple.app.data.api.ActivityTracker
import com.chehartemple.app.data.api.RetrofitClient
import com.chehartemple.app.data.model.Event
import com.chehartemple.app.data.model.News
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    var streamUrl by remember { mutableStateOf("") }
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var news by remember { mutableStateOf<List<News>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                streamUrl = RetrofitClient.api.getLiveStream().url
                if (streamUrl.isEmpty()) streamUrl = RetrofitClient.api.getFacebookLatestVideo().url
                if (streamUrl.isNotEmpty()) ActivityTracker.trackAction("VIEW_LIVE_STREAM", "Home", "Watching live darshan")
                events = RetrofitClient.api.getHomeEvents()
                news = RetrofitClient.api.getNews()
            } catch (_: Exception) {}
            loading = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("Chehar Temple", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF800000))
        }

        // Live Darshan
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Live Darshan", style = MaterialTheme.typography.titleMedium, color = Color(0xFF800000))
                    Spacer(Modifier.height(12.dp))
                    var videoError by remember { mutableStateOf(false) }
                    if (streamUrl.isNotEmpty() && !videoError) {
                        HomeVideoPlayer(url = streamUrl, onError = { videoError = true })
                    } else {
                        LiveDarshanComingSoon()
                    }
                }
            }
        }

        // Upcoming Events
        if (events.isNotEmpty()) {
            item { Text("Upcoming Events", style = MaterialTheme.typography.titleMedium) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(events) { event -> EventCard(event) }
                }
            }
        }

        // Latest News - Auto-scrolling carousel
        if (news.isNotEmpty()) {
            item { Text("Latest News", style = MaterialTheme.typography.titleMedium) }
            item { NewsCarousel(news) }
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewsCarousel(news: List<News>) {
    val pagerState = rememberPagerState(pageCount = { news.size })

    // Auto-scroll every 2 seconds
    LaunchedEffect(pagerState) {
        while (true) {
            delay(2000)
            val next = (pagerState.currentPage + 1) % news.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(160.dp),
            pageSpacing = 12.dp
        ) { page ->
            NewsCarouselCard(news[page])
        }

        // Dot indicators
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(news.size) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (pagerState.currentPage == i) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == i) Color(0xFF800000) else Color(0xFFD0D0D0))
                )
            }
        }
    }
}

@Composable
private fun NewsCarouselCard(news: News) {
    Card(
        modifier = Modifier.fillMaxSize().shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(Modifier.fillMaxSize()) {
            // Gradient background
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF8E8), Color.White)
                    )
                )
            )
            Column(
                Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        news.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF800000),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    news.content?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF555555),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                    }
                }
                news.createdAt?.let {
                    Text(
                        formatNewsDate(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    }
}



@Composable
private fun LiveDarshanComingSoon() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFFF8F0))))
            .padding(vertical = 32.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🙏", fontSize = 44.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Live Darshan — Coming Soon",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF800000)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Live darshan streaming will be available soon.\nPlease check back during aarti timings.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF888888),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF800000)
            ) {
                Text(
                    "🔔 Stay Tuned",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HomeVideoPlayer(url: String, onError: () -> Unit = {}) {
    val isFacebook = url.contains("fbcdn.net") || url.contains("facebook.com")

    Box(
        Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isFacebook) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.domStorageEnabled = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        setBackgroundColor(android.graphics.Color.BLACK)
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                if (request?.isForMainFrame == true) onError()
                            }
                            override fun onPageFinished(view: android.webkit.WebView?, loadedUrl: String?) {
                                // Inject JS to detect Facebook's "video unavailable" error block
                                view?.evaluateJavascript(
                                    "(function(){ var el = document.querySelector('._8jwo,._video_error,#u_0_0_error'); return el ? 'error' : 'ok'; })()"
                                ) { result -> if (result?.contains("error") == true) onError() }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        val embedUrl = "https://www.facebook.com/plugins/video.php?href=${java.net.URLEncoder.encode(url, "UTF-8")}&show_text=false&mute=0&autoplay=1"
                        loadUrl(embedUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            var error by remember { mutableStateOf(false) }
            var buffering by remember { mutableStateOf(true) }
            if (error) {
                LaunchedEffect(Unit) { onError() }
            } else {
                AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            setVideoURI(android.net.Uri.parse(url))
                            setOnPreparedListener { mp ->
                                buffering = false
                                mp.setVolume(1.0f, 1.0f)
                                mp.start()
                                val am = ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                am.requestAudioFocus(null, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN)
                            }
                            setOnErrorListener { _, _, _ -> error = true; onError(); true }
                            val mc = android.widget.MediaController(ctx)
                            mc.setAnchorView(this)
                            setMediaController(mc)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (buffering) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun EventCard(event: Event) {
    Card(
        modifier = Modifier.width(200.dp).shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            event.eventDate?.let {
                Text(formatEventDate(it), style = MaterialTheme.typography.bodySmall, color = Color(0xFFE8860C))
            }
            Spacer(Modifier.height(4.dp))
            if (event.allDayEvent) {
                Text("All Day", style = MaterialTheme.typography.bodySmall)
            } else if (!event.startTime.isNullOrEmpty()) {
                Text("${event.startTime} - ${event.endTime ?: ""}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatNewsDate(dt: String): String {
    return try {
        val parts = dt.split("T")[0].split("-")
        "${parts[2]}-${parts[1]}-${parts[0]}"
    } catch (_: Exception) { dt }
}

private fun formatEventDate(dt: String): String {
    return try {
        val parts = dt.split("T")[0].split("-")
        "${parts[2]}-${parts[1]}-${parts[0]}"
    } catch (_: Exception) { dt }
}
