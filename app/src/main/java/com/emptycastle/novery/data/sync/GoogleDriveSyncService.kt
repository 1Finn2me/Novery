package com.emptycastle.novery.data.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.emptycastle.novery.data.local.PreferencesManager
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.UserCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.net.URI
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Handles Google Drive auth and remote payload storage in the appData folder.
 */
class GoogleDriveSyncService(
    context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager.getInstance(context)
) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var driveService: Drive? = null

    init {
        initDriveService()
    }

    fun getSignInIntent(): Intent {
        val authState = UUID.randomUUID().toString()
        preferencesManager.setGoogleDriveAuthState(authState)

        val authorizationUrl = buildAuthorizationFlow()
            .newAuthorizationUrl()
            .setRedirectUri(REDIRECT_URI)
            .setState(authState)
            .setApprovalPrompt("force")
            .build()

        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(authorizationUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    suspend fun handleAuthorizationCode(code: String) = withContext(Dispatchers.IO) {
        val secrets = loadClientSecrets()
        val tokenResponse: GoogleTokenResponse = GoogleAuthorizationCodeTokenRequest(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            secrets.installed.clientId,
            secrets.installed.clientSecret.orEmpty(),
            code,
            REDIRECT_URI
        ).setGrantType("authorization_code").execute()

        val accessToken = tokenResponse.accessToken.orEmpty()
        val refreshToken = tokenResponse.refreshToken.orEmpty()

        if (accessToken.isBlank() || refreshToken.isBlank()) {
            throw IllegalStateException("Google Drive sign-in did not return usable tokens.")
        }

        preferencesManager.setGoogleDriveTokens(accessToken, refreshToken)
        setupDriveService(accessToken, refreshToken)
    }

    suspend fun pullSyncPayload(): SyncPayload? = withContext(Dispatchers.IO) {
        refreshToken()
        val drive = requireDriveService()
        val existingFile = getRemoteFile(drive) ?: return@withContext null

        drive.files().get(existingFile.id).executeMediaAsInputStream().use { input ->
            GZIPInputStream(input).use { gzipInput ->
                val payloadJson = gzipInput.readBytes().decodeToString()
                json.decodeFromString(SyncPayload.serializer(), payloadJson)
            }
        }
    }

    suspend fun pushSyncPayload(payload: SyncPayload) = withContext(Dispatchers.IO) {
        val drive = requireDriveService()
        val remoteFiles = getRemoteFiles(drive)
        val existingFile = remoteFiles.firstOrNull()
        val payloadBytes = gzip(json.encodeToString(SyncPayload.serializer(), payload))
        val media = ByteArrayContent(GZIP_MIME_TYPE, payloadBytes)
        val metadata = File().apply {
            name = REMOTE_FILE_NAME
            mimeType = GZIP_MIME_TYPE
            appProperties = mapOf("deviceId" to payload.deviceId)
        }

        if (existingFile != null) {
            drive.files().update(existingFile.id, metadata, media)
                .setFields("id, modifiedTime")
                .execute()
        } else {
            metadata.parents = listOf("appDataFolder")
            drive.files().create(metadata, media)
                .setFields("id, modifiedTime")
                .execute()
        }

        remoteFiles.drop(1).forEach { duplicate ->
            runCatching { drive.files().delete(duplicate.id).execute() }
        }
    }

    suspend fun purgeRemotePayload(): Boolean = withContext(Dispatchers.IO) {
        refreshToken()
        val drive = requireDriveService()
        val remoteFiles = getRemoteFiles(drive)
        if (remoteFiles.isEmpty()) {
            return@withContext false
        }

        remoteFiles.forEach { file ->
            drive.files().delete(file.id).execute()
        }
        return@withContext true
    }

    fun clearLocalAccount() {
        preferencesManager.clearGoogleDriveTokens()
        preferencesManager.clearGoogleDriveAuthState()
        driveService = null
    }

    fun getPendingAuthState(): String {
        return preferencesManager.getGoogleDriveAuthState()
    }

    fun clearPendingAuthState() {
        preferencesManager.clearGoogleDriveAuthState()
    }

    fun isConfigured(): Boolean {
        return try {
            loadClientSecrets()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun initDriveService() {
        val accessToken = preferencesManager.getGoogleDriveAccessToken()
        val refreshToken = preferencesManager.getGoogleDriveRefreshToken()
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            driveService = null
            return
        }

        setupDriveService(accessToken, refreshToken)
    }

    private suspend fun refreshToken() = withContext(Dispatchers.IO) {
        val refreshToken = preferencesManager.getGoogleDriveRefreshToken()
        if (refreshToken.isBlank()) {
            throw IllegalStateException("Google Drive is not signed in.")
        }

        try {
            val credentials = buildUserCredentials(
                accessToken = preferencesManager.getGoogleDriveAccessToken(),
                refreshToken = refreshToken
            )
            val refreshedToken = credentials.refreshAccessToken()
            val accessToken = refreshedToken.tokenValue.orEmpty()
            if (accessToken.isBlank()) {
                throw IllegalStateException("Google Drive did not return a refreshed access token.")
            }

            preferencesManager.setGoogleDriveTokens(accessToken, credentials.refreshToken.orEmpty())
            setupDriveService(accessToken, credentials.refreshToken.orEmpty())
        } catch (error: TokenResponseException) {
            if (error.details?.error == "invalid_grant") {
                clearLocalAccount()
            }
            throw error
        }
    }

    private fun setupDriveService(accessToken: String, refreshToken: String) {
        val credentials = buildUserCredentials(accessToken, refreshToken)

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        ).setApplicationName("Novery")
            .build()
    }

    private fun buildUserCredentials(accessToken: String, refreshToken: String): UserCredentials {
        val secrets = loadClientSecrets()
        val clientId = secrets.installed.clientId.orEmpty()
        if (clientId.isBlank()) {
            throw IllegalStateException("Google Drive OAuth client ID is missing.")
        }

        val builder = UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(secrets.installed.clientSecret.orEmpty())
            .setRefreshToken(refreshToken)
            .setTokenServerUri(GOOGLE_TOKEN_SERVER_URI)

        if (accessToken.isNotBlank()) {
            builder.setAccessToken(AccessToken(accessToken, null))
        }

        return builder.build()
    }

    private fun requireDriveService(): Drive {
        return driveService ?: throw IllegalStateException("Google Drive is not signed in.")
    }

    private fun getRemoteFile(drive: Drive): File? {
        return getRemoteFiles(drive).firstOrNull()
    }

    private fun getRemoteFiles(drive: Drive): List<File> {
        return drive.files()
            .list()
            .setSpaces("appDataFolder")
            .setQ("name = '$REMOTE_FILE_NAME'")
            .setOrderBy("modifiedTime desc")
            .setFields("files(id, name, createdTime, modifiedTime, appProperties)")
            .execute()
            .files
            .orEmpty()
    }

    private fun buildAuthorizationFlow(): GoogleAuthorizationCodeFlow {
        val secrets = loadClientSecrets()
        return GoogleAuthorizationCodeFlow.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            secrets,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).setAccessType("offline")
            .build()
    }

    private fun buildAuthorizationUrl(state: String): String {
        return buildAuthorizationFlow()
            .newAuthorizationUrl()
            .setRedirectUri(REDIRECT_URI)
            .setState(state)
            .setApprovalPrompt("force")
            .build()
    }

    private fun gzip(bytes: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { gzip ->
            gzip.write(bytes.toByteArray(Charsets.UTF_8))
        }
        return output.toByteArray()
    }

    private fun loadClientSecrets(): GoogleClientSecrets {
        try {
            appContext.assets.open(CLIENT_SECRETS_FILE).use { stream ->
                return GoogleClientSecrets.load(
                    GsonFactory.getDefaultInstance(),
                    stream.reader()
                )
            }
        } catch (error: FileNotFoundException) {
            throw IllegalStateException(
                "Google Drive OAuth is not configured for this build. " +
                    "Add $CLIENT_SECRETS_FILE under app/src/main/assets."
            )
        }
    }

    companion object {
        const val CLIENT_SECRETS_FILE = "client_secrets.json"
        const val REDIRECT_URI = "com.emptycastle.novery.google.oauth:/oauth2redirect"
        val GOOGLE_TOKEN_SERVER_URI: URI = URI.create("https://oauth2.googleapis.com/token")
        private const val REMOTE_FILE_NAME = "Novery_sync.json.gz"
        private const val GZIP_MIME_TYPE = "application/gzip"
    }
}
