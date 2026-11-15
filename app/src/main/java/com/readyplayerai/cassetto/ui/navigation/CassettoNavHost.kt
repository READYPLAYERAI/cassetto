package com.readyplayerai.cassetto.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.readyplayerai.cassetto.data.auth.Session
import com.readyplayerai.cassetto.ui.account.AccountRoute
import com.readyplayerai.cassetto.ui.detail.BookmarkDetailRoute
import com.readyplayerai.cassetto.ui.home.HomeRoute
import com.readyplayerai.cassetto.ui.lists.AllListsRoute
import com.readyplayerai.cassetto.ui.login.LoginRoute
import com.readyplayerai.cassetto.ui.session.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CassettoNavHost() {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val session by sessionViewModel.session.collectAsStateWithLifecycle()

    // The start destination is decided once, from the first resolved session, so
    // the graph is not rebuilt when the state changes afterwards.
    var startDestination by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(session) {
        if (startDestination == null) {
            startDestination = when (session) {
                Session.Loading -> null
                Session.LoggedOut -> Routes.LOGIN
                is Session.LoggedIn -> Routes.HOME
            }
        }
    }

    val start = startDestination
    if (start == null) {
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator(modifier = Modifier)
            }
        }
        return
    }

    // Keep the graph in step with the session, exactly once per transition.
    LaunchedEffect(session, start) {
        val current = navController.currentDestination?.route
        when {
            session is Session.LoggedIn && current == Routes.LOGIN ->
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            session is Session.LoggedOut && current != null && current != Routes.LOGIN ->
                navController.navigate(Routes.LOGIN) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
        }
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginRoute()
        }
        composable(Routes.HOME) {
            HomeRoute(
                onOpenAccount = {
                    navController.navigate(Routes.ACCOUNT) { launchSingleTop = true }
                },
                onOpenSettings = {},
                onSignOut = sessionViewModel::logout,
                onOpenBookmark = { bookmark ->
                    navController.navigate(Routes.detail(bookmark.id)) { launchSingleTop = true }
                },
                onOpenLists = {
                    navController.navigate(Routes.ALL_LISTS) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.ALL_LISTS) {
            AllListsRoute(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument(Routes.DETAIL_ARG) { type = NavType.StringType },
            ),
        ) {
            BookmarkDetailRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.ACCOUNT) {
            AccountRoute(onBack = { navController.popBackStack() })
        }
    }
}
