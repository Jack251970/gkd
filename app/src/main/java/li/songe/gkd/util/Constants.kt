package li.songe.gkd.util

const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"

const val FILE_UPLOAD_URL = "https://u.gkd.li/"
const val IMPORT_BASE_URL = "https://i.gkd.li/import/"

const val DEFAULT_SUBS_UPDATE_URL =
    "https://registry.npmmirror.com/@gkd-kit/subscription/latest/files"
// const val UPDATE_URL = "https://registry.npmmirror.com/@gkd-kit/app/latest/files/index.json"
const val FORKED_UPDATE_URL = "https://api.github.com/repos/Jack251970/gkd/releases/latest"

const val REPOSITORY_URL = "https://github.com/gkd-kit/gkd"
const val FORKED_REPOSITORY_URL = "https://github.com/Jack251970/gkd"

val safeRemoteBaseUrls = arrayOf(
    "https://registry.npmmirror.com/@gkd-kit/",
    "https://cdn.jsdelivr.net/npm/@gkd-kit/",
    "https://fastly.jsdelivr.net/npm/@gkd-kit/",
    "https://unpkg.com/@gkd-kit/",
    "https://github.com/gkd-kit/",
    "https://raw.githubusercontent.com/gkd-kit/"
)
