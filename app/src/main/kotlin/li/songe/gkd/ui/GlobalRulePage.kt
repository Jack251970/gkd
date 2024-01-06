package li.songe.gkd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import kotlinx.coroutines.Dispatchers
import li.songe.gkd.R
import li.songe.gkd.app
import li.songe.gkd.data.ExcludeData
import li.songe.gkd.data.RawSubscription
import li.songe.gkd.data.SubsConfig
import li.songe.gkd.data.stringify
import li.songe.gkd.db.DbSet
import li.songe.gkd.ui.component.getDialogResult
import li.songe.gkd.ui.destinations.GroupItemPageDestination
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.ProfileTransitions
import li.songe.gkd.util.encodeToJson5String
import li.songe.gkd.util.json
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.navigate
import li.songe.gkd.util.updateSubscription

@RootNavGraph
@Destination(style = ProfileTransitions::class)
@Composable
fun GlobalRulePage(subsItemId: Long, focusGroupKey: Int? = null) {
    val navController = LocalNavController.current
    val vm = hiltViewModel<GlobalRuleVm>()
    val subsItem by vm.subsItemFlow.collectAsState()
    val rawSubs = vm.subsRawFlow.collectAsState().value
    val subsConfigs by vm.subsConfigsFlow.collectAsState()

    val editable = subsItemId < 0 && rawSubs != null && subsItem != null
    val globalGroups = rawSubs?.globalGroups ?: emptyList()

    var showAddDlg by remember { mutableStateOf(false) }
    val (menuGroupRaw, setMenuGroupRaw) = remember {
        mutableStateOf<RawSubscription.RawGlobalGroup?>(null)
    }
    val (editGroupRaw, setEditGroupRaw) = remember {
        mutableStateOf<RawSubscription.RawGlobalGroup?>(null)
    }
    val (excludeGroupRaw, setExcludeGroupRaw) = remember {
        mutableStateOf<RawSubscription.RawGlobalGroup?>(null)
    }
    val (showGroupItem, setShowGroupItem) = remember {
        mutableStateOf<RawSubscription.RawGlobalGroup?>(
            null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(navigationIcon = {
                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                    )
                }
            }, title = {
                var name = rawSubs?.name ?: subsItemId.toString()
                when (name) {
                    "本地订阅" -> {
                        name = stringResource(R.string.local_subscription)
                    }
                    "默认订阅" -> {
                        name = stringResource(R.string.default_subscription)
                    }
                }
                Text(text = "${name}/${stringResource(R.string.global_rules)}")
            })
        }, floatingActionButton = {
            if (editable) {
                FloatingActionButton(onClick = { showAddDlg = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "add",
                    )
                }
            }
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier
                .padding(paddingValues)
                .padding(start = 10.dp, end = 10.dp)) {
                items(globalGroups, { g -> g.key }) { group ->
                    Row(
                        modifier = Modifier
                            .background(
                                if (group.key == focusGroupKey) MaterialTheme.colorScheme.inversePrimary else Color.Transparent
                            )
                            .clickable { setShowGroupItem(group) }
                            .padding(10.dp, 6.dp)
                            .fillMaxWidth()
                            .height(45.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = group.name,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (group.valid) {
                                Text(
                                    text = group.desc ?: "",
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontSize = 14.sp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.illegal_selector),
                                    modifier = Modifier.fillMaxWidth(),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(onClick = {
                            setMenuGroupRaw(group)
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))

                        val groupEnable = subsConfigs.find { c -> c.groupKey == group.key }?.enable
                            ?: group.enable ?: true
                        val subsConfig = subsConfigs.find { it.groupKey == group.key }
                        Switch(
                            checked = groupEnable, modifier = Modifier,
                            onCheckedChange = vm.viewModelScope.launchAsFn { enable ->
                                val newItem = (subsConfig?.copy(enable = enable) ?: SubsConfig(
                                    type = SubsConfig.GlobalGroupType,
                                    subsItemId = subsItemId,
                                    groupKey = group.key,
                                    enable = enable
                                ))
                                DbSet.subsConfigDao.insert(newItem)
                            }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    if (globalGroups.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_rules),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )

    if (showAddDlg && rawSubs != null) {
        var source by remember {
            mutableStateOf("")
        }
        AlertDialog(title = { Text(text = stringResource(R.string.add_global_rule_group)) }, text = {
            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.enter_rule_group)) },
                maxLines = 10,
            )
        }, onDismissRequest = { showAddDlg = false }, confirmButton = {
            TextButton(onClick = {
                val newGroup = try {
                    RawSubscription.parseRawGlobalGroup(source)
                } catch (e: Exception) {
                    ToastUtils.showShort(app.getString(R.string.illegal_rule, e.message))
                    return@TextButton
                }
                if (!newGroup.valid) {
                    ToastUtils.showShort(app.getString(R.string.illegal_rule_selector_illegal))
                    return@TextButton
                }
                if (rawSubs.globalGroups.any { g -> g.name == newGroup.name }) {
                    ToastUtils.showShort(app.getString(R.string.exist_rule_with_same_name, newGroup.name))
                    return@TextButton
                }
                val newKey = (rawSubs.globalGroups.maxByOrNull { g -> g.key }?.key ?: -1) + 1
                val newRawSubs = rawSubs.copy(
                    globalGroups = rawSubs.globalGroups.toMutableList()
                        .apply { add(newGroup.copy(key = newKey)) }
                )
                updateSubscription(newRawSubs)
                vm.viewModelScope.launchTry(Dispatchers.IO) {
                    showAddDlg = false
                    ToastUtils.showShort(app.getString(R.string.add_success))
                }
            }, enabled = source.isNotEmpty()) {
                Text(text = stringResource(R.string.add))
            }
        }, dismissButton = {
            TextButton(onClick = { showAddDlg = false }) {
                Text(text = stringResource(R.string.cancel))
            }
        })
    }

    if (menuGroupRaw != null && rawSubs != null) {
        Dialog(onDismissRequest = { setMenuGroupRaw(null) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column {
                    Text(text = stringResource(R.string.edit_disabled_item), modifier = Modifier
                        .clickable {
                            setExcludeGroupRaw(menuGroupRaw)
                            setMenuGroupRaw(null)
                        }
                        .padding(16.dp)
                        .fillMaxWidth())
                    if (editable) {
                        Text(text = stringResource(R.string.edit_rule_group), modifier = Modifier
                            .clickable {
                                setEditGroupRaw(menuGroupRaw)
                                setMenuGroupRaw(null)
                            }
                            .padding(16.dp)
                            .fillMaxWidth())
                        Text(text = stringResource(R.string.delete_rule_group), modifier = Modifier
                            .clickable {
                                setMenuGroupRaw(null)
                                vm.viewModelScope.launchTry {
                                    if (!getDialogResult(
                                            app.getString(
                                                R.string.if_delete,
                                                menuGroupRaw.name
                                            )
                                        )
                                    ) return@launchTry
                                    updateSubscription(
                                        rawSubs.copy(
                                            globalGroups = rawSubs.globalGroups.filter { g -> g.key != menuGroupRaw.key }
                                        )
                                    )
                                    val subsConfig =
                                        subsConfigs.find { it.groupKey == menuGroupRaw.key }
                                    if (subsConfig != null) {
                                        DbSet.subsConfigDao.delete(subsConfig)
                                    }
                                    DbSet.subsItemDao.updateMtime(rawSubs.id)
                                }
                            }
                            .padding(16.dp)
                            .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    if (editGroupRaw != null && rawSubs != null) {
        var source by remember {
            mutableStateOf(json.encodeToJson5String(editGroupRaw))
        }
        val oldSource = remember { source }
        AlertDialog(
            title = { Text(text = stringResource(R.string.edit_rule_group)) },
            text = {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(text = stringResource(R.string.enter_rule_group)) },
                    maxLines = 10,
                )
            },
            onDismissRequest = { setEditGroupRaw(null) },
            dismissButton = {
                TextButton(onClick = { setEditGroupRaw(null) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (oldSource == source) {
                        ToastUtils.showShort(app.getString(R.string.rule_group_unchanged))
                        return@TextButton
                    }
                    val newGroupRaw = try {
                        RawSubscription.parseRawGlobalGroup(source)
                    } catch (e: Exception) {
                        LogUtils.d(e)
                        ToastUtils.showShort(app.getString(R.string.illegal_rule, e.message))
                        return@TextButton
                    }
                    if (newGroupRaw.key != editGroupRaw.key) {
                        ToastUtils.showShort(app.getString(R.string.cannot_change_rule_group_key))
                        return@TextButton
                    }
                    if (!newGroupRaw.valid) {
                        ToastUtils.showShort(app.getString(R.string.illegal_rule_selector_illegal))
                        return@TextButton
                    }
                    setEditGroupRaw(null)
                    val newGlobalGroups = rawSubs.globalGroups.toMutableList().apply {
                        val i = rawSubs.globalGroups.indexOfFirst { g -> g.key == newGroupRaw.key }
                        if (i >= 0) {
                            set(i, newGroupRaw)
                        }
                    }
                    updateSubscription(rawSubs.copy(globalGroups = newGlobalGroups))
                    vm.viewModelScope.launchTry(Dispatchers.IO) {
                        DbSet.subsItemDao.updateMtime(rawSubs.id)
                        ToastUtils.showShort(app.getString(R.string.update_success))
                    }
                }, enabled = source.isNotEmpty()) {
                    Text(text = stringResource(R.string.update))
                }
            },
        )
    }

    if (excludeGroupRaw != null && rawSubs != null) {
        var source by remember {
            mutableStateOf(
                ExcludeData.parse(subsConfigs.find { s -> s.groupKey == excludeGroupRaw.key }?.exclude)
                    .stringify()
            )
        }
        val oldSource = remember { source }
        AlertDialog(
            title = { Text(text = stringResource(R.string.edit_disabled_item)) },
            text = {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            fontSize = 12.sp,
                            text = stringResource(R.string.edit_disabled_item_placeholder2)
                        )
                    },
                    maxLines = 10,
                )
            },
            onDismissRequest = { setExcludeGroupRaw(null) },
            dismissButton = {
                TextButton(onClick = { setExcludeGroupRaw(null) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (oldSource == source) {
                        ToastUtils.showShort(app.getString(R.string.disabled_item_unchanged))
                        return@TextButton
                    }
                    setExcludeGroupRaw(null)
                    val newSubsConfig =
                        (subsConfigs.find { s -> s.groupKey == excludeGroupRaw.key } ?: SubsConfig(
                            type = SubsConfig.GlobalGroupType,
                            subsItemId = subsItemId,
                            groupKey = excludeGroupRaw.key,
                        )).copy(exclude = ExcludeData.parse(source).stringify())
                    vm.viewModelScope.launchTry(Dispatchers.IO) {
                        DbSet.subsConfigDao.insert(newSubsConfig)
                        ToastUtils.showShort(app.getString(R.string.update_success))
                    }
                }) {
                    Text(text = stringResource(R.string.update))
                }
            },
        )
    }

    if (showGroupItem != null) {
        AlertDialog(
            modifier = Modifier.defaultMinSize(300.dp),
            onDismissRequest = { setShowGroupItem(null) },
            title = {
                Text(text = showGroupItem.name)
            },
            text = {
                Column {
                    if (showGroupItem.enable == false) {
                        Text(text = stringResource(R.string.rule_group_default_disable))
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Text(text = showGroupItem.desc ?: "")
                }
            },
            confirmButton = {
                Row {
                    if (showGroupItem.allExampleUrls.isNotEmpty()) {
                        TextButton(onClick = {
                            setShowGroupItem(null)
                            navController.navigate(
                                GroupItemPageDestination(
                                    subsInt = subsItemId,
                                    groupKey = showGroupItem.key
                                )
                            )
                        }) {
                            Text(text = stringResource(R.string.view_image))
                        }
                    }
                    TextButton(onClick = {
                        val groupAppText = json.encodeToJson5String(showGroupItem)
                        ClipboardUtils.copyText(groupAppText)
                        ToastUtils.showShort(app.getString(R.string.copy_success))
                    }) {
                        Text(text = stringResource(R.string.copy_rule_group))
                    }
                }
            })
    }
}