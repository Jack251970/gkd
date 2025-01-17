package li.songe.gkd.ui

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.UriUtils
import com.dylanc.activityresult.launcher.launchForResult
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.songe.gkd.R
import li.songe.gkd.app
import li.songe.gkd.data.Snapshot
import li.songe.gkd.db.DbSet
import li.songe.gkd.debug.SnapshotExt
import li.songe.gkd.ui.component.PageScaffold
import li.songe.gkd.ui.destinations.ImagePreviewPageDestination
import li.songe.gkd.util.IMPORT_BASE_URL
import li.songe.gkd.util.LoadStatus
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.LocalPickContentLauncher
import li.songe.gkd.util.LocalRequestPermissionLauncher
import li.songe.gkd.util.ProfileTransitions
import li.songe.gkd.util.format
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.navigate
import li.songe.gkd.util.shareFile
import li.songe.gkd.util.snapshotZipDir
import java.io.File

@RootNavGraph
@Destination(style = ProfileTransitions::class)
@Composable
fun SnapshotPage() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current as ComponentActivity
    val navController = LocalNavController.current
    val colorScheme = MaterialTheme.colorScheme

    val pickContentLauncher = LocalPickContentLauncher.current
    val requestPermissionLauncher = LocalRequestPermissionLauncher.current

    val vm = hiltViewModel<SnapshotVm>()
    val snapshots by vm.snapshotsState.collectAsState()
    val uploadStatus by vm.uploadStatusFlow.collectAsState()

    var selectedSnapshot by remember {
        mutableStateOf<Snapshot?>(null)
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
            title = { Text(
                text = if (snapshots.isEmpty()) stringResource(R.string.snapshot_history) else
                    stringResource(R.string.snapshot_history) +"-${snapshots.size}") },
            actions = {
                if (snapshots.isNotEmpty()) {
                    IconButton(onClick = { showDeleteDlg = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                        )
                    }
                }
            })
    }, content = { contentPadding ->
        if (snapshots.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(start = 10.dp, end = 10.dp),
            ) {
                items(snapshots, { it.id }) { snapshot ->
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedSnapshot = snapshot
                        }
                        .padding(10.dp)) {
                        Row {
                            Text(
                                text = snapshot.id.format("MM-dd HH:mm:ss"),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = snapshot.appName ?: snapshot.appId ?: snapshot.id.toString(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                        }
                        if (snapshot.activityId != null) {
                            val showActivityId =
                                if (snapshot.appId != null && snapshot.activityId.startsWith(
                                        snapshot.appId
                                    )
                                ) {
                                    snapshot.activityId.substring(snapshot.appId.length)
                                } else {
                                    snapshot.activityId
                                }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = showActivityId, overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
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

    selectedSnapshot?.let { snapshotVal ->
        Dialog(onDismissRequest = { selectedSnapshot = null }) {
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
                        text = stringResource(R.string.view), modifier = Modifier
                            .clickable(onClick = scope.launchAsFn {
                                navController.navigate(
                                    ImagePreviewPageDestination(
                                        filePath = snapshotVal.screenshotFile.absolutePath,
                                        title = snapshotVal.appName,
                                    )
                                )
                                selectedSnapshot = null
                            })
                            .then(modifier)
                    )
                    Divider()
                    Text(
                        text = stringResource(R.string.share),
                        modifier = Modifier
                            .clickable(onClick = vm.viewModelScope.launchAsFn {
                                val zipFile = SnapshotExt.getSnapshotZipFile(snapshotVal.id)
                                context.shareFile(
                                    zipFile,
                                    app.getString(R.string.share_snapshot_file)
                                )
                                selectedSnapshot = null
                            })
                            .then(modifier)
                    )
                    Divider()
                    if (snapshotVal.githubAssetId != null) {
                        Text(
                            text = stringResource(R.string.copy_link), modifier = Modifier
                                .clickable(onClick = {
                                    selectedSnapshot = null
                                    ClipboardUtils.copyText(IMPORT_BASE_URL + snapshotVal.githubAssetId)
                                    ToastUtils.showShort(app.getString(R.string.copy_success))
                                })
                                .then(modifier)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.generate_link), modifier = Modifier
                                .clickable(onClick = {
                                    selectedSnapshot = null
                                    vm.uploadZip(snapshotVal)
                                })
                                .then(modifier)
                        )
                    }
                    Divider()

                    Text(
                        text = stringResource(R.string.save_screenshot_to_gallery),
                        modifier = Modifier
                            .clickable(onClick = vm.viewModelScope.launchAsFn {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                    val isGranted =
                                        requestPermissionLauncher.launchForResult(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    if (!isGranted) {
                                        ToastUtils.showShort(app.getString(R.string.save_fail))
                                        return@launchAsFn
                                    }
                                }
                                ImageUtils.save2Album(
                                    ImageUtils.getBitmap(snapshotVal.screenshotFile),
                                    Bitmap.CompressFormat.PNG,
                                    true
                                )
                                ToastUtils.showShort(app.getString(R.string.save_success))
                                selectedSnapshot = null
                            })
                            .then(modifier)
                    )
                    Divider()
                    Text(
                        text = stringResource(R.string.replace_screenshot),
                        modifier = Modifier
                            .clickable(onClick = vm.viewModelScope.launchAsFn {
                                val uri = pickContentLauncher.launchForImageResult()
                                withContext(Dispatchers.IO) {
                                    val oldBitmap = ImageUtils.getBitmap(snapshotVal.screenshotFile)
                                    val newBytes = UriUtils.uri2Bytes(uri)
                                    val newBitmap = ImageUtils.getBitmap(newBytes, 0)
                                    if (oldBitmap.width == newBitmap.width && oldBitmap.height == newBitmap.height) {
                                        snapshotVal.screenshotFile.writeBytes(newBytes)
                                        File(snapshotZipDir, "${snapshotVal.id}.zip").apply {
                                            if (exists()) delete()
                                        }
                                        if (snapshotVal.githubAssetId != null) {
                                            // 当本地快照变更时, 移除快照链接
                                            DbSet.snapshotDao.update(snapshotVal.copy(githubAssetId = null))
                                        }
                                    } else {
                                        ToastUtils.showShort(app.getString(R.string.replace_fail))
                                        return@withContext
                                    }
                                }
                                ToastUtils.showShort(app.getString(R.string.replace_success))
                                selectedSnapshot = null
                            })
                            .then(modifier)
                    )
                    Divider()
                    Text(
                        text = stringResource(R.string.delete), modifier = Modifier
                            .clickable(onClick = scope.launchAsFn {
                                DbSet.snapshotDao.delete(snapshotVal)
                                withContext(Dispatchers.IO) {
                                    SnapshotExt.removeAssets(snapshotVal.id)
                                }
                                selectedSnapshot = null
                            })
                            .then(modifier), color = colorScheme.error
                    )
                }
            }
        }
    }

    when (val uploadStatusVal = uploadStatus) {
        is LoadStatus.Failure -> {
            AlertDialog(
                title = { Text(text = stringResource(R.string.load_fail)) },
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
                        vm.uploadJob?.cancel(CancellationException(app.getString(R.string.terminate_upload)))
                        vm.uploadJob = null
                    }) {
                        Text(text = stringResource(R.string.terminate_upload))
                    }
                },
            )
        }

        is LoadStatus.Success -> {
            AlertDialog(title = { Text(text = stringResource(R.string.upload_success)) }, text = {
                Text(text = IMPORT_BASE_URL + uploadStatusVal.result.id)
            }, onDismissRequest = {}, dismissButton = {
                TextButton(onClick = {
                    vm.uploadStatusFlow.value = null
                }) {
                    Text(text = stringResource(R.string.off))
                }
            }, confirmButton = {
                TextButton(onClick = {
                    ClipboardUtils.copyText(IMPORT_BASE_URL + uploadStatusVal.result.id)
                    ToastUtils.showShort(app.getString(R.string.copy_success))
                    vm.uploadStatusFlow.value = null
                }) {
                    Text(text = stringResource(R.string.copy))
                }
            })
        }

        else -> {}
    }

    if (showDeleteDlg) {
        AlertDialog(onDismissRequest = { showDeleteDlg = false },
            title = { Text(text = stringResource(R.string.if_delete_all_snapshot_history)) },
            confirmButton = {
                TextButton(
                    onClick = scope.launchAsFn(Dispatchers.IO) {
                        showDeleteDlg = false
                        snapshots.forEach { s ->
                            SnapshotExt.removeAssets(s.id)
                        }
                        DbSet.snapshotDao.deleteAll()
                    },
                ) {
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


