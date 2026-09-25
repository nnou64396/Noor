package com.noor.islamic.domain.repository

import com.noor.islamic.domain.model.tasbih.TasbihSession
import kotlinx.coroutines.flow.Flow

/**
 * The single local Tasbih session (count, target, lifetime total).
 *
 * State lives only on the device: no account, no sync, no backend.
 */
interface TasbihRepository {

    fun observeSession(): Flow<TasbihSession>

    suspend fun save(session: TasbihSession)
}
