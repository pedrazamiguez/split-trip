package es.pedrazamiguez.splittrip.features.settlement.presentation.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import es.pedrazamiguez.splittrip.core.designsystem.foundation.ChartColors
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.MemberSpendingBarUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SpendingBarPiece
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SpilloverSegment

@Composable
internal fun AnimatedSpendingBar(
    bar: MemberSpendingBarUiModel,
    globalMaxCents: Long,
    modifier: Modifier = Modifier
) {
    var isAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(bar.userId) {
        isAnimated = true
    }

    val segmentAmounts = remember(bar.spilloverSegments) {
        calculateSegmentAmounts(bar.spilloverSegments)
    }

    val targetOwnFraction = if (isAnimated && globalMaxCents > 0) {
        (bar.ownSpendingCents.toFloat() / globalMaxCents.toFloat()).coerceAtLeast(0f)
    } else {
        0f
    }

    val bouncySpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val animatedOwnFraction by animateFloatAsState(
        targetValue = targetOwnFraction,
        animationSpec = bouncySpringSpec,
        label = "own_fraction"
    )

    val animatedSegmentFractions = ChartColors.indices.map { idx ->
        val targetFraction = if (isAnimated && globalMaxCents > 0) {
            (segmentAmounts[idx] / globalMaxCents.toFloat()).coerceAtLeast(0f)
        } else {
            0f
        }

        animateFloatAsState(
            targetValue = targetFraction,
            animationSpec = bouncySpringSpec,
            label = "segment_fraction_$idx"
        )
    }

    val surfaceHighest = MaterialTheme.colorScheme.surfaceContainerHighest

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(MaterialTheme.spacing.Medium)
            .clip(RoundedCornerShape(percent = 50))
    ) {
        drawSpendingBar(
            surfaceColor = surfaceHighest,
            ownFraction = animatedOwnFraction.coerceAtLeast(0f),
            ownColorIndex = bar.memberColorIndex,
            segmentFractions = animatedSegmentFractions
        )
    }
}

private fun calculateSegmentAmounts(segments: List<SpilloverSegment>): FloatArray {
    val amounts = FloatArray(ChartColors.size)
    segments.forEach { segment ->
        val colorIdx = segment.ownerColorIndex % ChartColors.size
        amounts[colorIdx] += segment.amountCents.toFloat()
    }
    return amounts
}

private fun DrawScope.drawSpendingBar(
    surfaceColor: Color,
    ownFraction: Float,
    ownColorIndex: Int,
    segmentFractions: List<State<Float>>
) {
    val totalWidth = size.width
    val barHeight = size.height

    drawRect(color = surfaceColor, size = Size(totalWidth, barHeight))

    val pieces = mutableListOf<SpendingBarPiece>()
    var accumulatedWidth = 0f

    val ownWidth = ownFraction * totalWidth
    if (ownWidth > 0f) {
        accumulatedWidth += ownWidth
        val ownColor = ChartColors[ownColorIndex % ChartColors.size]
        pieces.add(SpendingBarPiece(color = ownColor, accumulatedWidthPx = accumulatedWidth))
    }

    ChartColors.indices.forEach { colorIdx ->
        val segmentFraction = segmentFractions[colorIdx].value.coerceAtLeast(0f)
        val segmentWidth = segmentFraction * totalWidth
        if (segmentWidth > 0f) {
            accumulatedWidth += segmentWidth
            pieces.add(SpendingBarPiece(color = ChartColors[colorIdx], accumulatedWidthPx = accumulatedWidth))
        }
    }

    val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)

    pieces.reversed().forEach { piece ->
        drawRoundRect(
            color = piece.color,
            topLeft = Offset(0f, 0f),
            size = Size(piece.accumulatedWidthPx, barHeight),
            cornerRadius = cornerRadius
        )
    }
}
