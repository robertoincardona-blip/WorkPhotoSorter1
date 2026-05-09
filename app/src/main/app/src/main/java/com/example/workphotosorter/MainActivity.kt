package com.example.workphotosorter

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.workphotosorter.databinding.ActivityMainBinding
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var geofencingClient: GeofencingClient
    private val OFFICE_LAT = 44.051852
    private val OFFICE_LNG = 8.215251
    private val GEOFENCE_RADIUS = 150f
    private val GEOFENCE_ID = "OFFICE_ZONE"

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.all { it }) { setupGeofence(); startService() }
        else { Toast.makeText(this, "Servono tutti i permessi", Toast.LENGTH_LONG).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geofencingClient = LocationServices.getGeofencingClient(this)
        binding.btnStart.setOnClickListener { checkPermissionsAndStart() }
        binding.btnStop.setOnClickListener { stopService() }
        updateOfficeStatus()
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 29) { permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    @SuppressLint("MissingPermission")
    private fun setupGeofence() {
        val geofence = Geofence.Builder().setRequestId(GEOFENCE_ID).setCircularRegion(OFFICE_LAT, OFFICE_LNG, GEOFENCE_RADIUS).setExpirationDuration(Geofence.NEVER_EXPIRE).setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT).build()
        val geofencingRequest = GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofence(geofence).build()
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
    }

    private fun updateOfficeStatus() {
        val prefs = getSharedPreferences(GeofenceBroadcastReceiver.PREFS_NAME, MODE_PRIVATE)
        val inOffice = prefs.getBoolean(GeofenceBroadcastReceiver.KEY_IN_OFFICE, false)
        binding.officeStatus.text = if (inOffice) "Sei in ufficio" else "Sei fuori ufficio"
    }

    private fun startService() {
        ContextCompat.startForegroundService(this, Intent(this, PhotoObserverService::class.java))
        binding.statusText.text = "Attivo"; updateOfficeStatus()
    }

    private fun stopService() {
        stopService(Intent(this, PhotoObserverService::class.java))
        binding.statusText.text = "Fermo"
    }
}
