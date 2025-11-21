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

// Hilfsfunktion für sichere Preferences (wird auch in ShareActivity benötigt)
fun getSecurePreferences(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        "papra_secure_prefs", // Neuer Name für sichere Datei
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

@Composable
fun SettingsScreen(context: Context) {
    // Nutze die sicheren Preferences
    val prefs = remember { getSecurePreferences(context) }

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
                // URL Validierung und Bereinigung
                var cleanUrl = serverUrl.trim()
                if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("http")) {
                    cleanUrl = "https://$cleanUrl"
                }
                cleanUrl = cleanUrl.trimEnd('/')

                // Aktualisiere State für UI Feedback
                serverUrl = cleanUrl

                prefs.edit().apply {
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
            // Hinweis: In einer echten App besser Scaffold/SnackbarHost nutzen
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