package com.curlygarden.app.ui.screens.draw

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.MoreHorizontal
import compose.icons.feathericons.Shuffle

@Composable
fun DrawTopicScreen(
    onBackClicked: () -> Unit = {},
    onTopicsReady: () -> Unit = {},
    onHistoryClicked: () -> Unit = {},
    viewModel: DrawViewModel = viewModel(factory = DrawViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSendingTopic by viewModel.isSendingTopic.collectAsState()

    LaunchedEffect(uiState.status) {
        if (uiState.status == DrawRoundStatus.DRAWING ||
            uiState.status == DrawRoundStatus.REVEALED
        ) {
            onTopicsReady()
        }
    }

    var topicInput by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    val topicSent = uiState.topicISent != null

    Box(modifier = Modifier.curlyDarkBackground()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.42f,
                center = Offset(x = size.width * 1.1f, y = -size.height * 0.05f)
            )
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.48f,
                center = Offset(x = -size.width * 0.15f, y = size.height * 1.05f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawCircleButton(
                    icon = { tint ->
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "volver", tint = tint)
                    },
                    onClick = onBackClicked
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Dibujar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurlyColors.DarkTextPrimary
                    )
                    Text(
                        "Tú eliges qué dibuja el otro",
                        fontSize = 12.sp,
                        color = CurlyColors.DarkTextSecondary
                    )
                }
                DrawCircleButton(
                    icon = { tint ->
                        Icon(
                            FeatherIcons.MoreHorizontal,
                            contentDescription = "historial",
                            tint = tint
                        )
                    },
                    onClick = onHistoryClicked
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(
                        color = CurlyColors.DarkLavenderPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )

                    !uiState.hasPartner -> InfoCard(
                        text = "Todavía no tienes a nadie vinculado. Invita a alguien para poder dibujar."
                    )

                    else -> {
                        if (!topicSent || isEditing) {
                            TopicInputSection(
                                title = if (isEditing) "Edita el tema" else "¿Qué va a dibujar el otro?",
                                topicInput = topicInput,
                                onTopicChange = { if (it.length <= 60) topicInput = it },
                                isSending = isSendingTopic,
                                sendLabel = if (isEditing) "Guardar" else "Enviar",
                                showEditNote = isEditing,
                                onRandomClicked = {
                                    viewModel.sendRandomTopic()
                                    isEditing = false
                                    topicInput = ""
                                },
                                onSendClicked = {
                                    if (topicInput.isNotBlank()) {
                                        viewModel.sendTopic(topicInput)
                                        isEditing = false
                                    }
                                }
                            )
                        } else {
                            WaitingSection()
                        }

                        Spacer(Modifier.height(28.dp))

                        TopicStatusCard(
                            title = "Tu tema para el otro",
                            done = topicSent,
                            value = uiState.topicISent,
                            waitingLabel = "Aún no lo has enviado",
                            onEditClicked = if (topicSent && !isEditing) {
                                {
                                    topicInput = uiState.topicISent.orEmpty()
                                    isEditing = true
                                }
                            } else {
                                null
                            }
                        )

                        Spacer(Modifier.height(14.dp))

                        TopicStatusCard(
                            title = "Lo que vas a dibujar",
                            done = uiState.partnerHasSentTopic,
                            value = if (uiState.partnerHasSentTopic) {
                                "Listo, se revela al empezar"
                            } else {
                                null
                            },
                            waitingLabel = "El otro todavía no lo envía",
                            onEditClicked = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicInputSection(
    title: String,
    topicInput: String,
    onTopicChange: (String) -> Unit,
    isSending: Boolean,
    sendLabel: String,
    showEditNote: Boolean,
    onRandomClicked: () -> Unit,
    onSendClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            color = CurlyColors.DarkTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 14.dp)
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = topicInput,
            onValueChange = onTopicChange,
            placeholder = {
                Text("Ej. un gato con sombrero", color = CurlyColors.DarkTextMuted)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CurlyColors.DarkSurfaceElevated,
                unfocusedContainerColor = CurlyColors.DarkSurfaceElevated,
                focusedBorderColor = CurlyColors.DarkLavenderPrimary,
                unfocusedBorderColor = CurlyColors.DarkBorder,
                focusedTextColor = CurlyColors.DarkTextPrimary,
                unfocusedTextColor = CurlyColors.DarkTextPrimary,
                cursorColor = CurlyColors.DarkLavenderPrimary
            )
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = onRandomClicked,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = CurlyColors.DarkSurfaceElevated,
                border = BorderStroke(1.dp, CurlyColors.DarkBorder),
                enabled = !isSending
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        FeatherIcons.Shuffle,
                        contentDescription = null,
                        tint = CurlyColors.DarkLavenderPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Aleatorio",
                        color = CurlyColors.DarkTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                onClick = onSendClicked,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = CurlyColors.DarkLavenderPrimary,
                enabled = topicInput.isNotBlank() && !isSending
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            sendLabel,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (showEditNote) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Si el otro envía su tema mientras editas, se queda el que ya tenías puesto.",
                color = CurlyColors.DarkTextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
    }
}

@Composable
private fun WaitingSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            FeatherIcons.Clock,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(76.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Esperando a que el otro\nenvíe su tema…",
            color = CurlyColors.DarkTextPrimary.copy(alpha = 0.85f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DrawCircleButton(
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon(Color.White)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CurlyColors.DarkSurfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                FeatherIcons.Clock,
                contentDescription = null,
                tint = CurlyColors.DarkLavenderLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(text, color = CurlyColors.DarkTextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TopicStatusCard(
    title: String,
    done: Boolean,
    value: String?,
    waitingLabel: String,
    onEditClicked: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CurlyColors.DarkSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 88.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (done) FeatherIcons.CheckCircle else FeatherIcons.Clock,
                contentDescription = null,
                tint = if (done) CurlyColors.DarkLavenderPrimary else CurlyColors.DarkTextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    value ?: waitingLabel,
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onEditClicked != null) {
                Surface(
                    onClick = onEditClicked,
                    shape = CircleShape,
                    color = CurlyColors.DarkSurface,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            FeatherIcons.Edit2,
                            contentDescription = "editar tema",
                            tint = CurlyColors.DarkLavenderLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}