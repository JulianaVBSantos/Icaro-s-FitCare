package br.com.icaros.fitcare

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

fun extrairIdVideoYoutube(url: String): String? {
    val limpa = Uri.decode(url.trim())
    return when {
        limpa.contains("/shorts/") -> limpa.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
        limpa.contains("youtu.be/") -> limpa.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
        limpa.contains("v=") -> limpa.substringAfter("v=").substringBefore("&")
        limpa.contains("/embed/") -> limpa.substringAfter("/embed/").substringBefore("?").substringBefore("/")
        limpa.contains("youtube.com") -> limpa.substringAfter("youtube.com").trim('/', ':', ' ')
        else -> null
    }?.takeIf { it.isNotBlank() }
}

@Composable
fun CardPreviewVideoExercicio(
    tituloExercicio: String,
    videoUrl: String,
    videoTipo: String = "LINK",
    modifier: Modifier = Modifier
        .width(108.dp)
        .height(145.dp),
    onAbrirPlayer: () -> Unit
) {
    val videoId = remember(videoUrl) { extrairIdVideoYoutube(videoUrl) }

    val thumbnailBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = videoUrl,
        key2 = videoTipo,
        key3 = videoId
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                if (videoTipo == "LOCAL" || videoUrl.startsWith("/")) {
                    val arquivo = File(videoUrl)
                    if (arquivo.exists()) {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(arquivo.absolutePath)
                        val frame = retriever.getFrameAtTime(1_000_000L)
                        retriever.release()
                        frame
                    } else null
                } else if (videoId != null) {
                    val thumbUrl = "https:" + "//://ytimg.com" + videoId + "/hqdefault.jpg"
                    URL(thumbUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(videoUrl, HashMap())
                    val frame = retriever.getFrameAtTime(1_000_000L)
                    retriever.release()
                    frame
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0E1B2A))
            .clickable { onAbrirPlayer() },
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                contentDescription = "Preview $tituloExercicio",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.88f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    color = Color(0xFF002B5C),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DialogoPlayerVideoExercicio(
    tituloExercicio: String,
    videoUrl: String,
    videoTipo: String = "LINK",
    onFechar: () -> Unit
) {
    val videoId = remember(videoUrl) { extrairIdVideoYoutube(videoUrl) }
    val ehYoutube = videoId != null || videoUrl.contains("youtube.com", true) || videoUrl.contains("youtu.be", true)
    val ehArquivoLocal = !ehYoutube && (
            videoTipo == "LOCAL" ||
                    videoUrl.startsWith("/") ||
                    videoUrl.endsWith(".mp4", true) ||
                    videoUrl.endsWith(".mov", true) ||
                    videoUrl.endsWith(".webm", true)
            )

    val urlFinalPlayer = remember(videoId, videoUrl) {
        if (videoId != null) {
            "https:" + "//www.youtube.com/shorts/" + videoId
        } else {
            videoUrl
        }
    }

    AlertDialog(
        onDismissRequest = onFechar,
        title = {
            Text(
                text = tituloExercicio,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (ehArquivoLocal) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                if (videoUrl.startsWith("/")) {
                                    setVideoPath(videoUrl)
                                } else {
                                    setVideoURI(Uri.parse(videoUrl))
                                }
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        }
                    )
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.BLACK)
                                scrollBarStyle = android.view.View.SCROLLBARS_OUTSIDE_OVERLAY
                                setInitialScale(100)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = false
                                settings.useWideViewPort = false
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.userAgentString =
                                    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val urlReq = request?.url?.toString().orEmpty()
                                        return !urlReq.startsWith("http://") && !urlReq.startsWith("https://")
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                var style = document.createElement('style');
                                                style.innerHTML = 'html, body { margin: 0 !important; padding: 0 !important; overflow-x: hidden !important; width: 100vw !important; background: #000 !important; } ytd-reel-video-renderer, #shorts-player, video { width: 100vw !important; left: 0 !important; margin-left: 0 !important; }';
                                                document.head.appendChild(style);
                                                window.scrollTo(0, 0);
                                            })();
                                            """.trimIndent(),
                                            null
                                        )
                                    }
                                }
                                loadUrl(urlFinalPlayer)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFechar) {
                Text("Fechar vídeo")
            }
        }
    )
}