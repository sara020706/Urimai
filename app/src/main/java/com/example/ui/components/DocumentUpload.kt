package com.example.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UploadedDocumentEntity
import com.example.ui.theme.*

private fun queryFileName(context: android.content.Context, uri: Uri): String {
    var name = uri.lastPathSegment ?: "document"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            name = cursor.getString(nameIndex) ?: name
        }
    }
    return name
}

@Composable
fun DocumentUploadRow(
    documentName: String,
    existingUpload: UploadedDocumentEntity?,
    onUpload: (fileUri: String, fileName: String, mimeType: String?) -> Unit,
    onRemove: (UploadedDocumentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers don't support persistable permissions; the URI still
                // works for this session, so proceed without persisting it.
            }
            val fileName = queryFileName(context, uri)
            val mimeType = context.contentResolver.getType(uri)
            onUpload(uri.toString(), fileName, mimeType)
        }
    }

    if (existingUpload == null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CivicNavy50)
                .clickable {
                    pickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                }
                .testTag("upload_doc_${documentName}")
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                tint = CivicNavy700,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Upload $documentName",
                style = MaterialTheme.typography.labelMedium,
                color = CivicNavy700,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(EmeraldContainer.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = EmeraldDark,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = existingUpload.fileName,
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onRemove(existingUpload) }
                    .testTag("remove_doc_${documentName}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove upload",
                    tint = EmeraldDark,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
