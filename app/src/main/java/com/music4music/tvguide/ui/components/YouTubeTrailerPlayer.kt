package com.music4music.tvguide.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.music4music.tvguide.R
import com.music4music.tvguide.data.remote.TrailerVideo

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeTrailerPlayer(
    trailers: List<TrailerVideo>,
    selectedTrailer: TrailerVideo?,
    onSelectTrailer: (TrailerVideo) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedTrailer == null) return
    val context = LocalContext.current
    var isPlaying by remember(selectedTrailer.videoId) { mutableStateOf(false) }
    var hasPlaybackError by remember(selectedTrailer.videoId) { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    DisposableEffect(selectedTrailer.videoId) {
        onDispose {
            try {
                webViewRef?.loadUrl("about:blank")
                webViewRef?.stopLoading()
                webViewRef?.destroy()
                webViewRef = null
            } catch (_: Throwable) {
            }
        }
    }

    if (customFullscreenView != null) {
        BackHandler {
            (customFullscreenView?.parent as? ViewGroup)?.removeView(customFullscreenView)
            customViewCallback?.onCustomViewHidden()
            customFullscreenView = null
            customViewCallback = null
        }
        Dialog(
            onDismissRequest = {
                (customFullscreenView?.parent as? ViewGroup)?.removeView(customFullscreenView)
                customViewCallback?.onCustomViewHidden()
                customFullscreenView = null
                customViewCallback = null
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        (customFullscreenView?.parent as? ViewGroup)?.removeView(customFullscreenView)
                        customFullscreenView ?: View(ctx)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        color = Color(0xFF141414),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                if (isPlaying && !hasPlaybackError) {
                    AndroidView(
                        factory = { ctx ->
                            val targetContext = ctx.findActivity() ?: ctx
                            try {
                                if (!isWebViewSupported(targetContext)) {
                                    hasPlaybackError = true
                                    View(ctx)
                                } else {
                                    WebView(targetContext).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.mediaPlaybackRequiresUserGesture = false
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                                        settings.userAgentString = DESKTOP_USER_AGENT
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                                (view?.parent as? ViewGroup)?.removeView(view)
                                                customFullscreenView = view
                                                customViewCallback = callback
                                            }

                                            override fun onHideCustomView() {
                                                (customFullscreenView?.parent as? ViewGroup)?.removeView(customFullscreenView)
                                                customViewCallback?.onCustomViewHidden()
                                                customFullscreenView = null
                                                customViewCallback = null
                                            }
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                val url = request?.url?.toString().orEmpty()
                                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                                    if (url.contains("youtube-nocookie.com") || url.contains("youtube.com/embed")) {
                                                        return false
                                                    }
                                                }
                                                return true
                                            }
                                        }
                                        tag = selectedTrailer.videoId
                                        webViewRef = this
                                        val html = buildPlayerHtml(selectedTrailer.videoId)
                                        loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                                    }
                                }
                            } catch (_: Throwable) {
                                hasPlaybackError = true
                                View(ctx)
                            }
                        },
                        update = { webView ->
                            if (webView is WebView) {
                                val currentTag = webView.tag as? String
                                if (currentTag != selectedTrailer.videoId) {
                                    webView.tag = selectedTrailer.videoId
                                    val html = buildPlayerHtml(selectedTrailer.videoId)
                                    webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = {
                            isPlaying = false
                            try {
                                webViewRef?.loadUrl("about:blank")
                                webViewRef?.stopLoading()
                            } catch (_: Throwable) {
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (hasPlaybackError) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = selectedTrailer.thumbnailUrl,
                            contentDescription = selectedTrailer.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.78f))
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFE50914),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.video_playback_error),
                                color = Color.White,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { launchExternalVideo(context, selectedTrailer.videoId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_open_youtube),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                isPlaying = false
                                hasPlaybackError = false
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (isWebViewSupported(context)) {
                                    isPlaying = true
                                    hasPlaybackError = false
                                } else {
                                    hasPlaybackError = true
                                }
                            }
                    ) {
                        AsyncImage(
                            model = selectedTrailer.thumbnailUrl,
                            contentDescription = selectedTrailer.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                        startY = 60f
                                    )
                                )
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE50914),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(56.dp)
                                .align(Alignment.Center)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        if (selectedTrailer.title.isNotBlank()) {
                            Text(
                                text = selectedTrailer.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (trailers.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trailers) { trailer ->
                        val isSelected = trailer.videoId == selectedTrailer.videoId
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(68.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) Color(0xFFE50914) else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    isPlaying = false
                                    hasPlaybackError = false
                                    onSelectTrailer(trailer)
                                }
                        ) {
                            AsyncImage(
                                model = trailer.thumbnailUrl,
                                contentDescription = trailer.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f))
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isWebViewSupported(context: Context): Boolean {
    return try {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
            return false
        }
        val targetContext = context.findActivity() ?: context
        val probe = WebView(targetContext)
        probe.destroy()
        true
    } catch (_: Throwable) {
        false
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun launchExternalVideo(context: Context, videoId: String) {
    val intentCandidates = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
            setPackage("com.google.android.youtube.tv")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
            setPackage("com.teamsmart.videomanager.tv")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
            setPackage("com.liskovsoft.videomanager")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )

    for (intent in intentCandidates) {
        try {
            context.startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }

    Toast.makeText(
        context,
        context.getString(R.string.no_video_player_found),
        Toast.LENGTH_SHORT
    ).show()
}

private fun buildPlayerHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin:0; padding:0; box-sizing:border-box; background:#000; overflow:hidden; }
                html, body { width:100%; height:100%; background:#000; }
                .video-container { position:relative; width:100%; height:100%; }
                iframe { width:100%; height:100%; border:none; }
            </style>
        </head>
        <body>
            <div class="video-container">
                <iframe src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1&fs=1"
                        frameborder="0"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
                        allowfullscreen>
                </iframe>
            </div>
        </body>
        </html>
    """.trimIndent()
}
