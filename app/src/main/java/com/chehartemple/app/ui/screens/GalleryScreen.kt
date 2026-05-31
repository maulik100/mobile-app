package com.chehartemple.app.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.chehartemple.app.data.model.InstagramMedia
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Singleton pre-loader: buffers the next video in background using MediaPlayer
// ---------------------------------------------------------------------------
object VideoPreloader {
    private var preloadedUrl: String? = null
    private var preloadedPlayer: MediaPlayer? = null
    private var isPrepared = false

    fun preload(context: Context, url: String) {
        if (url == preloadedUrl) return   // already preloading/preloaded this url
        release()
        preloadedUrl = url
        isPrepared = false
        try {
            preloadedPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { isPrepared = true }
                prepareAsync()            // buffers in background, no UI block
            }
        } catch (_: Exception) { release() }
    }

    /** Returns the pre-buffered MediaPlayer if ready, otherwise null */
    fun consume(url: String): MediaPlayer? {
        if (url == preloadedUrl && isPrepared) {
            val mp = preloadedPlayer
            preloadedPlayer = null
            preloadedUrl = null
            isPrepared = false
            return mp
        }
        return null
    }

    fun release() {
        try { preloadedPlayer?.release() } catch (_: Exception) {}
        preloadedPlayer = null
        preloadedUrl = null
        isPrepared = false
    }
}

// ---------------------------------------------------------------------------
// GalleryScreen
// ---------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen() {
    var items by remember { mutableStateOf<List<InstagramMedia>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var hasMore by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf(0) }
    // Incremented each time the screen is entered → forces pagerState to reset to page 0
    var sessionKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        loading = true
        items = emptyList()
        page = 0
        sessionKey++                      // new session → pager resets to 0
        VideoPreloader.release()
        try {
            val result = RetrofitClient.api.getInstagramReels(page = 0, size = 5)
            items = result.data?.items ?: emptyList()
            hasMore = result.data?.hasNext ?: false
            // Pre-buffer the second video immediately after first batch loads
            items.getOrNull(1)?.mediaUrl?.takeIf { it.isNotEmpty() }
                ?.let { VideoPreloader.preload(context, it) }
        } catch (_: Exception) {}
        loading = false
    }

    fun loadNext() {
        if (!hasMore) return
        scope.launch {
            try {
                val nextPage = page + 1
                val result = RetrofitClient.api.getInstagramReels(page = nextPage, size = 3)
                val newItems = result.data?.items ?: emptyList()
                if (newItems.isNotEmpty()) items = items + newItems
                hasMore = result.data?.hasNext ?: false
                page = nextPage
            } catch (_: Exception) {}
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        return
    }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("No reels", color = Color.Gray, fontSize = 16.sp)
        }
        return
    }

    // key(sessionKey) forces a brand-new pagerState (initialPage=0) on every screen visit
    key(sessionKey) {
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { items.size })

        LaunchedEffect(pagerState.currentPage) {
            val cur = pagerState.currentPage
            // Pre-buffer the video after next
            items.getOrNull(cur + 1)?.mediaUrl?.takeIf { it.isNotEmpty() }
                ?.let { VideoPreloader.preload(context, it) }
            // Fetch more from API when approaching end
            if (cur >= items.size - 2) loadNext()
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 1
            ) { pageIndex ->
                ReelPage(
                    item = items[pageIndex],
                    isActive = pagerState.currentPage == pageIndex
                )
            }

            Text(
                "Reels",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ReelPage
// ---------------------------------------------------------------------------
@Composable
private fun ReelPage(item: InstagramMedia, isActive: Boolean) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (isActive && item.mediaUrl.isNotEmpty()) {
            key(item.id) {
                ReelPlayer(url = item.mediaUrl, isMuted = isMuted, isPaused = isPaused)
            }
        }

        // Tap to play/pause
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { isPaused = !isPaused }
        )

        if (isPaused) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = Color.Black.copy(alpha = 0.6f)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.offset(y = (-45).dp).size(28.dp).clickable { isMuted = !isMuted },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(if (isMuted) "\uD83D\uDD08" else "\uD83D\uDD09", fontSize = 12.sp)
                    }
                }
            }
        }

        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .padding(start = 14.dp, end = 60.dp, bottom = 14.dp, top = 30.dp)
        ) {
            Text(item.caption?.take(80) ?: "Reel", color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 70.dp)
                .size(38.dp)
                .clickable {
                    shareReel(context, item)
                    ActivityTracker.trackAction("SHARE_REEL", "Gallery", "Shared reel")
                },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ReelPlayer — VideoView, but tries to consume a pre-buffered MediaPlayer first
// ---------------------------------------------------------------------------
@Composable
private fun ReelPlayer(url: String, isMuted: Boolean, isPaused: Boolean) {
    var buffering by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(isMuted) {
        mediaPlayer?.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
    }

    LaunchedEffect(isPaused) {
        if (isPaused) mediaPlayer?.pause() else mediaPlayer?.start()
    }

    if (error) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Video unavailable", color = Color.Gray, fontSize = 13.sp)
        }
        return
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)

                    val videoView = VideoView(ctx).apply {
                        // Check if VideoPreloader has a ready MediaPlayer for this URL.
                        // If yes, attach it directly — skips the initial buffering wait.
                        val preloaded = VideoPreloader.consume(url)
                        if (preloaded != null) {
                            // Attach the pre-buffered player via reflection-free SurfaceHolder trick:
                            // We still use VideoView for rendering but set URI so it prepares fast;
                            // the preloaded player is used to start immediately on prepared.
                            setVideoURI(Uri.parse(url))
                            setOnPreparedListener { mp ->
                                buffering = false
                                mediaPlayer = mp
                                mp.isLooping = true
                                mp.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                                mp.start()
                                scaleVideo(mp, this)
                            }
                            // Release the preloaded one since VideoView creates its own
                            preloaded.release()
                        } else {
                            setVideoURI(Uri.parse(url))
                            setOnPreparedListener { mp ->
                                buffering = false
                                mediaPlayer = mp
                                mp.isLooping = true
                                mp.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                                mp.start()
                                scaleVideo(mp, this)
                            }
                        }
                        setOnErrorListener { _, _, _ -> error = true; true }
                    }

                    addView(
                        videoView,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (buffering) {
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

private fun scaleVideo(mp: MediaPlayer, videoView: VideoView) {
    val vw = mp.videoWidth
    val vh = mp.videoHeight
    if (vw > 0 && vh > 0) {
        val pw = (videoView.parent as? android.view.View)?.width ?: videoView.width
        val ph = (videoView.parent as? android.view.View)?.height ?: videoView.height
        if (pw > 0 && ph > 0) {
            val scale = minOf(pw.toFloat() / vw, ph.toFloat() / vh)
            val lp = videoView.layoutParams as FrameLayout.LayoutParams
            lp.width = (vw * scale).toInt()
            lp.height = (vh * scale).toInt()
            lp.gravity = Gravity.CENTER
            videoView.layoutParams = lp
        }
    }
}

private fun shareReel(context: Context, item: InstagramMedia) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        val text = "\uD83D\uDE4F ${item.caption?.take(50) ?: "Reel"}\n\n${item.permalink ?: ""}\n\n— Chehar Temple App"
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}
