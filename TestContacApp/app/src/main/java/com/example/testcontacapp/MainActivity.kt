package com.example.testcontacapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.testcontacapp.data.Contact
import com.example.testcontacapp.data.ContactRepository
import com.example.testcontacapp.ui.ContactsList
import com.example.testcontacapp.ui.theme.TestContacAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
            val repository = ContactRepository()
            var pendingPhoneToCall by remember { mutableStateOf<String?>(null) }
            val permissions = arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
            )

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val contactsGranted = permissionsMap[Manifest.permission.READ_CONTACTS] == true
                val callGranted = permissionsMap[Manifest.permission.CALL_PHONE] == true
                if (contactsGranted) {
                    contacts = repository.getContacts(context)
                }
                if (callGranted && pendingPhoneToCall != null) {
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = "tel:${pendingPhoneToCall}".toUri()
                    }
                    context.startActivity(intent)
                    pendingPhoneToCall = null
                }

            }

            LaunchedEffect(Unit) {
                val contactsPermission = Manifest.permission.READ_CONTACTS
                val callPermission = Manifest.permission.CALL_PHONE
                val contactsGranted = ContextCompat.checkSelfPermission(context, contactsPermission) == PackageManager.PERMISSION_GRANTED
                val callGranted = ContextCompat.checkSelfPermission(context, callPermission) == PackageManager.PERMISSION_GRANTED

                if (contactsGranted) {
                    contacts = repository.getContacts(context)
                }
                if (!contactsGranted || !callGranted) {
                    permissionLauncher.launch(permissions)
                }
            }

            ContactsList(
                contacts = contacts,
                onRequestCallPermission = { phone ->
                    pendingPhoneToCall = phone
                    permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                }
            )
        }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TestContacAppTheme {
        Greeting("Android")
    }
}
}