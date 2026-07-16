package com.kps.trackmyweight.data.repository

import androidx.room.withTransaction
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.dao.UserDao
import com.kps.trackmyweight.data.db.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val userDao: UserDao,
    private val db: TrackMyWeightDatabase,
) {
    fun observeActive(): Flow<GoalEntity?> = userDao.observeActiveGoal()

    /**
     * Remplace l'objectif actif : désactive tous les autres et insère celui-ci en actif.
     */
    suspend fun switchActive(goal: GoalEntity): Long = db.withTransaction {
        val now = Clock.System.now()
        userDao.deactivateAllGoals(now)
        userDao.insertGoal(goal.copy(isActive = true))
    }
}
