package com.raindragonn.googlelogintest.oauth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.raindragonn.googlelogintest.BuildConfig
import com.raindragonn.googlelogintest.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * @author : raindragonn
 * @CreatedDate : 2025. 6. 25. 오후 2:17
 * @PackageName : com.raindragonn.googlelogintest.oauth
 * @ClassName: GoogleOauthClient
 * @Description:
 */
class GoogleOauthClient(
	private val context: Context,
) : OauthClient {
	private val auth: FirebaseAuth by lazy { Firebase.auth }
	private val credentialManager by lazy { CredentialManager.create(context) }
	private val activity: Activity = context as Activity

	override fun hasUser(): Boolean {
		return auth.currentUser != null
	}

	override fun startLoginFlow(): Flow<Result<User>> {
		val googleIdOption = GetGoogleIdOption.Builder()
			.setFilterByAuthorizedAccounts(false)
			.setServerClientId(BuildConfig.WEB_CLIENT_ID)
			.build()

		// Create the Credential Manager request
		val request = GetCredentialRequest.Builder()
			.addCredentialOption(googleIdOption)
			.build()

		return callbackFlow {
			val result = runCatching {
				signOut()
				val idToken = getIdToken(request).getOrThrow()
				getUser(idToken).getOrThrow()
			}
			send(result)
			awaitClose()
		}
	}

	suspend fun signOut() {
		auth.signOut()
		val clearRequest = ClearCredentialStateRequest()
		credentialManager.clearCredentialState(clearRequest)
	}

	suspend fun getIdToken(request: GetCredentialRequest) =
		runCatching {
			credentialManager.getCredential(activity, request)
		}.mapCatching { response ->
			val credential = response.credential
			if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
				GoogleIdTokenCredential.createFrom(credential.data).idToken
			} else {
				error("Invalid credential type")
			}
		}

	suspend fun getUser(idToken: String): Result<User> = suspendCancellableCoroutine {
		runCatching {
			val authCredential = GoogleAuthProvider.getCredential(idToken, null)
			auth.signInWithCredential(authCredential)
				.addOnCompleteListener(Executors.newSingleThreadExecutor()) { task ->
					val userResult = runCatching {
						if (task.isSuccessful) {
							val firebaseUser = checkNotNull(auth.currentUser)
							User(
								name = checkNotNull(firebaseUser.displayName),
								email = checkNotNull(firebaseUser.email),
							)
						} else {
							throw task.exception ?: error("error")
						}
					}
					it.resume(userResult)
				}
		}
	}
}

