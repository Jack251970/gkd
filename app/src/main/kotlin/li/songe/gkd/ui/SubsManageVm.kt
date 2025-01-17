package li.songe.gkd.ui

import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import li.songe.gkd.R
import li.songe.gkd.app
import li.songe.gkd.data.RawSubscription
import li.songe.gkd.data.SubsItem
import li.songe.gkd.data.SubsVersion
import li.songe.gkd.db.DbSet
import li.songe.gkd.util.client
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.subsIdToRawFlow
import li.songe.gkd.util.subsItemsFlow
import li.songe.gkd.util.updateSubscription
import javax.inject.Inject


@HiltViewModel
class SubsManageVm @Inject constructor() : ViewModel() {

    fun addSubsFromUrl(url: String) = viewModelScope.launchTry(Dispatchers.IO) {

        if (refreshingFlow.value) return@launchTry
        if (!URLUtil.isNetworkUrl(url)) {
            ToastUtils.showShort(app.getString(R.string.illegal_link))
            return@launchTry
        }
        val subItems = subsItemsFlow.value
        if (subItems.any { it.updateUrl == url }) {
            ToastUtils.showShort(app.getString(R.string.subscription_link_exist))
            return@launchTry
        }
        refreshingFlow.value = true
        try {
            val text = try {
                client.get(url).bodyAsText()
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtils.showShort(app.getString(R.string.download_subscription_file_fail))
                return@launchTry
            }
            val newSubsRaw = try {
                RawSubscription.parse(text)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtils.showShort(app.getString(R.string.parse_subscription_file_fail))
                return@launchTry
            }
            if (subItems.any { it.id == newSubsRaw.id }) {
                ToastUtils.showShort(app.getString(R.string.subscription_exist))
                return@launchTry
            }
            if (newSubsRaw.id < 0) {
                ToastUtils.showShort(app.getString(R.string.illegal_subscription_id, newSubsRaw.id))
                return@launchTry
            }
            val newItem = SubsItem(
                id = newSubsRaw.id,
                updateUrl = newSubsRaw.updateUrl ?: url,
                order = if (subItems.isEmpty()) 1 else (subItems.maxBy { it.order }.order + 1)
            )
            updateSubscription(newSubsRaw)
            DbSet.subsItemDao.insert(newItem)
            ToastUtils.showShort(app.getString(R.string.add_subscription_success))
        } finally {
            refreshingFlow.value = false
        }

    }

    val refreshingFlow = MutableStateFlow(false)
    fun refreshSubs() = viewModelScope.launch(Dispatchers.IO) {
        if (refreshingFlow.value) return@launch
        refreshingFlow.value = true
        var errorNum = 0
        val oldSubItems = subsItemsFlow.value
        val newSubsItems = oldSubItems.mapNotNull { oldItem ->
            if (oldItem.updateUrl == null) return@mapNotNull null
            val oldSubsRaw = subsIdToRawFlow.value[oldItem.id]
            try {
                if (oldSubsRaw?.checkUpdateUrl != null) {
                    try {
                        val subsVersion =
                            client.get(oldSubsRaw.checkUpdateUrl).body<SubsVersion>()
                        LogUtils.d("快速检测更新成功", subsVersion)
                        if (subsVersion.id == oldSubsRaw.id && subsVersion.version <= oldSubsRaw.version) {
                            return@mapNotNull null
                        }
                    } catch (e: Exception) {
                        LogUtils.d("快速检测更新失败", oldItem, e)
                    }
                }
                val newSubsRaw = RawSubscription.parse(
                    client.get(oldItem.updateUrl).bodyAsText()
                )
                if (oldSubsRaw != null && newSubsRaw.version <= oldSubsRaw.version) {
                    return@mapNotNull null
                }
                val newItem = oldItem.copy(
                    updateUrl = newSubsRaw.updateUrl ?: oldItem.updateUrl,
                    mtime = System.currentTimeMillis(),
                )
                updateSubscription(newSubsRaw)
                newItem
            } catch (e: Exception) {
                e.printStackTrace()
                errorNum++
                null
            }
        }
        if (newSubsItems.isEmpty()) {
            if (errorNum == oldSubItems.size) {
                ToastUtils.showShort(app.getString(R.string.update_fail))
            } else {
                ToastUtils.showShort(app.getString(R.string.no_update))
            }
        } else {
            DbSet.subsItemDao.update(*newSubsItems.toTypedArray())
            ToastUtils.showShort(app.getString(R.string.update_subscription_count, newSubsItems.size))
        }
        delay(500)
        refreshingFlow.value = false
    }

}