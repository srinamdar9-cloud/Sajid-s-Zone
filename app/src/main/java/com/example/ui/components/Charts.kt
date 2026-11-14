package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCoral
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusIndigo
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusSky
import com.example.ui.theme.StatusViolet
import com.example.util.FormatUtils

val ChartPalette =
  listOf(
    Color(0xFF0D9488),
    StatusAmber,
    StatusIndigo,
    StatusCoral,
    StatusSky,
    StatusViolet,
    StatusRose,
    StatusEmerald,
    Color(0xFFEA580C),
    Color(0xFF0284C7),
    Color(0xFF9333EA),
    Color(0xFF64748B),
  )

@Composable
fun CompletionRingChart(
  percentage: Float,
  label: String,
  sublabel: String,
  modifier: Modifier = Modifier,
  ringColor: Color = MaterialTheme.colorScheme.primary,
) {
  val trackColor = MaterialTheme.colorScheme.surfaceVariant
  val clamped = percentage.coerceIn(0f, 100f)

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
      Canvas(modifier = Modifier.size(96.dp)) {
        val strokeWidth = 10.dp.toPx()
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        drawArc(
          color = trackColor,
          startAngle = -90f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
          color = ringColor,
          startAngle = -90f,
          sweepAngle = 360f * (clamped / 100f),
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
      }
      Text(
        text = "${clamped.toInt()}%",
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
        fontWeight = FontWeight.Bold,
      )
    }
    Text(text = label, style = MaterialTheme.typography.labelLarge)
    Text(
      text = sublabel,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

data class BarDatum(
  val label: String,
  val primaryValue: Float,
  val secondaryValue: Float = 0f,
)

@Composable
fun DualBarChart(
  data: List<BarDatum>,
  primaryColor: Color,
  secondaryColor: Color,
  modifier: Modifier = Modifier,
) {
  val maxVal = (data.maxOfOrNull { maxOf(it.primaryValue, it.secondaryValue) } ?: 1f).coerceAtLeast(1f)
  val baselineColor = MaterialTheme.colorScheme.outlineVariant

  Column(modifier = modifier.fillMaxWidth()) {
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
      val count = data.size.coerceAtLeast(1)
      val slotWidth = size.width / count
      val barWidth = (slotWidth * 0.28f).coerceIn(6.dp.toPx(), 18.dp.toPx())
      val gap = 3.dp.toPx()
      val bottomY = size.height - 6.dp.toPx()
      val usableHeight = bottomY - 8.dp.toPx()

      drawLine(
        color = baselineColor,
        start = Offset(0f, bottomY),
        end = Offset(size.width, bottomY),
        strokeWidth = 1.dp.toPx(),
      )

      data.forEachIndexed { idx, item ->
        val centerX = idx * slotWidth + slotWidth / 2f
        val h1 = (item.primaryValue / maxVal) * usableHeight
        val h2 = (item.secondaryValue / maxVal) * usableHeight

        if (item.secondaryValue > 0f || item.primaryValue >= 0f) {
          val startX = centerX - barWidth - gap / 2f
          if (h1 > 0f) {
            drawRoundRect(
              color = primaryColor,
              topLeft = Offset(startX, bottomY - h1),
              size = Size(barWidth, h1),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
          }
          if (h2 > 0f) {
            drawRoundRect(
              color = secondaryColor,
              topLeft = Offset(centerX + gap / 2f, bottomY - h2),
              size = Size(barWidth, h2),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceAround,
    ) {
      data.forEach { datum ->
        Text(
          text = datum.label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
fun SingleSeriesBarChart(
  values: List<Pair<String, Double>>,
  barColor: Color,
  modifier: Modifier = Modifier,
  showAllLabels: Boolean = true,
) {
  val maxVal = (values.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
  val baselineColor = MaterialTheme.colorScheme.outlineVariant

  Column(modifier = modifier.fillMaxWidth()) {
    Canvas(modifier = Modifier.fillMaxWidth().height(135.dp)) {
      val count = values.size.coerceAtLeast(1)
      val slotWidth = size.width / count
      val barWidth = (slotWidth * 0.58f).coerceIn(3.dp.toPx(), 24.dp.toPx())
      val bottomY = size.height - 6.dp.toPx()
      val usableHeight = bottomY - 8.dp.toPx()

      drawLine(
        color = baselineColor,
        start = Offset(0f, bottomY),
        end = Offset(size.width, bottomY),
        strokeWidth = 1.dp.toPx(),
      )

      values.forEachIndexed { idx, (_, amount) ->
        val h = ((amount / maxVal).toFloat() * usableHeight).coerceAtLeast(if (amount > 0) 3.dp.toPx() else 0f)
        if (h > 0f) {
          val left = idx * slotWidth + (slotWidth - barWidth) / 2f
          drawRoundRect(
            color = barColor,
            topLeft = Offset(left, bottomY - h),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
          )
        }
      }
    }

    if (values.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        if (showAllLabels) {
          values.forEach { (label, _) ->
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        } else {
          Text(
            text = values.first().first,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = values[values.size / 2].first,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = values.last().first,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
fun CategoryBreakdownChart(
  categoryTotals: List<Pair<String, Double>>,
  modifier: Modifier = Modifier,
) {
  val total = categoryTotals.sumOf { it.second }.coerceAtLeast(0.01)

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Segmented proportional bar
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(14.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
      categoryTotals.forEachIndexed { idx, (_, amount) ->
        val weight = (amount / total).toFloat().coerceAtLeast(0.01f)
        Box(
          modifier =
            Modifier.weight(weight)
              .height(14.dp)
              .background(ChartPalette[idx % ChartPalette.size])
        )
      }
    }

    categoryTotals.forEachIndexed { idx, (name, amount) ->
      val color = ChartPalette[idx % ChartPalette.size]
      val pct = (amount / total) * 100.0
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
          ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = name,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = "${FormatUtils.formatInr(amount)} (${String.format("%.1f", pct)}%)",
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
          )
        }
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth((amount / total).toFloat().coerceIn(0.02f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
          )
        }
      }
    }
  }
}
