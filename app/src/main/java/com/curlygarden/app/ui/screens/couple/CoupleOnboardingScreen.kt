package com.curlygarden.app.ui.screens.couple

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curlygarden.app.couple.CoupleOnboardingStep
import com.curlygarden.app.couple.CoupleOnboardingViewModel
import com.curlygarden.app.couple.CoupleOnboardingViewModelFactory
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground

@Composable
fun CoupleOnboardingScreen(
    onLinked: () -> Unit,
    onBackClicked: (() -> Unit)? = null,
    onSkipClicked: (() -> Unit)? = null,
    viewModel: CoupleOnboardingViewModel = viewModel(factory = CoupleOnboardingViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.linked) {
        if (uiState.linked) onLinked()
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
                .padding(24.dp)
        ) {
            if (onBackClicked != null) {
                Surface(
                    onClick = onBackClicked,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = "volver",
                            tint = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (uiState.step) {
                    CoupleOnboardingStep.ChooseAction -> ChooseActionContent(
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onInviteChosen = viewModel::onInviteChosen,
                        onHaveCodeChosen = viewModel::onHaveCodeChosen,
                        onSkipClicked = onSkipClicked
                    )
                    CoupleOnboardingStep.ShowingInviteCode -> InviteCodeContent(
                        code = uiState.generatedCode.orEmpty(),
                        onContinue = onLinked
                    )
                    CoupleOnboardingStep.EnteringCode -> EnterCodeContent(
                        codeInput = uiState.codeInput,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onCodeChanged = viewModel::onCodeInputChanged,
                        onRedeemClicked = viewModel::onRedeemClicked,
                        onBack = viewModel::onBackToChoose
                    )
                }
            }
        }
    }
}

@Composable
private fun ChooseActionContent(
    isLoading: Boolean,
    errorMessage: String?,
    onInviteChosen: () -> Unit,
    onHaveCodeChosen: () -> Unit,
    onSkipClicked: (() -> Unit)?
) {
    Text(
        "Vinculemos su espacio",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "¿Tienes un código o vas a invitar a alguien?",
        fontSize = 14.sp,
        color = CurlyColors.DarkLavenderLight,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(36.dp))

    Button(
        onClick = onInviteChosen,
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CurlyColors.DarkLavenderPrimary),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text("Voy a invitar a alguien", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    Spacer(Modifier.height(12.dp))

    OutlinedButton(
        onClick = onHaveCodeChosen,
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CurlyColors.DarkLavenderLight),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text("Tengo un código", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }

    errorMessage?.let {
        Spacer(Modifier.height(16.dp))
        Text(it, color = CurlyColors.DarkLavenderLight, fontSize = 13.sp, textAlign = TextAlign.Center)
    }

    if (onSkipClicked != null) {
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSkipClicked, enabled = !isLoading) {
            Text("Más tarde", color = CurlyColors.DarkLavenderLight, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InviteCodeContent(code: String, onContinue: () -> Unit) {
    Text(
        "Comparte este código",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Envíaselo por WhatsApp, SMS o como prefieran. Es válido por 48 horas.",
        fontSize = 14.sp,
        color = CurlyColors.DarkLavenderLight,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(32.dp))

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CurlyColors.DarkSurfaceElevated,
        modifier = Modifier.border(1.dp, CurlyColors.DarkBorder, RoundedCornerShape(20.dp))
    ) {
        Text(
            code,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = CurlyColors.DarkLavenderLight,
            letterSpacing = 6.sp,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp)
        )
    }

    Spacer(Modifier.height(36.dp))

    Button(
        onClick = onContinue,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CurlyColors.DarkLavenderPrimary),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text("Listo, ya lo compartí", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun EnterCodeContent(
    codeInput: String,
    isLoading: Boolean,
    errorMessage: String?,
    onCodeChanged: (String) -> Unit,
    onRedeemClicked: () -> Unit,
    onBack: () -> Unit
) {
    Text(
        "Ingresa tu código",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "El código de 6 caracteres que te compartieron",
        fontSize = 14.sp,
        color = CurlyColors.DarkLavenderLight,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(28.dp))

    OutlinedTextField(
        value = codeInput,
        onValueChange = onCodeChanged,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CurlyColors.DarkLavenderPrimary,
            unfocusedBorderColor = CurlyColors.DarkBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = CurlyColors.DarkLavenderPrimary
        ),
        textStyle = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier.fillMaxWidth()
    )

    errorMessage?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = CurlyColors.DarkLavenderLight, fontSize = 13.sp, textAlign = TextAlign.Center)
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onRedeemClicked,
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CurlyColors.DarkLavenderPrimary),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text("Vincularnos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    Spacer(Modifier.height(12.dp))

    TextButton(onClick = onBack, enabled = !isLoading) {
        Text("Volver", color = CurlyColors.DarkLavenderLight)
    }
}