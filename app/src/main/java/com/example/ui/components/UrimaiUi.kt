package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.ui.theme.CivicNavy100
import com.example.ui.theme.CivicNavy700
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.SaffronPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared building blocks.
 *
 * These were previously duplicated across screens written in different passes:
 * four near-identical status pills, four chip variants, two detail rows, three
 * copies of the same four-field tuple. Each copy had slightly different padding
 * and corner radius, so the same concept looked different depending on which
 * screen you were on. Collapsing them here also means a future dark theme has
 * one place to change rather than fifteen.
 */

/** One spacing scale, so screens stop inventing 10/12/14/16dp variants. */
object UrimaiSpacing {
    val cardPadding = 16.dp
    val screenPadding = 16.dp
    val listGap = 12.dp
    val tight = 8.dp
}

// --- Chrome -----------------------------------------------------------------

/**
 * The single top bar for every screen.
 *
 * Replaces three competing systems (a bespoke Surface header, bare M3
 * TopAppBar, and CivicHeader). The back arrow is auto-mirrored so it points the
 * correct way in right-to-left locales — the app ships a language selector, and
 * most screens previously used the non-mirrored icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrimaiTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/** Bell with an unread badge. Hidden at zero so it never shows a bare "0". */
@Composable
fun NotificationAction(
    unreadCount: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(containerColor = SaffronPrimary) {
                        // Spoken as "3 unread notifications" rather than "3".
                        Text(if (unreadCount > 99) "99+" else "$unreadCount")
                    }
                }
            }
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = if (unreadCount > 0) {
                    "Notifications, $unreadCount unread"
                } else {
                    stringResource(R.string.cd_notifications)
                }
            )
        }
    }
}

// --- Status -----------------------------------------------------------------

/** Semantic tone, so colour choices live in one place. */
enum class StatusTone { POSITIVE, CAUTION, NEGATIVE, NEUTRAL }

/**
 * Colour-coded status label.
 *
 * Replaces StatusPill, StatusLine and AnswerCountPill, which were three copies
 * of the same Card+Text with the same padding and different `when` blocks.
 */
@Composable
fun StatusChip(
    text: String,
    tone: StatusTone = StatusTone.NEUTRAL,
    modifier: Modifier = Modifier
) {
    val (container, content) = when (tone) {
        StatusTone.POSITIVE -> EmeraldContainer to EmeraldText
        StatusTone.CAUTION -> AmberContainer to AmberText
        StatusTone.NEGATIVE -> CrimsonContainer to CrimsonText
        StatusTone.NEUTRAL -> CivicNavy100 to CivicNavy700
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = modifier
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/** Map a server status string to a tone and readable label. */
@Composable
fun ServerStatusChip(status: String, modifier: Modifier = Modifier) {
    val tone = when (status.uppercase()) {
        "VERIFIED", "ACTIVE", "RESOLVED", "ACCEPTED", "ANSWERED" -> StatusTone.POSITIVE
        "PENDING", "MORE_INFO_REQUESTED", "OPEN", "SUSPENDED" -> StatusTone.CAUTION
        "REJECTED", "BLOCKED", "REMOVED", "DECLINED" -> StatusTone.NEGATIVE
        else -> StatusTone.NEUTRAL
    }
    StatusChip(
        text = status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
        tone = tone,
        modifier = modifier
    )
}

/** Plain informational tag. */
@Composable
fun InfoChip(label: String, modifier: Modifier = Modifier) {
    StatusChip(label, StatusTone.NEUTRAL, modifier)
}

// --- Selection --------------------------------------------------------------

/**
 * Wrapping selectable chips.
 *
 * One implementation for single and multiple selection; previously
 * CategoryChips and MultiSelectChips were the same body twice.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectableChips(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UrimaiSpacing.tight),
        verticalArrangement = Arrangement.spacedBy(UrimaiSpacing.tight)
    ) {
        options.forEach { option ->
            val isSelected = option in selected
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CivicNavy700 else CivicNavy100
                ),
                onClick = { onToggle(option) }
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        CivicNavy700
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = UrimaiSpacing.tight)
                )
            }
        }
    }
}

/** Single-selection convenience over [SelectableChips]. */
@Composable
fun SingleSelectChips(
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    SelectableChips(
        options = options,
        selected = setOfNotNull(selected),
        onToggle = { onSelect(if (it == selected) null else it) },
        modifier = modifier
    )
}

// --- States -----------------------------------------------------------------

/** Nothing-here state. Lives here rather than inside a screen file. */
@Composable
fun UrimaiEmptyState(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UrimaiSpacing.tight)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (!body.isNullOrBlank()) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Something-went-wrong state, with a way out.
 *
 * Previously every failure was a transient snackbar with no retry: if you
 * missed it, the screen simply looked empty and you had no way to tell a real
 * "nothing here" from a failed request.
 */
@Composable
fun UrimaiErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UrimaiSpacing.listGap)
    ) {
        Text(
            stringResource(R.string.state_something_went_wrong),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onRetry != null) {
            OutlinedButton(onClick = onRetry) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(UrimaiSpacing.tight))
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

/** Thin progress line under the top bar. One loading idiom everywhere. */
@Composable
fun UrimaiLoadingBar(isLoading: Boolean, modifier: Modifier = Modifier) {
    if (isLoading) {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth())
    }
}

/** Centred spinner for a screen with no content yet. */
@Composable
fun UrimaiLoadingBlock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

// --- Content ----------------------------------------------------------------

/** Label/value row. Replaces AdminDetailRow and DetailRow. */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // width, not size: a fixed height clipped wrapped labels.
            modifier = Modifier.width(128.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * A tappable row on a landing screen.
 *
 * Used by every role's home, so lawyer and admin workspaces look like one app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    highlight: Boolean = false,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) AmberContainer else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(UrimaiSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                icon,
                // Decorative: the title beside it carries the meaning.
                contentDescription = null,
                tint = if (highlight) AmberText else CivicNavy700
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlight) AmberText else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highlight) {
                        AmberText
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (badgeCount > 0) {
                Surface(shape = CircleShape, color = SaffronPrimary) {
                    Text(
                        if (badgeCount > 99) "99+" else "$badgeCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/** Short, local date. Shared so the format is consistent app-wide. */
fun formatDate(epochMillis: Long): String = try {
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
} catch (_: Exception) {
    ""
}

/**
 * Pull-to-refresh wrapper.
 *
 * Lists previously loaded once, in a `LaunchedEffect(Unit)` on first
 * composition. A lawyer sitting on the question feed never saw a new question
 * until they navigated away and back; the same was true of the moderation queue
 * and notifications. This gives every list the gesture a user already expects.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UrimaiRefreshable(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        content()
    }
}
