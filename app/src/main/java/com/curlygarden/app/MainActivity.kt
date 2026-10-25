package com.curlygarden.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.curlygarden.app.auth.AuthDestination
import com.curlygarden.app.auth.AuthViewModel
import com.curlygarden.app.auth.AuthViewModelFactory
import com.curlygarden.app.auth.SessionStoreViewModel
import com.curlygarden.app.startup.StartupDestination
import com.curlygarden.app.startup.StartupRepository
import com.curlygarden.app.ui.screens.auth.LoginScreen
import com.curlygarden.app.ui.screens.auth.SplashScreen
import com.curlygarden.app.ui.screens.couple.CoupleOnboardingScreen
import com.curlygarden.app.ui.screens.draw.DrawFlow
import com.curlygarden.app.ui.screens.home.HomeScreen
import com.curlygarden.app.ui.screens.imagine.ImagineScreen
import com.curlygarden.app.ui.screens.memories.MemoriesScreen
import com.curlygarden.app.ui.screens.play.PlayScreen
import com.curlygarden.app.ui.screens.profile.CreateProfileScreen
import com.curlygarden.app.ui.screens.profile.ProfileScreen
import com.curlygarden.app.ui.screens.talk.TalkScreen
import com.curlygarden.app.ui.theme.CurlyGardenTheme
import com.curlygarden.app.ui.theme.CurlyStatusBarStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val highestRefreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.supportedModes?.maxOfOrNull { it.refreshRate }
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay?.supportedModes?.maxOfOrNull { it.refreshRate }
        }

        if (highestRefreshRate != null) {
            window.attributes = window.attributes.apply {
                preferredRefreshRate = highestRefreshRate
            }
        }

        setContent {
            CurlyGardenApp()
        }
    }
}

private enum class AppScreen {
    Splash, Login, CreateProfile, CoupleOnboarding, Home
}

private suspend fun resolveInitialScreen(): AppScreen =
    when (StartupRepository().resolveStartupDestination()) {
        StartupDestination.Splash -> AppScreen.Splash
        StartupDestination.CreateProfile -> AppScreen.CreateProfile
        StartupDestination.CoupleOnboarding -> AppScreen.CoupleOnboarding
        StartupDestination.Home -> AppScreen.Home
    }

@Composable
fun CurlyGardenApp() {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
    )

    val sessionStore: SessionStoreViewModel = viewModel()

    var screen by remember { mutableStateOf<AppScreen?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!authViewModel.currentUserExists()) {
            screen = AppScreen.Splash
            return@LaunchedEffect
        }
        screen = resolveInitialScreen()
    }

    CompositionLocalProvider(LocalViewModelStoreOwner provides sessionStore) {
        when (screen) {
            null -> CurlyGardenTheme(statusBarStyle = CurlyStatusBarStyle.Dark) {
                SplashScreen(onContinueClicked = {})
            }
            AppScreen.Splash -> CurlyGardenTheme(statusBarStyle = CurlyStatusBarStyle.Dark) {
                SplashScreen(onContinueClicked = { screen = AppScreen.Login })
            }
            AppScreen.Login -> CurlyGardenTheme(statusBarStyle = CurlyStatusBarStyle.Dark) {
                LoginScreen(
                    viewModel = authViewModel,
                    onBackClicked = { screen = AppScreen.Splash },
                    onNavigate = { destination ->
                        val fallback = when (destination) {
                            AuthDestination.CreateProfile -> AppScreen.CreateProfile
                            AuthDestination.CoupleOnboarding -> AppScreen.CoupleOnboarding
                        }
                        screen = null
                        scope.launch {
                            val resolved = runCatching { resolveInitialScreen() }.getOrNull()
                            screen = if (resolved == null || resolved == AppScreen.Splash) {
                                fallback
                            } else {
                                resolved
                            }
                        }
                    }
                )
            }
            AppScreen.CreateProfile -> CurlyGardenTheme(statusBarStyle = CurlyStatusBarStyle.Dark) {
                CreateProfileScreen(
                    onProfileSaved = { screen = AppScreen.CoupleOnboarding }
                )
            }
            AppScreen.CoupleOnboarding -> CurlyGardenTheme(statusBarStyle = CurlyStatusBarStyle.Dark) {
                CoupleOnboardingScreen(
                    onLinked = { screen = AppScreen.Home },
                    onSkipClicked = { screen = AppScreen.Home }
                )
            }
            AppScreen.Home -> CurlyGardenTheme(statusBarStyle = CurlyStatusBarStyle.Dark) {
                CurlyGardenHome(
                    onLoggedOut = {
                        sessionStore.reset()
                        authViewModel.signOut()
                        screen = AppScreen.Login
                    }
                )
            }
        }
    }
}

private enum class HomeDestination {
    Home, Me, Talk, Draw, Imagine, Play, Recuerdos, MeOnboarding, InviteOnboarding
}

private fun HomeDestination.depth(): Int = when (this) {
    HomeDestination.Home -> 0
    HomeDestination.MeOnboarding -> 2
    else -> 1
}

@Composable
private fun CurlyGardenHome(onLoggedOut: () -> Unit) {
    var destination by remember { mutableStateOf(HomeDestination.Home) }
    var introPending by remember { mutableStateOf(true) }

    LaunchedEffect(destination) {
        if (destination != HomeDestination.Home) introPending = false
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val forward = targetState.depth() > initialState.depth()
            val spec = if (forward) {
                (fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.93f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )) togetherWith ExitTransition.None
            } else {
                EnterTransition.None togetherWith
                    (fadeOut(tween(220, easing = FastOutSlowInEasing)) +
                        scaleOut(
                            targetScale = 0.95f,
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ))
            }
            spec.targetContentZIndex = if (forward) 1f else 0f
            spec
        },
        label = "homeDestination"
    ) { current ->
        when (current) {
            HomeDestination.Talk -> TalkScreen(onBack = { destination = HomeDestination.Home })
            HomeDestination.Draw -> DrawFlow(onExit = { destination = HomeDestination.Home })
            HomeDestination.Imagine -> ImagineScreen(onBack = { destination = HomeDestination.Home })
            HomeDestination.Play -> PlayScreen(onBack = { destination = HomeDestination.Home })
            HomeDestination.Recuerdos -> MemoriesScreen(onBack = { destination = HomeDestination.Home })
            HomeDestination.Home -> HomeScreen(
                onModuleClick = { module ->
                    when (module) {
                        "hablar" -> destination = HomeDestination.Talk
                        "dibujar" -> destination = HomeDestination.Draw
                        "imaginar" -> destination = HomeDestination.Imagine
                        "jugar" -> destination = HomeDestination.Play
                        "recuerdos" -> destination = HomeDestination.Recuerdos
                    }
                },
                onProfileClick = { destination = HomeDestination.Me },
                onInviteClick = { destination = HomeDestination.InviteOnboarding },
                animateEntrance = introPending
            )
            HomeDestination.InviteOnboarding -> CoupleOnboardingScreen(
                onLinked = { destination = HomeDestination.Home },
                onBackClicked = { destination = HomeDestination.Home }
            )
            HomeDestination.MeOnboarding -> CoupleOnboardingScreen(
                onLinked = { destination = HomeDestination.Me },
                onBackClicked = { destination = HomeDestination.Me }
            )
            HomeDestination.Me -> ProfileScreen(
                onBackClicked = { destination = HomeDestination.Home },
                onGoToOnboardingClicked = { destination = HomeDestination.MeOnboarding },
                onLogoutClicked = onLoggedOut
            )
        }
    }
}
