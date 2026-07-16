package com.kps.trackmyweight.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.enums.Sex
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

@RunWith(AndroidJUnit4::class)
class MeasurementRepositoryTest {

    private lateinit var db: TrackMyWeightDatabase
    private lateinit var repo: MeasurementRepository

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = MeasurementRepository(db.bodyDao())
    }

    @After fun tearDown() = db.close()

    @Test fun save_persists_session() = runTest {
        val date = LocalDate(2026, 7, 16)
        repo.save(
            date = date,
            neckCm = 40f, waistCm = 84f, hipCm = 95f, chestCm = 100f,
        )
        val row = repo.getOnDate(date)
        assertNotNull(row)
        assertEquals(40f, row!!.neckCm)
        assertEquals(84f, row.waistCm)
        assertEquals(95f, row.hipCm)
    }

    @Test fun save_same_date_updates_and_preserves_createdAt() = runTest {
        val date = LocalDate(2026, 7, 16)
        val id1 = repo.save(date = date, neckCm = 40f, waistCm = 84f)
        val first = repo.getOnDate(date)!!
        Thread.sleep(10)
        val id2 = repo.save(date = date, neckCm = 40f, waistCm = 82f, hipCm = 95f)
        val second = repo.getOnDate(date)!!
        assertEquals(id1, id2)
        assertEquals(82f, second.waistCm)
        assertEquals(95f, second.hipCm)
        assertEquals(first.createdAt, second.createdAt)
        assertTrue("updatedAt should have advanced", second.updatedAt > first.updatedAt)
    }

    @Test fun save_computes_and_stores_body_composition_when_sufficient_inputs() = runTest {
        val date = LocalDate(2026, 7, 16)
        repo.save(
            date = date,
            neckCm = 40f, waistCm = 84f,
            sex = Sex.MALE, heightCm = 180f, weightKg = 84f,
        )
        val comp = repo.observeLastComposition().first()
        assertNotNull(comp)
        assertTrue("body fat pct should be in typical range: ${comp!!.bodyFatPct}", comp.bodyFatPct in 10f..20f)
        assertEquals(84f, comp.leanMassKg + comp.fatMassKg, 0.1f)
    }

    @Test fun save_does_not_store_composition_when_missing_inputs() = runTest {
        val date = LocalDate(2026, 7, 16)
        repo.save(date = date, waistCm = 84f) // no neck, no sex/height/weight
        val comp = repo.observeLastComposition().first()
        assertNull(comp)
    }

    @Test fun observeAll_returns_sorted_by_date() = runTest {
        repo.save(date = LocalDate(2026, 6, 1), waistCm = 90f)
        repo.save(date = LocalDate(2026, 7, 1), waistCm = 88f)
        repo.save(date = LocalDate(2026, 5, 1), waistCm = 92f)
        val all = repo.observeAll().first()
        assertEquals(3, all.size)
        assertEquals(LocalDate(2026, 7, 1), all.first().date)
    }
}
