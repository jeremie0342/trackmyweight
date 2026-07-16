package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.UserDao
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val userDao: UserDao,
) {
    fun observe(): Flow<UserProfileEntity?> = userDao.observeProfile()

    suspend fun current(): UserProfileEntity? = userDao.getProfile()

    suspend fun save(profile: UserProfileEntity) {
        val now = Clock.System.now()
        userDao.upsertProfile(profile.copy(updatedAt = now))
    }

    /** Est-ce que l'onboarding a été fait (au moins un profil enregistré) ? */
    suspend fun isOnboarded(): Boolean = userDao.getProfile() != null
}
