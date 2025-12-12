package com.example.paprashare

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(this)
                }
            }
        }
    }
}

// Helper function for secure preferences (also required in ShareActivity)
fun getSecurePreferences(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        "papra_secure_prefs", // New name for secure file
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

@Composable
fun SettingsScreen(context: Context) {
    // Use the secure preferences
    val prefs = remember { getSecurePreferences(context) }

    var useHttps by remember {
        mutableStateOf(prefs.getBoolean("use_https_default", true))
    }

    var serverUrl by remember {
        mutableStateOf(prefs.getString("server_url", "") ?: "")
    }
    var apiKey by remember {
        mutableStateOf(prefs.getString("api_key", "") ?: "")
    }
    var organizationId by remember {
        mutableStateOf(prefs.getString("organization_id", "") ?: "")
    }
    var showSnackbar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text(stringResource(R.string.server_url)) },
            placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        // Transparency toggle for Play Store Compliance

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                // UX Element Translation: "HTTPS (Empfohlen)" -> "HTTPS (Recommended)" | "HTTP (Lokal/Unverschlüsselt)" -> "HTTP (Local/Unencrypted)"
                text = if (useHttps) stringResource(R.string.protocol_https_label) else stringResource(R.string.protocol_http_label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = useHttps,
                onCheckedChange = { useHttps = it }
            )
        }

        if (!useHttps) {
            Text(
                // UX Element Translation: "⚠️ WARNUNG: HTTP ist unverschlüsselt und sollte nur in gesicherten lokalen Netzwerken verwendet werden."
                text = stringResource(R.string.http_security_warning),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
            )
        }
        Text(
            text = stringResource(R.string.server_url_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(R.string.api_key)) },
            placeholder = { Text(stringResource(R.string.api_key_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            text = stringResource(R.string.api_key_tip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = organizationId,
            onValueChange = { organizationId = it },
            label = { Text(stringResource(R.string.organization_id)) },
            placeholder = { Text(stringResource(R.string.organization_id_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                // 1. URL Validation and cleanup
                var cleanUrl = serverUrl.trim()
                val defaultScheme = if (useHttps) "https" else "http"

                // Corrected schema check (http:// OR https://)
                if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("http://", true) && !cleanUrl.startsWith("https://", true)) {
                    // Set the scheme based on the toggle
                    cleanUrl = "$defaultScheme://$cleanUrl"
                }
                cleanUrl = cleanUrl.trimEnd('/')

                // Update state for UI Feedback
                serverUrl = cleanUrl

                prefs.edit().apply {
                    // 2. Save toggle state (CRITICAL)
                    putBoolean("use_https_default", useHttps)

                    // 3. Save data
                    putString("server_url", cleanUrl)
                    putString("api_key", apiKey.trim())
                    putString("organization_id", organizationId.trim())
                    apply()
                }
                showSnackbar = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save_settings))
        }

        if (showSnackbar) {
            // Note: Use Scaffold/SnackbarHost in a real app
            Text(
                text = stringResource(R.string.settings_saved),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            LaunchedEffect(Unit) {
                delay(2000)
                showSnackbar = false
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.settings_info),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}