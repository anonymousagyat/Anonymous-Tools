package com.anonymoustools.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.anonymoustools.app.ui.theme.CardWhite
import com.anonymoustools.app.ui.theme.PillShape
import com.anonymoustools.app.ui.theme.TextBlack

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isOutline: Boolean = false,
    backgroundColor: Color = TextBlack,
    textColor: Color = if (isOutline) TextBlack else CardWhite
) {
    if (isOutline) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = PillShape,
            border = BorderStroke(2.dp, TextBlack),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextBlack
            )
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextBlack,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = TextBlack,
                style = MaterialTheme.typography.labelLarge
            )
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = textColor
            )
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
