package com.curlygarden.app.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.R
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground

@Composable
fun SplashScreen(onContinueClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .curlyDarkBackground()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_leaf),
            contentDescription = null,
            modifier = Modifier.size(56.dp)
        )

        Spacer(Modifier.height(4.dp))

        Row {
            Text(
                "Curly",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Garden",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = CurlyColors.DarkLavenderPrimary
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "UN ESPACIO, DOS PERSONAS,\nINFINITAS IDEAS",
            fontSize = 13.sp,
            letterSpacing = 1.5.sp,
            color = CurlyColors.DarkLavenderLight,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(28.dp))

        Image(
            painter = painterResource(id = R.drawable.curlygarden_illustration),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(36.dp))

        Text(
            "Habla, juega, dibuja,\nimagina y guarda recuerdos juntos.",
            fontSize = 15.sp,
            color = CurlyColors.DarkLavenderLight,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onContinueClicked,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = CurlyColors.DarkBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continuar", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
    }
}