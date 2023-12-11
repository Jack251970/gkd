package li.songe.gkd.util

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blankj.utilcode.util.AppUtils
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import li.songe.gkd.BuildConfig
import li.songe.gkd.R
import li.songe.gkd.appScope
import java.io.File

@Serializable
data class NewVersion(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val versionLogs: List<VersionLog> = emptyList(),
    val fileSize: Long? = null,
)

@Serializable
data class VersionLog(
    val name: String,
    val code: Int,
    val desc: String,
)

@Serializable
data class GithubNewVersion(
    val url: String,
    @SerialName("assets_url")
    val assetsUrl: String,
    @SerialName("upload_url")
    val uploadUrl: String,
    @SerialName("html_url")
    val htmlUrl: String,
    val id: Int,
    val author: Author,
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("target_commitish")
    val targetCommitish: String,
    val name: String,
    val draft: Boolean,
    val prerelease: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("published_at")
    val publishedAt: String,
    val assets: List<Asset> = emptyList(),
    @SerialName("tarball_url")
    val tarballUrl: String,
    @SerialName("zipball_url")
    val zipballUrl: String,
    val body: String,
    val reactions: Reactions = Reactions(
        url = "",
        totalCount = 0,
        plusOne = 0,
        minusOne = 0,
        laugh = 0,
        hooray = 0,
        confused = 0,
        heart = 0,
        rocket = 0,
        eyes = 0
    )
)

@Serializable
data class Author(
    val login: String,
    val id: Int,
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("gravatar_id")
    val gravatarId: String,
    val url: String,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("followers_url")
    val followersUrl: String,
    @SerialName("following_url")
    val followingUrl: String,
    @SerialName("gists_url")
    val gistsUrl: String,
    @SerialName("starred_url")
    val starredUrl: String,
    @SerialName("subscriptions_url")
    val subscriptionsUrl: String,
    @SerialName("organizations_url")
    val organizationsUrl: String,
    @SerialName("repos_url")
    val reposUrl: String,
    @SerialName("events_url")
    val eventsUrl: String,
    @SerialName("received_events_url")
    val receivedEventsUrl: String,
    val type: String,
    @SerialName("site_admin")
    val siteAdmin: Boolean
)

@Serializable
data class Asset(
    val url: String,
    val id: Int,
    @SerialName("node_id")
    val nodeId: String,
    val name: String,
    val label: String,
    val uploader: Uploader,
    @SerialName("content_type")
    val contentType: String,
    val state: String,
    val size: Int,
    @SerialName("download_count")
    val downloadCount: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String
)

@Serializable
data class Uploader(
    val login: String,
    val id: Int,
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("gravatar_id")
    val gravatarId: String,
    val url: String,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("followers_url")
    val followersUrl: String,
    @SerialName("following_url")
    val followingUrl: String,
    @SerialName("gists_url")
    val gistsUrl: String,
    @SerialName("starred_url")
    val starredUrl: String,
    @SerialName("subscriptions_url")
    val subscriptionsUrl: String,
    @SerialName("organizations_url")
    val organizationsUrl: String,
    @SerialName("repos_url")
    val reposUrl: String,
    @SerialName("events_url")
    val eventsUrl: String,
    @SerialName("received_events_url")
    val receivedEventsUrl: String,
    val type: String,
    @SerialName("site_admin")
    val siteAdmin: Boolean
)

@Serializable
data class Reactions(
    val url: String,
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("+1")
    val plusOne: Int,
    @SerialName("-1")
    val minusOne: Int,
    val laugh: Int,
    val hooray: Int,
    val confused: Int,
    val heart: Int,
    val rocket: Int,
    val eyes: Int
)

data class NewVersionAsset(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val versionLogs: List<VersionLog> = emptyList(),
    val fileSize: Long? = null,
    val fromSource: Boolean,
)

val checkUpdatingFlow by lazy { MutableStateFlow(false) }
val newVersionFlow by lazy { MutableStateFlow<NewVersionAsset?>(null) }
val downloadStatusFlow by lazy { MutableStateFlow<LoadStatus<String>?>(null) }
suspend fun checkUpdate(): NewVersionAsset? {
    if (checkUpdatingFlow.value) return null
    checkUpdatingFlow.value = true
    try {
        val githubNewVersion = client.get(FORKED_UPDATE_URL).body<GithubNewVersion>()
        val versionName = githubNewVersion.tagName.substring(1)
        if (compareVersions(versionName, BuildConfig.VERSION_NAME) > 0) {
            if (githubNewVersion.assets.isNotEmpty()) {
                val akpAsset = githubNewVersion.assets.find { it.name.startsWith("gkd") }
                if (akpAsset != null) {
                    val newVersion = client.get(UPDATE_URL).body<NewVersion>()
                    val newVersionAsset = NewVersionAsset(
                        versionCode = BuildConfig.VERSION_CODE,
                        versionName = versionName,
                        downloadUrl = akpAsset.browserDownloadUrl,
                        versionLogs = newVersion.versionLogs
                            .takeWhile { v -> compareVersions(v.name, BuildConfig.VERSION_NAME) > 0 },
                        fileSize = akpAsset.size.toLong(),
                        fromSource = false,
                    )
                    newVersionFlow.value = newVersionAsset
                    return newVersionAsset
                }
                else
                {
                    Log.d("Upgrade", "Found new github version but no apk")
                }
            }
            else
            {
                Log.d("Upgrade", "Found new github version but no assets")
            }
        } else {
            Log.d("Upgrade", "Found no new github version")
        }
        val newVersion = client.get(UPDATE_URL).body<NewVersion>()
        if (newVersion.versionCode > BuildConfig.VERSION_CODE) {
            val newVersionAsset = NewVersionAsset(
                versionCode = newVersion.versionCode,
                versionName = newVersion.versionName,
                downloadUrl = newVersion.downloadUrl,
                versionLogs = newVersion.versionLogs
                    .takeWhile { v -> v.code > BuildConfig.VERSION_CODE },
                fileSize = newVersion.fileSize,
                fromSource = true,
            )
            newVersionFlow.value = newVersionAsset
            return newVersionAsset
        } else {
            Log.d("Upgrade", "no new version")
        }
    } finally {
        checkUpdatingFlow.value = false
    }
    return null
}

fun compareVersions(version1: String, version2: String): Int {
    val v1Parts = version1.replace("[^\\d.]".toRegex(), "")
        .split(".").map { it.toInt() }
    val v2Parts = version2.replace("[^\\d.]".toRegex(), "")
        .split(".").map { it.toInt() }

    val maxLength = maxOf(v1Parts.size, v2Parts.size)

    for (i in 0 until maxLength) {
        val part1 = if (i < v1Parts.size) v1Parts[i] else 0
        val part2 = if (i < v2Parts.size) v2Parts[i] else 0

        if (part1 < part2) {
            return -1 // Version 1 is older
        } else if (part1 > part2) {
            return 1 // Version 1 is newer
        }
    }

    return 0 // Both versions are equal
}

fun startDownload(newVersion: NewVersionAsset) {
    if (downloadStatusFlow.value is LoadStatus.Loading) return
    downloadStatusFlow.value = LoadStatus.Loading(0f)
    val newApkFile = File(newVersionApkDir, "v${newVersion.versionName}.apk")
    if (newApkFile.exists()) {
        newApkFile.delete()
    }
    var job: Job? = null
    job = appScope.launch(Dispatchers.IO) {
        try {
            val channel = client.get(newVersion.downloadUrl) {
                onDownload { bytesSentTotal, contentLength ->
                    // contentLength 在某些机型上概率错误
                    val downloadStatus = downloadStatusFlow.value
                    if (downloadStatus is LoadStatus.Loading) {
                        downloadStatusFlow.value = LoadStatus.Loading(
                            bytesSentTotal.toFloat() / (newVersion.fileSize ?: contentLength)
                        )
                    } else if (downloadStatus is LoadStatus.Failure) {
                        // 提前终止下载
                        job?.cancel()
                        }
                    }
                }.bodyAsChannel()
            if (downloadStatusFlow.value is LoadStatus.Loading) {
                channel.copyAndClose(newApkFile.writeChannel())
                downloadStatusFlow.value = LoadStatus.Success(newApkFile.absolutePath)
            }
        } catch (e: Exception) {
            if (downloadStatusFlow.value is LoadStatus.Loading) {
                downloadStatusFlow.value = LoadStatus.Failure(e)
            }
        }
    }
}

@Composable
fun UpgradeDialog() {
    val newVersion by newVersionFlow.collectAsState()
    var fromSourceDialog by remember { mutableStateOf(false) }
    newVersion?.let { newVersionVal ->

        AlertDialog(title = {
            Text(text = stringResource(R.string.detect_new_version))
        }, text = {
            Text(text = ((if (newVersionVal.fromSource) "${stringResource(R.string.update_from_source_project)}\n\n" else "") +
                    "v${BuildConfig.VERSION_NAME} -> v${newVersionVal.versionName}\n\n${ 
                        if (newVersionVal.versionLogs.size > 1) { 
                            newVersionVal.versionLogs.joinToString("\n\n") { v -> "v${v.name}\n${v.desc}" } 
                        } else if (newVersionVal.versionLogs.isNotEmpty()) {
                            newVersionVal.versionLogs.first().desc 
                        } else { 
                            ""
                        }
            }").trimEnd(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()))

        }, onDismissRequest = { }, confirmButton = {
            TextButton(onClick = {
                if (newVersionVal.fromSource){
                    fromSourceDialog = true
                } else {
                    newVersionFlow.value = null
                    startDownload(newVersionVal)
                }
            }) {
                Text(text = stringResource(R.string.download_new_version))
            }
        }, dismissButton = {
            TextButton(onClick = { newVersionFlow.value = null }) {
                Text(text = stringResource(R.string.cancel))
            }
        })

        if (fromSourceDialog){
            AlertDialog(
                title = { Text(text = stringResource(R.string.waning)) },
                text = { Text(text = stringResource(R.string.update_from_source_project)) },
                onDismissRequest = { }, confirmButton = {
                    TextButton(onClick = {
                        newVersionFlow.value = null
                        startDownload(newVersionVal)
                    }) {
                        Text(text = stringResource(R.string.confirm))
                    }
                }, dismissButton = {
                    TextButton(onClick = {
                        fromSourceDialog = false
                    }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }
    }

    val downloadStatus by downloadStatusFlow.collectAsState()
    downloadStatus?.let { downloadStatusVal ->
        fromSourceDialog = false
        when (downloadStatusVal) {
            is LoadStatus.Loading -> {
                AlertDialog(
                    title = { Text(text = stringResource(R.string.downloading_new_version)) },
                    text = {
                        LinearProgressIndicator(progress = downloadStatusVal.progress)
                    },
                    onDismissRequest = {},
                    confirmButton = {
                        TextButton(onClick = {
                            downloadStatusFlow.value = LoadStatus.Failure(
                                Exception("终止下载")
                            )
                        }) {
                            Text(text = stringResource(R.string.stop_download))
                        }
                    },
                )
            }

            is LoadStatus.Failure -> {
                AlertDialog(
                    title = { Text(text = stringResource(R.string.download_fail)) },
                    text = {
                        Text(text = downloadStatusVal.exception.let {
                            it.message ?: it.toString()
                        })
                    },
                    onDismissRequest = { downloadStatusFlow.value = null },
                    confirmButton = {
                        TextButton(onClick = {
                            downloadStatusFlow.value = null
                        }) {
                            Text(text = stringResource(R.string.off))
                        }
                    },
                )
            }

            is LoadStatus.Success -> {
                AlertDialog(title = { Text(text = stringResource(R.string.download_success)) },
                    onDismissRequest = {},
                    dismissButton = {
                        TextButton(onClick = {
                            downloadStatusFlow.value = null
                        }) {
                            Text(text = stringResource(R.string.off))
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            AppUtils.installApp(downloadStatusVal.result)
                        }) {
                            Text(text = stringResource(R.string.install))
                        }
                    })
            }
        }
    }
}












