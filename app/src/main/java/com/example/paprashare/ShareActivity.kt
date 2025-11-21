package com.example.paprashare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nutze die sichere Methode aus MainActivity (oder hier duplizieren wenn separate Datei)
        // Falls MainActivity.kt im gleichen Package ist, funktioniert der Aufruf:
        val prefs = try {
            getSecurePreferences(this)
        } catch (e: Exception) {
            // Fallback falls Encryption fehlschlägt (z.B. Restore Backup)
            getSharedPreferences("papra_prefs_fallback", MODE_PRIVATE)
        }

        val serverUrl = prefs.getString("server_url", "")
        val apiKey = prefs.getString("api_key", "")
        val organizationId = prefs.getString("organization_id", "")

        if (serverUrl.isNullOrEmpty() || apiKey.isNullOrEmpty() || organizationId.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Bitte konfiguriere zuerst die Papra-Einstellungen",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val uris = when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { listOf(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }
            else -> null
        }

        if (uris.isNullOrEmpty()) {
            Toast.makeText(this, "Keine Datei gefunden", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UploadScreen(uris, serverUrl, apiKey, organizationId)
                }
            }
        }
    }

    @Composable
    fun UploadScreen(
        uris: List<Uri>,
        serverUrl: String,
        apiKey: String,
        organizationId: String
    ) {
        var isUploading by remember { mutableStateOf(false) }
        var uploadComplete by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var uploadProgress by remember { mutableIntStateOf(0) }
        val totalFiles = uris.size

        LaunchedEffect(Unit) {
            isUploading = true
            try {
                uris.forEachIndexed { index, uri ->
                    uploadProgress = index
                    uploadFile(uri, serverUrl, apiKey, organizationId)
                }
                uploadComplete = true
                delay(1500)
                finish()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unbekannter Fehler"
                isUploading = false
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                if (isUploading && errorMessage == null) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    if (totalFiles > 1) {
                        Text("Lade Datei ${uploadProgress + 1} von $totalFiles hoch...")
                    } else {
                        Text("Datei wird hochgeladen...")
                    }
                } else if (uploadComplete) {
                    Text(
                        if (totalFiles > 1) "✓ $totalFiles Dateien erfolgreich!" else "✓ Upload erfolgreich!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (errorMessage != null) {
                    Text(
                        "Fehler beim Upload",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { finish() }) {
                        Text("Schließen")
                    }
                }
            }
        }
    }

    private suspend fun uploadFile(
        uri: Uri,
        serverUrl: String,
        apiKey: String,
        organizationId: String
    ) = withContext(Dispatchers.IO) {
        val fileName = getFileName(uri)
        val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}_$fileName")

        try {
            // Datei kopieren
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }
            } ?: throw Exception("Konnte Datei nicht lesen")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .build()

            val apiUrl = "${serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents"

            val request = Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            // Nutze den Singleton Client
            val response = NetworkClient.okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw Exception("Upload fehlgeschlagen (${response.code}): $errorBody")
            }
            response.body?.close() // Wichtig: Body schließen

        } finally {
            // Cleanup: Temporäre Datei IMMER löschen
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "document"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }
}

// Singleton für Netzwerk-Client (vermeidet Memory Leaks)
object NetworkClient {
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}