package dev.shorthouse.coinwatch.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shorthouse.coinwatch.BuildConfig
import dev.shorthouse.coinwatch.R
import dev.shorthouse.coinwatch.ui.insets.AppWindowInsets
import dev.shorthouse.coinwatch.ui.screen.settings.component.SettingsGroup
import dev.shorthouse.coinwatch.ui.screen.settings.component.SettingsItem

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onUpdateServerUrl = { newUrl ->
            viewModel.updateServerUrl(newUrl)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: CustomSettingsUiState,
    onNavigateUp: () -> Unit,
    onUpdateServerUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            SettingsTopBar(onNavigateUp = onNavigateUp)
        },
        contentWindowInsets = AppWindowInsets.horizontalAndBottomContent,
        modifier = modifier.fillMaxSize()
    ) { scaffoldPadding ->
        SettingsContent(
            serverUrl = uiState.serverUrl,
            onUpdateServerUrl = onUpdateServerUrl,
            modifier = Modifier.padding(scaffoldPadding)
        )
    }
}

@Composable
fun SettingsContent(
    serverUrl: String,
    onUpdateServerUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textValue by remember(serverUrl) { mutableStateOf(serverUrl) }

    Column(
        modifier = modifier
            .padding(all = 12.dp)
            .verticalScroll(rememberScrollState())
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SettingsGroup(title = "WSS Proxy Server") {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "wss_proxy.py Address (Host:Port)",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        onUpdateServerUrl(it)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Dns, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        SettingsGroup(title = stringResource(R.string.settings_group_about)) {
            SettingsItem(
                title = stringResource(R.string.settings_title_version),
                subtitle = BuildConfig.VERSION_NAME,
                leadingIcon = Icons.Rounded.Smartphone,
                onClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.settings_screen),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_top_bar_back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        modifier = modifier
    )
}
