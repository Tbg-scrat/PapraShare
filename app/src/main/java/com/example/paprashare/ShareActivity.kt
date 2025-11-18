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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prüfe, ob die Einstellungen vorhanden sind
        val prefs = getSharedPreferences("papra_prefs", MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", "")
        val apiKey = prefs.getString("api_key", "")
        val organizationId = prefs.getString("organization_id", "")

        if (serverUrl.isNullOrEmpty() || apiKey.isNullOrEmpty() || organizationId.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Bitte konfiguriere zuerst die Papra-Einstellungen",
                Toast.LENGTH_LONG
            ).show()

            // Öffne die MainActivity für Einstellungen
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Hole die geteilte(n) Datei(en)
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> {
                // Einzelne Datei
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { listOf(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                // Mehrere Dateien
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
        var uploadProgress by remember { mutableStateOf(0) }
        var totalFiles by remember { mutableStateOf(uris.size) }

        // Überprüfe ob die Werte nicht leer sind
        if (serverUrl.isEmpty() || apiKey.isEmpty() || organizationId.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Konfigurationsfehler",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text("Bitte konfiguriere die App-Einstellungen")
                    Button(onClick = { finish() }) {
                        Text("Schließen")
                    }
                }
            }
            return
        }

        LaunchedEffect(Unit) {
            isUploading = true
            try {
                // Lade alle Dateien nacheinander hoch
                uris.forEachIndexed { index, uri ->
                    uploadProgress = index
                    uploadFile(uri, serverUrl, apiKey, organizationId)
                }
                uploadComplete = true
                kotlinx.coroutines.delay(1500)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isUploading && errorMessage == null) {
                    CircularProgressIndicator()
                    Text("Datei wird zu Papra hochgeladen...")
                } else if (uploadComplete) {
                    Text(
                        "✓ Erfolgreich hochgeladen!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (errorMessage != null) {
                    Text(
                        "Fehler beim Upload:",
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
        // Kopiere die Datei in einen temporären Speicher
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw Exception("Datei konnte nicht geöffnet werden")

        val fileName = getFileName(uri)
        val tempFile = File(cacheDir, fileName)

        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()

        // Erstelle den API-Request
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                fileName,
                tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()

        val apiUrl = "${serverUrl.trimEnd('/')}/api/organizations/$organizationId/documents"

        // WICHTIG: Setze KEINEN manuellen Content-Type Header!
        // OkHttp setzt den automatisch mit der korrekten Boundary
        val request = Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        tempFile.delete()

        if (!response.isSuccessful) {
            throw Exception("Upload fehlgeschlagen: ${response.code} - $responseBody")
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