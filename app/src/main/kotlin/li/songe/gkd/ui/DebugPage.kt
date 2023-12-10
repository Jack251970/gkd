package li.songe.gkd.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dylanc.activityresult.launcher.launchForResult
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import kotlinx.coroutines.Dispatchers
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.app
import li.songe.gkd.appScope
import li.songe.gkd.debug.FloatingService
import li.songe.gkd.debug.HttpService
import li.songe.gkd.debug.ScreenshotService
import li.songe.gkd.shizuku.newActivityTaskManager
import li.songe.gkd.shizuku.safeGetTasks
import li.songe.gkd.shizuku.shizukuIsSafeOK
import li.songe.gkd.ui.component.AuthCard
import li.songe.gkd.ui.component.PageScaffold
import li.songe.gkd.ui.component.SettingItem
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.destinations.SnapshotPageDestination
import li.songe.gkd.util.Ext
import li.songe.gkd.util.LocalLauncher
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.ProfileTransitions
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.navigate
import li.songe.gkd.util.storeFlow
import li.songe.gkd.util.updateStorage
import li.songe.gkd.util.usePollState
import rikka.shizuku.Shizuku

@RootNavGraph
@Destination(style = ProfileTransitions::class)
@Composable
fun DebugPage() {
    val context = LocalContext.current as MainActivity
    val launcher = LocalLauncher.current
    val navController = LocalNavController.current
    val store by storeFlow.collectAsState()

    var showPortDlg by remember {
        mutableStateOf(false)
    }

    PageScaffold(topBar = {
        TopAppBar(navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                )
            }
        }, title = { Text(text = stringResource(R.string.advanced_settings)) }, actions = {})
    }, content = { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 10.dp, end = 10.dp)
                .padding(contentPadding),
        ) {
            val shizukuIsOk by usePollState { shizukuIsSafeOK() }
            if (!shizukuIsOk) {
                AuthCard(title = stringResource(R.string.shizuku_authorization),
                    desc = stringResource(R.string.shizuku_mode_desc),
                    onAuthClick = {
                        try {
                            Shizuku.requestPermission(Activity.RESULT_OK)
                        } catch (e: Exception) {
                            LogUtils.d("Shizuku授权错误", e)
                            ToastUtils.showShort(app.getString(R.string.shizuku_not_running))
                        }
                    })
                Divider()
            } else {
                TextSwitch(name = stringResource(R.string.shizuku_mode),
                    desc = stringResource(R.string.shizuku_mode_desc),
                    checked = store.enableShizuku,
                    onCheckedChange = { enableShizuku ->
                        if (enableShizuku) {
                            appScope.launchTry(Dispatchers.IO) {
                                // 校验方法是否适配, 再允许使用 shizuku
                                val tasks = newActivityTaskManager()?.safeGetTasks()?.firstOrNull()
                                if (tasks != null) {
                                    updateStorage(
                                        storeFlow, store.copy(
                                            enableShizuku = true
                                        )
                                    )
                                } else {
                                    ToastUtils.showShort(app.getString(R.string.shizuku_validation_failed))
                                }
                            }
                        } else {
                            updateStorage(
                                storeFlow, store.copy(
                                    enableShizuku = false
                                )
                            )
                        }

                    })
                Divider()
            }

            TextSwitch(
                name = stringResource(R.string.match_unknown_apps),
                desc = stringResource(R.string.match_unknown_apps_desc),
                checked = store.matchUnknownApp
            ) {
                updateStorage(
                    storeFlow, store.copy(
                        matchUnknownApp = it
                    )
                )
            }
            Divider()

            val httpServerRunning by usePollState { HttpService.isRunning() }
            TextSwitch(
                name = stringResource(R.string.http_service),
                desc = stringResource(R.string.http_service_desc) + if (httpServerRunning) "\n${
                    Ext.getIpAddressInLocalNetwork()
                        .map { host -> "http://${host}:${store.httpServerPort}" }.joinToString(",")
                }" else "",
                checked = httpServerRunning
            ) {
                if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    ToastUtils.showShort(app.getString(R.string.enable_notification_permission_first))
                    return@TextSwitch
                }
                if (it) {
                    HttpService.start()
                } else {
                    HttpService.stop()
                }
            }
            Divider()

            SettingItem(
                title = stringResource(R.string.http_service_port, store.httpServerPort),
                imageVector = Icons.Default.Edit
            ) {
                showPortDlg = true
            }
            Divider()

            TextSwitch(
                name = stringResource(R.string.auto_clear_subscription_cache),
                desc = stringResource(R.string.auto_clear_subscription_cache_desc),
                checked = store.autoClearMemorySubs
            ) {
                updateStorage(
                    storeFlow, store.copy(
                        autoClearMemorySubs = it
                    )
                )
            }
            Divider()

            // android 11 以上可以使用无障碍服务获取屏幕截图
            // Build.VERSION.SDK_INT < Build.VERSION_CODES.R
            val screenshotRunning by usePollState { ScreenshotService.isRunning() }
            TextSwitch(name = stringResource(R.string.screenshot_service),
                desc = stringResource(R.string.screenshot_service_desc),
                checked = screenshotRunning,
                onCheckedChange = appScope.launchAsFn<Boolean> {
                    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                        ToastUtils.showShort(app.getString(R.string.enable_notification_permission_first))
                        return@launchAsFn
                    }
                    if (it) {
                        val mediaProjectionManager =
                            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        val activityResult =
                            launcher.launchForResult(mediaProjectionManager.createScreenCaptureIntent())
                        if (activityResult.resultCode == Activity.RESULT_OK && activityResult.data != null) {
                            ScreenshotService.start(intent = activityResult.data!!)
                        }
                    } else {
                        ScreenshotService.stop()
                    }
                })
            Divider()


            val floatingRunning by usePollState {
                FloatingService.isRunning()
            }
            TextSwitch(
                name = stringResource(R.string.overlay_service),
                desc = stringResource(R.string.overlay_service_desc),
                checked = floatingRunning
            ) {
                if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    ToastUtils.showShort(app.getString(R.string.enable_notification_permission_first))
                    return@TextSwitch
                }

                if (it) {
                    if (Settings.canDrawOverlays(context)) {
                        val intent = Intent(context, FloatingService::class.java)
                        ContextCompat.startForegroundService(context, intent)
                    } else {
                        ToastUtils.showShort(app.getString(R.string.enable_overlay_permission_first))
                    }
                } else {
                    FloatingService.stop(context)
                }
            }
            Divider()
            TextSwitch(
                name = stringResource(R.string.volume_snapshot),
                desc = stringResource(R.string.volume_snapshot_desc),
                checked = store.captureVolumeChange
            ) {
                updateStorage(
                    storeFlow, store.copy(
                        captureVolumeChange = it
                    )
                )
            }

            Divider()
            TextSwitch(
                name = stringResource(R.string.screenshot_and_snapshot),
                desc = stringResource(R.string.screenshot_and_snapshot_desc),
                checked = store.captureScreenshot
            ) {
                updateStorage(
                    storeFlow, store.copy(
                        captureScreenshot = it
                    )
                )
            }

            Divider()
            TextSwitch(
                name = stringResource(R.string.hide_snapshot_status_bar),
                desc = stringResource(R.string.hide_snapshot_status_bar_desc),
                checked = store.hideSnapshotStatusBar
            ) {
                updateStorage(
                    storeFlow, store.copy(
                        hideSnapshotStatusBar = it
                    )
                )
            }
            Divider()

            SettingItem(title = stringResource(R.string.snapshot_history), onClick = {
                navController.navigate(SnapshotPageDestination)
            })
        }
    })

    if (showPortDlg) {
        Dialog(onDismissRequest = { showPortDlg = false }) {
            var value by remember {
                mutableStateOf(store.httpServerPort.toString())
            }
            AlertDialog(title = { Text(text = stringResource(R.string.enter_new_port)) }, text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it.filter { c -> c.isDigit() }.take(5)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text(
                            text = "${value.length} / 5",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    },
                )
            }, onDismissRequest = { showPortDlg = false }, confirmButton = {
                TextButton(onClick = {
                    val newPort = value.toIntOrNull()
                    if (newPort == null || !(5000 <= newPort && newPort <= 65535)) {
                        ToastUtils.showShort(app.getString(R.string.enter_new_port_desc))
                        return@TextButton
                    }
                    updateStorage(
                        storeFlow, store.copy(
                            httpServerPort = newPort
                        )
                    )
                    showPortDlg = false
                }) {
                    Text(
                        text = stringResource(R.string.confirm)
                    )
                }
            }, dismissButton = {
                TextButton(onClick = { showPortDlg = false }) {
                    Text(
                        text = stringResource(id = R.string.cancel)
                    )
                }
            })
        }
    }
}