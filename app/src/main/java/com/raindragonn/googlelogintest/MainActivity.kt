package com.raindragonn.googlelogintest

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.raindragonn.googlelogintest.databinding.ActivityMainBinding
import com.raindragonn.googlelogintest.model.User
import com.raindragonn.googlelogintest.oauth.GoogleOauthClient
import com.raindragonn.googlelogintest.oauth.OauthClient
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class MainActivity : AppCompatActivity() {
	private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
	private val googleOauthClient: OauthClient by lazy { GoogleOauthClient(this) }

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		setContentView(binding.root)

		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		binding.btnGoogleLogin.setOnClickListener { loginWithGoogle() }
	}

	fun loginWithGoogle() {
		googleOauthClient
			.startLoginFlow()
			.onEach { updateUI(it) }
			.onStart { binding.tvUser.text = "로그인 중..." }
			.launchIn(lifecycleScope)
	}

	fun updateUI(result: Result<User>) {
		val text = result.fold(
			onSuccess = { user ->
				buildString {
					appendLine("name: ${user.name}")
					appendLine("email: ${user.email}")
				}
			},
			onFailure = { "로그인 실패" }
		)
		binding.tvUser.text = text
	}
}

