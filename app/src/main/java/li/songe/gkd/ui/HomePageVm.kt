package li.songe.gkd.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.LogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import li.songe.gkd.appScope
import li.songe.gkd.data.GithubPoliciesAsset
import li.songe.gkd.data.RpcError
import li.songe.gkd.data.SubsItem
import li.songe.gkd.data.SubscriptionRaw
import li.songe.gkd.db.DbSet
import li.songe.gkd.debug.SnapshotExt
import li.songe.gkd.util.FILE_UPLOAD_URL
import li.songe.gkd.util.LoadStatus
import li.songe.gkd.util.checkUpdate
import li.songe.gkd.util.client
import li.songe.gkd.util.dbFolder
import li.songe.gkd.util.initFolder
import li.songe.gkd.util.json
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.storeFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomePageVm @Inject constructor() : ViewModel() {
    val tabFlow = MutableStateFlow(controlNav)

    init {
        appScope.launchTry(Dispatchers.IO) {
            val localSubsItem = SubsItem(
                id = -2, order = -2, mtime = System.currentTimeMillis()
            )
            if (!DbSet.subsItemDao.query().first().any { s -> s.id == localSubsItem.id }) {
                DbSet.subsItemDao.insert(localSubsItem)
            }
            if (!localSubsItem.subsFile.exists()) {
                localSubsItem.subsFile.writeText(
                    json.encodeToString(
                        SubscriptionRaw(
                            id = localSubsItem.id,
                            name = "本地订阅",
                            version = 0,
                            author = "gkd",
                        )
                    )
                )
            }
        }
        appScope.launchTry(Dispatchers.IO) {
            // 迁移快照记录
            val oldDbFile = File(dbFolder, "snapshot.db")
            if (oldDbFile.exists()) {
                SnapshotExt.snapshotDir.walk().maxDepth(1).filter { f -> f.isDirectory }
                    .mapNotNull { f -> f.name.toLongOrNull() }.forEach { snapshotId ->
                        DbSet.snapshotDao.insertOrIgnore(
                            json.decodeFromString(
                                File(SnapshotExt.getSnapshotPath(snapshotId)).readText()
                            )
                        )
                    }
                oldDbFile.delete()
                LogUtils.d("执行快照迁移")
            }
        }

        /*if (storeFlow.value.autoCheckAppUpdate) {
            appScope.launch {
                try {
                    checkUpdate()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }*/

        viewModelScope.launchTry(Dispatchers.IO) {
            // 在某些机型由于未知原因创建失败
            // 在此保证每次重新打开APP都能重新检测创建
            initFolder()
        }
    }

    val uploadStatusFlow = MutableStateFlow<LoadStatus<GithubPoliciesAsset>?>(null)
    var uploadJob: Job? = null

    fun uploadZip(zipFile: File) {
        uploadJob = viewModelScope.launchTry(Dispatchers.IO) {
            uploadStatusFlow.value = LoadStatus.Loading()
            try {
                val response =
                    client.submitFormWithBinaryData(url = FILE_UPLOAD_URL, formData = formData {
                        append("\"file\"", zipFile.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "application/x-zip-compressed")
                            append(HttpHeaders.ContentDisposition, "filename=\"file.zip\"")
                        })
                    }) {
                        onUpload { bytesSentTotal, contentLength ->
                            if (uploadStatusFlow.value is LoadStatus.Loading) {
                                uploadStatusFlow.value =
                                    LoadStatus.Loading(bytesSentTotal / contentLength.toFloat())
                            }
                        }
                    }
                if (response.headers["X_RPC_OK"] == "true") {
                    val policiesAsset = response.body<GithubPoliciesAsset>()
                    uploadStatusFlow.value = LoadStatus.Success(policiesAsset)
                } else if (response.headers["X_RPC_OK"] == "false") {
                    uploadStatusFlow.value = LoadStatus.Failure(response.body<RpcError>())
                } else {
                    uploadStatusFlow.value = LoadStatus.Failure(Exception(response.bodyAsText()))
                }
            } catch (e: Exception) {
                uploadStatusFlow.value = LoadStatus.Failure(e)
            }
        }
    }
}