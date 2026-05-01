package com.emptycastle.novery.ui.sync

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.emptycastle.novery.data.sync.GoogleDriveSyncService
import com.emptycastle.novery.data.sync.SyncWorker
import kotlinx.coroutines.launch

/**
 * Receives the Google Drive OAuth redirect and stores returned tokens.
 */
class GoogleDriveLoginActivity : ComponentActivity() {
    private lateinit var googleDriveSyncService: GoogleDriveSyncService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleDriveSyncService = GoogleDriveSyncService(applicationContext)
        handleIntent()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        val uri = intent?.data
        val code = uri?.getQueryParameter("code")
        val error = uri?.getQueryParameter("error")
        val returnedState = uri?.getQueryParameter("state")
        val expectedState = googleDriveSyncService.getPendingAuthState()

        when {
            code != null -> {
                if (expectedState.isBlank() || returnedState != expectedState) {
                    googleDriveSyncService.clearPendingAuthState()
                    Toast.makeText(
                        this@GoogleDriveLoginActivity,
                        "Google Drive sign-in was rejected because the callback state did not match.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return
                }

                lifecycleScope.launch {
                    runCatching {
                        googleDriveSyncService.handleAuthorizationCode(code)
                    }.onSuccess {
                        googleDriveSyncService.clearPendingAuthState()
                        SyncWorker.schedule(this@GoogleDriveLoginActivity, forceUpdate = true)
                        Toast.makeText(
                            this@GoogleDriveLoginActivity,
                            "Google Drive connected",
                            Toast.LENGTH_LONG
                            ).show()
                        finish()
                    }.onFailure { authError ->
                        googleDriveSyncService.clearPendingAuthState()
                        Toast.makeText(
                            this@GoogleDriveLoginActivity,
                            authError.safeSignInMessage(),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }

            error != null -> {
                googleDriveSyncService.clearPendingAuthState()
                Toast.makeText(this, "Google Drive sign-in was cancelled.", Toast.LENGTH_LONG).show()
                finish()
            }

            else -> {
                googleDriveSyncService.clearPendingAuthState()
                finish()
            }
        }
    }

    private fun Throwable.safeSignInMessage(): String {
        return if (message?.contains("not configured", ignoreCase = true) == true) {
            "Google Drive setup is missing for this build."
        } else {
            "Google Drive sign-in failed. Try again."
        }
    }
}
