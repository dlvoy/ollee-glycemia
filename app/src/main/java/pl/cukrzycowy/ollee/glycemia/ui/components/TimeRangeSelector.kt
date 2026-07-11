package pl.cukrzycowy.ollee.glycemia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.cukrzycowy.ollee.glycemia.R
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing
import java.time.LocalTime

@Composable
fun TimeRangeSelector(
    startTime: LocalTime,
    endTime: LocalTime,
    onStartTimeChange: (LocalTime) -> Unit,
    onEndTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = OlleeColors.SurfaceCard,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(OlleeSpacing.md)
    ) {
        // Title showing current range
        Text(
            text = stringResource(R.string.time_range_paused_from, formatTime(startTime), formatTime(endTime)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = OlleeSpacing.md),
            textAlign = TextAlign.Center,
            color = OlleeColors.TextSecondary,
            fontSize = 14.sp
        )

        // Time picker rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = OlleeSpacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start time picker
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.time_range_from),
                    fontSize = 12.sp,
                    color = OlleeColors.TextSecondary,
                    modifier = Modifier.padding(bottom = OlleeSpacing.xs)
                )
                TimePickerControl(
                    time = startTime,
                    onTimeChange = onStartTimeChange
                )
            }

            Spacer(modifier = Modifier.width(OlleeSpacing.md))

            // End time picker
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.time_range_to),
                    fontSize = 12.sp,
                    color = OlleeColors.TextSecondary,
                    modifier = Modifier.padding(bottom = OlleeSpacing.xs)
                )
                TimePickerControl(
                    time = endTime,
                    onTimeChange = onEndTimeChange
                )
            }
        }
    }
}

@Composable
private fun TimePickerControl(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val newHour = if (time.hour == 0) 23 else time.hour - 1
                onTimeChange(LocalTime.of(newHour, time.minute))
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = "Decrease hour",
                tint = OlleeColors.AccentPrimary
            )
        }

        Box(
            modifier = Modifier
                .width(60.dp)
                .background(
                    color = Color(0xFFF0F0F0),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(vertical = OlleeSpacing.xs, horizontal = OlleeSpacing.xs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatTime(time),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = OlleeColors.TextPrimary
            )
        }

        IconButton(
            onClick = {
                val newHour = if (time.hour == 23) 0 else time.hour + 1
                onTimeChange(LocalTime.of(newHour, time.minute))
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Increase hour",
                tint = OlleeColors.AccentPrimary
            )
        }
    }
}

private fun formatTime(time: LocalTime): String {
    val hour = time.hour.toString().padStart(2, '0')
    val minute = time.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}
