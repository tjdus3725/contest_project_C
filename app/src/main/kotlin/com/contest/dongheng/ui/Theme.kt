package com.contest.dongheng.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SosRed = Color(0xFFE53935)
val OkGreen = Color(0xFF66BB6A)
val WarnAmber = Color(0xFFFFB74D)

private val Scheme = darkColorScheme(
    primary = Color(0xFF6CB4FF),
    onPrimary = Color(0xFF00203A),
    background = Color(0xFF10141A),
    surface = Color(0xFF1A2029),
    surfaceVariant = Color(0xFF252D38),
    onSurface = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFFB4BEC9),
    error = Color(0xFFFF6B6B),
)

private val Type = Typography(
    headlineMedium = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 20.sp),
    bodyMedium = TextStyle(fontSize = 18.sp),
    bodySmall = TextStyle(fontSize = 16.sp),
    labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun DonghengTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, typography = Type, content = content)

/** 운전 중 조작을 고려해 터치 영역을 크게(최소 72dp) 둔 버튼. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    outlined: Boolean = false,
) {
    val shape = RoundedCornerShape(16.dp)
    val pad = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    if (outlined) {
        OutlinedButton(onClick, modifier.heightIn(min = 72.dp), enabled, shape, contentPadding = pad) { Text(text) }
    } else {
        Button(
            onClick, modifier.heightIn(min = 72.dp), enabled, shape,
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
            contentPadding = pad,
        ) { Text(text) }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, title: String? = null, content: @Composable () -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) =
    Text(text, modifier, color = color, style = MaterialTheme.typography.bodySmall)
