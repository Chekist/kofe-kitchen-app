package ru.kofenaberegu.kitchen.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun ReportPreviewSheet(
    initialText: String,
    photoPaths: List<String>,
    photoPromptTitle: String,
    photoPromptHint: String,
    onRequestCamera: (String, String) -> Unit,
    onSendTelegram: (String) -> Unit,
    onSaveOffline: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var editableText by remember(initialText) { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Предпросмотр отчёта", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Проверьте и при необходимости отредактируйте сообщение перед отправкой.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = editableText,
                    onValueChange = { editableText = it },
                    label = { Text("Текст сообщения") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    minLines = 5
                )
                OutlinedButton(
                    onClick = { onRequestCamera(photoPromptTitle, photoPromptHint) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Снять фото камерой (${photoPaths.size})")
                }
                if (photoPaths.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photoPaths) { path ->
                            AsyncImage(
                                model = File(path),
                                contentDescription = "Фото отчёта",
                                modifier = Modifier.size(76.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onSendTelegram(editableText) },
                    enabled = editableText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Отправить в Telegram")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { copyReportText(context, editableText) },
                        enabled = editableText.isNotBlank(),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Скопировать")
                    }
                    OutlinedButton(
                        onClick = { onSaveOffline(editableText) },
                        enabled = editableText.isNotBlank(),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Сохранить")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

internal fun copyReportText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Отчёт кухни", text))
    Toast.makeText(context, "Сообщение скопировано", Toast.LENGTH_SHORT).show()
}
