package com.example.workphotosorter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val PREFS_NAME = "WorkPhotoPrefs"
        const val KEY_IN_OFFICE = "in_office"
    }
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) return
        val geofenceTransition = geofencingEvent?.geofenceTransition
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> prefs.edit().putBoolean(KEY_IN_OFFICE, true).apply()
            Geofence.GEOFENCE_TRANSITION_EXIT -> prefs.edit().putBoolean(KEY_IN_OFFICE, false).apply()
        }
    }
}
