package com.raindragonn.googlelogintest.oauth

import com.raindragonn.googlelogintest.model.User
import kotlinx.coroutines.flow.Flow

/**
 * @author : raindragonn
 * @CreatedDate : 2025. 6. 25. 오후 2:16
 * @PackageName : com.raindragonn.googlelogintest.oauth
 * @ClassName: OauthClient
 * @Description:
 */
interface OauthClient {
	fun hasUser(): Boolean
	fun startLoginFlow(): Flow<Result<User>>
}
