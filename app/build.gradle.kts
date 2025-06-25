import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.google.services)
}


android {
	val localProperties = Properties()
	localProperties.load(project.rootProject.file("local.properties").inputStream())

	namespace = "com.raindragonn.googlelogintest"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.raindragonn.googlelogintest"
		minSdk = 23
		targetSdk = 35
		versionCode = 1
		versionName = "1.0"
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

		buildConfigField("String", "WEB_CLIENT_ID", localProperties["WEB_CLIENT_ID"].toString())
	}

	signingConfigs {
		getByName("debug") {
			storeFile = file(localProperties["STORE_FILE_PATH"].toString())
			storePassword = localProperties["STORE_PASSWORD"].toString()
			keyAlias = localProperties["KEY_ALIAS"].toString()
			keyPassword = localProperties["KEY_PASSWORD"].toString()
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	kotlinOptions {
		jvmTarget = "11"
	}
	buildFeatures {
		viewBinding = true
		buildConfig = true
	}
}

dependencies {

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.constraintlayout)

	implementation(platform(libs.firebase.bom))
	implementation(libs.firebase.auth)

	implementation(libs.androidx.credentials)
	implementation(libs.androidx.credentials.play.services.auth)
	implementation(libs.googleid)

	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
}
