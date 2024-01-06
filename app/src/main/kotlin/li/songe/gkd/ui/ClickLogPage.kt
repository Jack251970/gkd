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
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.ToastUtils
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import li.songe.gkd.R
import li.songe.gkd.app
import li.songe.gkd.data.ClickLog
import li.songe.gkd.data.ExcludeData
import li.songe.gkd.data.SubsConfig
import li.songe.gkd.data.stringify
import li.songe.gkd.db.DbSet
import li.songe.gkd.ui.component.PageScaffold
import li.songe.gkd.ui.destinations.AppItemPageDestination
import li.songe.gkd.ui.destinations.GlobalRulePageDestination
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
                        if (showActivityId != null) {
                            Text(
                                text = showActivityId,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                        }
                        if (group?.name != null) {
                            Text(text = group.name)
                        }
                        if (rule?.name != null) {
                            Text(text = rule.name ?: "")
                        } else if ((group?.rules?.size ?: 0) > 1) {
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

    previewClickLog?.let { clickLog ->
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
                            clickLog.appId ?: return@clickable
                            if (clickLog.groupType == SubsConfig.AppGroupType) {
                                navController.navigate(
                                    AppItemPageDestination(
                                        clickLog.subsId,
                                        clickLog.appId,
                                        clickLog.groupKey
                                    )
                                )
                            } else if (clickLog.groupType == SubsConfig.GlobalGroupType) {
                                navController.navigate(
                                    GlobalRulePageDestination(
                                        clickLog.subsId,
                                        clickLog.groupKey
                                    )
                                )
                            }
                            previewClickLog = null
                        }
                        .fillMaxWidth()
                        .padding(16.dp))

                    if (clickLog.groupType == SubsConfig.GlobalGroupType && clickLog.appId != null) {
                        Text(
                            text = stringResource(R.string.disable_rule_in_app),
                            modifier = Modifier
                                .clickable(onClick = vm.viewModelScope.launchAsFn(Dispatchers.IO) {
                                    val subsConfig = DbSet.subsConfigDao
                                        .queryGlobalGroupTypeConfig(
                                            clickLog.subsId,
                                            clickLog.groupKey
                                        )
                                        .first()
                                        .firstOrNull() ?: SubsConfig(
                                        type = SubsConfig.GlobalGroupType,
                                        subsItemId = clickLog.subsId,
                                        groupKey = clickLog.groupKey,
                                    )
                                    val excludeData = ExcludeData.parse(subsConfig.exclude)
                                    if (excludeData.appIds.contains(clickLog.appId)) {
                                        ToastUtils.showShort(app.getString(R.string.disable_already_exist))
                                        return@launchAsFn
                                    }
                                    previewClickLog = null
                                    val newSubsConfig = subsConfig.copy(
                                        exclude = excludeData
                                            .copy(
                                                appIds = excludeData.appIds
                                                    .toMutableSet()
                                                    .apply { add(clickLog.appId) })
                                            .stringify()
                                    )
                                    DbSet.subsConfigDao.insert(newSubsConfig)
                                    ToastUtils.showShort(app.getString(R.string.update_disable))
                                })
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    }
                    if (clickLog.appId != null && clickLog.activityId != null) {
                        Text(
                            text = stringResource(R.string.disable_rule_in_app),
                            modifier = Modifier
                                .clickable(onClick = vm.viewModelScope.launchAsFn(Dispatchers.IO) {
                                    val subsConfig =
                                        if (clickLog.groupType == SubsConfig.AppGroupType) {
                                            DbSet.subsConfigDao
                                                .queryAppGroupTypeConfig(
                                                    clickLog.subsId,
                                                    clickLog.appId,
                                                    clickLog.groupKey
                                                )
                                                .first()
                                                .firstOrNull() ?: SubsConfig(
                                                type = SubsConfig.AppGroupType,
                                                subsItemId = clickLog.subsId,
                                                appId = clickLog.appId,
                                                groupKey = clickLog.groupKey,
                                            )
                                        } else {
                                            DbSet.subsConfigDao
                                                .queryGlobalGroupTypeConfig(
                                                    clickLog.subsId,
                                                    clickLog.groupKey
                                                )
                                                .first()
                                                .firstOrNull() ?: SubsConfig(
                                                type = SubsConfig.GlobalGroupType,
                                                subsItemId = clickLog.subsId,
                                                groupKey = clickLog.groupKey,
                                            )
                                        }
                                    val excludeData = ExcludeData.parse(subsConfig.exclude)
                                    if (excludeData.appIds.contains(clickLog.appId) ||
                                        excludeData.activityMap[clickLog.appId]?.any { a ->
                                            a.startsWith(
                                                clickLog.activityId
                                            )
                                        } == true
                                    ) {
                                        ToastUtils.showShort(app.getString(R.string.disable_already_exist))
                                        return@launchAsFn
                                    }
                                    previewClickLog = null
                                    val newSubsConfig = subsConfig.copy(
                                        exclude = excludeData
                                            .copy(
                                                activityMap = excludeData.activityMap
                                                    .toMutableMap()
                                                    .apply {
                                                        this[clickLog.appId] = (get(clickLog.appId)
                                                            ?: emptyList())
                                                            .toMutableList()
                                                            .apply {
                                                                add(clickLog.activityId)
                                                            }
                                                    }
                                            )
                                            .stringify()
                                    )
                                    DbSet.subsConfigDao.insert(newSubsConfig)
                                    ToastUtils.showShort(app.getString(R.string.update_disable))
                                })
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    }

                    Text(
                        text = stringResource(R.string.delete_history),
                        modifier = Modifier
                            .clickable(onClick = scope.launchAsFn {
                                previewClickLog = null
                                DbSet.clickLogDao.delete(clickLog)
                                ToastUtils.showShort(app.getString(R.string.delete_success))
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