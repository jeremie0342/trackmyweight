package com.kps.trackmyweight.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.GymEntity
import com.kps.trackmyweight.data.db.entity.GymEquipmentEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)

    // Goals
    @Query("SELECT * FROM goal WHERE isActive = 1 LIMIT 1")
    fun observeActiveGoal(): Flow<GoalEntity?>

    @Query("UPDATE goal SET isActive = 0, updatedAt = :now WHERE isActive = 1")
    suspend fun deactivateAllGoals(now: kotlinx.datetime.Instant)

    @Insert
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    // Gyms
    @Query("SELECT * FROM gym ORDER BY isDefault DESC, name ASC")
    fun observeGyms(): Flow<List<GymEntity>>

    @Query("SELECT * FROM gym WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultGym(): GymEntity?

    @Insert
    suspend fun insertGym(gym: GymEntity): Long

    @Query("UPDATE gym SET isDefault = 0")
    suspend fun clearDefaultGyms()

    @Query("UPDATE gym SET isDefault = :isDefault WHERE id = :gymId")
    suspend fun setGymDefault(gymId: Long, isDefault: Boolean)

    // Equipment
    @Query("SELECT * FROM equipment ORDER BY category, displayName")
    fun observeEquipment(): Flow<List<EquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEquipmentAll(items: List<EquipmentEntity>)

    @Query("""
        SELECT e.* FROM equipment e
        INNER JOIN gym_equipment ge ON ge.equipmentId = e.id
        WHERE ge.gymId = :gymId
        ORDER BY e.category, e.displayName
    """)
    fun observeEquipmentForGym(gymId: Long): Flow<List<EquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setGymEquipment(link: GymEquipmentEntity)

    @Query("DELETE FROM gym_equipment WHERE gymId = :gymId AND equipmentId = :equipmentId")
    suspend fun removeGymEquipment(gymId: Long, equipmentId: Long)
}
