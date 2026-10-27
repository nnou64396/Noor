package dz.techvision.noor

import dz.techvision.noor.data.local.db.practice.AthkarDhikrProgressEntity
import dz.techvision.noor.data.local.db.practice.AthkarProgressDao
import dz.techvision.noor.data.local.db.practice.TasbihDao
import dz.techvision.noor.data.local.db.practice.TasbihSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory stand-ins for the Phase 5 tables, so the screens can be driven on a
 * device without creating a real practice-state database.
 */
class RecordingAthkarProgressDao : AthkarProgressDao {

    val rows = MutableStateFlow<List<AthkarDhikrProgressEntity>>(emptyList())

    override fun observeAll(): Flow<List<AthkarDhikrProgressEntity>> = rows

    override suspend fun upsert(entity: AthkarDhikrProgressEntity) {
        rows.value = rows.value.filterNot { it.dhikrId == entity.dhikrId } + entity
    }

    override suspend fun deleteByDhikrId(dhikrId: String) {
        rows.value = rows.value.filterNot { it.dhikrId == dhikrId }
    }

    override suspend fun deleteByCategory(categoryId: String) {
        rows.value = rows.value.filterNot { it.categoryId == categoryId }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}

class InMemoryTasbihDao : TasbihDao {

    val row = MutableStateFlow<TasbihSessionEntity?>(null)

    override fun observe(id: Int): Flow<TasbihSessionEntity?> = row

    override suspend fun upsert(entity: TasbihSessionEntity) {
        row.value = entity
    }

    override suspend fun deleteAll() {
        row.value = null
    }
}

