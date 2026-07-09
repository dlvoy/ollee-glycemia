package com.arthur.bgollee

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GlycemiaHistoryStoreTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        GlycemiaHistoryStore.clear(context)
    }

    @Test
    fun append_andReadRecentEntries_persistsValues() {
        val now = System.currentTimeMillis()

        GlycemiaHistoryStore.append(context, GlycemiaHistoryEntry(now - 10_000L, 101, 1.5))
        GlycemiaHistoryStore.append(context, GlycemiaHistoryEntry(now, 110, -0.5))

        val entries = GlycemiaHistoryStore.getRecentEntries(context, 60_000L)

        assertEquals(2, entries.size)
        assertEquals(101, entries.first().valueMgDl)
        assertEquals(110, entries.last().valueMgDl)
    }

    @Test
    fun append_trimsEntriesOlderThan24Hours() {
        val now = System.currentTimeMillis()

        GlycemiaHistoryStore.append(context, GlycemiaHistoryEntry(now - 25L * 60L * 60L * 1000L, 90, 0.0))
        GlycemiaHistoryStore.append(context, GlycemiaHistoryEntry(now, 120, 2.0))

        val entries = GlycemiaHistoryStore.getRecentEntries(context, 48L * 60L * 60L * 1000L)

        assertEquals(1, entries.size)
        assertEquals(120, entries.single().valueMgDl)
    }

    @Test
    fun clear_removesHistory() {
        GlycemiaHistoryStore.append(context, GlycemiaHistoryEntry(System.currentTimeMillis(), 100, 0.0))

        GlycemiaHistoryStore.clear(context)

        val entries = GlycemiaHistoryStore.getRecentEntries(context, 60_000L)
        assertTrue(entries.isEmpty())
    }
}