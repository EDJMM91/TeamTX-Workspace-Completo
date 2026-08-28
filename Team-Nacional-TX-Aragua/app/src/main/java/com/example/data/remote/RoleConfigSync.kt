package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.RoleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope

class RoleConfigSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<RoleConfig>(
    database = database,
    scope = scope,
    collectionName = "role_configs",
    entityClass = RoleConfig::class.java
) {
        override suspend fun dbInsert(item: RoleConfig) { database.roleConfigDao().insertRoleConfig(item) }
    override suspend fun dbUpsertAll(items: List<RoleConfig>) { database.roleConfigDao().upsertRoleConfigs(items) }
    override suspend fun dbDelete(item: RoleConfig) { }
    override fun dbGetAll(): Flow<List<RoleConfig>> = database.roleConfigDao().getAllRoleConfigs()
    override fun getId(item: RoleConfig): Long = item.roleKey.hashCode().toLong()
    override fun setId(item: RoleConfig, id: Long): RoleConfig = item // roleKey is the primary key
    override fun getTimestamp(item: RoleConfig): Long = item.lastUpdatedAt
}