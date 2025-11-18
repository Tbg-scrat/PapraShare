package com.example.paprashare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen()
                }
            }
        }
    }

    @Composable
    fun SettingsScreen() {
        val prefs = getSharedPreferences("papra_prefs", MODE_PRIVATE)

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
                text = "Papra Einstellungen",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://papra.example.com") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                placeholder = { Text("ppapi_...") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = organizationId,
                onValueChange = { organizationId = it },
                label = { Text("Organization ID") },
                placeholder = { Text("org_...") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    prefs.edit().apply {
                        putString("server_url", serverUrl.trim())
                        putString("api_key", apiKey.trim())
                        putString("organization_id", organizationId.trim())
                        apply()
                    }
                    showSnackbar = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Einstellungen speichern")
            }

            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Einstellungen gespeichert!")
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSnackbar = false
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nutze das 'Teilen'-Menü in anderen Apps, um Dateien zu Papra hochzuladen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}