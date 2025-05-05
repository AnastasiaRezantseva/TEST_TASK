package com.example.testcontacapp

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.testcontacapp.data.Contact
import com.example.testcontacapp.data.ContactRepository
import com.example.testcontacapp.ui.ContactsList
import com.example.testcontacapp.ui.theme.TestContacAppTheme

class MainActivity : ComponentActivity() {
    private var contactService: IContactService? = null
    private lateinit var serviceConnection: ServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestContacAppTheme {
                val context = LocalContext.current
                var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
                val repository = ContactRepository()
                var deleteStatus by remember { mutableStateOf<String?>(null) }
                var pendingPhoneToCall by remember { mutableStateOf<String?>(null) }
                var pendingDeleteDuplicates by remember { mutableStateOf(false) }

                // Permission launcher
                val permissions = arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.CALL_PHONE
                )
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissionsMap ->
                    val contactsGranted = permissionsMap[Manifest.permission.READ_CONTACTS] == true
                    val writeContactsGranted = permissionsMap[Manifest.permission.WRITE_CONTACTS] == true
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
                    if (writeContactsGranted && pendingDeleteDuplicates) {
                        contactService?.deleteDuplicateContacts(object : IOperationCallback.Stub() {
                            override fun onOperationCompleted(status: Int, message: String?) {
                                Handler(Looper.getMainLooper()).post {
                                    deleteStatus = message
                                    if (status == 0) {
                                        contacts = repository.getContacts(context)
                                    }
                                }
                            }
                        })
                        pendingDeleteDuplicates = false
                    }
                }

                // Service connection
                LaunchedEffect(Unit) {
                    // Permissions
                    val contactsPermission = Manifest.permission.READ_CONTACTS
                    val writeContactsPermission = Manifest.permission.WRITE_CONTACTS
                    val callPermission = Manifest.permission.CALL_PHONE
                    val contactsGranted = ContextCompat.checkSelfPermission(
                        context, contactsPermission
                    ) == PackageManager.PERMISSION_GRANTED
                    val writeContactsGranted = ContextCompat.checkSelfPermission(
                        context, writeContactsPermission
                    ) == PackageManager.PERMISSION_GRANTED
                    val callGranted = ContextCompat.checkSelfPermission(
                        context, callPermission
                    ) == PackageManager.PERMISSION_GRANTED
                    if (contactsGranted) {
                        contacts = repository.getContacts(context)
                    }
                    if (!contactsGranted || !writeContactsGranted || !callGranted) {
                        permissionLauncher.launch(permissions)
                    }

                    // Service binding
                    serviceConnection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                            contactService = IContactService.Stub.asInterface(service)
                        }
                        override fun onServiceDisconnected(name: ComponentName?) {
                            contactService = null
                        }
                    }
                    val intent = Intent(context, ContactService::class.java)
                    context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        if (::serviceConnection.isInitialized) {
                            unbindService(serviceConnection)
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    deleteStatus?.let {
                        Text(
                            text = it,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    ContactsList(
                        contacts = contacts,
                        onRequestCallPermission = { phone ->
                            pendingPhoneToCall = phone
                            permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            val writeContactsGranted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.WRITE_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (writeContactsGranted) {
                                contactService?.deleteDuplicateContacts(object : IOperationCallback.Stub() {
                                    override fun onOperationCompleted(status: Int, message: String?) {
                                        Handler(Looper.getMainLooper()).post {
                                            deleteStatus = message
                                            if (status == 0) {
                                                contacts = repository.getContacts(context)
                                            }
                                        }
                                    }
                                })
                            } else {
                                pendingDeleteDuplicates = true
                                permissionLauncher.launch(arrayOf(Manifest.permission.WRITE_CONTACTS))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete duplicate contacts")
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}
