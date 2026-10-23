package com.movtery.zalithlauncher.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RoseyWebViewOverlay(
    initialUrl: String = "https://www.google.com",
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    var address by remember { mutableStateOf(initialUrl) }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    fun navigate() {
        val input = address.trim()
        if (input.isEmpty()) return

        val target = when {
            input.startsWith("https://", true) ||
                input.startsWith("http://", true) ||
                input.startsWith("file://", true) ||
                input == "about:blank" -> input
            input.any { it.isWhitespace() } ->
                "https://www.google.com/search?q=${Uri.encode(input)}"
            else -> "https://$input"
        }

        address = target
        webViewRef.value?.loadUrl(target)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Website or search") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { navigate() })
                )

                Button(onClick = { navigate() }) {
                    Text("Go")
                }

                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    enabled = canGoBack,
                    onClick = { webViewRef.value?.goBack() }
                ) { Text("Back") }

                TextButton(
                    enabled = canGoForward,
                    onClick = { webViewRef.value?.goForward() }
                ) { Text("Forward") }

                TextButton(
                    onClick = { webViewRef.value?.reload() }
                ) { Text("Reload") }
            }

            if (loading) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = {
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                address = url ?: address
                                loading = true
                            }

                            override fun onPageFinished(
                                view: WebView?,
                                url: String?
                            ) {
                                address = url ?: address
                                loading = false
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(
                                view: WebView?,
                                newProgress: Int
                            ) {
                                progress = newProgress
                                loading = newProgress < 100
                            }
                        }

                        webViewRef.value = this
                        if (initialUrl.isNotBlank()) loadUrl(initialUrl)
                    }
                },
                update = {}
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef.value?.apply {
                stopLoading()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
            webViewRef.value = null
        }
    }
}
