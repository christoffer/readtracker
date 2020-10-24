package com.readtracker.android.utils

import com.readtracker.android.support.SessionPresenter
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionPresenterTest {
    @Test
    fun sessionPresenterTest_pagePresenter_format() {
        val presenter = SessionPresenter.PagePresenter(250f)
        assertEquals("125", presenter.format(0.5f))
        assertEquals("0", presenter.format(0.0f))
        assertEquals("3", presenter.format(0.01f))

        assertEquals("250", presenter.format(1000f))
        assertEquals("0", presenter.format(-1000f))
    }

    @Test
    fun sessionPresenterTest_pagePresenter_parse() {
        val presenter = SessionPresenter.PagePresenter(250f)
        val delta = 0.000001f
        assertEquals(0.5f, presenter.parse("125"), delta)
        assertEquals(0.0f, presenter.parse("0"), delta)
        assertEquals(0.1f, presenter.parse("25"), delta)
        assertEquals(0.008f, presenter.parse("2"), delta)
        assertEquals(0.012f, presenter.parse("3"), delta)
        assertEquals(1f, presenter.parse("250"), delta)

        assertEquals(-1f, presenter.parse("1000"), delta)
        assertEquals(-1f, presenter.parse("unicorn"), delta)
    }

    @Test
    fun sessionPresenterTest_percentPresenter_format() {
        val presenter = SessionPresenter.PercentPresenter()
        assertEquals("50.0", presenter.format(0.5f))
        assertEquals("0.0", presenter.format(0.0f))
        assertEquals("1.1", presenter.format(0.0111f))

        assertEquals("100.0", presenter.format(1000f))
        assertEquals("0.0", presenter.format(-1000f))
    }

    @Test
    fun sessionPresenterTest_percentPresenter_parse() {
        val presenter = SessionPresenter.PercentPresenter()
        val delta = 0.000001f

        assertEquals(0.5f, presenter.parse("50.0"), delta)
        assertEquals(0.0f, presenter.parse("0"), delta)
        assertEquals(0.25f, presenter.parse("25"), delta)
        assertEquals(0.02f, presenter.parse("2"), delta)
        assertEquals(0.03f, presenter.parse("3"), delta)

        assertEquals(-1f, presenter.parse("200"), delta)
        assertEquals(-1f, presenter.parse("unicorn"), delta)
    }
}