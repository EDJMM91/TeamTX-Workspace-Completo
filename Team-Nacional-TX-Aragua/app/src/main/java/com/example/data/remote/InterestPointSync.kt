package com.example.data.remote

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.BikerInterestPoint
import com.example.util.SanitizadorImagenUrl
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class InterestPointSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<BikerInterestPoint>(
    database = database,
    scope = scope,
    collectionName = "sitios_interes",
    entityClass = BikerInterestPoint::class.java
) {
    override suspend fun dbInsert(item: BikerInterestPoint) { database.interestPointDao().insertInterestPoint(item) }
    override suspend fun dbUpsertAll(items: List<BikerInterestPoint>) { database.interestPointDao().upsertInterestPoints(items) }
    override suspend fun dbDelete(item: BikerInterestPoint) { database.interestPointDao().deleteInterestPointById(item.id) }
    override suspend fun dbDeleteById(id: Long) { database.interestPointDao().deleteInterestPointById(id) }
    override fun dbGetAll(): Flow<List<BikerInterestPoint>> = database.interestPointDao().getAllInterestPoints()
    override fun getId(item: BikerInterestPoint): Long = item.id
    override fun setId(item: BikerInterestPoint, id: Long): BikerInterestPoint = item.copy(id = id)
    override fun getTimestamp(item: BikerInterestPoint): Long = item.timestamp

    override fun enriquecerCamposImagenes(doc: DocumentSnapshot, item: BikerInterestPoint): BikerInterestPoint {
        val rawImg = (item.imageUrl ?: "").ifBlank { null }
            ?: doc.getString("imageUrl")
            ?: doc.getString("image_url")
            ?: doc.getString("foto_url")
            ?: doc.getString("url")
        val effectiveUrl = SanitizadorImagenUrl.obtenerUrlEfectiva(rawImg)
        return if (effectiveUrl != item.imageUrl) item.copy(imageUrl = effectiveUrl) else item
    }

    init {
        scope.launch(Dispatchers.IO) {
            verificarYSembrarSitios()
        }
    }

    private suspend fun verificarYSembrarSitios() {
        val sitiosIniciales = listOf(
            BikerInterestPoint(
                id = 9001L,
                name = "Mirador de Choroní (Vía Henri Pittier)",
                category = "Mirador / Parador Biker",
                description = "Vista panorámica espectacular de la selva nublada del Parque Nacional Henri Pittier. Parada obligatoria de la caravana Aragua.",
                address = "Carretera Maracay - Choroní, Aragua",
                latitude = 10.3541,
                longitude = -67.6102,
                iconDrawableName = "ic_menu_compass",
                phone = "+58 412 0000000",
                addedBy = "Directiva Nacional TX Aragua",
                likesCount = 24,
                dislikesCount = 0
            ),
            BikerInterestPoint(
                id = 9002L,
                name = "Colonia Tovar - Parador Biker El Arco",
                category = "Destino / Parador Biker",
                description = "Clima de montaña, gastronomía alemana y punto de reunión de moteros de toda Venezuela.",
                address = "Entrada Colonia Tovar, Aragua",
                latitude = 10.4078,
                longitude = -67.2911,
                iconDrawableName = "ic_menu_compass",
                phone = "+58 412 1111111",
                addedBy = "Directiva Nacional TX Aragua",
                likesCount = 38,
                dislikesCount = 1
            ),
            BikerInterestPoint(
                id = 9003L,
                name = "Paso Transandino (Águila - Mérida)",
                category = "Montaña / Ruta",
                description = "Punto más alto de la red vial nacional a 4.118 msnm. Sello de honor para los aventureros TX.",
                address = "Paso del Cóndor, Estado Mérida",
                latitude = 8.8524,
                longitude = -70.8312,
                iconDrawableName = "ic_menu_compass",
                phone = "+58 412 2222222",
                addedBy = "Directiva Nacional TX Aragua",
                likesCount = 45,
                dislikesCount = 0
            ),
            BikerInterestPoint(
                id = 9004L,
                name = "Playa Grande - Puerto Colombia",
                category = "Playa / Costa",
                description = "Mar Caribe, ambiente festivo y parador turístico de costa para la caravana biker.",
                address = "Choroní, Estado Aragua",
                latitude = 10.5050,
                longitude = -67.6067,
                iconDrawableName = "ic_menu_compass",
                phone = "+58 412 3333333",
                addedBy = "Directiva Nacional TX Aragua",
                likesCount = 30,
                dislikesCount = 0
            )
        )

        try {
            val prefs = com.example.TeamTxApplication.instance?.getSharedPreferences("prefs_radar_tx", android.content.Context.MODE_PRIVATE)
            val yaSembrado = prefs?.getBoolean("sitios_interes_sembrados_v2", false) ?: false

            if (!yaSembrado) {
                database.interestPointDao().upsertInterestPoints(sitiosIniciales)

                val snapshot = db.collection(collectionName).get().await()
                val existingIds = snapshot.documents.mapNotNull { it.id.toLongOrNull() ?: it.getLong("id") }.toSet()
                for (item in sitiosIniciales) {
                    if (item.id !in existingIds) {
                        insertOrUpdate(item)
                    }
                }
                prefs?.let { p ->
                    val editor = p.edit()
                    editor.putBoolean("sitios_interes_sembrados_v2", true)
                    editor.apply()
                }
                Log.d("FIREBASE_SYNC", "🌱 Siembra inicial de sitios de interés completada.")
            } else {
                val snapshot = db.collection(collectionName).get().await()
                val remotos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BikerInterestPoint::class.java)?.let { enriquecerCamposImagenes(doc, it) }
                }
                if (remotos.isNotEmpty()) {
                    database.interestPointDao().upsertInterestPoints(remotos)
                }
            }
        } catch (e: Exception) {
            Log.w("FIREBASE_SYNC", "Aviso en sync de sitios de interés: ${e.message}")
        }
    }
}
