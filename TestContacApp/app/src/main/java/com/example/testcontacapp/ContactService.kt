package com.example.testcontacapp

import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.os.IBinder
import android.provider.ContactsContract

class ContactService : Service() {

    private val binder = object : IContactService.Stub() {
        override fun deleteDuplicateContacts(callback: IOperationCallback?) {
            val result = deleteDuplicates()
            when (result) {
                0 -> callback?.onOperationCompleted(0, "Duplicate contacts deleted successfully")
                1 -> callback?.onOperationCompleted(1, "No duplicate contacts found")
                else -> callback?.onOperationCompleted(2, "Error occurred while deleting duplicates")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = binder

    // 0 = success, 1 = not found, 2 = error
    private fun deleteDuplicates(): Int {
        return try {
            val resolver: ContentResolver = contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val cursor = resolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )
            val seen = mutableSetOf<String>()
            val duplicates = mutableListOf<Long>()

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val name = it.getString(1) ?: ""
                    val number = it.getString(2) ?: ""
                    val key = "$name|$number"
                    if (seen.contains(key)) {
                        duplicates.add(id)
                    } else {
                        seen.add(key)
                    }
                }
            }

            if (duplicates.isEmpty()) return 1 // not found

            for (id in duplicates) {
                resolver.delete(
                    ContactsContract.RawContacts.CONTENT_URI,
                    "${ContactsContract.RawContacts.CONTACT_ID}=?",
                    arrayOf(id.toString())
                )
            }
            0 // success
        } catch (e: Exception) {
            2 // error
        }
    }
}