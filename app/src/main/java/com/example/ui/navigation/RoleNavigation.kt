package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.theme.SaffronPrimary

/**
 * Role-aware bottom navigation.
 *
 * Before this, nothing persistent existed: every role reached its sections by
 * drilling into cards and backing out, and the citizen's legal features sat in
 * a card partway down a long scheme list. Each role now gets a small, fixed set
 * of destinations that matches the job they actually do, so the three roles
 * stop sharing one shape.
 *
 * Which tabs appear is presentation only. The server re-checks role and
 * verification status on every request, so a tampered client would just see a
 * tab whose contents return 403.
 */
data class NavTab(
    val route: String,
    /** Resolved where the bar renders; these lists are built outside composition. */
    @androidx.annotation.StringRes val labelRes: Int,
    val icon: ImageVector,
    /** Routes that should keep this tab highlighted while drilled in. */
    val childRoutes: Set<String> = emptySet(),
    val badgeCount: Int = 0
)

/** The citizen's four jobs: schemes, their questions, lawyers, their profile. */
fun citizenTabs(unread: Int): List<NavTab> = listOf(
    NavTab(
        route = UrimaiDestinations.DASHBOARD,
        labelRes = R.string.nav_schemes,
        icon = Icons.Filled.Home,
        childRoutes = setOf(
            UrimaiDestinations.SCHEME_DETAIL,
            UrimaiDestinations.SAVED_SCHEMES
        )
    ),
    NavTab(
        route = UrimaiDestinations.MY_QUESTIONS,
        labelRes = R.string.nav_legal_help,
        icon = Icons.Filled.QuestionAnswer,
        childRoutes = setOf(
            UrimaiDestinations.ASK_QUESTION,
            UrimaiDestinations.QUESTION_DETAIL
        )
    ),
    NavTab(
        route = UrimaiDestinations.FIND_LAWYER,
        labelRes = R.string.nav_lawyers,
        icon = Icons.Filled.Search,
        childRoutes = setOf(
            UrimaiDestinations.LAWYER_PROFILE,
            UrimaiDestinations.MY_CONTACT_REQUESTS
        )
    ),
    NavTab(
        route = UrimaiDestinations.PROFILE_VIEW_EDIT,
        labelRes = R.string.nav_profile,
        icon = Icons.Filled.Person,
        childRoutes = setOf(
            UrimaiDestinations.EXTRACT_REVIEW,
            UrimaiDestinations.NOTIFICATIONS
        ),
        badgeCount = unread
    )
)

/**
 * The lawyer's four jobs.
 *
 * An unverified lawyer keeps the same tabs: hiding them would leave someone
 * wondering where their application went. The screens behind them explain what
 * is blocking, which is more useful than an empty navigation bar.
 */
fun lawyerTabs(pendingQuestions: Int, pendingContacts: Int, unread: Int): List<NavTab> = listOf(
    NavTab(
        route = UrimaiDestinations.LAWYER_HOME,
        labelRes = R.string.nav_home,
        icon = Icons.Filled.Home,
        badgeCount = unread
    ),
    NavTab(
        route = UrimaiDestinations.LAWYER_FEED,
        labelRes = R.string.nav_questions,
        icon = Icons.Filled.QuestionAnswer,
        childRoutes = setOf(UrimaiDestinations.ANSWER_QUESTION),
        badgeCount = pendingQuestions
    ),
    NavTab(
        route = UrimaiDestinations.LAWYER_MY_ANSWERS,
        labelRes = R.string.nav_my_answers,
        icon = Icons.Filled.Description
    ),
    NavTab(
        route = UrimaiDestinations.LAWYER_INBOX,
        labelRes = R.string.nav_requests,
        icon = Icons.Filled.Email,
        badgeCount = pendingContacts
    )
)

/** The admin's four jobs, plus notifications and logout on the home screen. */
fun adminTabs(pendingLawyers: Int, openReports: Int, unread: Int): List<NavTab> = listOf(
    NavTab(
        route = UrimaiDestinations.ADMIN_DASHBOARD,
        labelRes = R.string.nav_overview,
        icon = Icons.Filled.AdminPanelSettings,
        childRoutes = setOf(UrimaiDestinations.NOTIFICATIONS),
        badgeCount = unread
    ),
    NavTab(
        route = UrimaiDestinations.ADMIN_LAWYER_QUEUE,
        labelRes = R.string.nav_lawyers,
        icon = Icons.Filled.Gavel,
        childRoutes = setOf(
            UrimaiDestinations.ADMIN_LAWYER_APPLICATION,
            UrimaiDestinations.ADMIN_DOCUMENT_VIEWER
        ),
        badgeCount = pendingLawyers
    ),
    NavTab(
        route = UrimaiDestinations.ADMIN_REPORTS,
        labelRes = R.string.nav_reports,
        icon = Icons.Filled.Flag,
        badgeCount = openReports
    ),
    NavTab(
        route = UrimaiDestinations.ADMIN_USERS,
        labelRes = R.string.nav_users,
        icon = Icons.Filled.Group,
        childRoutes = setOf(UrimaiDestinations.ADMIN_AUDIT_LOG)
    )
)

/**
 * The bar itself.
 *
 * Hidden on screens that are not part of a tab (login, the analysis animation,
 * full-screen detail views), so a modal-feeling screen is not undercut by
 * navigation that would discard what the user is doing.
 */
@Composable
fun UrimaiBottomBar(
    tabs: List<NavTab>,
    navController: NavHostController
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route ?: return

    // Only show the bar when the current screen belongs to a tab.
    val isTabScreen = tabs.any { tab ->
        currentRoute == tab.route || currentRoute in tab.childRoutes
    }
    if (!isTabScreen) return

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            val selected = currentDestination.hierarchy.any { it.route == tab.route } ||
                currentRoute in tab.childRoutes

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            // Single top with state restore, so switching tabs
                            // does not pile duplicates on the back stack and
                            // returning to a tab keeps where you were.
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    BadgedBox(
                        badge = {
                            if (tab.badgeCount > 0) {
                                Badge(containerColor = SaffronPrimary) {
                                    Text(
                                        if (tab.badgeCount > 99) "99+" else "${tab.badgeCount}"
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            tab.icon,
                            // The label below carries the name; describing the
                            // icon too would make a screen reader say it twice.
                            contentDescription = null
                        )
                    }
                },
                label = { Text(stringResource(tab.labelRes)) }
            )
        }
    }
}
