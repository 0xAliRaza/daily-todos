package com.aliraza.dailytodos.ui

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A back swipe seeks towards the page below; letting go short keeps the page, finishing it goes back. */
@RunWith(AndroidJUnit4::class)
class PageTransitionTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val settings = listOf(Route.TODAY, Route.SETTINGS)
    private lateinit var nav: Navigation
    private lateinit var pages: Transition<List<String>>

    private val dispatcher get() = compose.activity.onBackPressedDispatcher

    private fun show() {
        compose.setContent {
            nav = remember { Navigation(settings) }
            pages = rememberPageTransition(nav, backEnabled = true)
        }
        compose.waitForIdle()
    }

    private fun swipe(progress: Float) = compose.runOnIdle {
        dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT))
        dispatcher.dispatchOnBackProgressed(BackEventCompat(0f, 0f, progress, BackEventCompat.EDGE_LEFT))
    }

    @Test
    fun aSwipeSeeksTowardsThePageBelow() {
        show()
        swipe(0.4f)
        compose.waitForIdle()
        assertEquals(settings, pages.currentState)
        assertEquals(listOf(Route.TODAY), pages.targetState)
        assertEquals(settings, nav.stack)
    }

    @Test
    fun lettingGoShortKeepsThePage() {
        show()
        swipe(0.4f)
        compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
        compose.waitForIdle()
        assertEquals(settings, nav.stack)
        assertEquals(settings, pages.currentState)
        assertEquals(settings, pages.targetState)
    }

    @Test
    fun finishingTheSwipeGoesBack() {
        show()
        swipe(0.4f)
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.waitForIdle()
        assertEquals(listOf(Route.TODAY), nav.stack)
        assertEquals(listOf(Route.TODAY), pages.currentState)
    }

    @Test
    fun aPlainBackPressGoesBack() {
        show()
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.waitForIdle()
        assertEquals(listOf(Route.TODAY), nav.stack)
        assertEquals(listOf(Route.TODAY), pages.currentState)
    }

    /** A tab swipe that turns back past where it started heads for the other side from this page. */
    @Test
    fun aTabSwipeTurningBackSeeksFromThisPage() {
        lateinit var seek: TabSeek
        compose.setContent {
            seek = rememberTabSeek()
            nav = remember { Navigation(listOf(Route.UPCOMING)) }
            pages = rememberPageTransition(nav, backEnabled = true, seek)
        }
        compose.runOnIdle { seek.drag(Route.CALENDAR, fraction = 0.05f, edge = 0f) }
        compose.waitForIdle()
        compose.runOnIdle { seek.drag(Route.TODAY, fraction = 0.05f, edge = 0f) }
        compose.waitForIdle()
        assertEquals(listOf(Route.UPCOMING), pages.currentState)
        assertEquals(listOf(Route.TODAY), pages.targetState)
    }
}
