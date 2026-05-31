package com.chehartemple.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chehartemple.app.data.api.ActivityTracker
import com.chehartemple.app.data.api.RetrofitClient
import com.chehartemple.app.data.model.ContactInfo
import com.chehartemple.app.data.model.SocialMedia
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(onLogout: () -> Unit = {}) {
    var social by remember { mutableStateOf<SocialMedia?>(null) }
    var contact by remember { mutableStateOf<ContactInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try { social = RetrofitClient.api.getSocialMedia() } catch (_: Exception) {}
            try { contact = RetrofitClient.api.getContactInfo() } catch (_: Exception) {}
            loading = false
        }
    }

    fun openUrl(url: String, platform: String = "") {
        if (url.isNotEmpty()) {
            ActivityTracker.trackAction("OPEN_SOCIAL", "More", "Opened $platform")
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scrollable header
        Text("Chehar Temple", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF800000))

        // About
        Card(
            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("About Temple", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Chehar Temple is a sacred place of worship dedicated to Chehar Maa. The temple serves as a spiritual center for devotees seeking blessings and peace.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Follow Us
        Text("Follow Us", style = MaterialTheme.typography.titleMedium)
        social?.let { s ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SocialBtn("Facebook", Color(0xFF1877F2), Modifier.weight(1f)) { openUrl(s.facebook, "Facebook") }
                SocialBtn("Instagram", Color(0xFFE4405F), Modifier.weight(1f)) { openUrl(s.instagram, "Instagram") }
                SocialBtn("YouTube", Color(0xFFFF0000), Modifier.weight(1f)) { openUrl(s.youtube, "YouTube") }
            }
        }

        // Contact
        Card(
            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Contact", style = MaterialTheme.typography.titleMedium)
                Text("📍 ${contact?.address ?: "Chehar Temple, Gujarat, India"}", style = MaterialTheme.typography.bodyMedium)
                Text("📧 ${contact?.email ?: "info@chehartemple.com"}", style = MaterialTheme.typography.bodyMedium)
                Text("📞 ${contact?.phone ?: "+91-XXXXX-XXXXX"}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.weight(1f))

        // Logout
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Logout", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SocialBtn(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() }.shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}
