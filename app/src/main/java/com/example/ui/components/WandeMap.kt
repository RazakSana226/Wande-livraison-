package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeliveryStatus
import com.example.model.LatLngPoint
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WandeInteractiveMap(
    modifier: Modifier = Modifier,
    pickupPoint: LatLngPoint? = null,
    destinationPoint: LatLngPoint? = null,
    driverLat: Double? = null,
    driverLng: Double? = null,
    deliveryStatus: DeliveryStatus? = null,
    isSearching: Boolean = false,
    isSelectable: Boolean = false,
    onLocationSelected: ((LatLngPoint) -> Unit)? = null
) {
    // Pulse animation for search radar
    val infiniteTransition = rememberInfiniteTransition(label = "map_radar")
    val radarRadius by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_radius"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    // Courier smooth wobble/bounce animation
    val scooterBounce by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scooter_bounce"
    )

    // Interactive selector drag offset
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE2E8F0))
            .testTag("wande_interactive_map")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelectable) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                                onLocationSelected?.let { callback ->
                                    val lat = 12.3685 - (dragOffset.y / 8000.0)
                                    val lng = -1.5270 + (dragOffset.x / 8000.0)
                                    callback(LatLngPoint(lat, lng, "Point sur la carte (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})", "Emplacement ajusté"))
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)

            // 1. Draw Map City Grid (Avenues, Streets, Parks)
            drawRect(color = Color(0xFFF1F5F9))

            // Green Spaces / Parks
            drawCircle(
                color = Color(0xFFDCFCE7),
                radius = width * 0.28f,
                center = Offset(width * 0.2f, height * 0.25f)
            )
            drawCircle(
                color = Color(0xFFDCFCE7),
                radius = width * 0.22f,
                center = Offset(width * 0.85f, height * 0.75f)
            )

            // Boulevard & Arteries
            val roadColor = Color(0xFFFFFFFF)
            val roadBorder = Color(0xFFCBD5E1)

            // Main Avenue Diagonal
            drawLine(
                color = roadBorder,
                start = Offset(0f, height * 0.75f),
                end = Offset(width, height * 0.25f),
                strokeWidth = 32f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = roadColor,
                start = Offset(0f, height * 0.75f),
                end = Offset(width, height * 0.25f),
                strokeWidth = 26f,
                cap = StrokeCap.Round
            )

            // Crossing Boulevard
            drawLine(
                color = roadBorder,
                start = Offset(width * 0.25f, 0f),
                end = Offset(width * 0.75f, height),
                strokeWidth = 28f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = roadColor,
                start = Offset(width * 0.25f, 0f),
                end = Offset(width * 0.75f, height),
                strokeWidth = 22f,
                cap = StrokeCap.Round
            )

            // Secondary Streets
            for (i in 1..4) {
                val y = height * (i * 0.2f)
                drawLine(
                    color = roadBorder,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 14f
                )
                drawLine(
                    color = roadColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 10f
                )
            }
            for (i in 1..4) {
                val x = width * (i * 0.2f)
                drawLine(
                    color = roadBorder,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 14f
                )
                drawLine(
                    color = roadColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 10f
                )
            }

            // Central Ring / Rond-point
            drawCircle(
                color = roadBorder,
                radius = 48f,
                center = center
            )
            drawCircle(
                color = Color(0xFFFEF3C7),
                radius = 36f,
                center = center
            )

            // 2. Positions calculation
            val pickupPos = Offset(width * 0.3f, height * 0.65f)
            val destPos = Offset(width * 0.75f, height * 0.3f)

            // 3. Route Polyline
            if (pickupPoint != null && destinationPoint != null) {
                val path = Path().apply {
                    moveTo(pickupPos.x, pickupPos.y)
                    // Curving through central roundabout
                    cubicTo(
                        pickupPos.x + 40f, pickupPos.y - 80f,
                        center.x - 20f, center.y + 30f,
                        center.x, center.y
                    )
                    cubicTo(
                        center.x + 20f, center.y - 30f,
                        destPos.x - 40f, destPos.y + 60f,
                        destPos.x, destPos.y
                    )
                }

                // Polyline Glow/Shadow
                drawPath(
                    path = path,
                    color = WandePrimary.copy(alpha = 0.3f),
                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                )
                // Polyline Main Line
                drawPath(
                    path = path,
                    color = WandePrimary,
                    style = Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 12f), 0f)
                    )
                )
            }

            // 4. Searching Radar Pulse
            if (isSearching) {
                drawCircle(
                    color = WandeAccent.copy(alpha = radarAlpha),
                    radius = radarRadius,
                    center = pickupPos
                )
                drawCircle(
                    color = WandePrimary.copy(alpha = (radarAlpha * 0.7f)),
                    radius = radarRadius * 1.4f,
                    center = pickupPos
                )
            }

            // 5. Pickup Marker (Green Pin)
            if (pickupPoint != null) {
                // Pin pulse shadow
                drawCircle(
                    color = WandePrimary.copy(alpha = 0.25f),
                    radius = 24f,
                    center = pickupPos
                )
                drawCircle(
                    color = WandePrimary,
                    radius = 14f,
                    center = pickupPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = pickupPos
                )
            }

            // 6. Destination Marker (Red / Amber Pin)
            if (destinationPoint != null) {
                drawCircle(
                    color = StatusError.copy(alpha = 0.25f),
                    radius = 24f,
                    center = destPos
                )
                drawCircle(
                    color = StatusError,
                    radius = 14f,
                    center = destPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = destPos
                )
            }

            // 7. Driver Marker (Moving Courier Scooter Position)
            val driverPos = if (driverLat != null && driverLng != null && pickupPoint != null && destinationPoint != null) {
                // Interpolate along route based on status
                val progress = when (deliveryStatus) {
                    DeliveryStatus.DRIVER_ASSIGNED -> 0.1f
                    DeliveryStatus.DRIVER_ARRIVING -> 0.25f
                    DeliveryStatus.PACKAGE_PICKED_UP -> 0.35f
                    DeliveryStatus.IN_TRANSIT -> 0.65f
                    DeliveryStatus.DRIVER_ARRIVED -> 0.95f
                    DeliveryStatus.DELIVERED -> 1.0f
                    else -> 0.5f
                }
                Offset(
                    pickupPos.x + (destPos.x - pickupPos.x) * progress,
                    pickupPos.y + (destPos.y - pickupPos.y) * progress + scooterBounce
                )
            } else if (deliveryStatus != null && deliveryStatus != DeliveryStatus.REQUESTED && deliveryStatus != DeliveryStatus.SEARCHING_DRIVER) {
                Offset(center.x + scooterBounce, center.y - 20f)
            } else null

            driverPos?.let { pos ->
                // Driver Aura
                drawCircle(
                    color = WandeAccent.copy(alpha = 0.35f),
                    radius = 28f,
                    center = pos
                )
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = 18f,
                    center = pos
                )
                drawCircle(
                    color = WandeAccent,
                    radius = 8f,
                    center = pos
                )
            }
        }

        // Overlay Badges on Map
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = WandePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ouagadougou, BF",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Legend / Mode indicator
        if (isSelectable) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = WandePrimaryDark.copy(alpha = 0.9f)
            ) {
                Text(
                    text = "Glissez pour ajuster la position exacte",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
