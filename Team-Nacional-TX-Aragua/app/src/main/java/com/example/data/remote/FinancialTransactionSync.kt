package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.FinancialTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope

class FinancialTransactionSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<FinancialTransaction>(
    database = database,
    scope = scope,
    collectionName = "finances",
    entityClass = FinancialTransaction::class.java
) {
        override suspend fun dbInsert(item: FinancialTransaction) { database.financeDao().insertTransaction(item) }
    override suspend fun dbUpsertAll(items: List<FinancialTransaction>) { database.financeDao().upsertTransactions(items) }
    override suspend fun dbDelete(item: FinancialTransaction) { database.financeDao().deleteTransaction(item.id) }
    override fun dbGetAll(): Flow<List<FinancialTransaction>> = database.financeDao().getAllTransactions()
    override fun getId(item: FinancialTransaction): Long = item.id
    override fun setId(item: FinancialTransaction, id: Long): FinancialTransaction = item.copy(id = id)
    override fun getTimestamp(item: FinancialTransaction): Long = item.timestamp
}