@file:OptIn(androidx.media3.common.util.UnstableApi::class)

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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
                val liveUrl = RetrofitClient.api.getLiveStream().url
                streamUrl = if (!liveUrl.isNullOrBlank()) liveUrl
                            else RetrofitClient.api.getFacebookLatestVideo().url ?: ""
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

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(colors = listOf(Color(0xFFFFF8E8), Color.White))
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
                    Text(formatNewsDate(it), style = MaterialTheme.typography.labelSmall, color = Color(0xFF999999))
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
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF800000)) {
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
    val isYouTube = url.contains("youtube.com") || url.contains("youtu.be")
    val isFacebook = url.contains("facebook.com") || url.contains("fbcdn.net")

    Box(
        Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isYouTube -> YoutubeWebPlayer(url = url, onError = onError)
            isFacebook -> FacebookWebPlayer(url = url, onError = onError)
            else -> ExoLivePlayer(url = url, onError = onError)
        }
    }
}

@Composable
private fun YoutubeWebPlayer(url: String, onError: () -> Unit) {
    val videoId = remember(url) {
        listOf(
            Regex("v=([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("embed/([a-zA-Z0-9_-]{11})")
        ).firstNotNullOfOrNull { it.find(url)?.groupValues?.get(1) } ?: ""
    }
    if (videoId.isEmpty()) {
        LaunchedEffect(Unit) { onError() }
        return
    }
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
                }
                webChromeClient = WebChromeClient()
                val html = """
                    <html><body style="margin:0;padding:0;background:#000;">
                    <iframe width="100%" height="100%"
                        src="https://www.youtube.com/embed/$videoId?autoplay=1&mute=0&playsinline=1&rel=0"
                        frameborder="0" allow="autoplay; encrypted-media" allowfullscreen>
                    </iframe></body></html>
                """.trimIndent()
                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun FacebookWebPlayer(url: String, onError: () -> Unit) {
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
                }
                webChromeClient = WebChromeClient()
                val embedUrl = "https://www.facebook.com/plugins/video.php?href=${java.net.URLEncoder.encode(url, "UTF-8")}&show_text=false&mute=0&autoplay=1"
                loadUrl(embedUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ExoLivePlayer(url: String, onError: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isBuffering by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val exoPlayer = remember(url) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(1_000, 5_000, 500, 1_000)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(url))
                playWhenReady = true
                prepare()
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = state == Player.STATE_BUFFERING
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        hasError = true
                        onError()
                    }
                })
            }
    }

    if (hasError) return

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (isBuffering) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
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
