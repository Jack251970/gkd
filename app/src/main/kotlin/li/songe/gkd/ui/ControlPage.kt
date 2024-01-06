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
import androidx.compose.material.icons.filled.Home
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
import androidx.hilt.navigation.compose.hiltViewModel
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.app
import li.songe.gkd.appScope
import li.songe.gkd.service.GkdAbService
import li.songe.gkd.service.ManageService
import li.songe.gkd.ui.component.AuthCard
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.destinations.ClickLogPageDestination
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.checkOrRequestNotifPermission
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.navigate
import li.songe.gkd.util.storeFlow
import li.songe.gkd.util.updateStorage
import li.songe.gkd.util.usePollState

val controlNav = BottomNavItem(label = app.getString(R.string.homepage), icon = Icons.Default.Home)

@Composable
fun ControlPage() {
    val context = LocalContext.current as MainActivity
    val navController = LocalNavController.current
    val vm = hiltViewModel<ControlVm>()
    val latestRecordDesc by vm.latestRecordDescFlow.collectAsState()
    val subsStatus by vm.subsStatusFlow.collectAsState()
    val store by storeFlow.collectAsState()

    val gkdAccessRunning by GkdAbService.isRunning.collectAsState()
    val manageRunning by ManageService.isRunning.collectAsState()
    val canDrawOverlays by usePollState { Settings.canDrawOverlays(context) }

    Column(
        modifier = Modifier.verticalScroll(
            state = rememberScrollState()
        )
    ) {
        if (!gkdAccessRunning) {
            AuthCard(title = stringResource(R.string.accessibility_permission),
                desc = stringResource(R.string.accessibility_permission_desc),
                onAuthClick = {
                    appScope.launchTry {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        // android.content.ActivityNotFoundException
                        context.startActivity(intent)
                    }
                })
        } else {
            TextSwitch(
                name = stringResource(R.string.enable_service),
                desc = stringResource(R.string.enable_service_desc),
                checked = store.enableService,
                onCheckedChange = {
                    updateStorage(
                        storeFlow, store.copy(
                            enableService = it
                        )
                    )
                })
        }
        Divider()

        TextSwitch(
            name = stringResource(R.string.stay_notification_bar),
            desc = stringResource(R.string.stay_notification_bar_desc),
            checked = manageRunning && store.enableStatusService,
            onCheckedChange = {
                if (it) {
                    if (!checkOrRequestNotifPermission(context)) {
                        return@TextSwitch
                    }
                    updateStorage(
                        storeFlow, store.copy(
                            enableStatusService = true
                        )
                    )
                    ManageService.start(context)
                } else {
                    updateStorage(
                        storeFlow, store.copy(
                            enableStatusService = false
                        )
                    )
                    ManageService.stop(context)
                }
            })
        Divider()

        if (!canDrawOverlays) {
            AuthCard(
                title = stringResource(R.string.overlay_permission),
                desc = stringResource(R.string.overlay_permission_desc),
                onAuthClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
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
