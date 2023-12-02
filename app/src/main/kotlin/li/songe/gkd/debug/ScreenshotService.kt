package li.songe.gkd.debug

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ServiceUtils
import li.songe.gkd.app
import li.songe.gkd.composition.CompositionExt.useLifeCycleLog
import li.songe.gkd.composition.CompositionService
import li.songe.gkd.notif.createNotif
import li.songe.gkd.notif.screenshotChannel
import li.songe.gkd.notif.screenshotNotif
import li.songe.gkd.util.ScreenshotUtil

class ScreenshotService : CompositionService({
    useLifeCycleLog()
    createNotif(this, screenshotChannel.id, screenshotNotif)

    onStartCommand { intent, _, _ ->
        if (intent == null) return@onStartCommand
        screenshotUtil?.destroy()
        screenshotUtil = ScreenshotUtil(this, intent)
        LogUtils.d("screenshot restart")
    }
    onDestroy {
        screenshotUtil?.destroy()
        screenshotUtil = null
    }
}) {
    companion object {
        suspend fun screenshot() = screenshotUtil?.execute()

        @SuppressLint("StaticFieldLeak")
        private var screenshotUtil: ScreenshotUtil? = null

        fun start(context: Context = app, intent: Intent) {
            intent.component = ComponentName(context, ScreenshotService::class.java)
            context.startForegroundService(intent)
        }

        fun isRunning() = ServiceUtils.isServiceRunning(ScreenshotService::class.java)
        fun stop(context: Context = app) {
            if (isRunning()) {
                context.stopService(Intent(context, ScreenshotService::class.java))
            }
        }
    }
}