package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.EquipmentLoan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope

class EquipmentLoanSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<EquipmentLoan>(
    database = database,
    scope = scope,
    collectionName = "equipment_loans",
    entityClass = EquipmentLoan::class.java
) {
        override suspend fun dbInsert(item: EquipmentLoan) { database.inventoryDao().insertLoan(item) }
    override suspend fun dbUpsertAll(items: List<EquipmentLoan>) { database.inventoryDao().upsertLoans(items) }
    override suspend fun dbDelete(item: EquipmentLoan) { }
    override fun dbGetAll(): Flow<List<EquipmentLoan>> = database.inventoryDao().getAllLoans()
    override fun getId(item: EquipmentLoan): Long = item.id
    override fun setId(item: EquipmentLoan, id: Long): EquipmentLoan = item.copy(id = id)
    override fun getTimestamp(item: EquipmentLoan): Long = item.loanTimestamp
}