// PATH: app/src/main/java/com/nw/parentalcontrol/data/repository/ParentRepository.kt
package com.nw.parentalcontrol.data.repository

import com.google.firebase.database.*
import com.nw.parentalcontrol.data.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

class ParentRepository {

    private val db          = FirebaseDatabase.getInstance()
    private val pairingRef  = db.getReference("pairing")
    private val devicesRef  = db.getReference("devices")
    private val commandsRef = db.getReference("commands")
    private val requestsRef = db.getReference("requests")

    suspend fun generatePairingCode(parentDeviceId: String): String {
        val code = String.format("%06d", Random.nextInt(100000, 999999))
        val data = mapOf(
            "code"           to code,
            "parentDeviceId" to parentDeviceId,
            "createdAt"      to ServerValue.TIMESTAMP,
            "expiresAt"      to (System.currentTimeMillis() + 10 * 60 * 1000L),
            "used"           to false
        )
        pairingRef.child(code).setValue(data).await()
        return code
    }

    fun listenForPairing(code: String): Flow<ChildDevice?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val used = snapshot.child("used").getValue(Boolean::class.java) ?: false
                if (used) {
                    val deviceId = snapshot.child("childDeviceId").getValue(String::class.java)
                    if (deviceId != null) {
                        devicesRef.child(deviceId).get().addOnSuccessListener { snap ->
                            trySend(snap.getValue(ChildDevice::class.java))
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        pairingRef.child(code).addValueEventListener(listener)
        awaitClose { pairingRef.child(code).removeEventListener(listener) }
    }

    suspend fun invalidatePairingCode(code: String) {
        pairingRef.child(code).removeValue().await()
    }

    fun listenToChildDevice(deviceId: String): Flow<ChildDevice?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(ChildDevice::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        devicesRef.child(deviceId).addValueEventListener(listener)
        awaitClose { devicesRef.child(deviceId).removeEventListener(listener) }
    }

    suspend fun sendCommand(childDeviceId: String, command: ControlCommand) {
        val id = UUID.randomUUID().toString()
        commandsRef.child(childDeviceId).child(id).setValue(command).await()
    }

    fun listenToRequests(childDeviceId: String): Flow<Map<String, Any>?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                @Suppress("UNCHECKED_CAST")
                trySend(snapshot.value as? Map<String, Any>)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        requestsRef.child(childDeviceId).addValueEventListener(listener)
        awaitClose { requestsRef.child(childDeviceId).removeEventListener(listener) }
    }

    suspend fun approveDisconnect(childDeviceId: String) {
        sendCommand(childDeviceId, ControlCommand(type = CommandTypes.APPROVE_DISCONNECT, value = "approved"))
        devicesRef.child(childDeviceId).child("isConnected").setValue(false).await()
        requestsRef.child(childDeviceId).child("disconnect_request").removeValue().await()
    }

    suspend fun denyDisconnect(childDeviceId: String) {
        sendCommand(childDeviceId, ControlCommand(type = CommandTypes.DENY_DISCONNECT, value = "denied"))
        requestsRef.child(childDeviceId).child("disconnect_request").removeValue().await()
    }

    suspend fun forceDisconnect(childDeviceId: String) {
        sendCommand(childDeviceId, ControlCommand(type = CommandTypes.FORCE_DISCONNECT, value = "forced"))
        devicesRef.child(childDeviceId).child("isConnected").setValue(false).await()
    }

    suspend fun approveDelete(childDeviceId: String) {
        sendCommand(childDeviceId, ControlCommand(type = CommandTypes.APPROVE_DELETE, value = "approved"))
        requestsRef.child(childDeviceId).child("delete_request").removeValue().await()
    }

    suspend fun denyDelete(childDeviceId: String) {
        sendCommand(childDeviceId, ControlCommand(type = CommandTypes.DENY_DELETE, value = "denied"))
        requestsRef.child(childDeviceId).child("delete_request").removeValue().await()
    }

    suspend fun setAppLimit(childDeviceId: String, packageName: String, limitMinutes: Int) {
        sendCommand(childDeviceId, ControlCommand(
            type  = CommandTypes.SET_APP_LIMIT,
            value = "$packageName:$limitMinutes"
        ))
    }

    suspend fun blockApp(childDeviceId: String, packageName: String) {
        sendCommand(childDeviceId, ControlCommand(type = CommandTypes.BLOCK_APP, value = packageName))
    }

    suspend fun unblockApp(childDeviceId: String, packageName: String) {
        sendCommand(childDeviceId, ControlCommand(type = CommandTypes.UNBLOCK_APP, value = packageName))
    }
}