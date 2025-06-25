package com.raindragonn.googlelogintest

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.raindragonn.googlelogintest.databinding.ActivityMainBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {
	private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
	private val auth: FirebaseAuth by lazy { Firebase.auth }
	private val credentialManager by lazy { CredentialManager.create(this) }

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		setContentView(binding.root)

		auth.signOut()

		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		binding.btnGoogleLogin.setOnClickListener {
			login()
				.onEach {
					it.onFailure {
						Log.e("DEV_LOG", "onFailure", it)
					}.onSuccess {
						Log.e("DEV_LOG", "onSuccess -  $it")
					}
				}
				.launchIn(lifecycleScope)
		}
	}

	fun hasUser(): Boolean {
		return Firebase.auth.currentUser != null
	}

	val googleIdOption = GetGoogleIdOption.Builder()
		.setFilterByAuthorizedAccounts(false)
		.setServerClientId(BuildConfig.WEB_CLIENT_ID)
		.build()

	// Create the Credential Manager request
	val request = GetCredentialRequest.Builder()
		.addCredentialOption(googleIdOption)
		.build()

	fun login(): Flow<Result<Pair<String, String>>> = callbackFlow {
		auth.signOut()
		val credential = runCatching {
			val response = credentialManager.getCredential(
				context = this@MainActivity,
				request = request
			)
			val idToken = getTokenByCredential(response.credential)
			checkNotNull(idToken)
			GoogleAuthProvider.getCredential(idToken, null)
		}.onFailure {
			Log.e("DEV_LOG", "getCredential - onFailure", it)
		}

		val result = runCatching {
			val credential = credential.getOrThrow()

			suspendCancellableCoroutine {
				auth.signInWithCredential(credential)
					.addOnCompleteListener(this@MainActivity) { task ->
						val pair = runCatching {
							if (task.isSuccessful) {
								val user = checkNotNull(auth.currentUser)
								user.run {
									checkNotNull(displayName) to checkNotNull(email)
								}
							} else {
								error("error")
							}
						}
						it.resume(pair)
					}
			}.getOrThrow()
		}.onFailure {
			Log.e("DEV_LOG", "get Result - onFailure", it)
		}

		send(result)
		awaitClose()
	}

	fun getTokenByCredential(credential: Credential): String? {
		return GoogleIdTokenCredential
			.createFrom(credential.data)
			.takeIf { credential is CustomCredential && it.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL }
			?.idToken
	}

	private fun handleSignIn(credential: Credential) {
		// Check if credential is of type Google ID
		if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
			// Create Google ID Token
			val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

			// Sign in to Firebase with using the token
			firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
		} else {
			Log.w("DEV_LOG", "Credential is not of type Google ID!")
		}
	}

	private fun firebaseAuthWithGoogle(idToken: String) {
		val credential = GoogleAuthProvider.getCredential(idToken, null)
		auth.signInWithCredential(credential)
			.addOnCompleteListener(this) { task ->
				if (task.isSuccessful) {
					// Sign in success, update UI with the signed-in user's information
					Log.d("DEV_LOG", "signInWithCredential:success")
					val user = auth.currentUser
					updateUI(user)
				} else {
					// If sign in fails, display a message to the user
					Log.w("DEV_LOG", "signInWithCredential:failure", task.exception)
					updateUI(null)
				}
			}
	}

	override fun onStart() {
		super.onStart()
		updateUI(auth.currentUser)
	}

	fun updateUI(user: FirebaseUser?) {
		binding.tvLogin.text = user?.run {
			buildString {
				appendLine("name: $displayName")
				appendLine("email: $email")
			}
		} ?: "로그인 실패"
	}
}

