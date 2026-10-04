package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.viewmodel.OpenDocument

/**
 * View a lawyer's verification document.
 *
 * Until this existed, an administrator approved or rejected an application
 * without being able to open a single submitted credential — the decision was
 * made on metadata alone. The server has always audited every read of a
 * verification document; the client simply never asked for one.
 *
 * Images render inline. A PDF is handed to whatever app the device has, since
 * bundling a PDF renderer for one admin screen is not worth the size.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    document: OpenDocument?,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(document?.meta?.docType ?: stringResource(R.string.title_document)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (document == null) {
                EmptyState(
                    title = "Document unavailable",
                    body = "It could not be downloaded. Check your connection and try again."
                )
                return@Column
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        document.meta.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            append(document.meta.mimeType ?: "Unknown type")
                            document.meta.byteSize?.let {
                                append(" · ")
                                append(formatBytes(it))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                document.isImage -> {
                    AsyncImage(
                        model = document.file,
                        contentDescription = "Verification document",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                document.isPdf -> {
                    ExternalOpenPrompt(
                        message = "This is a PDF. Open it in your device's document viewer.",
                        onOpen = { openExternally(context, document) }
                    )
                }

                else -> {
                    ExternalOpenPrompt(
                        message = "This file type cannot be previewed here.",
                        onOpen = { openExternally(context, document) }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = AmberContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Opening this document has been recorded in the audit log.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberText,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ExternalOpenPrompt(message: String, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Filled.Description,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onOpen) {
            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.action_open))
        }
    }
}

/**
 * Hand the file to another app through FileProvider.
 *
 * A raw `file://` URI would throw FileUriExposedException on modern Android, so
 * the content URI is granted read permission for this one launch only.
 */
private fun openExternally(context: android.content.Context, document: OpenDocument) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            document.file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, document.meta.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open document"))
    } catch (_: Exception) {
        // No handler installed, or the provider is misconfigured. The admin can
        // still act on the metadata; silently doing nothing is better than a
        // crash in the middle of a review.
    }
}

private fun formatBytes(bytes: Int): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
