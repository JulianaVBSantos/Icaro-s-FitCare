package br.com.icaros.fitcare

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.webkit.WebResourceError
import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

fun extrairIdVideoYoutube(url: String): String? {
    val uri = runCatching { Uri.parse(Uri.decode(url.trim())) }.getOrNull() ?: return null
    val host = uri.host?.lowercase().orEmpty()
    val ehYoutube = host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")
    if (!ehYoutube) return null

    val segmentos = uri.pathSegments
    val idBruto = when {
        host == "youtu.be" -> segmentos.firstOrNull()
        segmentos.size >= 2 && segmentos[0] in listOf("shorts", "embed", "v") -> segmentos[1]
        uri.path == "/watch" -> uri.getQueryParameter("v")
        else -> null
    }
    return idBruto
        ?.takeIf { it.matches(Regex("^[a-zA-Z0-9_-]{11}$")) }
}

@Composable
fun CardPreviewVideoExercicio(
    tituloExercicio: String,
    videoUrl: String,
    videoTipo: String = "LINK",
    modifier: Modifier = Modifier
        .width(96.dp)
        .height(128.dp),
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
                    val candidatos = listOf("0.jpg", "hqdefault.jpg", "mqdefault.jpg").map { arquivoImg ->
                        Uri.Builder()
                            .scheme("https")
                            .authority("i" + ".ytimg.com")
                            .appendPath("vi")
                            .appendPath(videoId)
                            .appendPath(arquivoImg)
                            .build()
                            .toString()
                    }
                    var bmpEncontrado: Bitmap? = null
                    for (urlCand in candidatos) {
                        try {
                            val conn = (URL(urlCand).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 5000
                                readTimeout = 5000
                                setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                                doInput = true
                                connect()
                            }
                            if (conn.responseCode == 200) {
                                conn.inputStream.use { stream ->
                                    val decoded = BitmapFactory.decodeStream(stream)
                                    if (decoded != null && decoded.width > 120) {
                                        bmpEncontrado = decoded
                                    }
                                }
                            }
                            conn.disconnect()
                            if (bmpEncontrado != null) break
                        } catch (_: Exception) {
                        }
                    }
                    bmpEncontrado
                } else {
                    null
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
                .background(Color.Black.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.88f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    color = Color(0xFF002B5C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AbrirNoNavegador(
    mensagem: String,
    videoUrl: String,
    onFechar: () -> Unit
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = mensagem,
            color = Color.White,
            fontSize = 13.sp
        )
        TextButton(
            onClick = {
                runCatching {
                    val uri = Uri.parse(videoUrl.trim())
                    if (uri.scheme == "http" || uri.scheme == "https") {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                }
                onFechar()
            }
        ) {
            Text("Abrir no navegador externo", color = Color(0xFF7AB8F5))
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
    val ehArquivoLocal = videoId == null && (
            videoTipo == "LOCAL" ||
                    videoUrl.startsWith("/") ||
                    videoUrl.endsWith(".mp4", true) ||
                    videoUrl.endsWith(".mov", true) ||
                    videoUrl.endsWith(".webm", true)
            )

    val ehShort = remember(videoUrl) { videoUrl.contains("/shorts/", ignoreCase = true) }

    val urlFinalPlayer = remember(videoId, ehShort) {
        if (videoId != null) {
            if (ehShort) {
                Uri.Builder()
                    .scheme("https")
                    .authority("www.youtube.com")
                    .appendPath("shorts")
                    .appendPath(videoId)
                    .build()
                    .toString()
            } else {
                Uri.Builder()
                    .scheme("https")
                    .authority("www.youtube.com")
                    .appendPath("watch")
                    .appendQueryParameter("v", videoId)
                    .build()
                    .toString()
            }
        } else {
            ""
        }
    }

    // Vira true quando a página do player não carrega
    var falhou by remember(urlFinalPlayer) { mutableStateOf(false) }

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
                when {
                    ehArquivoLocal -> {
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
                    }

                    urlFinalPlayer.isNotBlank() -> {
                        if (falhou) {
                            AbrirNoNavegador(
                                mensagem = "Não foi possível carregar o vídeo aqui.",
                                videoUrl = videoUrl,
                                onFechar = onFechar
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
                                                val hostReq = request?.url?.host?.lowercase().orEmpty()
                                                val permitido = hostReq == "youtube.com" ||
                                                        hostReq.endsWith(".youtube.com") ||
                                                        hostReq == "youtu.be"
                                                return !permitido
                                            }

                                            override fun onReceivedError(
                                                view: WebView?,
                                                request: WebResourceRequest?,
                                                error: WebResourceError?
                                            ) {
                                                if (request?.isForMainFrame == true) {
                                                    falhou = true
                                                }
                                            }
                                        }
                                        loadUrl(urlFinalPlayer)
                                    }
                                }
                            )
                        }
                    }

                    else -> {
                        AbrirNoNavegador(
                            mensagem = "Link externo fora do YouTube.",
                            videoUrl = videoUrl,
                            onFechar = onFechar
                        )
                    }
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