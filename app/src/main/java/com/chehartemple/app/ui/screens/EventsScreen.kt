package com.chehartemple.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chehartemple.app.data.api.RetrofitClient
import com.chehartemple.app.data.model.Event
import com.chehartemple.app.data.model.LimitedEvents
import kotlinx.coroutines.launch

@Composable
fun EventsScreen() {
    var data by remember { mutableStateOf(LimitedEvents()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try { data = RetrofitClient.api.getLimitedEvents() } catch (_: Exception) {}
            loading = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scrollable header
        item { Text("Chehar Temple", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF800000)) }

        if (data.today.isNotEmpty()) {
            item { Text("Today", style = MaterialTheme.typography.titleMedium, color = Color(0xFFE8860C)) }
            items(data.today) { event -> EventItem(event) }
            item { Spacer(Modifier.height(8.dp)) }
        }

        item { Text("Upcoming", style = MaterialTheme.typography.titleMedium) }
        if (data.upcoming.isEmpty()) {
            item { Text("No upcoming events", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(data.upcoming) { event -> EventItem(event) }
        }
    }
}

@Composable
private fun EventItem(event: Event) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Orange date indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp).background(Color(0xFFFFF8E8), RoundedCornerShape(8.dp)).padding(8.dp)
            ) {
                Text(event.eventDate?.takeLast(2) ?: "--", style = MaterialTheme.typography.titleMedium, color = Color(0xFF800000), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                if (event.allDayEvent) {
                    Text("All Day", style = MaterialTheme.typography.bodySmall)
                } else if (!event.startTime.isNullOrEmpty()) {
                    Text("${event.startTime} - ${event.endTime ?: ""}", style = MaterialTheme.typography.bodySmall)
                }
                event.description?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}
