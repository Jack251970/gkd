package li.songe.gkd.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import kotlinx.coroutines.Dispatchers
import li.songe.gkd.R
import li.songe.gkd.data.ClickLog
import li.songe.gkd.db.DbSet
import li.songe.gkd.ui.component.PageScaffold
import li.songe.gkd.ui.destinations.AppItemPageDestination
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.ProfileTransitions
import li.songe.gkd.util.appInfoCacheFlow
import li.songe.gkd.util.format
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.navigate

@RootNavGraph
@Destination(style = ProfileTransitions::class)
@Composable
fun ClickLogPage() {
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current

    val vm = hiltViewModel<ClickLogVm>()
    val clickDataList by vm.clickDataListFlow.collectAsState()
    val clickLogCount by vm.clickLogCountFlow.collectAsState()
    val appInfoCache by appInfoCacheFlow.collectAsState()

    var previewClickLog by remember {
        mutableStateOf<ClickLog?>(null)
    }
    var showDeleteDlg by remember {
        mutableStateOf(false)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    PageScaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        TopAppBar(scrollBehavior = scrollBehavior,
            navigationIcon = {
                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                    )
                }
            },
            title = { Text(text = stringResource(R.string.click_history) + if (clickLogCount <= 0) "" else ("-$clickLogCount")) },
            actions = {
                if (clickDataList.isNotEmpty()) {
                    IconButton(onClick = { showDeleteDlg = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                        )
                    }
                }
            })
    }, content = { contentPadding ->
        if (clickDataList.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(start = 10.dp, end = 10.dp),
            ) {
                items(clickDataList, { it.t0.id }) { (clickLog, group, rule) ->
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .clickable {
                            previewClickLog = clickLog
                        }
                        .fillMaxWidth()
                        .padding(10.dp)) {
                        Row {
                            Text(
                                text = clickLog.id.format("MM-dd HH:mm:ss"),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = appInfoCache[clickLog.appId]?.name ?: clickLog.appId ?: ""
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        val showActivityId = if (clickLog.activityId != null) {
                            if (clickLog.appId != null && clickLog.activityId.startsWith(
                                    clickLog.appId
                                )
                            ) {
                                clickLog.activityId.substring(clickLog.appId.length)
                            } else {
                                clickLog.activityId
                            }
                        } else {
                            null
                        }
                        Text(
                            text = showActivityId ?: "null",
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                        if (group?.name != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = group.name)
                        }
                        if (rule?.name != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = rule.name)
                        } else if ((group?.rules?.size ?: 0) > 1) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = (if (clickLog.ruleKey != null) "key=${clickLog.ruleKey}, " else "") + "index=${clickLog.ruleIndex}")
                        }
                    }
                    Divider()
                }
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(text = stringResource(R.string.no_history))
            }
        }
    })

    previewClickLog?.let { previewTriggerLogVal ->
        Dialog(onDismissRequest = { previewClickLog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column {
                    Text(text = stringResource(R.string.view_rule_group), modifier = Modifier
                        .clickable {
                            previewTriggerLogVal.appId ?: return@clickable
                            navController.navigate(
                                AppItemPageDestination(
                                    previewTriggerLogVal.subsId,
                                    previewTriggerLogVal.appId,
                                    previewTriggerLogVal.groupKey
                                )
                            )
                            previewClickLog = null
                        }
                        .fillMaxWidth()
                        .padding(16.dp))
                    Text(
                        text = stringResource(R.string.delete),
                        modifier = Modifier
                            .clickable(onClick = scope.launchAsFn {
                                previewClickLog = null
                                DbSet.clickLogDao.delete(previewTriggerLogVal)
                            })
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

        }
    }

    if (showDeleteDlg) {
        AlertDialog(onDismissRequest = { showDeleteDlg = false },
            title = { Text(text = stringResource(R.string.if_delete_all_click_history)) },
            confirmButton = {
                TextButton(onClick = scope.launchAsFn(Dispatchers.IO) {
                    showDeleteDlg = false
                    DbSet.clickLogDao.deleteAll()
                }) {
                    Text(text = stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDlg = false
                }) {
                    Text(text = stringResource(R.string.no))
                }
            })
    }

}