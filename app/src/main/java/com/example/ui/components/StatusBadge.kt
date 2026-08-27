package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeliveryStatus
import com.example.model.DriverVerificationStatus
import com.example.model.PayoutStatus
import com.example.ui.theme.*

@Composable
fun DeliveryStatusBadge(
    status: DeliveryStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status) {
        DeliveryStatus.REQUESTED -> Triple(Color(0xFFE2E8F0), Color(0xFF334155), Icons.Default.Pending)
        DeliveryStatus.SEARCHING_DRIVER -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.Radar)
        DeliveryStatus.DRIVER_ACCEPTED -> Triple(Color(0xFFE0E7FF), Color(0xFF4338CA), Icons.Default.CheckCircle)
        DeliveryStatus.DRIVER_COUNTER_OFFERED -> Triple(Color(0xFFFFFBEB), Color(0xFFD97706), Icons.Default.LocalOffer)
        DeliveryStatus.COUNTER_OFFER_ACCEPTED -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.ThumbUp)
        DeliveryStatus.COUNTER_OFFER_REJECTED -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Icons.Default.ThumbDown)
        DeliveryStatus.DRIVER_ASSIGNED -> Triple(Color(0xFFE0E7FF), Color(0xFF4338CA), Icons.Default.PersonPin)
        DeliveryStatus.DRIVER_ARRIVING -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), Icons.Default.TwoWheeler)
        DeliveryStatus.PACKAGE_PICKED_UP -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), Icons.Default.Inventory2)
        DeliveryStatus.DELIVERY_IN_PROGRESS,
        DeliveryStatus.IN_TRANSIT -> Triple(Color(0xFFCFFAFE), Color(0xFF0E7490), Icons.Default.Navigation)
        DeliveryStatus.DRIVER_ARRIVED -> Triple(Color(0xFFFEF9C3), Color(0xFFA16207), Icons.Default.Place)
        DeliveryStatus.DELIVERED -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.CheckCircle)
        DeliveryStatus.CANCELLED -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Icons.Default.Cancel)
        DeliveryStatus.DISPUTED -> Triple(Color(0xFFFFEDD5), Color(0xFFC2410C), Icons.Default.ReportProblem)
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = status.label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun DriverVerificationBadge(
    status: DriverVerificationStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (status) {
        DriverVerificationStatus.VERIFIED -> Quad(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.Verified, "Vérifié & Actif")
        DriverVerificationStatus.PENDING_VERIFICATION -> Quad(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.HourglassTop, "En attente de validation")
        DriverVerificationStatus.REJECTED -> Quad(Color(0xFFFEE2E2), Color(0xFFB91C1C), Icons.Default.Close, "Dossier Rejeté")
        DriverVerificationStatus.SUSPENDED -> Quad(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.Block, "Suspendu")
        DriverVerificationStatus.ACTION_REQUIRED -> Quad(Color(0xFFFFEDD5), Color(0xFFC2410C), Icons.Default.Warning, "Action requise (Photo à refaire)")
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * Trust Badge shown on Driver Profile & Client Tracking Screen.
 * Strictly shown ONLY if verificationStatus == VERIFIED
 */
@Composable
fun DriverTrustBadge(
    status: DriverVerificationStatus,
    modifier: Modifier = Modifier
) {
    if (status == DriverVerificationStatus.VERIFIED) {
        Surface(
            modifier = modifier.clip(RoundedCornerShape(20.dp)),
            color = Color(0xFFDCFCE7)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Identité vérifiée",
                    tint = Color(0xFF15803D),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "✓ Identité vérifiée",
                    color = Color(0xFF15803D),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun PayoutStatusBadge(
    status: PayoutStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        PayoutStatus.PAYOUT_PENDING -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        PayoutStatus.PAYOUT_PROCESSING -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        PayoutStatus.PAYOUT_COMPLETED -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
        PayoutStatus.PAYOUT_FAILED -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
        color = bgColor
    ) {
        Text(
            text = status.label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
