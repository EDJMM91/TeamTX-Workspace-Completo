package com.example.data.remote

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.MemberProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class MemberSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<MemberProfile>(
    database = database,
    scope = scope,
    collectionName = "members",
    entityClass = MemberProfile::class.java
) {
    init {
        // Purgar perfiles locales duplicados al iniciar
        scope.launch(Dispatchers.IO) {
            try {
                val locales = database.memberDao().getAllMembers().first()
                purgarDuplicados(locales)
            } catch (e: Exception) {
                Log.w("MEMBER_SYNC", "Error en saneamiento inicial de duplicados: ${e.message}")
            }
        }
    }

    private suspend fun purgarDuplicados(lista: List<MemberProfile>) {
        val agrupados = lista.filter { !it.email.isNullOrBlank() }
            .groupBy { it.email!!.trim().lowercase() }
        for ((email, grupo) in agrupados) {
            if (grupo.size > 1) {
                val ordenados = grupo.sortedByDescending { it.lastActiveTimestamp }
                val masReciente = ordenados.first()
                val sobrantes = ordenados.drop(1)
                for (dup in sobrantes) {
                    Log.i("MEMBER_SYNC", "🗑️ Purgando duplicado de $email: ID ${dup.id} (conservando ${masReciente.id})")
                    database.memberDao().deleteMemberById(dup.id)
                    try {
                        db.collection("members").document(dup.id.toString()).delete()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    override suspend fun dbInsert(item: MemberProfile) { database.memberDao().insertMember(item) }

    override suspend fun dbUpsertAll(items: List<MemberProfile>) {
        // Purgar duplicados antes de insertar en base local
        val agrupados = items.filter { !it.email.isNullOrBlank() }
            .groupBy { it.email!!.trim().lowercase() }
        val idsBorrar = mutableSetOf<Long>()
        for ((email, grupo) in agrupados) {
            if (grupo.size > 1) {
                val ordenados = grupo.sortedByDescending { it.lastActiveTimestamp }
                val masReciente = ordenados.first()
                val sobrantes = ordenados.drop(1)
                for (dup in sobrantes) {
                    idsBorrar.add(dup.id)
                    try {
                        db.collection("members").document(dup.id.toString()).delete()
                    } catch (_: Exception) {}
                }
            }
        }
        val itemsLimpios = items.filter { it.id !in idsBorrar }
        database.memberDao().upsertMembers(itemsLimpios)
        for (id in idsBorrar) {
            database.memberDao().deleteMemberById(id)
        }
    }

    override suspend fun dbDelete(item: MemberProfile) { database.memberDao().deleteMember(item) }
    override suspend fun dbDeleteById(id: Long) { database.memberDao().deleteMemberById(id) }
    override fun dbGetAll(): Flow<List<MemberProfile>> = database.memberDao().getAllMembers()
    override fun getId(item: MemberProfile): Long = item.id
    override fun setId(item: MemberProfile, id: Long): MemberProfile = item.copy(id = id)
    override fun getTimestamp(item: MemberProfile): Long = item.lastActiveTimestamp
}
