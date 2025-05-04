package com.example.testcontacapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.example.testcontacapp.data.Contact

@Composable
fun ContactsList(contacts: List<Contact>,onRequestCallPermission: (String) -> Unit) {
    val context = LocalContext.current
    if (contacts.isEmpty()) {
        Text("No contacts")
    } else {
        LazyColumn {
            contacts
                .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
                .toSortedMap()
                .forEach { (letter, group) ->
                    item {
                        Column {
                            HorizontalDivider(thickness = 2.dp, color = Color.Blue)
                            // Letter of group
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                            HorizontalDivider(thickness = 2.dp, color = Color.Blue)
                        }
                    }
                    items(group) { contact ->
                        // Card of contact with padding
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .fillMaxWidth()
                                .border(2.dp, Color.Blue, shape = MaterialTheme.shapes.medium)
                                .background(Color.White, shape = MaterialTheme.shapes.medium)
                                .clickable {
                                    contact.phone?.let { phone ->
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CALL_PHONE
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            val intent = Intent(Intent.ACTION_CALL).apply {
                                                data = "tel:$phone".toUri()
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            onRequestCallPermission(phone)
                                        }
                                    }
                                }
                             )
                        {
                            ContactListItem(contact)
                        }
                    }
                }
        }
    }
}

fun getPhoneTypeLabel(type: String?): String {
    return when (type?.toIntOrNull()) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "mobile"
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "home"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "work"
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "main"
        else -> ""
    }
}

@Composable
fun ContactListItem(contact: Contact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        if (contact.photoUri != null) {
            Image(
                painter = rememberAsyncImagePainter(contact.photoUri),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray, CircleShape)
            )
        } else {

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1976D2))
            ) {
                val initials = contact.name.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString()?.uppercase() }
                    .take(2)
                    .joinToString("")
                Text(
                    text = initials,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = contact.name, style = MaterialTheme.typography.bodyLarge)
            Row {
                contact.phone?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = getPhoneTypeLabel(contact.phoneType),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}