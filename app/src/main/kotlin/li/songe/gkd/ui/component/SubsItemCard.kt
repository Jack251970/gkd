package li.songe.gkd.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import li.songe.gkd.R
import li.songe.gkd.data.RawSubscription
import li.songe.gkd.data.SubsItem
import li.songe.gkd.util.formatTimeAgo
import li.songe.gkd.util.safeRemoteBaseUrls


@Composable
fun SubsItemCard(
    subsItem: SubsItem,
    rawSubscription: RawSubscription?,
    index: Int,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (rawSubscription != null) {
                Row {
                    Text(
                        text = index.toString() + ". " + (rawSubscription.name),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Row {
                    Text(
                        text = formatTimeAgo(subsItem.mtime),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    val sourceText =
                        if (subsItem.id < 0) {
                            stringResource(R.string.local_source)
                        } else if (subsItem.updateUrl != null && safeRemoteBaseUrls.any { s ->
                                subsItem.updateUrl.startsWith(
                                    s
                                )
                            }) {
                            stringResource(R.string.trusted_source)
                        } else {
                            stringResource(R.string.unknown_source)
                        }
                    Text(text = sourceText, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(5.dp))
                Row {
                    if (subsItem.id >= 0) {
                        Text(
                            text = "v" + (rawSubscription.version.toString()),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Text(
                        text = rawSubscription.numText,
                        fontSize = 14.sp
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.no_local_subscription_file_refresh_page),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = subsItem.enable,
            onCheckedChange = onCheckedChange,
        )
    }
}
