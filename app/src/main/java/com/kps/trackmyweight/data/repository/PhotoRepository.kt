package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.BodyDao
import com.kps.trackmyweight.data.db.entity.ProgressPhotoEntity
import com.kps.trackmyweight.data.db.enums.PhotoAngle
import com.kps.trackmyweight.data.photo.EncryptedPhotoStore
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    private val bodyDao: BodyDao,
    private val store: EncryptedPhotoStore,
) {
    fun observeAll(): Flow<List<ProgressPhotoEntity>> = bodyDao.observePhotos()
    suspend fun getOnDate(date: LocalDate): List<ProgressPhotoEntity> = bodyDao.getPhotosOnDate(date)
    suspend fun getLastForAngle(angle: PhotoAngle): ProgressPhotoEntity? = bodyDao.getLastPhotoForAngle(angle)

    suspend fun capture(
        date: LocalDate,
        angle: PhotoAngle,
        bytes: ByteArray,
        widthPx: Int?,
        heightPx: Int?,
    ): ProgressPhotoEntity {
        val baseName = "${date}_${angle.name.lowercase()}_${System.currentTimeMillis()}"
        val (encPath, thumbPath) = store.save(bytes, baseName)
        val referencePhotoId = bodyDao.getLastPhotoForAngle(angle)?.id
        val id = bodyDao.insertPhoto(
            ProgressPhotoEntity(
                date = date,
                angle = angle,
                encryptedFilePath = encPath,
                thumbnailPath = thumbPath,
                overlayReferencePhotoId = referencePhotoId,
                widthPx = widthPx,
                heightPx = heightPx,
                createdAt = Clock.System.now(),
            )
        )
        return bodyDao.getPhotosOnDate(date).first { it.id == id }
    }

    suspend fun delete(photo: ProgressPhotoEntity) {
        store.delete(photo.encryptedFilePath, photo.thumbnailPath)
        bodyDao.deletePhoto(photo.id)
    }
}
