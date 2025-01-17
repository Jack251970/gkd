package li.songe.gkd.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.ZipUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.app
import li.songe.gkd.appScope
import li.songe.gkd.ui.component.SettingItem
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.destinations.AboutPageDestination
import li.songe.gkd.ui.destinations.DebugPageDestination
import li.songe.gkd.util.LoadStatus
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.authActionFlow
import li.songe.gkd.util.canDrawOverlaysAuthAction
import li.songe.gkd.util.checkUpdate
import li.songe.gkd.util.checkUpdatingFlow
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.logZipDir
import li.songe.gkd.util.navigate
import li.songe.gkd.util.storeFlow
import li.songe.gkd.util.updateStorage
import java.io.File

val settingsNav = BottomNavItem(
    label = app.getString(R.string.settings), icon = Icons.Default.Settings
)

@Composable
fun SettingsPage() {
    Icons.Default.Settings
    val context = LocalContext.current as MainActivity
    val navController = LocalNavController.current
    val store by storeFlow.collectAsState()
    val vm = hiltViewModel<HomePageVm>()
    val uploadStatus by vm.uploadStatusFlow.collectAsState()

    var showSubsIntervalDlg by remember {
        mutableStateOf(false)
    }
    var showEnableDarkThemeDlg by remember {
        mutableStateOf(false)
    }
    var showToastInputDlg by remember {
        mutableStateOf(false)
    }

    var showShareLogDlg by remember {
        mutableStateOf(false)
    }

    val checkUpdating by checkUpdatingFlow.collectAsState()


    Column(
        modifier = Modifier.verticalScroll(
            state = rememberScrollState()
        )
    ) {
        TextSwitch(name = stringResource(R.string.hide_background),
            desc = stringResource(R.string.hide_background_desc),
            checked = store.excludeFromRecents,
            onCheckedChange = {
                updateStorage(
                    storeFlow, store.copy(
                        excludeFromRecents = it
                    )
                )
            })
        Divider()

        TextSwitch(name = stringResource(R.string.accessibility_foreground),
            desc = stringResource(R.string.accessibility_foreground_desc),
            checked = store.enableAbFloatWindow,
            onCheckedChange = {
                updateStorage(
                    storeFlow, store.copy(
                        enableAbFloatWindow = it
                    )
                )
            })
        Divider()

        TextSwitch(name = stringResource(R.string.click_notification),
            desc = stringResource(R.string.click_notification_desc, store.clickToast),
            checked = store.toastWhenClick,
            modifier = Modifier.clickable {
                showToastInputDlg = true
            },
            onCheckedChange = {
                if (it && !Settings.canDrawOverlays(context)) {
                    authActionFlow.value = canDrawOverlaysAuthAction
                    return@TextSwitch
                }
                updateStorage(
                    storeFlow, store.copy(
                        toastWhenClick = it
                    )
                )
            })
        Divider()

        Row(modifier = Modifier
            .clickable {
                showSubsIntervalDlg = true
            }
            .padding(10.dp, 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f), text = stringResource(R.string.auto_update_subscription), fontSize = 18.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = updateTimeRadioOptions.find { it.second == store.updateSubsInterval }?.first
                        ?: store.updateSubsInterval.toString(), fontSize = 14.sp
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "more"
                )
            }
        }
        Divider()

        TextSwitch(name = stringResource(R.string.auto_update_app),
            desc = stringResource(R.string.auto_update_app_desc),
            checked = store.autoCheckAppUpdate,
            onCheckedChange = {
                updateStorage(
                    storeFlow, store.copy(
                        autoCheckAppUpdate = it
                    )
                )
            })
        Divider()

        SettingItem(title = if (checkUpdating) stringResource(R.string.checking_update) else stringResource(R.string.check_update), onClick = {
            appScope.launchTry {
                if (checkUpdatingFlow.value) return@launchTry
                val newVersion = checkUpdate()
                if (newVersion == null) {
                    ToastUtils.showShort(context.getString(R.string.no_update))
                }
            }
        })
        Divider()

        Row(modifier = Modifier
            .clickable {
                showEnableDarkThemeDlg = true
            }
            .padding(10.dp, 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f), text = stringResource(R.string.dark_mode), fontSize = 18.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = darkThemeRadioOptions.find { it.second == store.enableDarkTheme }?.first
                        ?: store.enableDarkTheme.toString(), fontSize = 14.sp
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "more"
                )
            }
        }
        Divider()

        TextSwitch(name = stringResource(R.string.save_log),
            desc = stringResource(R.string.save_log_desc),
            checked = store.log2FileSwitch,
            onCheckedChange = {
                updateStorage(
                    storeFlow, store.copy(
                        log2FileSwitch = it
                    )
                )
                if (!it) {
                    appScope.launchTry(Dispatchers.IO) {
                        val logFiles = LogUtils.getLogFiles()
                        if (logFiles.isNotEmpty()) {
                            logFiles.forEach { f ->
                                f.delete()
                            }
                            ToastUtils.showShort(context.getString(R.string.delete_success))
                        }
                    }
                }
            })
        Divider()

        SettingItem(title = stringResource(R.string.share_log), onClick = {
            vm.viewModelScope.launchTry(Dispatchers.IO) {
                val logFiles = LogUtils.getLogFiles()
                if (logFiles.isNotEmpty()) {
                    showShareLogDlg = true
                } else {
                    ToastUtils.showShort(context.getString(R.string.no_log))
                }
            }
        })
        Divider()

        SettingItem(title = stringResource(R.string.advanced_settings), onClick = {
            navController.navigate(DebugPageDestination)
        })
        Divider()

        SettingItem(title = stringResource(R.string.about), onClick = {
            navController.navigate(AboutPageDestination)
        })

        Spacer(modifier = Modifier.height(40.dp))
    }


    if (showSubsIntervalDlg) {
        Dialog(onDismissRequest = { showSubsIntervalDlg = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column {
                    updateTimeRadioOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = (option.second == store.updateSubsInterval),
                                    onClick = {
                                        updateStorage(
                                            storeFlow,
                                            storeFlow.value.copy(updateSubsInterval = option.second)
                                        )
                                    })
                                .padding(horizontal = 16.dp)
                        ) {
                            RadioButton(
                                selected = (option.second == store.updateSubsInterval),
                                onClick = {
                                    updateStorage(
                                        storeFlow,
                                        storeFlow.value.copy(updateSubsInterval = option.second)
                                    )
                                })
                            Text(
                                text = option.first, modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEnableDarkThemeDlg) {
        Dialog(onDismissRequest = { showEnableDarkThemeDlg = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column {
                    darkThemeRadioOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = (option.second == store.enableDarkTheme),
                                    onClick = {
                                        updateStorage(
                                            storeFlow,
                                            storeFlow.value.copy(enableDarkTheme = option.second)
                                        )
                                    })
                                .padding(horizontal = 16.dp)
                        ) {
                            RadioButton(
                                selected = (option.second == store.enableDarkTheme),
                                onClick = {
                                    updateStorage(
                                        storeFlow,
                                        storeFlow.value.copy(enableDarkTheme = option.second)
                                    )
                                })
                            Text(
                                text = option.first, modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showToastInputDlg) {
        var value by remember {
            mutableStateOf(store.clickToast)
        }
        val maxCharLen = 32
        AlertDialog(title = { Text(text = stringResource(R.string.enter_prompt_text)) }, text = {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it.take(maxCharLen)
                },
                singleLine = true,
                supportingText = {
                    Text(
                        text = "${value.length} / $maxCharLen",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
            )
        }, onDismissRequest = { showToastInputDlg = false }, confirmButton = {
            TextButton(onClick = {
                updateStorage(
                    storeFlow, store.copy(
                        clickToast = value
                    )
                )
                showToastInputDlg = false
            }) {
                Text(
                    text = stringResource(R.string.confirm), modifier = Modifier
                )
            }
        }, dismissButton = {
            TextButton(onClick = { showToastInputDlg = false }) {
                Text(
                    text = stringResource(R.string.cancel), modifier = Modifier
                )
            }
        })
    }

    if (showShareLogDlg) {
        Dialog(onDismissRequest = { showShareLogDlg = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column {
                    val modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                    Text(
                        text = stringResource(R.string.invoke_system_sharing), modifier = Modifier
                            .clickable(onClick = {
                                showShareLogDlg = false
                                vm.viewModelScope.launchTry(Dispatchers.IO) {
                                    val logZipFile = File(logZipDir, "log.zip")
                                    ZipUtils.zipFiles(LogUtils.getLogFiles(), logZipFile)
                                    val uri = FileProvider.getUriForFile(
                                        context, "${context.packageName}.provider", logZipFile
                                    )
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        type = "application/zip"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            intent, context.getString(R.string.share_log)
                                        )
                                    )
                                }
                            })
                            .then(modifier)
                    )
                    Text(
                        text = stringResource(R.string.generate_link), modifier = Modifier
                            .clickable(onClick = {
                                showShareLogDlg = false
                                vm.viewModelScope.launchTry(Dispatchers.IO) {
                                    val logZipFile = File(logZipDir, "log.zip")
                                    ZipUtils.zipFiles(LogUtils.getLogFiles(), logZipFile)
                                    vm.uploadZip(logZipFile)
                                }
                            })
                            .then(modifier)
                    )
                }
            }
        }
    }

    when (val uploadStatusVal = uploadStatus) {
        is LoadStatus.Failure -> {
            AlertDialog(
                title = { Text(text = stringResource(R.string.upload_fail)) },
                text = {
                    Text(text = uploadStatusVal.exception.let {
                        it.message ?: it.toString()
                    })
                },
                onDismissRequest = { vm.uploadStatusFlow.value = null },
                confirmButton = {
                    TextButton(onClick = {
                        vm.uploadStatusFlow.value = null
                    }) {
                        Text(text = stringResource(R.string.off))
                    }
                },
            )
        }

        is LoadStatus.Loading -> {
            AlertDialog(
                title = { Text(text = stringResource(R.string.uploading_file)) },
                text = {
                    LinearProgressIndicator(progress = uploadStatusVal.progress)
                },
                onDismissRequest = { },
                confirmButton = {
                    TextButton(onClick = {
                        vm.uploadJob?.cancel(CancellationException(context.getString(R.string.terminate_upload)))
                        vm.uploadJob = null
                    }) {
                        Text(text = stringResource(R.string.terminate_upload))
                    }
                },
            )
        }

        is LoadStatus.Success -> {
            AlertDialog(title = { Text(text = stringResource(R.string.upload_success)) }, text = {
                Text(text = uploadStatusVal.result.href)
            }, onDismissRequest = {}, dismissButton = {
                TextButton(onClick = {
                    vm.uploadStatusFlow.value = null
                }) {
                    Text(text = stringResource(R.string.off))
                }
            }, confirmButton = {
                TextButton(onClick = {
                    ClipboardUtils.copyText(uploadStatusVal.result.href)
                    ToastUtils.showShort(context.getString(R.string.copy_success))
                    vm.uploadStatusFlow.value = null
                }) {
                    Text(text = stringResource(R.string.copy))
                }
            })
        }

        else -> {}
    }
}

val updateTimeRadioOptions = app.resources.getStringArray(R.array.update_time_options)
    .zip(listOf(-1L, 60 * 60_000L, 6 * 60 * 60_000L, 12 * 60 * 60_000L, 24 * 60 * 60_000L))

val darkThemeRadioOptions = app.resources.getStringArray(R.array.dark_theme_options)
    .zip(listOf(null, true, false))
val enableGroupRadioOptions = app.resources.getStringArray(R.array.enable_group_options)
    .zip(listOf(null, true, false))