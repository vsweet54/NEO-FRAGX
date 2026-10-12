package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    glowColor: Color = NeonCyan,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderModifier = if (glow) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.horizontalGradient(listOf(glowColor.copy(alpha = 0.8f), NeonBlue.copy(alpha = 0.4f))),
            shape = shape
        )
    } else {
        Modifier.border(width = 1.dp, color = CyberCardStroke, shape = shape)
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else Modifier

    Column(
        modifier = modifier
            .clip(shape)
            .then(borderModifier)
            .background(CyberCard)
            .then(clickableModifier)
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun MetricProgressBar(
    title: String,
    valueLabel: String,
    progress: Float, // 0.0 to 1.0
    color: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "progress")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueLabel,
                fontSize = 13.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF142030))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.padding(end = 6.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (action != null) {
            Box(contentAlignment = Alignment.CenterEnd) {
                action()
            }
        }
    }
}

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    color: Color = NeonCyan,
    enabled: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.5.sp
) {
    val shape = RoundedCornerShape(10.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(40.dp)
            .border(
                1.dp,
                if (isPrimary) color.copy(alpha = 0.8f) else CyberCardStroke,
                shape
            ),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) color.copy(alpha = 0.15f) else CyberCard,
            contentColor = if (isPrimary) color else TextPrimary,
            disabledContainerColor = CyberCard.copy(alpha = 0.5f),
            disabledContentColor = TextSecondary.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = if (isPrimary) color else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
}
