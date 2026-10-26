package com.readyplayerai.cassetto.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.navigation.compose.rememberNavController
import com.readyplayerai.cassetto.data.auth.Session
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
                is Session.LoggedIn -> Routes.ACCOUNT
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
                navController.navigate(Routes.ACCOUNT) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            session is Session.LoggedOut && current == Routes.ACCOUNT ->
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
        composable(Routes.ACCOUNT) {
            // Replaced by the real account screen in Step 7.
            Scaffold { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Signed in")
                }
            }
        }
    }
}
