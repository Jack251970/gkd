package li.songe.gkd.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pullrefresh.PullRefreshIndicator
import androidx.compose.material3.pullrefresh.pullRefresh
import androidx.compose.material3.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import li.songe.gkd.R
import li.songe.gkd.app
import li.songe.gkd.data.SubsItem
import li.songe.gkd.data.SubscriptionRaw
import li.songe.gkd.db.DbSet
import li.songe.gkd.ui.component.PageScaffold
import li.songe.gkd.ui.component.SubsItemCard
import li.songe.gkd.ui.destinations.CategoryPageDestination
import li.songe.gkd.ui.destinations.SubsPageDestination
import li.songe.gkd.util.DEFAULT_SUBS_UPDATE_URL
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.SafeR
import li.songe.gkd.util.formatTimeAgo
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.navigate
import li.songe.gkd.util.shareFile
import li.songe.gkd.util.subsIdToRawFlow
import li.songe.gkd.util.subsItemsFlow
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

val subsNav = BottomNavItem(
    label = app.getString(R.string.subscription), icon = SafeR.ic_link, route = "subscription"
)


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SubsManagePage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current

    val vm = hiltViewModel<SubsManageVm>()
    val subItems by subsItemsFlow.collectAsState()
    val subsIdToRaw by subsIdToRawFlow.collectAsState()

    val orderSubItems = remember {
        mutableStateOf(subItems)
    }
    LaunchedEffect(subItems, block = {
        orderSubItems.value = subItems
    })


    var deleteSubItem: SubsItem? by remember { mutableStateOf(null) }
    var menuSubItem: SubsItem? by remember { mutableStateOf(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }

    val (showSubsRaw, setShowSubsRaw) = remember {
        mutableStateOf<SubscriptionRaw?>(null)
    }


    val refreshing by vm.refreshingFlow.collectAsState()
    val pullRefreshState = rememberPullRefreshState(refreshing, vm::refreshSubs)

    val state = rememberReorderableLazyListState(onMove = { from, to ->
        orderSubItems.value = orderSubItems.value.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }, onDragEnd = { _, _ ->
        vm.viewModelScope.launch(Dispatchers.IO) {
            val changeItems = mutableListOf<SubsItem>()
            orderSubItems.value.forEachIndexed { index, subsItem ->
                if (subItems[index] != subsItem) {
                    changeItems.add(
                        subsItem.copy(
                            order = index
                        )
                    )
                }
            }
            DbSet.subsItemDao.update(*changeItems.toTypedArray())
        }
    })

    PageScaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (subItems.any { it.id == 0L }) {
                    showAddLinkDialog = true
                } else {
                    showAddDialog = true
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "info",
                )
            }
        },
        content = { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState, subItems.isNotEmpty())
            ) {
                LazyColumn(
                    state = state.listState,
                    modifier = Modifier
                        .reorderable(state)
                        .detectReorderAfterLongPress(state)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(orderSubItems.value, { _, subItem -> subItem.id }) { index, subItem ->
                        ReorderableItem(state, key = subItem.id) { isDragging ->
                            val elevation = animateDpAsState(
                                if (isDragging) 16.dp else 0.dp, label = ""
                            )
                            Card(
                                modifier = Modifier
                                    .shadow(elevation.value)
                                    .animateItemPlacement()
                                    .padding(vertical = 3.dp, horizontal = 8.dp)
                                    .clickable {
                                        navController.navigate(SubsPageDestination(subItem.id))
                                    },
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                val subsRaw = subsIdToRaw[subItem.id]
                                var name = subsRaw?.name ?: subItem.id.toString()
                                when (name) {
                                    "本地订阅" -> {
                                        name = stringResource(R.string.local_subscription)
                                    }
                                    "默认订阅" -> {
                                        name = stringResource(R.string.default_subscription)
                                    }
                                }
                                subsRaw?.name = name
                                SubsItemCard(
                                    subsItem = subItem,
                                    subscriptionRaw = subsRaw,
                                    index = index + 1,
                                    onMenuClick = {
                                        menuSubItem = subItem
                                    },
                                    onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                                        DbSet.subsItemDao.update(subItem.copy(enable = it))
                                    },
                                )
                            }
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = refreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        },
    )

    menuSubItem?.let { menuSubItemVal ->

        Dialog(onDismissRequest = { menuSubItem = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column {
                    val subsRawVal = subsIdToRaw[menuSubItemVal.id]
                    if (subsRawVal != null) {
                        Text(text = stringResource(R.string.view_rules), modifier = Modifier
                            .clickable {
                                menuSubItem = null
                                navController.navigate(SubsPageDestination(subsRawVal.id))
                            }
                            .fillMaxWidth()
                            .padding(16.dp))
                        Divider()
                        Text(text = "查看类别", modifier = Modifier
                            .clickable {
                                menuSubItem = null
                                navController.navigate(CategoryPageDestination(subsRawVal.id))
                            }
                            .fillMaxWidth()
                            .padding(16.dp))
                        Divider()
                    }
                    if (menuSubItemVal.id < 0 && subsRawVal != null && menuSubItemVal.subsFile.exists()) {
                        Text(text = stringResource(R.string.share_files), modifier = Modifier
                            .clickable {
                                menuSubItem = null
                                context.shareFile(
                                    menuSubItemVal.subsFile,
                                    context.getString(R.string.share_files)
                                )
                            }
                            .fillMaxWidth()
                            .padding(16.dp))
                        Divider()
                    }
                    if (menuSubItemVal.updateUrl != null) {
                        Text(text = stringResource(R.string.copy_link), modifier = Modifier
                            .clickable {
                                menuSubItem = null
                                ClipboardUtils.copyText(menuSubItemVal.updateUrl)
                                ToastUtils.showShort(context.getString(R.string.copy_success))
                            }
                            .fillMaxWidth()
                            .padding(16.dp))
                        Divider()
                    }
                    if (subsRawVal?.supportUri != null) {
                        Text(text = stringResource(R.string.feedback), modifier = Modifier
                            .clickable {
                                menuSubItem = null
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW, Uri.parse(subsRawVal.supportUri)
                                    )
                                )
                            }
                            .fillMaxWidth()
                            .padding(16.dp))
                        Divider()
                    }
                    if (menuSubItemVal.id != -2L) {
                        Text(text = stringResource(R.string.delete_subscription), modifier = Modifier
                            .clickable {
                                deleteSubItem = menuSubItemVal
                                menuSubItem = null
                            }
                            .fillMaxWidth()
                            .padding(16.dp), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }


    deleteSubItem?.let { deleteSubItemVal ->
        AlertDialog(onDismissRequest = { deleteSubItem = null },
            title = {
                Text(
                    text = stringResource(R.string.if_delete, subsIdToRaw[deleteSubItemVal.id]?.name ?: "")
                )},
            confirmButton = {
                TextButton(onClick = scope.launchAsFn {
                    deleteSubItem = null
                    deleteSubItemVal.removeAssets()
                }) {
                    Text(text = stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteSubItem = null
                }) {
                    Text(text = stringResource(R.string.no))
                }
            })
    }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column {
                    Text(text = stringResource(R.string.import_default_subscription), modifier = Modifier
                        .clickable {
                            showAddDialog = false
                            vm.addSubsFromUrl(DEFAULT_SUBS_UPDATE_URL)
                        }
                        .fillMaxWidth()
                        .padding(16.dp))
                    Divider()
                    Text(text = stringResource(R.string.import_other_subscription), modifier = Modifier
                        .clickable {
                            showAddDialog = false
                            showAddLinkDialog = true
                        }
                        .fillMaxWidth()
                        .padding(16.dp))
                }
            }
        }
    }



    LaunchedEffect(showAddLinkDialog) {
        if (!showAddLinkDialog) {
            link = ""
        }
    }
    if (showAddLinkDialog) {
        AlertDialog(title = { Text(text = stringResource(R.string.enter_subscription_link)) }, text = {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it.trim() },
                maxLines = 2,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }, onDismissRequest = { showAddLinkDialog = false }, confirmButton = {
            TextButton(onClick = {
                if (!URLUtil.isNetworkUrl(link)) {
                    ToastUtils.showShort(context.getString(R.string.illegal_link))
                    return@TextButton
                }
                if (subItems.any { s -> s.updateUrl == link }) {
                    ToastUtils.showShort(context.getString(R.string.link_exists))
                    return@TextButton
                }
                showAddLinkDialog = false
                vm.addSubsFromUrl(url = link)
            }) {
                Text(text = stringResource(R.string.add))
            }
        })
    }

    if (showSubsRaw != null) {
        AlertDialog(onDismissRequest = { setShowSubsRaw(null) }, title = {
            Text(text = stringResource(R.string.subscription_details))
        }, text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier
            ) {
                Text(text = stringResource(R.string.name) + showSubsRaw.name)
                Text(text = stringResource(R.string.version) + showSubsRaw.version)
                if (showSubsRaw.author != null) {
                    Text(text = stringResource(R.string.author) + showSubsRaw.author)
                }
                val apps = showSubsRaw.apps
                val groupsSize = apps.sumOf { it.groups.size }
                if (groupsSize > 0) {
                    Text(text = stringResource(R.string.rule_info, apps.size, groupsSize))
                }

                Text(
                    text = stringResource(R.string.update) + formatTimeAgo(
                        subItems.find { s -> s.id == showSubsRaw.id }?.mtime ?: 0
                    )
                )
            }
        }, confirmButton = {
            TextButton(onClick = {
                setShowSubsRaw(null)
            }) {
                Text(text = stringResource(R.string.off))
            }
        })
    }
}