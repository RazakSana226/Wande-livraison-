package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

/**
 * Draws the precise official WÀNDÉ dynamic 'W' monogram with connecting nodes and northeast arrow.
 */
@Composable
fun WandeMonogramVector(
    modifier: Modifier = Modifier,
    color: Color = WandePrimary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val strokeWidthPx = w * 0.09f

        // 1. Leftmost diagonal bar
        drawLine(
            color = color,
            start = Offset(w * 0.22f, h * 0.38f),
            end = Offset(w * 0.34f, h * 0.62f),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )

        // 2. Bottom-left terminal node
        drawCircle(
            color = color,
            radius = strokeWidthPx * 0.75f,
            center = Offset(w * 0.22f, h * 0.74f)
        )

        // 3. Main connected dynamic W stroke
        val mainPath = Path().apply {
            moveTo(w * 0.22f, h * 0.74f)
            lineTo(w * 0.38f, h * 0.40f)
            cubicTo(
                w * 0.41f, h * 0.34f,
                w * 0.47f, h * 0.34f,
                w * 0.50f, h * 0.40f
            )
            lineTo(w * 0.58f, h * 0.64f)
            cubicTo(
                w * 0.61f, h * 0.72f,
                w * 0.67f, h * 0.72f,
                w * 0.70f, h * 0.64f
            )
            lineTo(w * 0.80f, h * 0.44f)
        }
        drawPath(
            path = mainPath,
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 4. Intermediate connection node on rising arm
        drawCircle(
            color = color,
            radius = strokeWidthPx * 0.65f,
            center = Offset(w * 0.65f, h * 0.54f)
        )

        // 5. Northeast Arrowhead at top right
        val arrowPath = Path().apply {
            moveTo(w * 0.70f, h * 0.34f)
            lineTo(w * 0.88f, h * 0.36f)
            lineTo(w * 0.84f, h * 0.54f)
            lineTo(w * 0.80f, h * 0.48f)
            lineTo(w * 0.74f, h * 0.56f)
            lineTo(w * 0.69f, h * 0.52f)
            lineTo(w * 0.75f, h * 0.44f)
            close()
        }
        drawPath(
            path = arrowPath,
            color = color
        )
    }
}

/**
 * App Icon Squircle Badge as shown on the right of the official brand asset.
 */
@Composable
fun WandeBrandBadge(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(WandePrimary)
            .border(
                width = (size * 0.035f).coerceAtLeast(1.dp),
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(size * 0.28f)
            )
            .padding(size * 0.12f),
        contentAlignment = Alignment.Center
    ) {
        WandeMonogramVector(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        )
    }
}

/**
 * Full brand lockup: [Monogram Icon] + [WÀNDÉ] text.
 */
@Composable
fun WandeFullBrandLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 44.dp,
    textColor: Color = WandePrimary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WandeMonogramVector(
            modifier = Modifier.size(iconSize),
            color = textColor
        )
        Text(
            text = "WÀNDÉ",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            color = textColor
        )
    }
}

@Composable
fun WandeHeaderLogo(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag("wande_header_logo"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WandeBrandBadge(size = 42.dp)

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "WÀNDÉ",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = WandePrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = WandePrimary.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = "EXPRESS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WandePrimary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "Livraison instantanée & sécurisée",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WandeHeroLogo(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WandeBrandBadge(size = 72.dp)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "WÀNDÉ",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = WandePrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = WandePrimary
            ) {
                Text(
                    text = "PRO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Plateforme de livraison urbaine bleu & blanc",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

