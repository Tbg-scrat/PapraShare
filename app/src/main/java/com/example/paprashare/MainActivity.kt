package com.example.paprashare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text(stringResource(R.string.server_url)) },
                placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
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
                Text(stringResource(R.string.save_settings))
            }

            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(stringResource(R.string.settings_saved))
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
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
}