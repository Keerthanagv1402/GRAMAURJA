package com.gramaUrja.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.gramaUrja.model.PowerHistoryEntry
import com.gramaUrja.model.PowerStatus
import com.gramaUrja.model.Zone
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor(
    private val db: FirebaseDatabase,
    private val auth: FirebaseAuth
) {

    // ── Auth ─────────────────────────────────────────────────────────────────
    suspend fun ensureAnonymousAuth(): String {
        if (auth.currentUser == null) auth.signInAnonymously().await()
        return auth.currentUser?.uid ?: "anon"
    }

    // ── Zones ─────────────────────────────────────────────────────────────────
    suspend fun fetchZones(): List<Zone> {
        ensureAnonymousAuth()
        val snapshot = db.getReference("zones").get().await()
        val zones = mutableListOf<Zone>()
        snapshot.children.forEach { child ->
            val zone = Zone(
                id       = child.key ?: "",
                name     = child.child("name").getValue(String::class.java) ?: "",
                district = child.child("district").getValue(String::class.java) ?: "",
                taluk    = child.child("taluk").getValue(String::class.java) ?: "",
                village  = child.child("village").getValue(String::class.java) ?: ""
            )
            zones.add(zone)
        }
        return zones
    }

    // ── Power Status (real-time listener as Flow) ─────────────────────────────
    fun observePowerStatus(zoneId: String): Flow<PowerStatus> = callbackFlow {
        val ref = db.getReference("zones/$zoneId/status_info")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(PowerStatus(zoneId = zoneId, status = "UNKNOWN"))
                    return
                }
                val status = PowerStatus(
                    zoneId       = zoneId,
                    status       = snapshot.child("status").getValue(String::class.java) ?: "UNKNOWN",
                    updatedAt    = snapshot.child("updated_at").getValue(Long::class.java) ?: 0L,
                    reporterId   = snapshot.child("reporter").getValue(String::class.java) ?: "",
                    confirmCount = snapshot.child("confirm_count").getValue(Int::class.java) ?: 0
                )
                trySend(status)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Update Status (Atomic Status + History) ─────────────────────────────
    suspend fun updatePowerStatus(zoneId: String, newStatus: String, reporterId: String) {
        ensureAnonymousAuth()
        val updates = mutableMapOf<String, Any?>()
        
        // 1. Update the live status info
        val statusPath = "zones/$zoneId/status_info"
        updates[statusPath] = mapOf(
            "status"        to newStatus,
            "updated_at"    to ServerValue.TIMESTAMP,
            "reporter"      to reporterId,
            "confirm_count" to 0
        )
        
        // 2. Add to history log with a unique push key
        val historyPushRef = db.getReference("zones/$zoneId/history").push()
        val historyKey = historyPushRef.key ?: return
        val historyPath = "zones/$zoneId/history/$historyKey"
        updates[historyPath] = mapOf(
            "status"    to newStatus,
            "ts"        to ServerValue.TIMESTAMP,
            "reporter"  to reporterId
        )
        
        // Execute atomic update
        db.reference.updateChildren(updates).await()
    }

    // ── Confirm Status ────────────────────────────────────────────────────────
    suspend fun confirmStatus(zoneId: String) {
        val ref = db.getReference("zones/$zoneId/status_info/confirm_count")
        ref.get().await().getValue(Int::class.java)?.let { current ->
            ref.setValue(current + 1).await()
        }
    }

    // ── History Log (Real-time Flow) ──────────────────────────────────────────
    fun observeHistory(zoneId: String, limit: Int = 30): Flow<List<PowerHistoryEntry>> = callbackFlow {
        val ref = db.getReference("zones/$zoneId/history").orderByChild("ts").limitToLast(limit)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = mutableListOf<PowerHistoryEntry>()
                snapshot.children.forEach { child ->
                    entries.add(PowerHistoryEntry(
                        id        = child.key ?: "",
                        zoneId    = zoneId,
                        status    = child.child("status").getValue(String::class.java) ?: "",
                        timestamp = child.child("ts").getValue(Long::class.java) ?: 0L,
                        reporterId = child.child("reporter").getValue(String::class.java) ?: ""
                    ))
                }
                trySend(entries.reversed())
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── FCM Topic Subscription ────────────────────────────────────────────────
    fun subscribeToZone(zoneId: String) {
        FirebaseMessaging.getInstance().subscribeToTopic("zone_$zoneId")
    }

    fun unsubscribeFromZone(zoneId: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("zone_$zoneId")
    }

    // ── Seed Mock Data (Optimized Batch Update) ──────────────────────────────
    suspend fun seedMockZones() {
        ensureAnonymousAuth()
        val zonesData = mutableMapOf<String, Any?>()
        
        val demoZones = listOf(
            mapOf("name" to "Arakalagudu Zone A", "district" to "Hassan", "taluk" to "Arakalagudu", "village" to "Arakalagudu"),
            mapOf("name" to "Belur Zone B",       "district" to "Hassan", "taluk" to "Belur",       "village" to "Belur"),
            mapOf("name" to "Channapatna Zone C", "district" to "Ramanagara", "taluk" to "Channapatna", "village" to "Channapatna"),
            mapOf("name" to "Davangere Zone A",   "district" to "Davangere", "taluk" to "Davangere", "village" to "Davangere"),
            mapOf("name" to "Hospet Zone D",      "district" to "Vijayanagara", "taluk" to "Hospet", "village" to "Hospet"),
            mapOf("name" to "Kolar Zone A",       "district" to "Kolar", "taluk" to "Kolar",         "village" to "Kolar"),
            mapOf("name" to "Mandya Zone B",      "district" to "Mandya", "taluk" to "Mandya",       "village" to "Mandya"),
            mapOf("name" to "Mysuru Zone C",      "district" to "Mysuru", "taluk" to "Mysuru",       "village" to "Mysuru"),
            mapOf("name" to "Raichur Zone A",     "district" to "Raichur", "taluk" to "Raichur",    "village" to "Raichur"),
            mapOf("name" to "Tumkur Zone B",      "district" to "Tumkur", "taluk" to "Tumkur",      "village" to "Tumkur"),
        )

        demoZones.forEachIndexed { i, zone ->
            val zoneId = "zone_${String.format(Locale.US, "%02d", i + 1)}"
            val initialStatus = if (i % 2 == 0) "ON" else "OFF"
            
            // Build the data structure carefully to avoid ancestor errors
            zonesData["zones/$zoneId/name"] = zone["name"]
            zonesData["zones/$zoneId/district"] = zone["district"]
            zonesData["zones/$zoneId/taluk"] = zone["taluk"]
            zonesData["zones/$zoneId/village"] = zone["village"]
            
            zonesData["zones/$zoneId/status_info"] = mapOf(
                "status"        to initialStatus,
                "updated_at"    to ServerValue.TIMESTAMP,
                "reporter"      to "seed",
                "confirm_count" to 0
            )
            
            zonesData["zones/$zoneId/history/initial_entry"] = mapOf(
                "status" to initialStatus,
                "ts" to ServerValue.TIMESTAMP,
                "reporter" to "seed"
            )
        }
        
        db.reference.updateChildren(zonesData).await()
    }
}
