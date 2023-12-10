package li.songe.gkd.debug

import android.graphics.Bitmap
import androidx.core.graphics.set
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import li.songe.gkd.app
import li.songe.gkd.data.ComplexSnapshot
import li.songe.gkd.data.RpcError
import li.songe.gkd.data.createComplexSnapshot
import li.songe.gkd.data.toSnapshot
import li.songe.gkd.db.DbSet
import li.songe.gkd.service.GkdAbService
import li.songe.gkd.util.keepNullJson
import li.songe.gkd.util.snapshotZipDir
import li.songe.gkd.util.storeFlow
import java.io.File
import kotlin.math.min

object SnapshotExt {
    val snapshotDir by lazy {
        app.getExternalFilesDir("snapshot")!!.apply { if (!exists()) mkdir() }
    }

    private fun getSnapshotParentPath(snapshotId: Long) =
        "${snapshotDir.absolutePath}/${snapshotId}"

    fun getSnapshotPath(snapshotId: Long) =
        "${getSnapshotParentPath(snapshotId)}/${snapshotId}.json"

    fun getScreenshotPath(snapshotId: Long) =
        "${getSnapshotParentPath(snapshotId)}/${snapshotId}.png"

    suspend fun getSnapshotZipFile(snapshotId: Long): File {
        val file = File(snapshotZipDir, "${snapshotId}.zip")
        if (file.exists()) {
            return file
        }
        withContext(Dispatchers.IO) {
            ZipUtils.zipFiles(
                listOf(
                    getSnapshotPath(snapshotId), getScreenshotPath(snapshotId)
                ), file.absolutePath
            )
        }
        return file
    }

    fun removeAssets(id: Long) {
        File(getSnapshotParentPath(id)).apply {
            if (exists()) {
                deleteRecursively()
            }
        }
    }

    private val captureLoading = MutableStateFlow(false)

    suspend fun captureSnapshot(skipScreenshot: Boolean = false): ComplexSnapshot {
        if (!GkdAbService.isRunning()) {
            throw RpcError("无障碍不可用")
        }
        if (captureLoading.value) {
            throw RpcError("正在保存快照,不可重复操作")
        }
        captureLoading.value = true
        ToastUtils.showShort("正在保存快照...")

        try {
            val snapshotDef = coroutineScope { async(Dispatchers.IO) { createComplexSnapshot() } }
            val bitmapDef = coroutineScope {// TODO 也许在分屏模式下可能需要处理
                async(Dispatchers.IO) {
                    if (skipScreenshot) {
                        LogUtils.d("跳过截屏，即将使用空白图片")
                        Bitmap.createBitmap(
                            ScreenUtils.getScreenWidth(),
                            ScreenUtils.getScreenHeight(),
                            Bitmap.Config.ARGB_8888
                        )
                    } else {
                        GkdAbService.currentScreenshot() ?: withTimeoutOrNull(3_000) {
                            if (!ScreenshotService.isRunning()) {
                                return@withTimeoutOrNull null
                            }
                            ScreenshotService.screenshot()
                        } ?: Bitmap.createBitmap(
                            ScreenUtils.getScreenWidth(),
                            ScreenUtils.getScreenHeight(),
                            Bitmap.Config.ARGB_8888
                        ).apply {
                            LogUtils.d("截屏不可用，即将使用空白图片")
                        }
                    }
                }
            }

            var bitmap = bitmapDef.await()
            if (storeFlow.value.hideSnapshotStatusBar) {
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                for (x in 0 until bitmap.width) {
                    for (y in 0 until min(BarUtils.getStatusBarHeight(), bitmap.height)) {
                        bitmap[x, y] = 0
                    }
                }
            }
            val snapshot = snapshotDef.await()

            withContext(Dispatchers.IO) {
                File(getSnapshotParentPath(snapshot.id)).apply { if (!exists()) mkdirs() }
                File(getScreenshotPath(snapshot.id)).outputStream().use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                val text = keepNullJson.encodeToString(snapshot)
                File(getSnapshotPath(snapshot.id)).writeText(text)
                DbSet.snapshotDao.insert(snapshot.toSnapshot())
            }
            ToastUtils.showShort("快照捕获成功")
            return snapshot
        } finally {
            captureLoading.value = false
        }
    }
}