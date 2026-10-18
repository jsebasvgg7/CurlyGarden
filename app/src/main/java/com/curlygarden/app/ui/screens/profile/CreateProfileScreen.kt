package com.curlygarden.app.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.curlygarden.app.profile.CreateProfileViewModel
import com.curlygarden.app.profile.CreateProfileViewModelFactory
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground

@Composable
fun CreateProfileScreen(
    onProfileSaved: () -> Unit,
    viewModel: CreateProfileViewModel = viewModel(factory = CreateProfileViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onProfileSaved()
    }

    Box(modifier = Modifier.curlyDarkBackground()) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                "Cuéntanos de ti",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Así te van a ver en su espacio compartido",
                fontSize = 14.sp,
                color = CurlyColors.DarkLavenderLight
            )

            Spacer(Modifier.height(36.dp))

            ProfilePhoto(photoUrl = uiState.photoUrl, name = uiState.name)

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Tu nombre") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CurlyColors.DarkSurfaceElevated,
                    unfocusedContainerColor = CurlyColors.DarkSurfaceElevated,
                    focusedBorderColor = CurlyColors.DarkLavenderPrimary,
                    unfocusedBorderColor = CurlyColors.DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = CurlyColors.DarkLavenderLight,
                    unfocusedLabelColor = CurlyColors.DarkTextMuted,
                    cursorColor = CurlyColors.DarkLavenderPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    color = CurlyColors.DarkLavenderLight,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = viewModel::onSaveClicked,
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CurlyColors.DarkLavenderPrimary,
                    disabledContainerColor = CurlyColors.DarkLavenderPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        "Continuar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilePhoto(photoUrl: String?, name: String) {
    Box(
        Modifier
            .size(96.dp)
            .background(CurlyColors.DarkBorder, shape = CircleShape)
            .padding(4.dp)
            .background(CurlyColors.DarkLavenderPrimary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
            Text(
                initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )
        }
    }
}