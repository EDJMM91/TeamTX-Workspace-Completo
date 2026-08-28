package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.InvitationCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class InvitationCodeCleanupJob(
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    init {
        scope.launch(Dispatchers.IO) {
            runCleanupLoop()
        }
    }

    private suspend fun runCleanupLoop() {
        while (true) {
            delay(6 * 60 * 60 * 1000L) // Every 6 hours
            cleanupExpiredCodes()
        }
    }

    private suspend fun cleanupExpiredCodes() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val allCodes = database.invitationCodeDao().getAllInvitationCodes().first()
        
        val expiredCodes = allCodes.filter { !it.isMaster && it.expiresAt < now }
        for (code in expiredCodes) {
            database.invitationCodeDao().deleteInvitationCodeById(code.id)
        }
        
        // Also clean up old used non-master codes (older than 7 days)
        val oldUsedCodes = allCodes.filter { !it.isMaster && it.isUsed && (now - it.createdAt) > 7 * 24 * 60 * 60 * 1000L }
        for (code in oldUsedCodes) {
            database.invitationCodeDao().deleteInvitationCodeById(code.id)
        }
    }
}