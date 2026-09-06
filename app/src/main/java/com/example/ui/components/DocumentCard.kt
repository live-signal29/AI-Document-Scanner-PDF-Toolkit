package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ScannedDocument
import com.example.ui.theme.HighDensityBadgeBlueBg
import com.example.ui.theme.HighDensityBadgeBlueText
import com.example.ui.theme.HighDensityBadgeRedBg
import com.example.ui.theme.HighDensityBadgeRedText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun DocumentCard(
    document: ScannedDocument,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onOcr: () -> Unit,
    onAiAssistant: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)? = null,
    isGridView: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    if (isGridView) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .testTag("document_grid_card_${document.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Thumbnail Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh ?: MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    val thumbFile = document.thumbnailPath?.let { File(it) }
                    if (thumbFile != null && thumbFile.exists()) {
                        AsyncImage(
                            model = thumbFile,
                            contentDescription = document.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Page count badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${document.pageCount}p",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Info row
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = document.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp).testTag("doc_menu_btn_${document.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DocumentDropdownMenu(
                                isTrash = document.isTrash,
                                expanded = showMenu,
                                onDismiss = { showMenu = false },
                                onShare = onShare,
                                onOcr = onOcr,
                                onAiAssistant = onAiAssistant,
                                onRename = onRename,
                                onDelete = onDelete,
                                onRestore = onRestore
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDate(document.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatFileSize(document.fileSizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        // List View Card
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .testTag("document_list_card_${document.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High Density Document Badge / Thumbnail
                val thumbFile = document.thumbnailPath?.let { File(it) }
                val isAiBadge = document.title.contains("AI", ignoreCase = true) || document.title.contains("OCR", ignoreCase = true) || document.ocrText?.isNotBlank() == true

                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                thumbFile != null && thumbFile.exists() -> MaterialTheme.colorScheme.surfaceVariant
                                isAiBadge -> HighDensityBadgeBlueBg
                                else -> HighDensityBadgeRedBg
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbFile != null && thumbFile.exists()) {
                        AsyncImage(
                            model = thumbFile,
                            contentDescription = document.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isAiBadge) {
                        Text(
                            text = "AI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityBadgeBlueText
                        )
                    } else {
                        Text(
                            text = "PDF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityBadgeRedText
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Document Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${formatDate(document.createdAt)} • ${formatFileSize(document.fileSizeBytes)} • ${document.pageCount} Pages",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Options Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp).testTag("doc_menu_btn_${document.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    DocumentDropdownMenu(
                        isTrash = document.isTrash,
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onShare = onShare,
                        onOcr = onOcr,
                        onAiAssistant = onAiAssistant,
                        onRename = onRename,
                        onDelete = onDelete,
                        onRestore = onRestore
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentDropdownMenu(
    isTrash: Boolean,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOcr: () -> Unit,
    onAiAssistant: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)? = null
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (isTrash) {
            DropdownMenuItem(
                text = { Text("Restore") },
                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onRestore?.invoke()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete Permanently") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )
        } else {
            DropdownMenuItem(
                text = { Text("Share PDF") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onShare()
                }
            )
            DropdownMenuItem(
                text = { Text("OCR Extract Text") },
                leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onOcr()
                }
            )
            DropdownMenuItem(
                text = { Text("AI Assistant") },
                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onAiAssistant()
                }
            )
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text("Move to Trash") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )
        }
    }
}
