package com.kps.trackmyweight.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.enums.PhotoAngle
import com.kps.trackmyweight.data.photo.EncryptedPhotoStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class PhotoRepositoryTest {

    private lateinit var db: TrackMyWeightDatabase
    private lateinit var repo: PhotoRepository

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val store = EncryptedPhotoStore(ctx)
        repo = PhotoRepository(db.bodyDao(), store)
    }

    @After fun tearDown() = db.close()

    @Test fun capture_persists_photo_with_encrypted_path_and_thumbnail() = runTest {
        val bytes = fakeJpegBytes()
        val entity = repo.capture(
            date = LocalDate(2026, 7, 16),
            angle = PhotoAngle.FRONT,
            bytes = bytes,
            widthPx = 100,
            heightPx = 100,
        )
        assertNotNull(entity.encryptedFilePath)
        assertNotNull(entity.thumbnailPath)
        assertTrue(entity.encryptedFilePath.endsWith(".enc"))
    }

    @Test fun capture_sets_overlayReference_to_previous_photo_of_same_angle() = runTest {
        val first = repo.capture(LocalDate(2026, 7, 1), PhotoAngle.FRONT, fakeJpegBytes(), null, null)
        val second = repo.capture(LocalDate(2026, 7, 16), PhotoAngle.FRONT, fakeJpegBytes(), null, null)
        assertEquals(first.id, second.overlayReferencePhotoId)
    }

    @Test fun capture_different_angles_do_not_reference_each_other() = runTest {
        val front = repo.capture(LocalDate(2026, 7, 1), PhotoAngle.FRONT, fakeJpegBytes(), null, null)
        val back = repo.capture(LocalDate(2026, 7, 16), PhotoAngle.BACK, fakeJpegBytes(), null, null)
        assertNull(back.overlayReferencePhotoId)
    }

    @Test fun getLastForAngle_returns_most_recent() = runTest {
        repo.capture(LocalDate(2026, 6, 1), PhotoAngle.SIDE_LEFT, fakeJpegBytes(), null, null)
        val newer = repo.capture(LocalDate(2026, 7, 16), PhotoAngle.SIDE_LEFT, fakeJpegBytes(), null, null)
        val last = repo.getLastForAngle(PhotoAngle.SIDE_LEFT)
        assertEquals(newer.id, last!!.id)
    }

    @Test fun delete_removes_from_db_and_disk() = runTest {
        val entity = repo.capture(LocalDate(2026, 7, 16), PhotoAngle.FRONT, fakeJpegBytes(), null, null)
        assertEquals(1, repo.observeAll().first().size)
        repo.delete(entity)
        assertEquals(0, repo.observeAll().first().size)
        val enc = java.io.File(entity.encryptedFilePath)
        assertTrue("encrypted file should be deleted", !enc.exists())
    }

    /** Génère un petit JPEG valide (1x1 pixel) pour les tests. */
    private fun fakeJpegBytes(): ByteArray {
        val bmp = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        return out.toByteArray()
    }
}
