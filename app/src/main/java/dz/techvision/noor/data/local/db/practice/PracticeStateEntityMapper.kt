package dz.techvision.noor.data.local.db.practice

import dz.techvision.noor.domain.model.tasbih.TasbihSession

/**
 * Maps the Tasbih session row to and from the domain model.
 *
 * Both directions run through [TasbihSession.sanitized], so a value that was
 * corrupted or written by an older build can never reach the UI as a negative
 * count or a count above the target.
 */
object PracticeStateEntityMapper {

    fun toDomain(entity: TasbihSessionEntity): TasbihSession =
        TasbihSession.sanitized(
            count = entity.count,
            target = entity.target,
            totalCount = entity.totalCount,
        )

    fun toEntity(session: TasbihSession, updatedAt: Long): TasbihSessionEntity {
        val safe = TasbihSession.sanitized(session.count, session.target, session.totalCount)
        return TasbihSessionEntity(
            count = safe.count,
            target = safe.target,
            totalCount = safe.totalCount,
            updatedAt = updatedAt,
        )
    }
}

