package com.kps.trackmyweight.data.repository

import androidx.room.withTransaction
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.dao.UserDao
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.GymEntity
import com.kps.trackmyweight.data.db.entity.GymEquipmentEntity
import com.kps.trackmyweight.data.seed.EquipmentSeed
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GymRepository @Inject constructor(
    private val userDao: UserDao,
    private val db: TrackMyWeightDatabase,
) {
    /** Idempotent : seed la table equipment si vide. À appeler à chaque cold-start. */
    suspend fun seedEquipmentIfEmpty() {
        userDao.insertEquipmentAll(EquipmentSeed.items)
    }

    fun observeAllEquipment(): Flow<List<EquipmentEntity>> = userDao.observeEquipment()

    fun observeGyms(): Flow<List<GymEntity>> = userDao.observeGyms()

    suspend fun getDefaultGym(): GymEntity? = userDao.getDefaultGym()

    /**
     * Crée une nouvelle salle par défaut (désactive les autres si `makeDefault = true`)
     * puis associe la liste d'équipements fournie.
     */
    suspend fun createGymWithEquipment(
        name: String,
        equipmentIds: Set<Long>,
        makeDefault: Boolean = true,
    ): Long = db.withTransaction {
        if (makeDefault) userDao.clearDefaultGyms()
        val gymId = userDao.insertGym(
            GymEntity(
                name = name,
                isDefault = makeDefault,
                createdAt = Clock.System.now(),
            )
        )
        equipmentIds.forEach { equipmentId ->
            userDao.setGymEquipment(
                GymEquipmentEntity(gymId = gymId, equipmentId = equipmentId)
            )
        }
        gymId
    }

    fun observeEquipmentForGym(gymId: Long): Flow<List<EquipmentEntity>> =
        userDao.observeEquipmentForGym(gymId)
}
