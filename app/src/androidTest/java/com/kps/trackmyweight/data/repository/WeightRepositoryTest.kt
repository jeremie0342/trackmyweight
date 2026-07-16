package com.kps.trackmyweight.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeightRepositoryTest {

    private lateinit var db: TrackMyWeightDatabase
    private lateinit var repo: WeightRepository

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = WeightRepository(db.bodyDao())
    }

    @After fun tearDown() = db.close()

    @Test fun log_creates_entry() = runTest {
        val date = LocalDate(2026, 7, 16)
        repo.log(date, 84f)
        val row = repo.getOnDate(date)
        assertNotNull(row)
        assertEquals(84f, row!!.weightKg)
    }

    @Test fun log_same_date_replaces_previous() = runTest {
        val date = LocalDate(2026, 7, 16)
        repo.log(date, 84f)
        repo.log(date, 83.6f)
        val row = repo.getOnDate(date)
        assertEquals(83.6f, row!!.weightKg)
    }

    @Test fun observeLast_returns_most_recent() = runTest {
        repo.log(LocalDate(2026, 7, 14), 84f)
        repo.log(LocalDate(2026, 7, 15), 83.8f)
        repo.log(LocalDate(2026, 7, 16), 83.5f)
        val last = repo.observeLast().first()
        assertEquals(83.5f, last!!.weightKg)
    }

    @Test fun softDelete_removes_from_observable_lists_but_not_from_hard_table() = runTest {
        val date = LocalDate(2026, 7, 16)
        val id = repo.log(date, 84f)
        repo.softDelete(id)
        assertNull(repo.getOnDate(date)) // soft-deleted → not returned
    }
}
