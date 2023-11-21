package li.songe.gkd.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.appScope
import li.songe.gkd.service.GkdAbService
import li.songe.gkd.ui.component.AuthCard
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.destinations.ClickLogPageDestination
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.SafeR
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.navigate
import li.songe.gkd.util.storeFlow
import li.songe.gkd.util.updateStorage
import li.songe.gkd.util.usePollState

val controlNav = BottomNavItem(label = "主页", icon = SafeR.ic_home, route = "control")

@Composable
fun ControlPage() {
    val context = LocalContext.current as MainActivity
    val navController = LocalNavController.current
    val vm = hiltViewModel<ControlVm>()
    val latestRecordDesc by vm.latestRecordDescFlow.collectAsState()
    val subsStatus by vm.subsStatusFlow.collectAsState()
    val store by storeFlow.collectAsState()

    val gkdAccessRunning by usePollState { GkdAbService.isRunning() }
    val notifyEnabled by usePollState {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val canDrawOverlays by usePollState { Settings.canDrawOverlays(context) }


    Column(
        modifier = Modifier.verticalScroll(
            state = rememberScrollState()
        )
    ) {
        if (!notifyEnabled) {
            AuthCard(title = stringResource(R.string.notification_permission),
                desc = stringResource(R.string.notification_permission_desc),
                onAuthClick = {
                val intent = Intent()
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.applicationInfo.uid)
                context.startActivity(intent)
            })
            Divider()
        }

        if (!gkdAccessRunning) {
            AuthCard(title = stringResource(R.string.accessibility_permission),
                desc = stringResource(R.string.accessibility_permission_desc),
                onAuthClick = {
                    if (notifyEnabled) {
                        appScope.launchTry(Dispatchers.IO) {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            // android.content.ActivityNotFoundException
                            // https://bugly.qq.com/v2/crash-reporting/crashes/d0ce46b353/113010?pid=1
                            context.startActivity(intent)
                        }
                    } else {
                        ToastUtils.showShort(context.getString(R.string.enable_notification_permission_first))
                    }
                })
            Divider()
        }

        if (!canDrawOverlays) {
            AuthCard(title = stringResource(R.string.draw_overlays_permission),
                desc = stringResource(R.string.draw_overlays_permission_desc),
                onAuthClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                })
            Divider()
        }

        if (gkdAccessRunning) {
            TextSwitch(name = stringResource(R.string.enable_service),
                desc = stringResource(R.string.enable_service_desc),
                checked = store.enableService,
                onCheckedChange = {
                    updateStorage(
                        storeFlow, store.copy(
                            enableService = it
                        )
                    )
                })
            Divider()
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    navController.navigate(ClickLogPageDestination)
                }
                .padding(10.dp, 5.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.click_history), fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.quickly_locate_disabling_rules_here), fontSize = 14.sp
                )
            }
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null)
        }
        Divider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 5.dp)
        ) {
            Text(text = subsStatus, fontSize = 18.sp)
            if (latestRecordDesc != null) {
                Text(
                    text = stringResource(R.string.latest_clicking, latestRecordDesc!!), fontSize = 14.sp
                )
            }
        }

    }
}
