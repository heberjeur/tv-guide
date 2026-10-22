package com.music4music.tvguide.ui.components

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.music4music.tvguide.R
import com.music4music.tvguide.data.remote.TrailerVideo

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
    var isWebViewAvailable by remember { mutableStateOf(true) }
    var playerViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(selectedTrailer.videoId) {
        onDispose {
            try {
                playerViewRef?.loadUrl("about:blank")
                playerViewRef?.stopLoading()
                playerViewRef?.destroy()
                playerViewRef = null
            } catch (_: Throwable) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFFF0000),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (selectedTrailer.title.isNotBlank()) selectedTrailer.title else stringResource(R.string.label_trailer),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                IconButton(
                    onClick = {
                        try {
                            playerViewRef?.loadUrl("about:blank")
                            playerViewRef?.stopLoading()
                        } catch (_: Throwable) {
                        }
                        onClose()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_close_trailer),
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                if (isWebViewAvailable) {
                    AndroidView(
                        factory = { ctx ->
                            try {
                                WebView(ctx).apply {
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
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            return false
                                        }
                                    }
                                    tag = selectedTrailer.videoId
                                    playerViewRef = this
                                    val html = buildPlayerHtml(selectedTrailer.videoId)
                                    loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                                }
                            } catch (_: Throwable) {
                                isWebViewAvailable = false
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
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { launchTrailer(context, selectedTrailer.videoId) }
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
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                        startY = 80f
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
                                    contentDescription = stringResource(R.string.action_open_youtube),
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_open_youtube),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(trailers) { index, trailer ->
                        val isSelected = trailer.videoId == selectedTrailer.videoId
                        val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2C)
                        val chipText = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .clickable { onSelectTrailer(trailer) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${stringResource(R.string.label_trailer)} ${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = chipText
                            )
                        }
                    }
                }
            }
        }
    }
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
                .player-frame { position:relative; width:100%; height:100%; }
                iframe { width:100%; height:100%; border:none; }
            </style>
        </head>
        <body>
            <div class="player-frame">
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

fun launchTrailer(context: Context, videoId: String) {
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
