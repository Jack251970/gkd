package li.songe.gkd.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import li.songe.gkd.data.SubsItem
import li.songe.gkd.data.SubscriptionRaw
import li.songe.gkd.util.formatTimeAgo
import li.songe.gkd.util.safeRemoteBaseUrls


@Composable
fun SubsItemCard(
    subsItem: SubsItem,
    subscriptionRaw: SubscriptionRaw?,
    index: Int,
    onMenuClick: (() -> Unit)? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    Box {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (subsItem.id < 0) {
                Text(text = stringResource(R.string.local_source), fontSize = 12.sp)
            } else if (subsItem.updateUrl != null && safeRemoteBaseUrls.any { s ->
                    subsItem.updateUrl.startsWith(
                        s
                    )
                }) {
                Text(text = stringResource(R.string.trusted_source), fontSize = 12.sp)
            } else {
                Text(text = stringResource(R.string.unknown_source), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (subscriptionRaw != null) {
                    Row {
                        Text(
                            text = index.toString() + ". " + (subscriptionRaw.name),
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
                        if (subsItem.id >= 0) {
                            Text(
                                text = "v" + (subscriptionRaw.version.toString()),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        val apps = subscriptionRaw.apps
                        val groupsSize = apps.sumOf { it.groups.size }
                        if (groupsSize > 0) {
                            Text(
                                text = stringResource(R.string.app_rule_group_count, apps.size, groupsSize), fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.no_rules), fontSize = 14.sp
                            )
                        }
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
            Spacer(modifier = Modifier.width(5.dp))

            IconButton(onClick = { onMenuClick?.invoke() }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "more",
                )
            }

            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = subsItem.enable,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
