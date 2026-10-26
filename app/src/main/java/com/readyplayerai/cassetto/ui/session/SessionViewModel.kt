package com.readyplayerai.cassetto.ui.session

import androidx.lifecycle.ViewModel
import com.readyplayerai.cassetto.data.auth.AuthRepository
import com.readyplayerai.cassetto.data.auth.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Read-only view of the app-wide session, for the nav host and (later) settings. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    auth: AuthRepository,
) : ViewModel() {
    val session: StateFlow<Session> = auth.session
}
