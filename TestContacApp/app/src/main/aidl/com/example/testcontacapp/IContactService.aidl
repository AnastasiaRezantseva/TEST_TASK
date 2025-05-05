package com.example.testcontacapp;

import com.example.testcontacapp.IOperationCallback;

interface IContactService {
    void deleteDuplicateContacts(IOperationCallback callback);
}