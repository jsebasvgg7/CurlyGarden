package com.curlygarden.app.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.R
import com.curlygarden.app.auth.AuthDestination
import com.curlygarden.app.auth.AuthViewModel
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigate: (AuthDestination) -> Unit,
    onBackClicked: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateTo) {
        uiState.navigateTo?.let {
            onNavigate(it)
            viewModel.onNavigationHandled()
        }
    }

    Box(
        modifier = Modifier.curlyDarkBackground()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.42f,
                center = Offset(x = -size.width * 0.12f, y = -size.height * 0.04f)
            )
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.48f,
                center = Offset(x = size.width * 1.08f, y = size.height * 1.03f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Surface(
                onClick = onBackClicked,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "Atras",
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_leaf),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Text("Curly", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Garden",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurlyColors.DarkLavenderPrimary
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    "¡Bienvenido!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Inicia sesión para continuar\ncon tu espacio compartido.",
                    fontSize = 15.sp,
                    color = CurlyColors.DarkLavenderLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Image(
                    painter = painterResource(id = R.drawable.curlygarden_illustration_2),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(1.05f)
                )
            }

            GoogleSignInButton(
                isLoading = uiState.isLoading,
                onClick = viewModel::onSignInWithGoogleClicked
            )

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(14.dp))
                Text(
                    message,
                    color = CurlyColors.Pink,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.15f)
                )
                Text(
                    "o",
                    color = CurlyColors.DarkLavenderLight,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.15f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Tu privacidad es importante para nosotros.",
                fontSize = 12.sp,
                color = CurlyColors.DarkLavenderLight.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GoogleSignInButton(isLoading: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = CurlyColors.DarkLavenderPrimary
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Continuar con Google",
                    color = CurlyColors.TextDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}