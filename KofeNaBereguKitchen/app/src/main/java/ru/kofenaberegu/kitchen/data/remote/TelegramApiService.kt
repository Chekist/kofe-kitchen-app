package ru.kofenaberegu.kitchen.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URLEncoder

object TelegramSender {
    private const val PREFS_NAME = "telegram_settings"
    private const val KEY_BOT_TOKEN = "bot_token"
    private const val KEY_CHAT_ID = "chat_id"
    private const val KEY_QUIET_START = "quiet_start"
    private const val KEY_QUIET_END = "quiet_end"

    private val client = OkHttpClient()

    fun saveSettings(context: Context, token: String, chatId: String, quietStart: String, quietEnd: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_BOT_TOKEN, token.trim())
            .putString(KEY_CHAT_ID, chatId.trim())
            .putString(KEY_QUIET_START, quietStart.trim())
            .putString(KEY_QUIET_END, quietEnd.trim())
            .apply()
    }

    fun getBotToken(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_BOT_TOKEN, "") ?: ""

    fun getChatId(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_CHAT_ID, "") ?: ""

    fun getQuietHours(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return (prefs.getString(KEY_QUIET_START, "01:00") ?: "01:00") to
                (prefs.getString(KEY_QUIET_END, "08:00") ?: "08:00")
    }

    fun sendViaBotApi(context: Context, text: String, photoPaths: List<String>): Boolean {
        val token = getBotToken(context)
        val chatId = getChatId(context)
        if (token.isBlank() || chatId.isBlank()) return false

        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val msgUrl = "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$encodedText"
            val msgResponse = client.newCall(Request.Builder().url(msgUrl).build()).execute()
            val ok = msgResponse.isSuccessful
            msgResponse.close()
            if (!ok) return false

            photoPaths.filter { it.isNotBlank() }.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    val body = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("chat_id", chatId)
                        .addFormDataPart(
                            "photo",
                            file.name,
                            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        )
                        .build()
                    val photoRequest = Request.Builder()
                        .url("https://api.telegram.org/bot$token/sendPhoto")
                        .post(body)
                        .build()
                    client.newCall(photoRequest).execute().close()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareViaTelegramIntent(context: Context, text: String, photoPaths: List<String>) {
        val validFiles = photoPaths.map { File(it) }.filter { it.exists() }
        val intent = if (validFiles.isEmpty()) {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        } else {
            val uris = ArrayList<Uri>(validFiles.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            })
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_TEXT, text)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        intent.setPackage("org.telegram.messenger")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent.createChooser(intent.apply { setPackage(null) }, "Отправить отчёт")
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        }
    }
}
