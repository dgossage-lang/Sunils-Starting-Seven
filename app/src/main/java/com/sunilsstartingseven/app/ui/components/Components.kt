package com.sunilsstartingseven.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sunilsstartingseven.app.ui.theme.*

@Composable
fun PrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.heightIn(min = 52.dp), enabled = enabled, shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = Space.Large, vertical = Space.Medium)) { Text(label) }
}

@Composable
fun EmptyCard(title: String, message: String, action: String, onAction: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Medium)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            PrimaryAction(action, onAction)
        }
    }
}

@Composable
fun SoccerPitch(modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1.45f).clip(RoundedCornerShape(24.dp)).semantics { contentDescription = "Empty seven-a-side soccer pitch" }) {
        drawRect(Brand.Primary)
        for (stripe in 0..5 step 2) drawRect(Brand.Green, Offset(0f, size.height * stripe / 6), Size(size.width, size.height / 6))
        val inset = size.minDimension * .08f
        val left = inset
        val top = inset
        val w = size.width - 2 * inset
        val h = size.height - 2 * inset
        val line = Color.White.copy(alpha = .75f)
        val stroke = Stroke(2.dp.toPx())
        drawRect(line, Offset(left, top), Size(w, h), style = stroke)
        drawLine(line, Offset(size.width / 2, top), Offset(size.width / 2, top + h), stroke.width)
        drawCircle(line, h * .19f, center, style = stroke)
        drawCircle(line, 2.5.dp.toPx(), center)
        for (right in listOf(false, true)) {
            drawRect(line, Offset(if (right) left + w * .82f else left, top + h * .23f), Size(w * .18f, h * .54f), style = stroke)
            drawRect(line, Offset(if (right) left + w * .93f else left, top + h * .37f), Size(w * .07f, h * .26f), style = stroke)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PitchPreview() { StartingSevenTheme { SoccerPitch(Modifier.padding(16.dp)) } }
