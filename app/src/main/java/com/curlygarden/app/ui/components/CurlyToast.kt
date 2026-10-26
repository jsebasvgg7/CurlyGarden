package com.curlygarden.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.theme.CurlyColors

@Composable
fun CurlyToast(message: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CurlyColors.DarkSurfaceElevated,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            message,
            color = CurlyColors.DarkTextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
