package ua.vn.home.bptracker.core.auth

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private val Context.authDataStore by preferencesDataStore(name = "auth")

// TODO: read / write / clear the device token via authDataStore.
class TokenStore(private val context: Context)
