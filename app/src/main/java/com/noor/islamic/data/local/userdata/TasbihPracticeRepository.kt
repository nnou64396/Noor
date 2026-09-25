package com.noor.islamic.data.local.userdata

import com.noor.islamic.data.local.db.practice.PracticeStateEntityMapper
import com.noor.islamic.data.local.db.practice.TasbihDao
import com.noor.islamic.domain.model.tasbih.TasbihSession
import com.noor.islamic.domain.repository.TasbihRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Local-only Tasbih session storage.
 *
 * Reads fall back to [TasbihSession.Empty] when no row exists yet, so a first
 * launch and a restored session are the same code path. There is no account, no
 * sync and no remote call anywhere in this class.
 */
class TasbihPracticeRepository(
    private val tasbihDao: TasbihDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TasbihRepository {

    override fun observeSession(): Flow<TasbihSession> =
        tasbihDao.observe().map { entity ->
            entity?.let(PracticeStateEntityMapper::toDomain) ?: TasbihSession.Empty
        }

    override suspend fun save(session: TasbihSession) = withContext(ioDispatcher) {
        tasbihDao.upsert(PracticeStateEntityMapper.toEntity(session, clock()))
    }
}
