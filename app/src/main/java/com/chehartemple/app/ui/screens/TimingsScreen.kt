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
import com.chehartemple.app.data.model.TempleTiming
import kotlinx.coroutines.launch

@Composable
fun TimingsScreen() {
    var timings by remember { mutableStateOf<List<TempleTiming>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try { timings = RetrofitClient.api.getTempleTimings() } catch (_: Exception) {}
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Chehar Temple", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF800000)) }
        item { Text("Temple Timings", style = MaterialTheme.typography.titleMedium) }
        items(timings) { timing -> TimingCard(timing) }
    }
}

@Composable
private fun TimingCard(timing: TempleTiming) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(timing.day, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF800000))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Open", style = MaterialTheme.typography.bodySmall)
                    Text(timing.openTime, style = MaterialTheme.typography.titleSmall)
                }
                Column {
                    Text("Close", style = MaterialTheme.typography.bodySmall)
                    Text(timing.closeTime, style = MaterialTheme.typography.titleSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Divider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                timing.morningAartiTime?.let {
                    Column {
                        Text("Morning Aarti", style = MaterialTheme.typography.bodySmall)
                        Text(it, style = MaterialTheme.typography.titleSmall, color = Color(0xFFE8860C))
                    }
                }
                timing.eveningAartiTime?.let {
                    Column {
                        Text("Evening Aarti", style = MaterialTheme.typography.bodySmall)
                        Text(it, style = MaterialTheme.typography.titleSmall, color = Color(0xFFE8860C))
                    }
                }
            }
            timing.specialNote?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
            }
        }
    }
}
